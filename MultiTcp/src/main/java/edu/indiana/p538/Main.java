package edu.indiana.p538;

import java.net.ServerSocket;
import java.util.HashMap;

public class Main {
    private static int PROXY_PORT = 6000;
    private static int SERVER_PORT = 6001;

    //I am not sure how this is going to work across the threads....
    protected static HashMap<Integer, ConnInfo> CONNECTIONS = new HashMap<Integer, ConnInfo>();

    public static void main(String[] args) {
        //number of pipes
        int pipes = Integer.parseInt(args[0]);
        //classes
        //parse headers
        //sockets???
        //we need a queue for both ends
        boolean listening = true;
        //Socket to listen for LP
        (new Thread(new ServerThread(PROXY_PORT))).start();
        /*
        try(ServerSocket proxyServer = new ServerSocket(PROXY_PORT)){
            //while socket is listening
            while(listening){
                System.out.println("Liatening");
                new ServerThread(proxyServer.accept()).start();
                System.out.println("Liatening again");

            }
        }catch(Exception e){
            System.err.println("Error opening socket");
            System.exit(-1);
        }
        */
    }
}
