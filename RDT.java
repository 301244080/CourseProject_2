/* Name: Yang Yang, yya123 */
/**
 * @author mohamed
 *
 */
package rdt;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
import java.util.ArrayList;

public class RDT {

	// public static final int MSS = 100; // Max segement size in bytes
	// public static final int RTO = 500; // Retransmission Timeout in msec
	public static final int ERROR = -1;
	public static final int MAX_BUF_SIZE = 3;
	public static final int WINDOW_SIZE = 1; // Set window size as 1
	public static final int GBN = 1;   // Go back N protocol
	public static final int SR = 2;    // Selective Repeat
	// public static final int protocol = GBN;

	public static int protocol = GBN;
	public static int MSS = 100; // Max segement size in bytes
	public static int RTO = 500; // Retransmission Timeout in msec


	public static double lossRate = 0.0;
	public static Random random = new Random(); 
	public static Timer timer = new Timer();	
	
	private DatagramSocket socket; 
	private InetAddress dst_ip;
	private int dst_port;
	private int local_port; 
	
	private RDTBuffer sndBuf;
	private RDTBuffer rcvBuf;
	
	private ReceiverThread rcvThread;


	
	
	RDT (String dst_hostname_, int dst_port_, int local_port_) 
	{
		local_port = local_port_;
		dst_port = dst_port_; 
		try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }
		sndBuf = new RDTBuffer(MAX_BUF_SIZE);
		if (protocol == GBN)
			rcvBuf = new RDTBuffer(1);
		else 
			rcvBuf = new RDTBuffer(MAX_BUF_SIZE);
		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port);
		rcvThread.start();
	}

	public static void setMSS(int mss) {MSS = mss;}
	public static void setProtocol(int protocol_) {protocol = protocol_;}
	public static void setRTO(int rto) {RTO = rto;}

	RDT (String dst_hostname_, int dst_port_, int local_port_, int sndBufSize, int rcvBufSize)
	{
		local_port = local_port_;
		dst_port = dst_port_;
		 try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }
		sndBuf = new RDTBuffer(sndBufSize);
		if (protocol == GBN)
			rcvBuf = new RDTBuffer(1);
		else 
			rcvBuf = new RDTBuffer(rcvBufSize);
		
		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port);
		rcvThread.start();
	}
	
	public static void setLossRate(double rate) {lossRate = rate;}
	
	// called by app
	// returns total number of sent bytes  
	public int send(byte[] data, int size) {

		//****** complete
		int i = 0; // data index
		//System.out.println("data size: " + size + " bytes");
		int lastSeqNum = (int) Math.ceil((double) size / MSS);
		//System.out.println("Number of Segments: " + lastSeqNum);
		// divide data into segments
		sndBuf = new RDTBuffer(MAX_BUF_SIZE);
		RDTSegment[] SegmentSeq = new RDTSegment[lastSeqNum];

		// divide data into segments
		int segmentIndex = 0; // to track the index in the RDTSegment array
		while(i < size) { // size = length of all data
			int j = 0; // index in RDTSegment array
			byte[] tempSeg = new byte[MSS]; // initialize a temp array to store the data
			while(true) {
				tempSeg[j] = data[i]; // copy data to a temp array
				i++;
				j++;
				if (j >= MSS || i >= size){
					SegmentSeq[segmentIndex] = new RDTSegment(tempSeg, j, segmentIndex, lastSeqNum);
					segmentIndex ++;
					// System.out.println("Segment index is: " + segmentIndex + "\n");
					// System.out.printf("%d", segmentIndex);
					break;
				}
			}
		}

		int currentIndex = 0;
		int buffSize = 0;
		// TimeoutHandler[] Timeout = new TimeoutHandler[sndBuf.size];
		// int timeoutIndex = 0;
		// RDTSegment gbntime = new RDTSegment();
		while (sndBuf.nextSeg < lastSeqNum) {
			while (buffSize < MAX_BUF_SIZE && currentIndex < lastSeqNum) {
				sndBuf.putNext(SegmentSeq[currentIndex]);
				buffSize++;
				currentIndex++;
			}
			// send using udp_send()
			if (sndBuf.nextSeg < currentIndex) {
				RDTSegment temp_ = sndBuf.getNext();
				Utility.udp_send(temp_, socket, dst_ip, dst_port);
				// TimeoutHandler timeOut = new TimeoutHandler(sndBuf,temp_, socket, dst_ip, dst_port);
				// timer.scheduleAtFixedRate(timeOut,0,RTO);				// Timeout[timeoutIndex%sndBuf.size] = new TimeoutHandler(sndBuf,gbntime,socket,);
			}
			// waiting for ack from the receiver
			byte[] ack = new byte[MSS];

			DatagramPacket ackSeg = new DatagramPacket(ack,MSS);
			try{
				// socket.setSoTimeout(RTO);

				socket.receive(ackSeg);
			} catch (Exception e) {
				System.out.println("udp_receive by sender: " + e);
			}

			// long temp_time=System.currentTimeMillis();
			byte[] data_ = ackSeg.getData();
			//int size_ = data.length;
			int ackNum = Utility.byteToInt(data_,4);
			if (sndBuf.base == ackNum){
				sndBuf.base++;
				buffSize--;
				try {
					sndBuf.semEmpty.release();
					sndBuf.semFull.acquire();
					//System.out.println("Ack received!");
				} catch (InterruptedException e) {
					System.out.println("Wrong" + e);
				}
			}
			// System.out.printf("TimeChange%d\n",(int)(System.currentTimeMillis()-temp_time));
		}
		return size;
	}
	
	
	// called by app
	// receive one segment at a time
	// returns number of bytes copied in buf
	public int receive (byte[] buf, int size)
	{
		// *****  complete
		int size_ = 0;
		int header = 24;
		int current = 0;
		int total_segment = (buf.length)/size;
		int fixedMax = 0;
		while (current < total_segment) {
			// System.out.printf("SIZE + HEADER: %d\n", size+header);
			byte[] temp =new byte[size+header];
			DatagramPacket packet = new DatagramPacket(temp, size+header);
			// System.out.printf("start receiving acks\n");
			try {
				// socket.setSoTimeout(RTO);
				socket.receive(packet);
			} catch (Exception e) {
				System.out.println("udp_receive: " + e);
			}
			byte[] data_ = packet.getData();
			//System.out.printf("Data length: %d", data_.length);
			//System.out.println("segment received");
			// RDTSegment segment = new RDTSegment(data_, size_, 0);
			//for (int i = 0; i < size_; i++)
			//	System.out.printf("%d", data_[i]);

			int seqNum = Utility.byteToInt(data_, 0);
			int ackNum_ = Utility.byteToInt(data_, 4);
			int flag = Utility.byteToInt(data_, 8);
			// int checkSum = Utility.byteToInt(data_, 12);
			// int rcvWin = Utility.byteToInt(data_, 16);
			int length = Utility.byteToInt(data_, 20);

			//System.out.printf("\nseqNum: %d, ack Num: %d, length: %d, flag: %d\n", seqNum, ackNum_, length, flag);

			fixedMax = seqNum % total_segment ;
			if (current == fixedMax) {
				RDTSegment ackSeg = new RDTSegment(seqNum);
				++current;
				//System.out.println("Sending Ack");
				Utility.udp_send(ackSeg, socket, dst_ip, dst_port); // send ack
				if (flag == 1){
					//System.out.printf("last packet received!\n");
					// break;
				}
				// System.out.printf("\nseqNum: %d, ack Num: %d", seqNum, ackNum_);
				//System.out.println("==");
			} else if (current > fixedMax) {
				RDTSegment ackSeg = new RDTSegment(current);
				//System.out.println("Wrong Packet Received");
				//System.out.println("Sending Ack");
				Utility.udp_send(ackSeg, socket, dst_ip, dst_port); // send ack
				// System.out.printf("\nseqNum: %d, ack Num: %d", seqNum, ackNum_);
				// System.out.println(">");
			}
			for (int i = header; i < header + length; i++){
				buf[size_++] = data_[i];
			}
			if (flag ==1) {
				break;
			}
		}

		return size_;   // fixed
	}
	
	// called by app
	public void close() {
		// OPTIONAL: close the connection gracefully
		// you can use TCP-style connection termination process
	}
	
}  // end RDT class 


class RDTBuffer {
	public RDTSegment[] buf;
	public int size;	
	public int base;
	public int next;
	public int nextSeg;
	public Semaphore semMutex; // for mutual execlusion
	public Semaphore semFull; // #of full slots
	public Semaphore semEmpty; // #of Empty slots
	
	RDTBuffer (int bufSize) {
		buf = new RDTSegment[bufSize];
		for (int i=0; i<bufSize; i++)
			buf[i] = null;
		size = bufSize;
		base = next = 0;
		semMutex = new Semaphore(1, true);
		semFull =  new Semaphore(0, true);
		semEmpty = new Semaphore(bufSize, true);
	}

	
	
	// Put a segment in the next available slot in the buffer
	public void putNext(RDTSegment seg) {		
		try {
			semEmpty.acquire(); // wait for an empty slot 
			semMutex.acquire(); // wait for mutex 
				buf[next%size] = seg;
				next++;  
			semMutex.release();
			semFull.release(); // increase #of full slots
		} catch(InterruptedException e) {
			System.out.println("Buffer put(): " + e);
		}
	}
	
	// return the next in-order segment
	public RDTSegment getNext() {
		nextSeg++;
		// **** Complete
		return buf[(nextSeg-1)%size];

		//return null;  // fix
	}
	
	// Put a segment in the *right* slot based on seg.seqNum
	// used by receiver in Selective Repeat
	public void putSeqNum (RDTSegment seg) {
		// ***** compelte

	}
	
	// for debugging
	public void dump() {
		System.out.println("Dumping the receiver buffer ...");
		// Complete, if you want to 
		
	}
} // end RDTBuffer class



class ReceiverThread extends Thread {
	RDTBuffer rcvBuf, sndBuf;
	DatagramSocket socket;
	InetAddress dst_ip;
	int dst_port;
	
	ReceiverThread (RDTBuffer rcv_buf, RDTBuffer snd_buf, DatagramSocket s, 
			InetAddress dst_ip_, int dst_port_) {
		rcvBuf = rcv_buf;
		sndBuf = snd_buf;
		socket = s;
		dst_ip = dst_ip_;
		dst_port = dst_port_;
	}	
	public void run() {
		
		// *** complete 
		// Essentially:  while(cond==true){  // may loop for ever if you will not implement RDT::close()  
		//                socket.receive(pkt)
		//                seg = make a segment from the pkt
		//                verify checksum of seg
		//	              if seg contains ACK, process it potentailly removing segments from sndBuf
		//                if seg contains data, put the data in rcvBuf and do any necessary 
		//                             stuff (e.g, send ACK)
		//
	}
	
	
//	 create a segment from received bytes 
	void makeSegment(RDTSegment seg, byte[] payload) {
	
		seg.seqNum = Utility.byteToInt(payload, RDTSegment.SEQ_NUM_OFFSET);
		seg.ackNum = Utility.byteToInt(payload, RDTSegment.ACK_NUM_OFFSET);
		seg.flags  = Utility.byteToInt(payload, RDTSegment.FLAGS_OFFSET);
		seg.checksum = Utility.byteToInt(payload, RDTSegment.CHECKSUM_OFFSET);
		seg.rcvWin = Utility.byteToInt(payload, RDTSegment.RCV_WIN_OFFSET);
		seg.length = Utility.byteToInt(payload, RDTSegment.LENGTH_OFFSET);
		//Note: Unlike C/C++, Java does not support explicit use of pointers! 
		// we have to make another copy of the data
		// This is not effecient in protocol implementation
		for (int i=0; i< seg.length; i++)
			seg.data[i] = payload[i + RDTSegment.HDR_SIZE]; 
	}
	
} // end ReceiverThread class

