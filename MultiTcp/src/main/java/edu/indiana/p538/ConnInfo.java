package edu.indiana.p538;

/**
 * Created by ladyl on 11/11/2016.
 */
public class ConnInfo {
    private String ip;
    private String port;

    public ConnInfo(String ip, String port){
        this.ip = ip;
        this.port = port;
    }
    public ConnInfo(){

    }
    public String getIp(){
        return this.ip;
    }

    public String getPort(){
        return this.port;
    }
}
