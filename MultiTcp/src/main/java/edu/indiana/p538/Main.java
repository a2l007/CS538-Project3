package edu.indiana.p538;

import java.util.HashMap;

public class Main {
    private static int PROXY_PORT = 6000;

    public static void main(String[] args) {
        //number of pipes
        int pipes = Integer.parseInt(args[0]);
        //classes
        //parse headers
        //sockets???
        //we need a queue for both ends

        try{
            ProxyWorker worker = new ProxyWorker();
            new Thread(worker).start();
            new Thread(new Proxy(PROXY_PORT, worker)).start();
        }catch(Exception e){
            System.err.println("Error opening socket");
            System.exit(-1);
        }

    }
}
