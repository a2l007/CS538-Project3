package edu.indiana.p538;

import java.io.*;
import java.net.Socket;

/**
 * Created by ladyl on 11/11/2016.
 */
public class ServerThread extends Thread{

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
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                ){
            //TODO: PASS INFO BETWEEN STREAMS
                // Client OutputStream needs to be fed to another InputStream
                    //I think this may be through the SERVER'S OutputStream????
                // Client InputStream needs to be fed to (from other OutputStream)
                    //I think this may be via the SERVER'S InputStream
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
