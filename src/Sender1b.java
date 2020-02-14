import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Sender1b {

	final static int PACKET_SIZE = 1027;
	final static int PAYLOAD_SIZE = 1024;
	final static int HEADER_SIZE = 3;
	final static int TRIALLIMIT = 10;
	public static void print(String s) {
		System.out.println(s);
	}
	
	public static double [] sendFile(FileInputStream fileInputStream, InetAddress remoteHostIP, int remoteHostPort, int retryTimeOut) throws IOException{
		// create client socket
		DatagramSocket clientSocket = new DatagramSocket();
		byte[]   sendDataBuffer = new byte[PACKET_SIZE];
		byte[] receiveACK = new byte[2];
		int currSequence = 0;
		int transN = 0;
		int finalTransN =0;
		int i = 0;
		int packet_data_size;
		int trials;
		
		long startTime = System.currentTimeMillis();
		long stopTime=0;
		
		BufferedInputStream reader = new BufferedInputStream(fileInputStream);
		do {
			// rdt_rcv(rcvpkt)
//			byte[] buffer = new byte[PACKET_SIZE];
			i = reader.read(sendDataBuffer, HEADER_SIZE, PAYLOAD_SIZE);
			packet_data_size =i;
			//EOF case
			if(i<PAYLOAD_SIZE) {
				sendDataBuffer[2] = 1;
				if(i==-1)
					packet_data_size = 0;
			}
			//add sequence number
			sendDataBuffer[1] = (byte) currSequence;
			
			//rdt_send(data)
			// create datagram with data-to-send length, IP addr, port
			DatagramPacket sendPacket = 
					new DatagramPacket(sendDataBuffer,packet_data_size+3, remoteHostIP, remoteHostPort);
			
			boolean isPositiveACK = false;
			print(""+currSequence);
			trials = 0;
			do{
				// start a timer
				try {
					// recording on transmission time and duration when the last pack is sent.
					if(i<PAYLOAD_SIZE && trials ==0) {
						finalTransN = ++transN;
						stopTime = System.currentTimeMillis();
						print("the end ");
					}
					++transN;
					++trials;
					// send datagram to the receiver 
					clientSocket.send(sendPacket);
					clientSocket.setSoTimeout(retryTimeOut);
					
					DatagramPacket receivePacket = 
							new DatagramPacket(receiveACK, receiveACK.length);
//					print("yest");
					clientSocket.receive(receivePacket);
//					print("and yest");
					
					int ACK = (int) receivePacket.getData()[1];
					print(currSequence+" ACK:"+ACK);
					if(ACK == currSequence)
						isPositiveACK = true;
					else
						System.out.println(" ack didn't match so resend..");
				}catch (SocketTimeoutException e) {
					// possibly include a limit of timeout allowed.
					System.out.println(" time out then continue");
					continue;
				}catch (IOException e) {
					e.printStackTrace();
		        break;
				}
				
			}while(!isPositiveACK && trials < TRIALLIMIT);
			
			currSequence = (currSequence +1) % 2;

			
		} while(i == PAYLOAD_SIZE && trials<TRIALLIMIT);
		clientSocket.close();
		fileInputStream.close();
		double totoalTime = ((double) (stopTime - startTime))/1000;
		
		double[] result = new double[2];
		result[0] = (double) finalTransN;
		result[1] = totoalTime;
		
		return result;
		
	}

	public static void main(String[] args) throws Exception{
		// validate the length of arguments passed
		Common.checkArgumentLength(args.length, 4);
		
		String remoteHost = args[0];
		int port = Integer.parseInt(args[1]);
		String fileName = args[2];
		int retryTimeOut = Integer.parseInt(args[3]);
		
		//translate hostname to IP address using DNS
		InetAddress IPAddress = InetAddress.getByName(remoteHost);
		double[] results = new double[2];
		
		FileInputStream fileInputStream = null;
		File file =new File(fileName);
		double fileSize = file.length();
		
		

		try
		{
			fileInputStream = new FileInputStream(file);
			results = sendFile(fileInputStream, IPAddress, port, retryTimeOut);
			fileInputStream.close();
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(fileInputStream != null)
				fileInputStream.close();
		}
		
		int transN = (int) results[0];
		double totoalTime = results[1];
		double throughput = (fileSize/1024)/ totoalTime;
		print("filesize is "+fileSize +" filesize / 1024 is"+(fileSize/1024)+"totol time in sec"+ totoalTime);
		print("number of transmission is"+transN);
		print(""+throughput);

				

	}

}
