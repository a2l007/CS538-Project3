package edu.indiana.p538;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

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


    public Proxy(int port. ProxyWorker worker) throws IOException{
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
                this.selector.select();
                Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
                while(keys.hasNext()){
                    SelectionKey key = keys.next();
                    keys.remove();

                    if(key.isValid()){
                        if(key.isAcceptable()){
                            //accept key
                            this.accept(key);
                        }else if(key.isReadable()){
                            //read the key
                            this.read(key);
                        }else if(key.isWritable()){
                            //write to the key
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
        Socket socket = sockCh.socket();
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

        //hand to worker thread
        this.worker.processData(this, sockCh, this.readBuf.array(), numRead);
    }

    public void send(ConnInfo connInfo, byte[] data){
        //TODO: IMPLEMENT
        //add it to the buffer queue, send on as we can
    }

    public void establishConn(ConnInfo msgInfo, byte[] data){
        //TODO: IMPLEMENT
        //add to event queue; create connection as possible
    }

    public void sendFin(ConnInfo connInfo, int reason){
        //TODO: IMPLEMENT
        //add to event queue; end connection as possible
    }
}
