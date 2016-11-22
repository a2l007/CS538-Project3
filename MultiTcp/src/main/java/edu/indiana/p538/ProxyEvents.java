package edu.indiana.p538;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

/**
 * Created by ladyl on 11/20/2016.
 */
public class ProxyEvents {
    /* CONSTANTS */
    protected static final int CONNECTING = 1;
    protected static final int ENDING = 100;
    protected static final int  WRITING = 10;

    /* FIELDS */
    private InetSocketAddress connInfo;
    private byte[] data;

    private int type;
    private int ops;

    protected ProxyEvents(InetSocketAddress connInfo, byte[] message, int type, int ops){
        this.connInfo = connInfo;
        this.data = message;
        this.ops = ops;
        this.type=type;
    }

    /* GETTERS */

    public byte[] getData() {
        return data;
    }

    protected InetSocketAddress getConnInfo() {
        return connInfo;
    }

    protected int getOps() {
        return ops;
    }

    protected int getType() {
        return type;
    }

    /* SETTERS */
    public void setType(int type) {
        this.type = type;
    }

}
