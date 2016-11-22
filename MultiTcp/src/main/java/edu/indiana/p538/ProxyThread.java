package edu.indiana.p538;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ladyl on 11/11/2016.
 */
public class ProxyThread implements Runnable{

    /*FIELDS AND CONSTANTS*/
    private ServerSocket socket = null;
    private int serverPort=6000;
    private Thread runningThread= null;

    /*GLOBAL VARIABLES*/
    private static ConcurrentHashMap<Integer, byte[]> MESSAGE_DATA = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, ClientThread> CLIENTS = new ConcurrentHashMap<>();

    /*CONSTRUCTORS*/
    public ProxyThread(int port){
        this.serverPort = port;
    }

    public void run() {

        boolean listening=true;
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        openSocketServer();

        while (listening) {
            try {
                Socket serverSocket = this.socket.accept();
                (new Thread(new ProxyThreadWorker(serverSocket))).start();
            } catch (IOException e) {
                throw new RuntimeException("Cannot accept client connection", e);
            }
        /*
        */
        }
    }

    private void openSocketServer(){
        try {
            this.socket = new ServerSocket(this.serverPort);
        }
        catch(IOException e){
            throw new RuntimeException("cannot open port",e);
        }

    }

    /* ProxyThreadWorker: Threads to handle different pipes between LP and RP
     */
    private class ProxyThreadWorker implements Runnable {
        private Socket clientSocket = null;
        private ProxyThreadWorker(Socket socket){
            this.clientSocket=socket;
        }

        public void run(){
            try(
                    //TODO: RESOURCES NEEDED
                    // OutputStream from socket (client) << needs to be able to write to diff InputStream (not client)
                    //not sure which is best here...
                    // this may not be the best choice but i am going with it for now
                    BufferedOutputStream clientOut = new BufferedOutputStream(clientSocket.getOutputStream());

                    // InputStream to socket (client)
                    BufferedInputStream clientIn = new BufferedInputStream(clientSocket.getInputStream());
            ){
                //TODO: PASS INFO BETWEEN STREAMS
                // Client InputStream needs to be fed to another OutputStream after parsing
                //I think this may be through the SERVER'S OutputStream????
                // Client OutputStream needs to be fed to (from other InputStream)
                //I think this may be via the SERVER'S InputStream
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] clientInput = new byte[4104];
                int done = 0;
                try{
                    while((done = clientIn.read(clientInput)) != -1){
                        //test header
                        byte[] header = Arrays.copyOfRange(clientInput, 0, AppConstants.MHEADER);
                        //test if MSYN or MFIN
                        if(PacketAnalyzer.isMSyn(header)){
                            //get destIp and destPort
                            ConnInfo newConn = PacketAnalyzer.fetchConnectionInfo(
                                    Arrays.copyOfRange(clientInput, 0, AppConstants.MSYN_LEN));
                            //start new socket??
                            //how is this going to work....
                            ClientThread client = new ClientThread(newConn);
                            int connId = PacketAnalyzer.getConnId(header);

                            //sync necessary??? don't think so....
                            if(!CLIENTS.containsKey(connId)){
                                CLIENTS.put(connId, client);
                                break; //is necessary? i don't feel good about having this here....
                            }
                            new Thread(client).start();

                        }else if(PacketAnalyzer.isMFin(header)){
                            //end connection with reason given
                            byte payload = clientInput[8];
                            int reason = PacketAnalyzer.getMFin(payload);
                            if(reason == AppConstants.FIN_FLAG){
                                int connId = PacketAnalyzer.getConnId(header);
                                //end connection
                                //TODO: IF FIN
                                if(CLIENTS.containsKey(connId)){
                                    ClientThread toExit = CLIENTS.get(connId);
                                    toExit.sendFin(); //TODO: IMPLEMENT SENDFIN()
                                }
                            }//TODO: ELSE IF RST

                        }else if(PacketAnalyzer.getLen(header) != 0){
                            //parse data message; send up to ProxyThread
                            //TODO: ELSE IF DATA
                        }

                        //if MSYN >>> establish connection with server at destIP/port given in next 6 bytes.
                        //if MFIN >>> examine reason; end connection with server appropriately.


                    }
                    baos.close();

                }catch(IOException e){
                    System.err.println("clientSocket: unable to get data");
                }

                //see if message is a SYNl, FIN, or data msg
                // if SYN, request connection to server -> set up ConnInfo and HashMap entry
                // if FIN, end connection with server
                // if data, confirm in order, unpack, and send on to server

            }catch(IOException e){
                System.err.print(e.getMessage());
            }
        }
    }
}
