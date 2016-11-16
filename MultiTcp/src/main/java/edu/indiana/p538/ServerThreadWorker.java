package edu.indiana.p538;

import java.io.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by atmohan on 16-11-2016.
 */
public class ServerThreadWorker extends Thread {
    protected Socket clientSocket = null;
    public ServerThreadWorker(Socket socket){
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
                        //not convinced we need this anymore....
                        ConnInfo newConn = PacketAnalyzer.fetchConnectionInfo(Arrays.copyOfRange(clientInput, 0,AppConstants.MSYN_LEN));
                        //start new socket??
                        //how is this going to work....
                        new Thread(new ClientThread(newConn)).start();
                     /*    ClientThread client=new ClientThread(newConn);
                        client.start();
                       synchronized (client){
                            //get the data byte array and notify client thread to resume data transfer
                            client.notify();
                        }*/

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
