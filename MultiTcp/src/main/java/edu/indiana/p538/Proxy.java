package edu.indiana.p538;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by ladyl on 11/19/2016.
 */
public class Proxy implements Runnable {

    /* FIELDS */
    private int port = AppConstants.PROXY_PORT;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ProxyWorker worker;
    private int expectedSequenceNumber=0;
    private ByteBuffer readBuf = ByteBuffer.allocate(8208); //buffer equal to 2 pipe messages; can adjust as necessary
    private int pipes;
    //TODO: Modify the following channel variable
    private ArrayList<SocketChannel> clientChannel;

    private BlockingQueue<ProxyEvents> pendingEvents = new ArrayBlockingQueue<>(100);
    private ConcurrentHashMap<Integer,SocketChannel> connectionChannelMap = new ConcurrentHashMap<>();
    //this map is to map connection IDs with the list of data
    //Renamed this map to avoid confusion
    private ConcurrentHashMap<Integer,Integer> expectedSequenceList=new ConcurrentHashMap<>();
    //this hashmap is of the form connID->{seqId->data}
    private ConcurrentHashMap<Integer,ConcurrentHashMap<Integer,byte[]>> connectionDataList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer,ArrayList<byte[]>> responseDataList = new ConcurrentHashMap<>();
    protected final ReentrantLock selectorLock = new ReentrantLock();
    public Proxy(int port, ProxyWorker worker, int numPipes) throws IOException{
        this.port = port;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.worker = worker;
        //initiate the selector
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.pipes=numPipes;
        this.clientChannel=new ArrayList<>(pipes);
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
                    //System.out.prinln.println("Removed event "+event.getConnId());
                    switch (event.getType()){
                        case ProxyEvents.WRITING:
                            //System.out.prinln.println("INSIDE WRITE EVENT");
                            SocketChannel connectChannel=this.connectionChannelMap.get(event.getConnId());
                            if(connectChannel!=null) {
                                SelectionKey key = connectChannel.keyFor(this.selector);
                                key.interestOps(event.getOps());
                            }
                            break;
                        case ProxyEvents.CONNECTING:
                            connectChannel=this.connectionChannelMap.get(event.getConnId());
                            //Need to double check the register call.
                            //I'm attaching the connectionID with this socket for now.
                            // We might to make this an arraylist of connectionIDs soon
                            //System.out.prinln.println("Registering server channel with connID "+ event.getConnId());
                            connectChannel.register(this.selector,event.getOps(),event.getConnId());
                            break;
                        case ProxyEvents.ENDING:
                            ////System.out.prinln.println("Inside ending");
                            connectChannel=this.connectionChannelMap.get(event.getConnId());
                             //connectChannel.register(this.selector, event.getOps(), event.getConnId());
                            if(responseDataList.containsKey(event.getConnId())){
                                ////System.out.prinln.println("Ending event for connID"+event.getConnId());
                            }
                            this.responseDataList.remove(event.getConnId());
                            //System.out.prinln.println("Removed connID"+event.getConnId());
                            SelectionKey endKey = connectChannel.keyFor(this.selector);
                            System.out.println("Close connection "+event.getConnId());
                            expectedSequenceNumber=0;
                            connectChannel.close();
                            //endKey.cancel();
                            break;
                        default: break;
                    }
                }
                selectorLock.lock();
                selectorLock.unlock();
                this.selector.select();
                Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();

                while(keys.hasNext()){
                    SelectionKey key = keys.next();
                    int conn=0;
                    if(key.attachment()!=null){
                        conn=(int)key.attachment();
                    }
                    keys.remove();
                    if(key.isValid()){
                        if(key.isConnectable()){
                            //System.out.prinln.println("Key processed with operation connection"+ conn);
                            this.completeConnection(key);
                        }
                        if(key.isAcceptable()){
                            //accept key
                            //System.out.prinln.println("Key processed with operation accept"+ conn);
                            this.accept(key);
                        }else if(key.isReadable()){
                            //read the key
                            //System.out.prinln.println("Key processed with operation read"+ conn);
                            this.read(key);

                        }else if(key.isWritable()){
                            //write to the key
                            //System.out.prinln.println("Key processed with operation write"+ conn);
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
        System.out.println("Accepted connection");
        clientChannel.add(sockCh);
        //tells the selector we want to know when data is available to be read
        //System.out.prinln.println("---------Registering interest in OP_READ for LP channel");
        sockCh.register(this.selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException{
        SocketChannel sockCh = (SocketChannel) key.channel();
        //clear the buffer. if we've reached this point again we've already passed data on
        this.readBuf.clear();
        ////System.out.prinln.println("Inside rread");
        //TODO DEBUG
        if(key.attachment()==null) {
        //    ////System.out.prinln.println("This is the LP socket being read");
        }
        int numRead;
        try{
            numRead = sockCh.read(this.readBuf);
        }catch (IOException e) {
            //entering here means the remote has forced the connection closed
            key.cancel();
            sockCh.close();
            return;
        }

        if(numRead == -1) {
            //socket shut down cleanly. cancel channel
            //System.out.prinln.println("Closed socket");

          //TODO DEBUG
            key.channel().close();
            key.cancel();
          //  return;
           // key.interestOps(SelectionKey.OP_WRITE);
            return;
        }

        // Since we're not attaching anything to the LP socket channel, attachment would be empty
        //hand to worker thread only if the read is called from the LP socket
        if(key.attachment()==null) {
            System.out.println("Process data read from LP");
            this.worker.processData(this, sockCh, this.readBuf.array(), numRead);
            //key.interestOps(SelectionKey.OP_READ);

        }
        else{
            ////System.out.prinln.write(this.readBuf.array());
            //Just a dummy print statement for now to view the data
            ////System.out.prinln.write(this.readBuf.array());
            //TODO DEBUG
            //key.interestOps(SelectionKey.OP_WRITE);
            //TODO XXY DEBUG

//            SelectionKey lpSocketKey = this.clientChannel.keyFor(this.selector);
//            lpSocketKey.interestOps(SelectionKey.OP_WRITE);
            //TODO create the data message which needs to be pushed back to the LP socket
            int connectionId=(int)key.attachment();
            System.out.println("Process data read from server for connection: "+connectionId);

            //////System.out.prinln.println("Conn id is"+connectionId);
            if(this.responseDataList.containsKey(connectionId)){
                ArrayList<byte[]> dataMessages=this.responseDataList.get(connectionId);
                //We could either keep track of the sequence number by checking the size of the dataMessages arraylist
                //Or we would need a new hashmap that keeps track of the current sequence number for each connectionID
                //System.out.prinln.println("Sequence number for packet generation:"+expectedSequenceNumber);
                byte[] dataMsg=PacketAnalyzer.generateDataMessage(readBuf,connectionId,expectedSequenceNumber,numRead);
                //////System.out.prinln.println("Contains key");
                //expectedSequenceNumber+=1;
                //////System.out.prinln.println("Adding data to list of size"+dataMsg.length);
                dataMessages.add(dataMsg);
                this.responseDataList.put(connectionId,dataMessages);
            }
            else{
                ArrayList<byte[]> dataMessages=new ArrayList<>();

                //System.out.prinln.println("Sequence number for packet generation:"+expectedSequenceNumber);
                byte[] dataMsg=PacketAnalyzer.generateDataMessage(readBuf,connectionId,expectedSequenceNumber,numRead);
                //expectedSequenceNumber+=1;
                //////System.out.prinln.println("Adding data to list of size now"+dataMsg.length);
                dataMessages.add(dataMsg);
                this.responseDataList.put(connectionId,dataMessages);
            }
            //TODO XXY Moved here from previous TODO XXY
          /*  SelectionKey lpSocketKey;
            for(SocketChannel sc:this.clientChannel){
                lpSocketKey = sc.keyFor(this.selector);

                if(lpSocketKey.isWritable()) {
                    lpSocketKey.interestOps(SelectionKey.OP_WRITE);
                    break;
                }

            }*/
            SelectionKey lpSocketKey = this.clientChannel.get(0).keyFor(this.selector);
            //System.out.prinln.println("-------------Registering interest in OP_WRITE for LP channel");
            lpSocketKey.interestOps(SelectionKey.OP_WRITE);
        }
    }

    protected  void send(int connInfo, byte[] data,int seqId){
        //TODO: IMPLEMENT
        //add it to the buffer queue, send on as we can
        //NOPE WE DO NOT NEED THE SOCKET STOP THINKING WE DO JEEZ.
        //SocketChannel connChannel=this.connectionChannelMap.get(connInfo);
        //Null check needed
        //TODO: Add data to a list and then add to hashmap. Need to keep track of data sequence as well.
        //Need to read data into buffer here and raise ProxyDataEvent
       // this.pendingEvents.add(new ProxyEvents(data, connInfo, ProxyEvents.WRITING,SelectionKey.OP_WRITE,seqId));
        //Pull the data based on the connection ID
        boolean isWrite=false;
        //Here the data comes in first and so we're expecting the SYN packet first
        if(!expectedSequenceList.containsKey(connInfo)){
            expectedSequenceList.put(connInfo,0);
            ////System.out.prinln.println("Added to sequencelist");
        }
        else{
            int expectedSeq=expectedSequenceList.get(connInfo);
            if(expectedSeq==seqId){
                expectedSequenceList.put(connInfo,seqId+1);
            }
            if(expectedSeq>=1){
                isWrite=true;
            }
            //////System.out.prinln.println("Expected sequence is"+expectedSeq);
            //If the expected seq is what comes in, we increment the expectedseq number

        }
        if(connectionDataList.containsKey(connInfo)){
            //Buffer the data
            ConcurrentHashMap<Integer,byte[]> dataMap= connectionDataList.get(connInfo);
            dataMap.put(seqId,data);
            connectionDataList.put(connInfo,dataMap);
            int expectedSeq=expectedSequenceList.get(connInfo);
            for(Map.Entry<Integer,byte[]> connections:dataMap.entrySet()) {
                if(connections.getKey()==expectedSeq)
            ////System.out.prinln.println("expected seq id is"+expectedSeq);
           // while(availSequences.contains(expectedSeq)&&dataMap.get(expectedSeq)!=null){
                //This ensures that expectedSeq does not point to a sequence number that already exists in the map
                expectedSeq+=1;
            }
            expectedSequenceList.put(connInfo,expectedSeq);

        }
        else{
            ////System.out.prinln.println(" data to write is"+Utils.bytesToHex(data));

            ConcurrentHashMap<Integer,byte[]> dataMap=new ConcurrentHashMap<>();
            dataMap.put(seqId,data);

            connectionDataList.put(connInfo,dataMap);


            ////System.out.prinln.println("Created first data in connectionDataList");

        }
        if(isWrite) {
            //System.out.prinln.println("-------------Direct interest in OP_WRITE for connid" + connInfo);
            SocketChannel connectChannel = this.connectionChannelMap.get(connInfo);
            if (connectChannel != null) {
                SelectionKey key = connectChannel.keyFor(this.selector);
                key.interestOps(SelectionKey.OP_WRITE);
                //this.pendingEvents.add(new ProxyEvents(data, connInfo, ProxyEvents.WRITING,SelectionKey.OP_WRITE,seqId));
            }
        }
       // this.selector.wakeup();
    }

    protected void establishConn(InetSocketAddress msgInfo, byte[] data, int connId){
        //TODO: IMPLEMENT
        //add to event queue; create connection as possible
        try {
            ////System.out.prinln.println("Inside establish connection");
            SocketChannel serverChannel = SocketChannel.open();
            serverChannel.configureBlocking(false);
            // Kick off connection establishment
            //I've temporarily added port as the key to this map. Should we think of making this the connectionID?
            connectionChannelMap.put(connId,serverChannel);
            //Since it is a SYN packet, we effectively have received the packet with sequence number 0
            //Now we expect for sequence number 1

            //OP_CONNECT is getting masked by the call from ProxyWorker. Safe to listen to OP_WRITE here
            //System.out.prinln.println("-------------Registering interest in connection for connid"+connId);
            selectorLock.lock();
            try {
                selector.wakeup();
                //System.out.prinln.println("-------------Locked and registered for connID "+connId);
                serverChannel.register(this.selector,SelectionKey.OP_CONNECT,connId);
            } finally {
                selectorLock.unlock();
            }

           // this.pendingEvents.add(new ProxyEvents( data, connId,ProxyEvents.CONNECTING,SelectionKey.OP_CONNECT,-1));
            //serverChannel.connect(msgInfo);
            //No point in waking up here as there may not be enough data to write into the channel
            //this.selector.wakeup();
            ////System.out.prinln.println("Address is "+msgInfo.getHostString()+" "+ msgInfo.getPort());
            boolean isConnected=serverChannel.connect(msgInfo);
            //System.out.prinln.println("Connected? "+isConnected);

        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    protected void sendFin(int connId, int reason,int seqNum) {
        //TODO: IMPLEMENT
        //add to event queue; end connection as possible
        //how to close????? Yeah how to?
        //this.pendingEvents.add(new ProxyEvents(connInfo, new byte[0], SelectionKey.OP_CONNECT,ProxyEvents.ENDING));
       // this.selector.wakeup();
        if(connectionChannelMap.containsKey(connId)) {
            //////System.out.prinln.println("Send fin event");
            int expectedSequence=expectedSequenceList.get(connId);
            if(expectedSequence==seqNum) {

                //System.out.prinln.println("-------------Registering interest in OP_FIN for server channel "+connId);
                //this.connectionChannelMap.get(connId).close();
                this.pendingEvents.add(new ProxyEvents(new byte[0], connId, ProxyEvents.ENDING, SelectionKey.OP_CONNECT, -1));
            }
            else{
                if(connectionDataList.containsKey(connId)){
                    ConcurrentHashMap<Integer,byte[]>  dataMap= connectionDataList.get(connId);
                    dataMap.put(seqNum,null);
                    connectionDataList.put(connId,dataMap);
                }
                else {
                    ConcurrentHashMap<Integer, byte[]> dataMap = new ConcurrentHashMap<>();
                    dataMap.put(seqNum,null);
                    connectionDataList.put(connId,dataMap);
                }
            }
        }
    }
    private void completeConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ////System.out.prinln.println("Inside complete connection");
        //Complete connecting. This would return true if the connection is successful
        //socketChannel.configureBlocking(false);
        if(socketChannel.isConnectionPending()) {
            try {
                socketChannel.finishConnect();
            } catch (IOException e) {
                e.printStackTrace();
                key.cancel();
                return;
            }


            //Since connection is established, show interest in writing data to the server
            //////System.out.prinln.println("connected. Setting key to write");
            ////System.out.prinln.println("-------------Registering interest in OP_WRITE for Server channellll");
            //TODO XXYYZZ Uncomment below
            int connectionId = (int) key.attachment();
            System.out.println("Established Connection "+connectionId);
            if(!expectedSequenceList.containsKey(connectionId)){
                //System.out.prinln.println("SETTING expected sequence as 1");
                expectedSequenceList.put(connectionId,1);
            }

            //So, there are already other out of order packets captured for this connId
            //Since this is the initial SYN packet, we commence sending the data
            else{
                int newExpectedSeq=1;
                ConcurrentHashMap<Integer,byte[]> dataMap=this.connectionDataList.get(connectionId);
                while(dataMap.containsKey(newExpectedSeq)){
                    newExpectedSeq+=1;
                }
                expectedSequenceList.put(connectionId,newExpectedSeq);
                //System.out.prinln.println("New expected seq"+newExpectedSeq);
                //TODO Retrieve the out of order data from connectionDatalist and keep sending packets
            }
            ////System.out.prinln.println("Connection id is"+connectionId);
            // ////System.out.prinln.println("SIze of this.connectionDataList.get(connectionId).size() is"+
            //         this.connectionDataList.get(connectionId).size());
            if (this.connectionDataList.containsKey(connectionId)) {
                key.interestOps(SelectionKey.OP_WRITE);
                //System.out.prinln.println("Setting server channel to write since sequencelist is > 1");
            }
        }
        }

    private void write(SelectionKey key) throws IOException{
        SocketChannel sockCh = (SocketChannel) key.channel();
        if(key.attachment()!=null) {
            int connId = (int) key.attachment();
            if (connectionDataList.containsKey(connId)) {
                System.out.println("Process data write for connection "+ connId);
                //For now, assuming that we're reaching here only if the SYN packet is available
                ConcurrentHashMap<Integer,byte[]> dataMap = connectionDataList.get(connId);
                int expectedSequence=expectedSequenceList.get(connId);
             //   ////System.out.prinln.println("Expected sequence number is"+expectedSequence);
                //NavigableSet<Integer> seqNumberList=dataMap.keySet();
            //    ////System.out.prinln.println("Size of seqnumberlisr"+seqNumberList.size());
              //  ////System.out.prinln.println("Write socket channel data"+dataMap.size());
                for(Map.Entry<Integer,byte[]> connections:dataMap.entrySet()) {
                    int availSequence=connections.getKey();
                   // for(int availSequence:keySet) {
                    if (availSequence < expectedSequence) {
                        ByteBuffer buf = ByteBuffer.wrap(dataMap.get(availSequence));
                        int x = sockCh.write(buf);
                        ////System.out.prinln.println("wrote bytes to server"+ x);
                        if (buf.remaining() > 0) {
                            break;
                        }
                    }
                }
                for(Map.Entry<Integer,byte[]> connections:dataMap.entrySet()) {
                    int availSequence=connections.getKey();
                    ////System.out.prinln.println("avail seq "+ " expectedseq"+ availSequence+" "+ expectedSequence);
                    if(availSequence<expectedSequence) {
                            dataMap.remove(availSequence);
                        }
                    }
                    if((dataMap.containsKey(expectedSequence))&&(dataMap.get(expectedSequence)==null)){
                   //     ////System.out.prinln.println("Null??");
                        //System.out.prinln.println("Registering interest in ending for conn ID"+connId);
                        this.pendingEvents.add(new ProxyEvents(new byte[0], connId, ProxyEvents.ENDING, SelectionKey.OP_CONNECT, -1));
                    }
                    else {
                          if (dataMap.isEmpty()) {
                        //System.out.prinln.println("-------------Registering interest in OP_READ for server channel as datamap is empty"+connId);
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
            }
            else{
               // ////System.out.prinln.println("-------------Registering interest in OP_WRITE for server channel"+connId);
               // key.interestOps(SelectionKey.OP_WRITE);
            }
        }
        //This case is when the LP socket is ready to be written into
        else{
            System.out.println("Process data write to LP ");
            for(Map.Entry<Integer,ArrayList<byte[]>> connections:responseDataList.entrySet()) {
                ArrayList<byte[]> dataList = responseDataList.get(connections.getKey());
                //////System.out.prinln.println("Writing data into "+connections.getKey()+"size is "+dataList.size());

                while (!dataList.isEmpty()) {
                    ByteBuffer buf = ByteBuffer.wrap(dataList.get(0));
                    ////System.out.prinln.println("Written data is"+Utils.bytesToHex(buf.array()));
                    int x = sockCh.write(buf);
                    //    ////System.out.prinln.println("Wrote bytes"+x);
                    if (buf.remaining() > 0) {
                        break;
                    }
                    dataList.remove(0);
                }
            }
               // if (dataList.isEmpty()) {
                    //System.out.prinln.println("-------------Datalist empty. Registering interest in OP_READ for LP channel");

                    key.interestOps(SelectionKey.OP_READ);
            //    }

        }
    }
}
