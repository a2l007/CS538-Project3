package edu.indiana.p538;

import java.net.InetAddress;
import java.net.Socket;

public class Main {
    private static int PROXY_PORT = 6000;

    public static void main(String[] args) {
	    //number of pipes
        int pipes = Integer.parseInt(args[0]);
        //classes
            //parse headers
        //sockets???
        //we need a queue for both ends

        //Socket to listen for LP
        try{
            InetAddress hostIp = InetAddress.getLocalHost();
            Socket server = new Socket(hostIp, PROXY_PORT);

            //while socket is listening
            
        }catch(Exception e){
            System.err.println("Invalid host IP address");
        }


    }
}
