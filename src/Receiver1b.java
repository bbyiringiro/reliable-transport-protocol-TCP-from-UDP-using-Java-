import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * The Class Receiver1b.
 */
public class Receiver1b {

	/** The Constant PACKET_SIZE. */
	final static int PACKET_SIZE = 1027;
	
	/** The Constant PAYLOAD_SIZE. */
	final static int PAYLOAD_SIZE = 1024;
	
	/** The Constant HEADER_SIZE. */
	final static int HEADER_SIZE = 3;
	
	/**
	 * Prints the.
	 *
	 * @param s the s
	 */
	public static void print(String s) {
		System.out.println(s);
	}

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
		int currSequence = 0;
//		print("Starting Receiver ...");
		DatagramSocket serverSocket = null;
		try {
	
			// create datagram socket at port 9876
			serverSocket = new DatagramSocket(port);
			FileOutputStream fileOutputStream = new FileOutputStream(new File(receivedFileName));
			byte[] receiveData = new byte[PACKET_SIZE];
			byte[] ACKData = new byte[HEADER_SIZE-1];
			
			byte EOF = 0;
			while(EOF!=1){
				// create space for received datagram
				receiveData = new byte[PACKET_SIZE];
				DatagramPacket receivedPacket = 
						new DatagramPacket(receiveData, receiveData.length);
				//receive datagram
				serverSocket.receive(receivedPacket);
				InetAddress SenderIPAddress = receivedPacket.getAddress();
				int SenderPort = receivedPacket.getPort();
				EOF = receivedPacket.getData()[2];
				// getting the sequence number
				int senderSequence = (int) receivedPacket.getData()[1];
				ACKData[0] =  receivedPacket.getData()[0];
				ACKData[1] =  receivedPacket.getData()[1];
				
//				print(senderSequence+" "+currSequence);
				// write only if the right sequence number is sent.
				if(currSequence == senderSequence) {
					print(receivedPacket.getLength()+" "+receivedPacket.getData().length);
					fileOutputStream.write(receivedPacket.getData(), HEADER_SIZE, receivedPacket.getLength()-HEADER_SIZE);
					currSequence = (currSequence + 1) % 2;
				}
				
				DatagramPacket sendPacket = new DatagramPacket(ACKData, ACKData.length, SenderIPAddress, SenderPort);
				serverSocket.send(sendPacket);
				System.out.println(" just sent ACK to"+SenderPort+ " "+ SenderIPAddress);
				
			}
			
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
