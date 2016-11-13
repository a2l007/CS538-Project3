package edu.indiana.p538;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by ladyl on 11/11/2016.
 */
public class ServerThread extends Thread{

    /*CONSTANTS*/
    private static int MSYN_LEN = 14;
    private static int MFIN_LEN = 9;
    private static int MHEADER = 8;
    
    /*FIELDS*/
    private Socket socket = null;

    /*CONSTRUCTORS*/
    public ServerThread(Socket socket){
        super("ServerThread");
        this.socket = socket;
    }

    public void run(){
        try(
                //TODO: RESOURCES NEEDED
                // OutputStream from socket (client) << needs to be able to write to diff InputStream (not client)
                    //not sure which is best here...
                // this may not be the best choice but i am going with it for now
                BufferedOutputStream clientOut = new BufferedOutputStream(socket.getOutputStream());

                // InputStream to socket (client)
                BufferedInputStream clientIn = new BufferedInputStream(socket.getInputStream());
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
                    byte[] header = Arrays.copyOfRange(clientInput, 0, MHEADER);
                    //test if MSYN or MFIN


                }
                baos.close();

            }catch(IOException e){
                System.err.println("clientSocket: unable to get data");
            }

            //see if message is a SYNl, FIN, or data msg
            // if SYN, request connection to server -> set up ConnId and HashMap entry
            // if FIN, end connection with server
            // if data, confirm in order, unpack, and send on to server

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
