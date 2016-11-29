package edu.indiana.p538;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
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

    public void processData(Proxy proxy, SocketChannel socket, byte[] data, int count){
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);
        queue.add(new ProxyDataEvent(proxy,socket, dataCopy));
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

               //     byte payload = message[AppConstants.MHEADER];
               //     int reason = PacketUtils.getMFin(payload);
               //     int connId = PacketUtils.getConnId(header);
                   /* if(reason == AppConstants.FIN_FLAG || reason == AppConstants.RST_FLAG){
                        //end connection
                        (event.getProxy()).sendFin(msgInfo, reason);
                    }
                    */
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
                    (event.getProxy()).send(connId, payload,seqNumber);
                }
            }

            //test for MSYN



        }
    }
}
