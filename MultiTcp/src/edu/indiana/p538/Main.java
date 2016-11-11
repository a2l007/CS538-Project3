package edu.indiana.p538;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Main {
    private static int PROXY_PORT = 6000;
    private static int SERVER_PORT = 6001;

    //I am not sure how this is going to work across the threads....
    private static HashMap<Integer, ConnId> CONNECTIONS = new HashMap<Integer, ConnId>();

    public static void main(String[] args) {
        //number of pipes
        int pipes = Integer.parseInt(args[0]);
        //classes
        //parse headers
        //sockets???
        //we need a queue for both ends

        boolean listening = true;
        //Socket to listen for LP
        try(ServerSocket proxyServer = new ServerSocket(PROXY_PORT)){
            //while socket is listening
            while(listening){
                new ServerThread(proxyServer.accept()).start();
            }
        }catch(Exception e){
            System.err.println("Error opening socket");
            System.exit(-1);
        }
    }
}
