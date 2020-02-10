import java.io.File;
import java.io.FileOutputStream;
import java.net.*;


public class Receiver1a {
	final static int PACKET_SIZE = 1027;
	final static int PAYLOAD_SIZE = 1024;
	final static int HEADER_SIZE = 3;
	
	public static void print(String s) {
		System.out.println(s);
	}

	public static void main(String[] args) throws Exception{
		 
		
		int port = Integer.parseInt(args[0]);
		String receivedFileName = args[1];
		
		
		
		try {
	
			// create datagram socket at port 9876
			DatagramSocket serverSocket = new DatagramSocket(port);
			FileOutputStream fileOutputStream = new FileOutputStream(new File(receivedFileName));
			byte[] receiveData = new byte[PACKET_SIZE];
			byte EOF = 0;
			
			while(EOF!=1){
				// create space for received datagram
				DatagramPacket receivedPacket = 
						new DatagramPacket(receiveData, receiveData.length);
				//receive datagram
				serverSocket.receive(receivedPacket);
				EOF = receiveData[2];
				print(receivedPacket.getLength()+" "+receivedPacket.getData().length);
				fileOutputStream.write(receivedPacket.getData(), HEADER_SIZE, receivedPacket.getLength()-HEADER_SIZE);
			}
			
			fileOutputStream.close();
			
			
				
		}
		catch(Exception e) {
			e.printStackTrace();
			
		}

	}

}
