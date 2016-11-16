package edu.indiana.p538;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by ladyl on 11/11/2016.
 */
public class ServerThread extends Thread{



    /*FIELDS*/
    protected ServerSocket socket = null;
    protected int serverPort=6000;
    protected Thread runningThread= null;
    protected boolean listening=true;
    /*CONSTRUCTORS*/
    public ServerThread(int port){
        super("ServerThread");
        this.serverPort = port;
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        openSocketServer();

        while (listening) {
            try {
                Socket serverSocket = this.socket.accept();
                new Thread(new ServerThreadWorker(serverSocket)).start();
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
}
