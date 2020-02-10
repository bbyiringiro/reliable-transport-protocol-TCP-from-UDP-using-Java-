import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import javax.sound.midi.Sequence;
import javax.swing.Timer;

public class Sender1b {

	final static int PACKET_SIZE = 1027;
	final static int PAYLOAD_SIZE = 1024;
	final static int HEADER_SIZE = 3;
	public static void print(String s) {
		System.out.println(s);
	}
	
	int byte2Integer(byte b) {
		return (int) b;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException, Exception{
		
//		int sequence = 0;
//		byte [] b= new byte[2];
//		
//		for(int i=0; i<10; ++i) {
//			b[1] = (byte) sequence;
//			System.out.println((int) b[1]);
//			
//			sequence = (sequence+1) % 2;
//		}
		
		
		String remoteHost = args[0];
		int port = Integer.parseInt(args[1]);
		String fileName = args[2];
		int retryTimeOut = Integer.parseInt(args[3]);
		int currSequence = 0;
		
		print("Starting Sender ...");
		
		
		
		// create client socket
		DatagramSocket clientSocket = new DatagramSocket();
		
		//translate hostname to IP address using DNS
		InetAddress IPAddress = InetAddress.getByName("localhost");
		
		
		byte[]   sendData = new byte[1024];
		byte[] receiveACK = new byte[2];
		


		long startTime = System.currentTimeMillis();
		long stopTime;
		FileInputStream fileInputStream = null;
		double fileSize = (new File(fileName)).length();
		try {
			fileInputStream = new FileInputStream(fileName);
			int i = 0;
			int packet_data_size;
			do {
				// rdt_rcv(rcvpkt)
				byte[] buffer = new byte[PACKET_SIZE];
				i = fileInputStream.read(buffer, HEADER_SIZE, PAYLOAD_SIZE);
				packet_data_size =i;
				//EOF case
				if(i<PAYLOAD_SIZE) {
					buffer[2] = 1;
					if(i==-1)
						packet_data_size = 0;
				}
				
				//add sequence number
				buffer[1] = (byte) currSequence;
				
				//rdt_send(data)
				// create datagram with data-to-send length, IP addr, port
				DatagramPacket sendPacket = 
						new DatagramPacket(buffer,packet_data_size+3, IPAddress, port);
				
				boolean isPositiveACK = false;
				print(""+currSequence);
				
				do{
					// start a timer
					try {
						// send datagram to the receiver 
						clientSocket.send(sendPacket);
						clientSocket.setSoTimeout(retryTimeOut);
						
						DatagramPacket receivePacket = 
								new DatagramPacket(receiveACK, receiveACK.length);
						clientSocket.receive(receivePacket);
						
						int ACK = (int) receiveACK[1];
						print(ACK+" "+currSequence);
						if(ACK == currSequence)
							isPositiveACK = true;
						else
							System.out.println(" ack didn't match so resend..");
					}catch (SocketTimeoutException e) {
						// possibly include a limit of timeout allowed.
						System.out.println(" time out then continue");
						continue;
					}
					
				}while(!isPositiveACK);
				
				currSequence = (currSequence +1) % 2;

				
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
		stopTime = System.currentTimeMillis();
		
		double throughput = (fileSize/1024)/(stopTime - startTime)/1000;
		
		
		// create input stream
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
