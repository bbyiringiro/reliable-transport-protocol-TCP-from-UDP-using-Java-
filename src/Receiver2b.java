import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.SocketTimeoutException;

/**
 * The Class Receiver2a.
 * Server side of Selective repeat protocol
 */
public class Receiver2b{

	/** The Constant PACKET_SIZE. */
	final static int PACKET_SIZE = 1027;
	
	/** The Constant PAYLOAD_SIZE. */
	final static int PAYLOAD_SIZE = 1024;
	
	/** The Constant HEADER_SIZE. */
	final static int HEADER_SIZE = 3;
	
	/** The file buffer. */
	static List<byte[]> fileBuffer;

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception{
		// validate the length of arguments passed
		Common.checkArgumentLength(args.length, 3);
		int port = Integer.parseInt(args[0]);
		String receivedFileName = args[1];
		int windowSize = Integer.parseInt(args[2]);
		fileBuffer = new ArrayList<byte[]>();
		int rcv_base = 0;
		Map<Integer, DatagramPacket> buffer = new HashMap<Integer, DatagramPacket>();
//		print("Starting Receiver ...");
		DatagramSocket serverSocket = null; 
		// sndpkt = make_packet(0, ACK, checksum)
		try {
	
			// create datagram socket at port 9876
			serverSocket = new DatagramSocket(port);
			FileOutputStream fileOutputStream = new FileOutputStream(new File(receivedFileName));
			byte[] receiveData = new byte[PACKET_SIZE];
			byte[] ACKData = new byte[HEADER_SIZE-1];
			
			// sndpkt = make_pkt(0, ACK, checksum)
			DatagramPacket sendPacket = null;
			InetAddress SenderIPAddress = null;
			int SenderPort = 0;
			int senderSequence = 0;
			int lastPckLength = 0;
			byte EOF = 0;
			byte eofByte = 0;
			int lastPck = -1;
			while(EOF!=1){
				// create space for received datagram
				receiveData = new byte[PACKET_SIZE];
				DatagramPacket receivedPacket = 
						new DatagramPacket(receiveData, receiveData.length);
				//receive datagram
				serverSocket.receive(receivedPacket);
				SenderIPAddress = receivedPacket.getAddress();
				SenderPort = receivedPacket.getPort();
				eofByte = receivedPacket.getData()[0];
				// getting the sequence number
				
				ACKData[0] =  receivedPacket.getData()[1];
				ACKData[1] =  receivedPacket.getData()[2];
				senderSequence =((ACKData[0] & 0xFF)  << 8) + (ACKData[1] & 0xFF);
				
				if(senderSequence >= rcv_base && senderSequence < rcv_base+windowSize)
				{
					if(eofByte==1) {
						lastPck = senderSequence;
						lastPckLength  = receivedPacket.getLength();
					}
					
					if(!buffer.containsKey(senderSequence))
						buffer.put(senderSequence, receivedPacket);
					if(senderSequence == rcv_base) {
						while(buffer.containsKey(rcv_base)) {
							fileBuffer.add(receivedPacket.getData());
							if(lastPck==rcv_base) {
								EOF=1;
								break;
							}
							rcv_base++;
						}

					}
					sendPacket = new DatagramPacket(ACKData, ACKData.length, SenderIPAddress, SenderPort);
					serverSocket.send(sendPacket);
				}else if (senderSequence >= rcv_base-windowSize && senderSequence <rcv_base)
				{
					sendPacket = new DatagramPacket(ACKData, ACKData.length, SenderIPAddress, SenderPort);
					serverSocket.send(sendPacket);
				}
				
//				else { ignore the packet}
				
				
			}
			
			// handle some acks for the last window are lost
			// wait for sender for around 3 seconds
			long startTime = System.currentTimeMillis();
			long stopTime = 0;
			while(stopTime-startTime<3000) {
				receiveData = new byte[PACKET_SIZE];			
				DatagramPacket receivedPacket = 
						new DatagramPacket(receiveData, receiveData.length);
				try {
					serverSocket.setSoTimeout(3000);
					serverSocket.receive(receivedPacket);
				
				}catch (SocketTimeoutException e) {
//					e.printStackTrace();
					break;
				}
				
				SenderIPAddress = receivedPacket.getAddress();
				SenderPort = receivedPacket.getPort();
				eofByte = receivedPacket.getData()[0];
				
				ACKData[0] =  receivedPacket.getData()[1];
				ACKData[1] =  receivedPacket.getData()[2];
				senderSequence =((ACKData[0] & 0xFF)  << 8) + (ACKData[1] & 0xFF);
				stopTime = System.currentTimeMillis();
				
				sendPacket = new DatagramPacket(ACKData, ACKData.length, SenderIPAddress, SenderPort);
				serverSocket.send(sendPacket);
				
				
				
				
				
			}
			
			// write to file
			for(int i =0; i<fileBuffer.size()-1;  i++) {
				byte packet[] = fileBuffer.get(i);
				fileOutputStream.write(packet, HEADER_SIZE, packet.length-HEADER_SIZE);
			}
			fileOutputStream.write(fileBuffer.get(fileBuffer.size()-1), HEADER_SIZE, lastPckLength-HEADER_SIZE);
			
			fileOutputStream.close();
			serverSocket.close();

		}
		catch(Exception e) {
			e.printStackTrace();
			
		}
		finally {
			if(serverSocket != null) {
				serverSocket.close();
			}
		}

	}

}
