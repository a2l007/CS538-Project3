package edu.indiana.p538;

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

}
