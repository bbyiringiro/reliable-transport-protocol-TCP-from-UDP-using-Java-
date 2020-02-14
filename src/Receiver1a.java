import java.io.File;
import java.io.FileOutputStream;
import java.net.*;


/**
 * The Class Receiver1a.
 */
public class Receiver1a {
	
	/** The Constant PACKET_SIZE. */
	final static int PACKET_SIZE = 1027;
	
	/** The Constant PAYLOAD_SIZE. */
	final static int PAYLOAD_SIZE = 1024;
	
	/** The Constant HEADER_SIZE. */
	final static int HEADER_SIZE = 3;

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws IllegalArgumentException{
		// validate the length of arguments passed
		Common.checkArgumentLength(args.length, 2);
		
		int port = Integer.parseInt(args[0]);
		String receivedFileName = args[1];
		
		FileOutputStream fileOutputStream = null;
		DatagramSocket serverSocket = null;
		try {
			// create datagram socket at port 9876
			serverSocket = new DatagramSocket(port);
			fileOutputStream = new FileOutputStream(new File(receivedFileName));
			byte[] receiveData = new byte[PACKET_SIZE];
			byte EOF = 0;
			
			while(EOF!=1){
				// create space for received datagram
				DatagramPacket receivedPacket = 
						new DatagramPacket(receiveData, receiveData.length);
				//receive datagram
				serverSocket.receive(receivedPacket);
				//check if EOF was passed
				EOF = receivedPacket.getData()[2];
//				System.out.println(receivedPacket.getLength()+" "+receivedPacket.getData().length);
				//offset the header bytes and write only payload
				fileOutputStream.write(receivedPacket.getData(), HEADER_SIZE, receivedPacket.getLength()-HEADER_SIZE);
			}
			
			fileOutputStream.close();
			serverSocket.close();
			
				
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
			
		}finally {
			if(serverSocket !=null) {
				serverSocket.close();
			}
		}

	}

}
