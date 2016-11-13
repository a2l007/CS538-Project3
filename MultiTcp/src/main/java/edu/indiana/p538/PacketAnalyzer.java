package edu.indiana.p538;

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
	public ConnId fetchConnectionInfo(byte[] packetStream) {
		int packetPointer = 0, synPointer = 0;
		if (packetPointer < packetStream.length) {
			synPointer = packetPointer + 6;

			StringBuffer synBuffer = new StringBuffer(String.format("%02X ",
					packetStream[synPointer]));
			synBuffer.append(String.format("%02X ",
					packetStream[synPointer + 1]));
			String synHexString = synBuffer.toString();
			if (synHexString.equals("FFFF")) {

				System.out.println("SYN Packet");
				String hexString = String.format("%02X ",
						packetStream[packetPointer]);
				int connectIdentifier = Utils.hextoDecimal(hexString);

				packetPointer += 2;

				int seqNumber = packetStream[packetPointer];
				packetPointer += 6;

				StringBuffer sourceIpBuf = new StringBuffer(
						String.valueOf(Utils.hextoDecimal(String.format("%02X",
								packetStream[packetPointer]))));

				sourceIpBuf.append(".").append(
						String.valueOf(Utils.hextoDecimal(String.format("%02X",
								packetStream[packetPointer + 1]))));
				sourceIpBuf.append(".").append(
						String.valueOf(Utils.hextoDecimal(String.format("%02X",
								packetStream[packetPointer + 2]))));
				sourceIpBuf.append(".").append(
						String.valueOf(Utils.hextoDecimal(String.format("%02X",
								packetStream[packetPointer + 3]))));
				packetPointer += 4; // navigating to destination ip byte

				StringBuffer portBuf = new StringBuffer(String.format("%02X",
						packetStream[packetPointer]));
				portBuf.append(String.format("%02X",
						packetStream[packetPointer + 1]));
				String sourceIp = sourceIpBuf.toString();
				String port = portBuf.toString();
				ConnId connection = new ConnId(sourceIp, port);
				return connection;
			}
			else{
				System.out.println("Missing SYN packet");
				ConnId c = new ConnId();
				return c;
			}
		} else {
			System.out.println("Empty Stream");
			ConnId c = new ConnId();
			return c;
		}
	}

	public boolean isMSyn(byte[] header){
        byte[] head3 = Arrays.copyOfRange(header, 6, 8);
        ByteBuffer buf = ByteBuffer.wrap(head3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int val = (int) buf.getShort();

        if(val == AppConstants.MSYN){
            return true;
        }else{
            return false;
        }
    }

    public boolean isMFin(byte[] header){
        byte[] head3 = Arrays.copyOfRange(header, 6, 8);
        ByteBuffer buf = ByteBuffer.wrap(head3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int val = (int) buf.getShort();

        if(val == AppConstants.MFIN){
            return true;
        }else{
            return false;
        }
    }

    public int getConnId(byte[] header){
        byte[] head1 = Arrays.copyOf(header, 2);
        ByteBuffer buf = ByteBuffer.wrap(head1);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int id = (int) buf.getShort();

        return id;
    }

    public int getLen(byte[] header){
        byte[] head3 = Arrays.copyOfRange(header, 6, 8);
        ByteBuffer buf = ByteBuffer.wrap(head3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int len = (int) buf.getShort();

        return len;
    }

    public int getSeqNum(byte[] header){
        byte[] head2 = Arrays.copyOfRange(header, 2, 6);
        ByteBuffer buf = ByteBuffer.wrap(head2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int seq = buf.getInt();

        return seq;
    }

}
