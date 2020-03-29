import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;

// TODO: Auto-generated Javadoc
/**
 * The Class Sender1b.
 */
public class Sender2a {

	/** The Constant PACKET_SIZE. */
	final static int PACKET_SIZE = 1027;
	
	/** The Constant PAYLOAD_SIZE. */
	final static int PAYLOAD_SIZE = 1024;
	
	/** The Constant HEADER_SIZE. */
	final static int HEADER_SIZE = 3;
	
	/** The retrans. */
	static int retrans =0;
	
	/** The bchuncks. */
	static List<byte[]> bchuncks;
	
	/** The base. */
	static int base = 0; // base = 1  this was handled in the implementation
	
	/** The nextseqnum. */
	static int nextseqnum = 0; //nextseqnum = 1
	
	/** The timer lock. */
	public static Object timer_lock = new Object();
	
	/** The time print lock. */
	public static Object time_print_lock = new Object();
	
	/** The timer. */
	public static   Timer timer;
	
	/** The re trans task. */
	public static   Timeout reTransTask;
	
	/** The client socket. */
	public static DatagramSocket clientSocket;
	
	/** The start time. */
	public static long startTime;
	
	/** The stop time. */
	public static long stopTime;
	
	
	
	/**
	 * The Class Timeout.
	 * Manages the re-sending of of packets from base after timeout. 
	 */
	public static class Timeout extends TimerTask {
		
		/** The remote host IP. */
		InetAddress remoteHostIP;
		
		/** The remote host port. */
		int remoteHostPort;
		
		/** The retry time out. */
		int retryTimeOut;
		
		/**
		 * Instantiates a new timeout.
		 *
		 * @param IP the ip
		 * @param port the port
		 * @param timer the timer
		 */
		public Timeout(InetAddress IP, int port, int timer) {
			this.remoteHostIP = IP;
			this.remoteHostPort = port;
			this.retryTimeOut = timer;
		}
		
		/* (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
		public void run(){
			synchronized(timer_lock) { 
				++retrans;
				//start_timer
				reTransTask = new Timeout(remoteHostIP, remoteHostPort, retryTimeOut);
				timer.schedule(reTransTask, retryTimeOut);
				//udt_send from base to base up to nextseqnum-1
				int i = base==0? 0:base-1;
				for(;i< nextseqnum; ++i) {
					sendPacket(bchuncks.get(i), remoteHostIP, remoteHostPort);
				}
				
			}
		}
	}
	
	
	/**
	 * The Class AckReceiver.
	 * Runnable Acknowledgement Receiving Thread
	 */
	public static class AckReceiver implements Runnable {
		
		/** The receive ACK. */
		byte[] receiveACK = new byte[2];
		
		/** The remote host IP. */
		InetAddress remoteHostIP;
		
		/** The remote host port. */
		int remoteHostPort;
		
		/** The retry time out. */
		int retryTimeOut;
		
		/** The stop. */
		public boolean stop = false;
		
		/**
		 * Instantiates a new ack receiver.
		 *
		 * @param IP the ip
		 * @param port the port
		 * @param timer the timer
		 */
		public AckReceiver(InetAddress IP, int port, int timer) {
			this.remoteHostIP = IP;
			this.remoteHostPort = port;
			this.retryTimeOut = timer;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			while(!stop) {
//				 rdt_rcv(rcvpkt) & notcorrupt (rcvpkt)
				DatagramPacket receivePacket = 
						new DatagramPacket(receiveACK, receiveACK.length);
				try {
					clientSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				//base = getacknum(rcpkt) + 1
				base = ((receivePacket.getData()[0] & 0xFF)  << 8) + (receivePacket.getData()[1] & 0xFF)+1;
				// handling the last Ack and end the thread;
				if(base == bchuncks.size())
				{
					synchronized(timer_lock) {
						reTransTask.cancel();
						timer.cancel();	
					}
					synchronized(time_print_lock) {
						stopTime = System.currentTimeMillis();
						time_print_lock.notify();
					}
					return;
	
				}
				// managing timer 
				if(base == nextseqnum) {
					synchronized(timer_lock) {
					reTransTask.cancel();
					}
				}else {
					synchronized(timer_lock) {
//						timer.cancel();
//						timer = new Timer();
						reTransTask.cancel();
						reTransTask = new Timeout(remoteHostIP, remoteHostPort, retryTimeOut);
						timer.schedule(reTransTask, retryTimeOut);
					}
				}
			}
			
		}
		
		/**
		 * Finish a thread function.
		 */
		public void stop() {
			stop = true;
		}
	}
	
	/**
	 * Send File.
	 *
	 * @param packets the packets
	 * @param remoteHostIP the remote host IP
	 * @param remoteHostPort the remote host port
	 * @param retryTimeOut the retry time out
	 * @param N the n
	 * @return void
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void sendFile(List<byte[]> packets, InetAddress remoteHostIP, int remoteHostPort, int retryTimeOut, int N) throws IOException{
		// create client socket
		clientSocket = new DatagramSocket();
		startTime = System.currentTimeMillis();
		timer = new Timer();
		//start receiving thread
		AckReceiver  ackReceiver = new AckReceiver(remoteHostIP, remoteHostPort, retryTimeOut);
		Thread AckThread = new Thread(ackReceiver);
		AckThread.start();
		
		while(nextseqnum<packets.size()){
			
			if(nextseqnum < base+N) {
				
//				//add sequence number to a packet
				byte[] currPacket = packets.get(nextseqnum);
				currPacket[1] = (byte) (nextseqnum >>8);
				currPacket[2] = (byte) (nextseqnum);  
				
				// add EOF bit to the last packet 
				if(nextseqnum+1 == packets.size())
					currPacket[0] =  1;
//				System.out.println(""); //
				
//				//udt_send(sendpkt[nextseqnum])
				sendPacket(currPacket, remoteHostIP, remoteHostPort);

				if(base == nextseqnum ) {
					synchronized(timer_lock) {
					reTransTask = new Timeout(remoteHostIP, remoteHostPort, retryTimeOut);  
					timer.schedule(reTransTask, retryTimeOut);
					}
					
				}
				nextseqnum++;
			}

		}
	
	}
	
	/**
	 * Send packet wrapper fucntion.
	 *
	 * @param packet the packet
	 * @param remoteHostIP the remote host IP
	 * @param remoteHostPort the remote host port
	 */
	private static void sendPacket(byte[] packet, InetAddress remoteHostIP, int remoteHostPort) {
		
		
		DatagramPacket sendPacket = 
				new DatagramPacket(packet,packet.length, remoteHostIP, remoteHostPort);
		try {
			clientSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws Exception the exception
	 */
	public static void main(String[] args) throws Exception{
		// validate the length of arguments passed
		Common.checkArgumentLength(args.length, 5);
		
		String remoteHost = args[0];
		int port = Integer.parseInt(args[1]);
		String fileName = args[2];
		int retryTimeOut = Integer.parseInt(args[3]);
		int windowSize = Integer.parseInt(args[4]);
		
		if(windowSize > Math.pow(2, PACKET_SIZE*8-1))
			throw new IllegalArgumentException("The Window size can't be greater than " + Math.pow(2, PACKET_SIZE*8-1));
		
		bchuncks = new ArrayList<byte[]>();
		//translate hostname to IP address using DNS
		InetAddress IPAddress = InetAddress.getByName(remoteHost);
		
		FileInputStream fileInputStream = null;
		File file =new File(fileName);
		long fileSize = file.length();
		try
		{
			fileInputStream = new FileInputStream(file);
			int i;
			long counter = 0;
			byte[]   sendDataBuffer;
			// loading the whole file into memory List in chunks
			do {

				if(counter+PAYLOAD_SIZE >fileSize) {
					sendDataBuffer = new byte[(int)(fileSize-counter)+HEADER_SIZE];
					i = fileInputStream.read(sendDataBuffer, HEADER_SIZE, (int)(fileSize-counter));
				}
				else {
					sendDataBuffer = new byte[PACKET_SIZE];
					i = fileInputStream.read(sendDataBuffer, HEADER_SIZE, PAYLOAD_SIZE);
				}
				bchuncks.add(sendDataBuffer);
				counter +=i;
			} while(i == PAYLOAD_SIZE);
			
			
			
			sendFile(bchuncks, IPAddress, port, retryTimeOut, windowSize);
			fileInputStream.close();
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(fileInputStream != null)
				fileInputStream.close();
		}
		
		
		// wait fort the last ack then calculate time and throughput
		synchronized(time_print_lock) {
			while(stopTime == 0) {
				time_print_lock.wait();
			}
			double timeTaken = ((double) (stopTime - startTime))/1000;
			double throughput = (fileSize/1024)/ timeTaken; 
			Common.print(""+Math.round(throughput));
		}
		
				

	}

}
