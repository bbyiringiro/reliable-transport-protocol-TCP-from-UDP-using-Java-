import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * The Class Sender1a.
 */
public class Sender1a {
	
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
	 * Send File Function
	 *
	 * @param fileInputStream the file input stream
	 * @param remoteHostIP the remote host IP
	 * @param remoteHostPort the remote host port
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void sendFile(FileInputStream fileInputStream, InetAddress remoteHostIP, int remoteHostPort) throws IOException{
		// create client socket
		DatagramSocket clientSocket = new DatagramSocket();
		byte[]   sendDataBuffer = new byte[PACKET_SIZE];
		int i = 0;
		int packet_data_size;
		do {
			i = fileInputStream.read(sendDataBuffer, HEADER_SIZE, PAYLOAD_SIZE);
			packet_data_size =i;
			//EOF case
			if(i<PAYLOAD_SIZE) {
				sendDataBuffer[2] = 1;
				if(i==-1)
					packet_data_size = 0;
			}
			
			// create datagram with data-to-send length, IP addr, port
			DatagramPacket sendPacket = 
					new DatagramPacket(sendDataBuffer,packet_data_size+3, remoteHostIP, remoteHostPort);
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
		
		
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception{
		// validate the length of arguments passed
		Common.checkArgumentLength(args.length, 3);
		
		String remoteHost = args[0];
		int port = Integer.parseInt(args[1]);
		String fileName = args[2];
		//translate hostname to IP address using DNS
		InetAddress IPAddress = InetAddress.getByName(remoteHost);
		
		FileInputStream fileInputStream = null;
		try
		{
			fileInputStream = new FileInputStream(fileName);
			sendFile(fileInputStream, IPAddress, port);
			fileInputStream.close();
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(fileInputStream != null)
				fileInputStream.close();
		}
	}

}
