package edu.indiana.p538;

/**
 * Created by atmohan on 13-11-2016.
 */
public class AppConstants {
    public static final int PACKET_TYPE_OFFSET=6;

    public static final int MSYN_LEN = 14;

    public static final int MSYN = 0xFFFF;

    public static final int MFIN_LEN = 9;

    public static final int MFIN = 0xFFFE;

    public static final int MHEADER = 8;

    public static final int IP_LEN = 4;

    public static final int PORT_LEN = 2;

    public static final int FIN_FLAG = 0x88;

    public static final int RST_FLAG = 0x99;
}
