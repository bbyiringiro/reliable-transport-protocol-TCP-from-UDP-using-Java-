import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * The Class Sender2b.
 * Sender for Selective Repeat algorithm
 * 
 */
public class Sender2b {

	/** The Constant PACKET_SIZE. */
	final static int PACKET_SIZE = 1027;
	
	/** The Constant PAYLOAD_SIZE. */
	final static int PAYLOAD_SIZE = 1024;
	
	/** The Constant HEADER_SIZE. */
	final static int HEADER_SIZE = 3;
	
	/** The Constant TRIALLIMIT. */
	final static int TRIALLIMIT = 10;
	
	/** The bchuncks. */
	static List<byte[]> bchuncks;
	
	/** The base. */
	static volatile int base = 0;
	
	/** The nextseqnum. */
	static int nextseqnum = 0;
	
	/** The lock 1. */
	public static volatile Object lock1 = new Object();
	
	/** The time print lock. */
	public static volatile Object time_print_lock = new Object();
	
	/** The timer. */
	public static  volatile Timer timer;
	
	/** The re trans map. */
	public static  volatile Map<Integer, ReTransTask> reTransMap;
	
	/** The client socket. */
	public static DatagramSocket clientSocket;
	
	
	
	/** The retrans. */
	static int retrans =0;
	
	
	
	/** The start time. */
	public static long startTime;
	
	/** The stop time. */
	public static long stopTime;
	
	
	
	/**
	 * The Class ReTransTask.
	 * Handles the reset of packets on timeouts 
	 */
	public static class ReTransTask extends TimerTask {
		
		/** The packet. */
		private DatagramPacket packet;
		
		/** The socket. */
		private DatagramSocket socket;
		
		/** The n. */
		int n;
		
		/**
		 * Instantiates a new re trans task.
		 *
		 * @param _socket the socket
		 * @param _packet the packet
		 */
		public ReTransTask(DatagramSocket _socket,DatagramPacket _packet) {
			this.socket = _socket;
			this.packet = _packet;

			this.n = ((_packet.getData()[1] & 0xFF)  << 8) + (_packet.getData()[2] & 0xFF);
		}
		
		/* (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
		public void run(){
				++retrans;
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	
	/**
	 * The Class AckReceiver.
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
		
		/** The n. */
		int N;
		
		/** The stop. */
		public boolean stop = false;
		
		/**
		 * Instantiates a new ack receiver.
		 *
		 * @param IP the ip
		 * @param port the port
		 * @param timer the timer
		 * @param winSize the win size
		 */
		public AckReceiver(InetAddress IP, int port, int timer, int winSize) {
			this.remoteHostIP = IP;
			this.remoteHostPort = port;
			this.retryTimeOut = timer;
			this.N = winSize;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {

				int temp_rcv;
				while(true) {
	//				 rdt_rcv(rcvpkt) & notcorrupt (rcvpkt
					DatagramPacket receivePacket = 
							new DatagramPacket(receiveACK, receiveACK.length);
					try {
						clientSocket.receive(receivePacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
					temp_rcv = ((receivePacket.getData()[0] & 0xFF)  << 8) + (receivePacket.getData()[1] & 0xFF);
					synchronized (lock1) {
					if(reTransMap.get(temp_rcv) !=null && temp_rcv >= base && temp_rcv <=base+N)
					{
						reTransMap.get(temp_rcv).cancel();
						reTransMap.put(temp_rcv, null);
						if(temp_rcv == base)
						{
							for(int i = temp_rcv+1; i<= base+N; ++i) {
								if(reTransMap.get(i) != null || !reTransMap.containsKey(i)) {
									base = i; 
									break;
								}
							}
							
						}
					
						
						
						if(base == bchuncks.size())
						{
							timer.cancel();
							synchronized(time_print_lock) {
							stopTime=System.currentTimeMillis();
							time_print_lock.notify();
							}
							return;
						}

					}
				}
				}	
			
		}
		
		/**
		 * Stop.
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
		
		reTransMap = new HashMap<Integer, ReTransTask>();
		timer = new Timer();
		AckReceiver  ackReceiver = new AckReceiver(remoteHostIP, remoteHostPort, retryTimeOut, N);
		Thread AckThread = new Thread(ackReceiver);
		AckThread.start();
		
		while(nextseqnum<packets.size()){
			if(nextseqnum < base+N) {

				byte[] currPacket = packets.get(nextseqnum);
				currPacket[1] = (byte) (nextseqnum >>8);
				currPacket[2] = (byte) (nextseqnum);  
				
				
				if(nextseqnum+1 == packets.size())
					currPacket[0] =  1;
				DatagramPacket sendPacket = 
						new DatagramPacket(currPacket,currPacket.length, remoteHostIP, remoteHostPort);
				ReTransTask tempTask = new ReTransTask(clientSocket, sendPacket);
				//timer locker for cancelling
				synchronized (lock1) {
				try {
					clientSocket.send(sendPacket);
					timer.scheduleAtFixedRate(tempTask,retryTimeOut, retryTimeOut);
					reTransMap.put(nextseqnum, tempTask);
				} catch (IOException e) {
					e.printStackTrace();
				}
				}
				
				nextseqnum++;
			}

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
