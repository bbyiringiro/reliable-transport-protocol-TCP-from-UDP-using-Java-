import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver1b {

	final static int PACKET_SIZE = 1027;
	final static int PAYLOAD_SIZE = 1024;
	final static int HEADER_SIZE = 3;
	
	public static void print(String s) {
		System.out.println(s);
	}

	public static void main(String[] args) throws Exception{
		// validate the length of arguments passed
		Common.checkArgumentLength(args.length, 2);

		int port = Integer.parseInt(args[0]);
		String receivedFileName = args[1];
		int currSequence = 0;
		print("Starting Receiver ...");
		
		
		
		
		try {
	
			// create datagram socket at port 9876
			DatagramSocket serverSocket = new DatagramSocket(port);
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
				
				int senderSequence = (int) receivedPacket.getData()[1];
				ACKData[0] =  receivedPacket.getData()[0];
				ACKData[1] =  receivedPacket.getData()[1];
				
				print(senderSequence+" "+currSequence);
				
				
				if(currSequence == senderSequence) {
					print(receivedPacket.getLength()+" "+receivedPacket.getData().length);
					fileOutputStream.write(receivedPacket.getData(), HEADER_SIZE, receivedPacket.getLength()-HEADER_SIZE);
					currSequence = (currSequence + 1) % 2;
					System.out.println(" Positive");
				}
				
				DatagramPacket sendPacket = new DatagramPacket(ACKData, ACKData.length, SenderIPAddress, SenderPort);
				serverSocket.send(sendPacket);
				System.out.println(" just sent ACK to"+SenderPort+ " "+ SenderIPAddress);
				
			}
			
			fileOutputStream.close();
			
			
				
		}
		catch(Exception e) {
			e.printStackTrace();
			
		}

	}

}
