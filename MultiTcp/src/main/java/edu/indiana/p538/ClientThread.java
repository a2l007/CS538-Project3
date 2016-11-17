package edu.indiana.p538;

import java.io.*;
import java.net.Socket;

/**
 * Created by atmohan on 13-11-2016.
 */
public class ClientThread implements Runnable{
    /*FIELDS AND CONSTANTS */
    public ConnInfo conn;

    /*GETTERS AND SETTERS*/
    public byte[] getDataMessage() {
        return dataMessage;
    }

    public void setDataMessage(byte[] dataMessage) {
        this.dataMessage = dataMessage;
    }

    public static byte dataMessage[];
    private Socket socket = null;
    public ClientThread(ConnInfo c){
        this.conn=c;
    }

    public void run() {
        try {
            System.out.println("Successful11");

            int port = conn.getPort();
            //Establish connection to server
            Socket socket = new Socket(conn.getIp(), port);
//            synchronized(this){
//                this.wait();
//            }
            OutputStream  out = socket.getOutputStream();
            out.write(dataMessage,0,dataMessage.length);
            System.out.println("Successful");
        } catch (IOException e) {
            System.err.print(e.getMessage());
        }
//        catch(InterruptedException e){
//            System.err.print(e.getMessage());
//
//        }
    }
}
