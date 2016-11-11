package edu.indiana.p538;

/**
 * Created by ladyl on 11/11/2016.
 */
public class ConnId {
    private long ip;
    private short port;

    public ConnId(long ip, short port){
        this.ip = ip;
        this.port = port;
    }

    public long getIp(){
        return this.ip;
    }

    public short getPort(){
        return this.port;
    }
}
