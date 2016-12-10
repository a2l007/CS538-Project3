package edu.indiana.p538;

import java.net.InetSocketAddress;
import java.nio.channels.InterruptedByTimeoutException;
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

            byte[] message = event.getData();
            System.out.println("Msg is"+Utils.bytesToHex(message));

            //Tracker will keep track of navigating through the data array
            int tracker=0;
            //This loop is to ensure that the entire data array is read
           // System.out.println("size is "+message.length);
            while(tracker < message.length){
                byte[] header = Arrays.copyOfRange(message, tracker, tracker+AppConstants.MHEADER);
                if(PacketAnalyzer.isMSyn(header)){
                    InetSocketAddress msgInfo = PacketAnalyzer.fetchConnectionInfo(message);
                    int connId=PacketAnalyzer.getConnId(header);
                    System.out.println("Syn packet");
                    //send back to the proxy
                    (event.getProxy()).establishConn(msgInfo, message,connId);
                    tracker+=AppConstants.MSYN_LEN;

                }
                else if(PacketAnalyzer.isMFin(header)){
                    //Code commented for now
                    //else test for MFIN
               //     InetSocketAddress msgInfo = PacketAnalyzer.fetchConnectionInfo(message);
                    //System.out.println("Finner");
                    byte payload = message[AppConstants.MHEADER];
                    String reason = PacketAnalyzer.getMFin(payload);
                    int connId = PacketAnalyzer.getConnId(header);
                    int seqNumber=PacketAnalyzer.getSeqNum(header);
                    if(reason.equals(AppConstants.FIN_FLAG) || reason.equals(AppConstants.RST_FLAG)){
                        //end connection
                        (event.getProxy()).sendFin(connId, Integer.parseInt(reason),seqNumber);
                    }
                    tracker+=AppConstants.MFIN_LEN;

                }else{
                    //else process and send data
                    //messageLength is inclusive of the data header. Need to keep that in mind.
                    int messageLength=PacketAnalyzer.getLen(header);
                    byte[] payload = PacketAnalyzer.getPayload(message,tracker,messageLength);
                    int seqNumber=PacketAnalyzer.getSeqNum(header);
                    int connId=PacketAnalyzer.getConnId(header);
                    tracker+=messageLength+AppConstants.MHEADER;

                    //return to the sender
                    (event.getProxy()).send(connId, payload,seqNumber);
                }
            }

            //test for MSYN

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
