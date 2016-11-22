package edu.indiana.p538;

import java.nio.channels.SelectionKey;

/**
 * Created by ladyl on 11/20/2016.
 */
public class ProxyEvents {
    /* CONSTANTS */
    public static final int CONNECTING = 1;
    public static final int ENDING = 100;
    public static final int  WRITING = 10;

    /* FIELDS */
    private ConnInfo connInfo;
    private byte[] data;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private int type;
    private int ops;

    public ProxyEvents(ConnInfo connInfo, byte[] message, int type, int ops){
        this.connInfo = connInfo;
        this.data = message;
        this.ops = ops;
        this.type=type;
    }

    /* GETTERS */

    public byte[] getData() {
        return data;
    }

    public ConnInfo getConnInfo() {
        return connInfo;
    }

    public int getOps() {
        return ops;
    }
}
