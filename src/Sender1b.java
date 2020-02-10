import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender1b {

	final static int PACKET_SIZE = 1027;
	final static int PAYLOAD_SIZE = 1024;
	final static int HEADER_SIZE = 3;
	public static void print(String s) {
		System.out.println(s);
	}

	public static void main(String[] args) throws FileNotFoundException, IOException, Exception{
		
		String remoteHost = args[1];
		int port = Integer.parseInt(args[2]);
		String fileName = args[3];
		
		
		// create client socket
		DatagramSocket clientSocket = new DatagramSocket();
		
		//translate hostname to IP address using DNS
		InetAddress IPAddress = InetAddress.getByName("localhost");
		
		
		byte[]   sendData = new byte[1024];
		byte[] receiveACK = new byte[1];
		


		
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(fileName);
			int i = 0;
			int packet_data_size;
			do {
				byte[] buffer = new byte[PACKET_SIZE];
				i = fileInputStream.read(buffer, HEADER_SIZE, PAYLOAD_SIZE);
				packet_data_size =i;
				//EOF case
				if(i<PAYLOAD_SIZE) {
					buffer[2] = 1;
					if(i==-1)
						packet_data_size = 0;
				}
				
				// create datagram with data-to-send length, IP addr, port
				DatagramPacket sendPacket = 
						new DatagramPacket(buffer,packet_data_size+3, IPAddress, port);
				// send datagram to the receiver 
				clientSocket.send(sendPacket);

				try {
					Thread.sleep(10);
				}
				catch(InterruptedException e) {
					e.printStackTrace();
				}
				
				
				
				
				
			} while(i == PAYLOAD_SIZE);
			clientSocket.close();
			fileInputStream.close();
		}
		catch (Exception e) {
			System.out.println(e);
		} finally {
			if(fileInputStream != null)
				fileInputStream.close();
		}
		
		
//		// create input stream
//		BufferedReader inFromUser = 
//				new BufferedReader(new InputStreamReader(System.in));
//		// create client socket
//		DatagramSocket clientSocket = new DatagramSocket();
//		
//		//translate hostname to IP address using DNS
//		
//		InetAddress IPAddress = InetAddress.getByName("localhost");
//		
//		byte[]   sendData = new byte[1024];
//		byte[] receiveData = new byte[1024];
//		
//		String sentence = inFromUser.readLine();
//		sendData = sentence.getBytes();
//		
//		// create datagram with data-to-send length, IP addr, port
//		DatagramPacket sendPacket = 
//				new DatagramPacket(sendData,sendData.length, IPAddress, port);
//		// send datagram to the receiver 
//		clientSocket.send(sendPacket);
//		
//		//read datagram fropm server
//		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//		clientSocket.receive(receivePacket);
//		
//		String modifiedSentence =
//				new String(receivePacket.getData());
//		System.out.println("From SERVER:" + modifiedSentence);
//		clientSocket.close();
				

	}

}
