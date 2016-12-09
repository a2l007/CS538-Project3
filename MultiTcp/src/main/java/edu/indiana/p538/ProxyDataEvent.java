package edu.indiana.p538;

import java.nio.channels.SocketChannel;

/**
 * Created by ladyl on 11/20/2016.
 */
public class ProxyDataEvent {
    private SocketChannel socket;
    private byte[] data;
    private Proxy proxy;

    public ProxyDataEvent(Proxy proxy, SocketChannel sock, byte[] data){
        this.proxy = proxy;
        this.socket = sock;
        this.data = data;
    }

    /* GETTERS */

    public byte[] getData() {
        return data;
    }

    public SocketChannel getSocket() {
        return socket;
    }

    public Proxy getProxy() {
        return proxy;
    }
}
