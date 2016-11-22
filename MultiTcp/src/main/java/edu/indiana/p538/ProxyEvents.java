package edu.indiana.p538;

import java.nio.channels.SelectionKey;

/**
 * Created by ladyl on 11/20/2016.
 */
public class ProxyEvents {
    /* CONSTANTS */
    public static int CONNECTING = 1;
    public static int ENDING = 100;
    public static int WRITING = 10;

    /* FIELDS */
    private ConnInfo connInfo;
    private byte[] data;
    public int ops;

    public ProxyEvents(ConnInfo connInfo, byte[] message, int ops){
        this.connInfo = connInfo;
        this.data = message;
        this.ops = ops;
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
