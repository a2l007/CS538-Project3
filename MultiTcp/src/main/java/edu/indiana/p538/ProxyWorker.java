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
        queue.add(new ProxyDataEvent(socket, dataCopy));
        queue.notify();
    }

    @Override
    public void run() {
        ProxyDataEvent event;
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
                byte payload = message[AppConstants.MHEADER];
                int reason = PacketAnalyzer.getMFin(payload);
                int connId = PacketAnalyzer.getConnId(header);
                if(reason == AppConstants.FIN_FLAG){
                    //end connection
                    //TODO: IF FIN
                }else if(reason == AppConstants.RST_FLAG){
                    //end connection FORCEFULLY
                    //TODO: IF RST
                }
            }else{
                //else process and send data
                //return to the sender
                (event.getProxy()).send(event.getSocket(), message);
            }


        }
    }
}
