package edu.indiana.p538;

/**
 * Created by atmohan on 13-11-2016.
 */
public class AppConstants {
    protected static final int PACKET_TYPE_OFFSET=6;

    protected static final int MSYN_LEN = 14;

    protected static final int MSYN = 0xFFFF;

    public static final int MFIN_LEN = 9;

    protected static final int MFIN = 0xFFFE;

    protected static final int MHEADER = 8;

    protected static final int IP_LEN = 4;

    protected static final int PORT_LEN = 2;

    protected static final String FIN_FLAG = "88";

    protected static final String RST_FLAG = "99";

    protected static final int PROXY_PORT = 6000;

    protected static final int SERVER_PORT = 6001;
}
