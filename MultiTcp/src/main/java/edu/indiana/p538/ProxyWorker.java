package edu.indiana.p538;

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
        queue.notify();
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
            byte[] header = Arrays.copyOfRange(message, 0, AppConstants.MHEADER);
            //test for MSYN
            if(PacketAnalyzer.isMSyn(header)){
                ConnInfo msgInfo = PacketAnalyzer.fetchConnectionInfo(message);
                //send back to the proxy
                (event.getProxy()).establishConn(msgInfo, message);
            }else if(PacketAnalyzer.isMFin(header)){
                //else test for MFIN
                ConnInfo msgInfo = PacketAnalyzer.fetchConnectionInfo(message);

                byte payload = message[AppConstants.MHEADER];
                int reason = PacketAnalyzer.getMFin(payload);
                int connId = PacketAnalyzer.getConnId(header);
                if(reason == AppConstants.FIN_FLAG || reason == AppConstants.RST_FLAG){
                    //end connection
                    (event.getProxy()).sendFin(msgInfo, reason);
                }
            }else{
                //else process and send data
                byte[] payload = PacketAnalyzer.getPayload(message);
                ConnInfo connInfo = PacketAnalyzer.fetchConnectionInfo(header);
                //return to the sender
                (event.getProxy()).send(connInfo, payload);
            }


        }
    }
}
