package edu.indiana.p538;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static int PROXY_PORT = 6000;
    private static int SERVER_PORT = 6001;

    public static void main(String[] args) {
        //number of pipes
        int pipes = Integer.parseInt(args[0]);
        //classes
        //parse headers
        //sockets???
        //we need a queue for both ends

        //Socket to listen for LP
        try(
                ServerSocket proxyServer = new ServerSocket(PROXY_PORT);
                Socket client = proxyServer.accept();
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
        ){

        }catch(Exception e){
            System.err.println("Error opening socket");
        }

        //while socket is listening


    }
}
