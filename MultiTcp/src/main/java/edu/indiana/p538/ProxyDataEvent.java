package edu.indiana.p538;

import java.nio.channels.SocketChannel;

/**
 * Created by ladyl on 11/20/2016.
 */
public class ProxyDataEvent {
    private int connId;
    private byte[] data;
    private Proxy proxy;
    private String direction;

    public ProxyDataEvent(String dir, Proxy proxy, int connId, byte[] data){
        this.proxy = proxy;
        this.connId = connId;
        this.data = data;
        this.direction = dir;
    }

    /* GETTERS */

    public byte[] getData() {
        return data;
    }

    public int getConnectionId() {
        return connId;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public String getDirection() {
        return direction;
    }
}
