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
    private int connId;
    private int seqNum;
    protected ProxyEvents(byte[] message, int connId, int type, int ops, int seqNum){
        this.data = message;
        this.ops = ops;
        this.type=type;
        this.connId=connId;
        this.seqNum = seqNum;
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

    protected int getConnId() {
        return connId;
    }

    /* SETTERS */
    public void setType(int type) {
        this.type = type;
    }

}
