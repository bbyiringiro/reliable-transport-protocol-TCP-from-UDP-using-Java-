import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class Receiver2a.
 */
public class Receiver2a{

	/** The Constant PACKET_SIZE. */
	final static int PACKET_SIZE = 1027;
	
	/** The Constant PAYLOAD_SIZE. */
	final static int PAYLOAD_SIZE = 1024;
	
	/** The Constant HEADER_SIZE. */
	final static int HEADER_SIZE = 3;
	static List<byte[]> fileBuffer;
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception{
		// validate the length of arguments passed
		Common.checkArgumentLength(args.length, 2);
		int port = Integer.parseInt(args[0]);
		String receivedFileName = args[1];
		fileBuffer = new ArrayList<byte[]>();
		int expectedseqnum = 0;
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
			DatagramPacket receivedPacket = null;
			byte EOF = 0;
			while(EOF!=1){
				// create space for received datagram
				receiveData = new byte[PACKET_SIZE];
				receivedPacket = new DatagramPacket(receiveData, receiveData.length);
				//receive datagram
				serverSocket.receive(receivedPacket);
				SenderIPAddress = receivedPacket.getAddress();
				SenderPort = receivedPacket.getPort();
				
				// getting the sequence number
				
				ACKData[0] =  receivedPacket.getData()[1];
				ACKData[1] =  receivedPacket.getData()[2];
				senderSequence =((ACKData[0] & 0xFF)  << 8) + (ACKData[1] & 0xFF);
				
				if(senderSequence == expectedseqnum) {
					EOF = receivedPacket.getData()[0];
					fileBuffer.add(receivedPacket.getData());
					expectedseqnum = expectedseqnum+1;
				
				}else {
					
					ACKData[0] =  (byte) ((expectedseqnum-1) >> 8);
					ACKData[1] =  (byte) (expectedseqnum-1);
				}
				
				sendPacket = new DatagramPacket(ACKData, ACKData.length, SenderIPAddress, SenderPort);
				serverSocket.send(sendPacket);
	
			}
			// send the last packet 10 before we end the program times 
			for(int i=0; i<10; ++i) {
				ACKData[0] = (byte) ((senderSequence) >> 8);
				ACKData[1] =  (byte) (senderSequence);
				sendPacket = new DatagramPacket(ACKData, ACKData.length, SenderIPAddress, SenderPort);
				serverSocket.send(sendPacket);
				
			}
			// write to file
			for(int i =0; i<fileBuffer.size()-1;  i++) {
				byte packet[] = fileBuffer.get(i);
				fileOutputStream.write(packet, HEADER_SIZE, packet.length-HEADER_SIZE);
			}
			fileOutputStream.write(fileBuffer.get(fileBuffer.size()-1), HEADER_SIZE, receivedPacket.getLength()-HEADER_SIZE);
			
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
