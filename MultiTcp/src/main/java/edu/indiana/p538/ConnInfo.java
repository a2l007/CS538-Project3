package edu.indiana.p538;

import java.net.InetAddress;

/**
 * Created by ladyl on 11/11/2016.
 */
public class ConnInfo {
    private InetAddress ip;
    private int port;

    public ConnInfo(InetAddress ip, int port){
        this.ip = ip;
        this.port = port;
    }
    public ConnInfo(){
        this.ip = null;
        this.port = 0;
    }
    public InetAddress getIp(){
        return this.ip;
    }

    public int getPort(){
        return this.port;
    }
}
