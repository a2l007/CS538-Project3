package edu.indiana.p538;

import javax.xml.bind.DatatypeConverter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/*
 * @Atul
 */
public class PacketAnalyzer {

	/*
	 * Returns a connection object with information retrieved from the SYN
	 * packet
	 */
	public static InetSocketAddress fetchConnectionInfo(byte[] packetStream) {
		int packetPointer = 0, synPointer = 0;
        byte[] ip = Arrays.copyOfRange(packetStream, 8, 12);
        byte[] port = Arrays.copyOfRange(packetStream, 12, 14);

        try {
            InetAddress ipAddress = InetAddress.getByAddress(DatatypeConverter.parseHexBinary(Utils.bytesToHex(ip)));
            ByteBuffer buf = ByteBuffer.wrap(port);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int portNum = (int) buf.getShort();
            return new InetSocketAddress(ipAddress,portNum);
        }
        catch(UnknownHostException e){
            throw new RuntimeException("Unknown host",e);
        }
	}

	public static boolean isMSyn(byte[] header){
        byte[] head3 = Arrays.copyOfRange(header, 6, 8);
		if(Utils.bytesToHex(head3).equals("FFFF")){
			return true;
		}
		else{
			return false;
		}
		// Commented out for now as buf.getShort() was returning -1. Can you take a look?
		/*
		ByteBuffer buf = ByteBuffer.wrap(head3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int val = (int) Integer.getUnsignedLong(buf.getShort());
		System.out.println("Val is"+val);
		if(val == AppConstants.MSYN){
            return true;
        }else{
            return false;
        }
        */
    }

    public static boolean isMFin(byte[] header){
        byte[] head3 = Arrays.copyOfRange(header, 6, 8);
        /*ByteBuffer buf = ByteBuffer.wrap(head3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int val = (int) buf.getShort();

        if(val == AppConstants.MFIN){
            return true;
        }else{
            return false;
        }*/
        //Fin is identifed by FEFF and not FFFE. Need to follow up with prof on this
        if(Utils.bytesToHex(head3).equals("FEFF")){
            return true;
        }
        else{
            return false;
        }
    }

    public static int getConnId(byte[] header){
        byte[] head1 = Arrays.copyOf(header, 2);
        ByteBuffer buf = ByteBuffer.wrap(head1);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int id = (int) buf.getShort();

        return id;
    }

    public static int getLen(byte[] header){
        byte[] head3 = Arrays.copyOfRange(header, 6, 8);
        ByteBuffer buf = ByteBuffer.wrap(head3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int len = (int) buf.getShort();
        len-=AppConstants.MHEADER;
        return len;
    }

    public static int getSeqNum(byte[] header){
        byte[] head2 = Arrays.copyOfRange(header, 2, 6);
        ByteBuffer buf = ByteBuffer.wrap(head2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int seq = buf.getInt();

        return seq;
    }

    public static int getMFin(byte payload){
        return (int) payload;
    }

    public static byte[] getPayload(byte[] message, int offset, int messageLength){
        //Change to ensure that in each iteration, the payload is retrieved based on the present position of tracker
        byte[] payload = Arrays.copyOfRange(message, offset+AppConstants.MHEADER, offset+AppConstants.MHEADER+messageLength);
        return payload;
    }

}
