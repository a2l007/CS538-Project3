package edu.indiana.p538;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ladyl on 11/19/2016.
 */
public class Proxy implements Runnable {

    /* FIELDS */
    private int port = AppConstants.PROXY_PORT;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ProxyWorker worker;

    private ByteBuffer readBuf = ByteBuffer.allocate(8208); //buffer equal to 2 pipe messages; can adjust as necessary


    private BlockingQueue<ProxyEvents> pendingEvents = new ArrayBlockingQueue<>(50);
    private ConcurrentHashMap<InetSocketAddress,SocketChannel> connectionChannelMap = new ConcurrentHashMap<>();
    //this map is to map connection IDs with the list of data
    private ConcurrentHashMap<Integer,ArrayList<byte[]>> outOfOrder = new ConcurrentHashMap<>();

    public Proxy(int port, ProxyWorker worker) throws IOException{
        this.port = port;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.worker = worker;

        //initiate the selector
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();

        //this says we're looking for new connections to the given port
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        while(true){
            try {
                //in order to do this we need to figure out how to find specific sockets
                //either we are going to be iterating over all the sockets as well as the
                //events, or we are going to have to find the correct method to use to find
                //a specific socket.

                //it would be great if we could ID a socket by IP/port pair with the selector
                //I don't have internet right now but that should be the next step to figuring this out.
                Iterator<ProxyEvents> iter = this.pendingEvents.iterator();
                while(iter.hasNext()){
                    ProxyEvents event = iter.next();
                    this.pendingEvents.remove(event);
                    switch (event.getType()){
                        case ProxyEvents.WRITING:
                            SocketChannel connectChannel=this.connectionChannelMap.get(event.getConnInfo());
                            SelectionKey key = connectChannel.keyFor(this.selector);
                            key.interestOps(event.getOps());
                            break;
                        case ProxyEvents.CONNECTING:
                            connectChannel=this.connectionChannelMap.get(event.getConnInfo());
                            //Need to double check the register call.
                            //I'm attaching the connectionID with this socket for now.
                            // We might to make this an arraylist of connectionIDs soon
                            connectChannel.register(this.selector,event.getOps(),event.getConnId());

                    }
                }

                this.selector.select();
                Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
                while(keys.hasNext()){
                    SelectionKey key = keys.next();
                    keys.remove();
                    if(key.isValid()){
                        if(key.isConnectable()){
                            this.completeConnection(key);
                        }
                        if(key.isAcceptable()){
                            //accept key
                            this.accept(key);
                        }else if(key.isReadable()){
                            //read the key
                            this.read(key);
                        }else if(key.isWritable()){
                            //write to the key
                            this.write(key);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void accept(SelectionKey key) throws IOException{
        ServerSocketChannel servCh = (ServerSocketChannel) key.channel();

        SocketChannel sockCh = servCh.accept();
        sockCh.configureBlocking(false);
        //tells the selector we want to know when data is available to be read
        sockCh.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException{
        SocketChannel sockCh = (SocketChannel) key.channel();
        //clear the buffer. if we've reached this point again we've already passed data on
        this.readBuf.clear();

        int numRead;
        try{
            numRead = sockCh.read(this.readBuf);
            key.cancel();
            sockCh.close();
        }catch (IOException e){
            //entering here means the remote has forced the connection closed
            key.cancel();
            sockCh.close();
            return;
        }

        if(numRead == -1){
            //socket shut down cleanly. cancel channel
            key.channel().close();
            key.cancel();
            return;
        }

        // Since we're not attaching anything to the LP socket channel, attachment would be empty
        //hand to worker thread only if the read is called from the LP socket
        if(key.attachment()==null) {
            this.worker.processData(this, sockCh, this.readBuf.array(), numRead);
        }
        else{
            //Just a dummy print statement for now to view the data
            System.out.println(new String(this.readBuf.array()));
            key.interestOps(SelectionKey.OP_WRITE);

        }
    }

    protected  void send(InetSocketAddress connInfo, byte[] data,int connId){
        //TODO: IMPLEMENT
        //add it to the buffer queue, send on as we can
        //NOPE WE DO NOT NEED THE SOCKET STOP THINKING WE DO JEEZ.
        //SocketChannel connChannel=this.connectionChannelMap.get(connInfo);
        //Null check needed
        //TODO: Add data to a list and then add to hashmap. Need to keep track of data sequence as well.
        //Need to read data into buffer here and raise ProxyDataEvent
        this.pendingEvents.add(new ProxyEvents(connInfo, data, connId, ProxyEvents.WRITING,SelectionKey.OP_WRITE));
        //Pull the data based on the connection ID
        if(outOfOrder.containsKey(connId)){
            ArrayList<byte[]> dataList=outOfOrder.get(connId);
            dataList.add(data);
            outOfOrder.put(connId,dataList);
        }
        else{
            ArrayList<byte[]> dataList=new ArrayList<>(20);
            dataList.add(data);
            outOfOrder.put(connId,dataList);
        }
       // this.selector.wakeup();
    }

    protected void establishConn(InetSocketAddress msgInfo, byte[] data, int connId){
        //TODO: IMPLEMENT
        //add to event queue; create connection as possible
        try {
            SocketChannel serverChannel = SocketChannel.open();
            serverChannel.configureBlocking(false);
            // Kick off connection establishment
            //I've temporarily added port as the key to this map. Should we think of making this the connectionID?
            connectionChannelMap.put(msgInfo,serverChannel);
            //OP_CONNECT is getting masked by the call from ProxyWorker. Safe to listen to OP_WRITE here
            this.pendingEvents.add(new ProxyEvents(msgInfo, data, connId,ProxyEvents.CONNECTING,SelectionKey.OP_CONNECT));
            //serverChannel.connect(msgInfo);
            //No point in waking up here as there may not be enough data to write into the channel
            //this.selector.wakeup();
            serverChannel.connect(msgInfo);

        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    protected void sendFin(InetSocketAddress connInfo, int reason){
        //TODO: IMPLEMENT
        //add to event queue; end connection as possible
        //how to close????? Yeah how to?
        //this.pendingEvents.add(new ProxyEvents(connInfo, new byte[0], SelectionKey.OP_CONNECT,ProxyEvents.ENDING));

        this.selector.wakeup();
    }
    private void completeConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        //Complete connecting. This would return true if the connection is successful
        //socketChannel.configureBlocking(false);
        try {
            System.out.println("Connected to Server");
            socketChannel.finishConnect();
        } catch (IOException e) {
            e.printStackTrace();
            key.cancel();
            return;
        }

        //Since connection is established, show interest in writing data to the server
      //  key.interestOps(SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) throws IOException{
        SocketChannel sockCh = (SocketChannel) key.channel();
        int connId=(int)key.attachment();
        if(outOfOrder.containsKey(connId)) {
            ArrayList<byte[]> dataList = outOfOrder.get(connId);
            while (!dataList.isEmpty()) {
                ByteBuffer buf = ByteBuffer.wrap(dataList.get(0));
                int x = sockCh.write(buf);
                if (buf.remaining() > 0) {
                    break;
                }
                dataList.remove(0);
            }
            if (dataList.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }
}
