/* Name: Yang Yang, yya123 */

/**
 * @author mhefeeda
 *
 */

package rdt;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

class TimeoutHandler extends TimerTask {
	public static int flag;
	RDTBuffer sndBuf;
	RDTSegment seg; 
	DatagramSocket socket;
	InetAddress ip;
	int port;
	private static long timer = 0;

	
	TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock, 
			InetAddress ip_addr, int p) {
		sndBuf = sndBuf_;
		seg = s;
		socket = sock;
		ip = ip_addr;
		port = p;
		flag = 0;
	}



	public void run() {

		// System.out.printf("Timer now starts at %d",System.currentTimeMillis());

		if(flag < sndBuf.size) {
			System.out.println(System.currentTimeMillis()+ ":Timeout for seg: " + seg.seqNum);
			sndBuf.nextSeg = sndBuf.base;
			for (int j = sndBuf.base; j < sndBuf.base + sndBuf.size; j++) {
				System.out.printf("\nI'm trying to resend..");
				RDTSegment resend = sndBuf.getNext();
				Utility.udp_send(resend, socket, ip, port);
			}
			return;
		}
		else{
			return;
		}
		// System.out.flush();

/*
		// complete
		switch(RDT.protocol){
			case RDT.GBN:
				timer = System.currentTimeMillis();
				if (System.currentTimeMillis() - timer > 500) {
					sndBuf.nextSeg = sndBuf.base;
					// for (int i = 0; i < sndBuf)
					return;
				}
				//break;
			case RDT.SR:
				break;
			default:
				System.out.println("Error in TimeoutHandler:run(): unknown protocol");
		}
*/
	}
} // end TimeoutHandler class

