package edu.indiana.p538;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ladyl on 11/20/2016.
 */
public class ProxyWorker implements Runnable{

    //this is where the bulk of the work will be done

    //need to analyze the MultiTCP header, establish connections according to MSYNS, send data, etc.
    //this is really where the refactoring comes in

    //blocking queue for events; init. to 50, can change.
    private BlockingQueue<ProxyDataEvent> queue = new ArrayBlockingQueue<ProxyDataEvent>(50);

    //Temporary fix for single pipe scenario. Keeps track of the expected sequence number. Will change this for multiple pipes
    private int expectedSequenceNumber=0;

    protected static String TO_SERVER = "S";
    protected static String TO_LP = "LP";

    public void processData(String dir, Proxy proxy, int connId, byte[] data, int count){
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);
        queue.add(new ProxyDataEvent(dir, proxy,connId, dataCopy));
        // add will do the notify
       // queue.notify();
    }

    @Override
    public void run() {
        ProxyDataEvent event=null;
        while(true){
            try {
                event = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] message = event.getData();
            //Tracker will keep track of navigating through the data array
            int tracker=0;

            //This loop is to ensure that the entire data array is read
            while(tracker < message.length){
                if(event.getDirection() == TO_SERVER){
                    byte[] header = Arrays.copyOfRange(message, tracker, tracker+AppConstants.MHEADER);
                    if(PacketUtils.isMSyn(header)){
                        InetSocketAddress msgInfo = PacketUtils.fetchConnectionInfo(message);
                        int connId= PacketUtils.getConnId(header);
                        //send back to the proxy
                        (event.getProxy()).establishConn(msgInfo, message,connId);
                        tracker+=AppConstants.MSYN_LEN;

                    }
                    else if(PacketUtils.isMFin(header)){
                        //Code commented for now
                        //else test for MFIN
                        //     InetSocketAddress msgInfo = PacketUtils.fetchConnectionInfo(message);

                        byte payload = message[AppConstants.MHEADER];
                        //Integer value was giving me -120. Switched to a String comparison for now. Needs to be refactored
                        String reason = PacketUtils.getMFin(payload);
                        int connId = PacketUtils.getConnId(header);
                        if(reason.equals(AppConstants.FIN_FLAG) || reason.equals(AppConstants.RST_FLAG)){
                            //end connection
                            (event.getProxy()).sendFin(connId, Integer.parseInt(reason));
                        }

                        tracker+=AppConstants.MFIN_LEN;

                    }else{
                        //else process and send data
                        //messageLength is inclusive of the data header. Need to keep that in mind.
                        int messageLength= PacketUtils.getLen(header);
                        byte[] payload = PacketUtils.getPayload(message,tracker,messageLength);
                        int seqNumber= PacketUtils.getSeqNum(header);
                        int connId= PacketUtils.getConnId(header);
                        tracker+=messageLength+AppConstants.MHEADER;

                        //return to the sender
                        (event.getProxy()).send(connId, payload,seqNumber, TO_SERVER);
                    }
                }else if(event.getDirection() == TO_LP){
                    //System.out.println("<Data Print>");
                    int connectionId = event.getConnectionId();
                    expectedSequenceNumber++;
                    int seqNum = expectedSequenceNumber;
                    byte[] payload = event.getData();
                    (event.getProxy()).send(connectionId, payload, seqNum, TO_LP);
                }
            }

            //test for MSYN



        }
    }
}
