Project Description

In this project, you will implement in Java a Reliable Data Transfer protocol (RDT) that provides reliability (and optionally flow control) to applications using it.  The reliability is achieved that using the Sliding Window protocol discussed in class. You will implement the Go-Back-N version with no buffer at the receiver. You will implement the Selective Repeat version.  As a transport protocol, RDT is supposed to be implemented in the OS Kernel and run over the unreliable IP protocol. That would be complex, and would require changing the OS code. Instead, RDT will run as an application-layer protocol over UDP, adds very little functionality to IP: checksum and multiplexing/demultiplexing. 

The following pseudo code shows how an application (sender and receiver) would use RDT.

//  Sender  
…………
RDT rdt = new RDT (dst_hostname, dst_port, local_port);
int  ret = rdt.send (data, size);  // data is an array of  bytes
// rdt.send() may be called arbitrary number of times by the app
……
rdt.close();  
 
//    Receiver  
…………
RDT rdt = new RDT (src_hostname, src_port, local_port);
int  ret  = rdt.receive (buffer, size);   
// rdt.receive() may be called arbitrary number of times by the app
……
rdt.close();  
 
Suggested Steps

1.Download and unzip this source code.  This will create a directory called rdt, which is a java package containing the following classes:
o   RDT.java: contains the RDT class which has the send() and receive() functions called by the application. There are two additional private classes: RDTBuffer and ReceiverThread. RDTBuffer contains a partial implementation of a synchronized shared buffer using counting semaphores.  (Overview of Semaphores in Java) . The ReceiverThread class implements a thread that concurrently runs with the send thread. An instance of the ReceiverThread continually waits on the socket to process the incoming data.
o   RDTSegment.java:  defines the structure of RDT segments as well as some supporting methods.
o   TimeoutHandler.java: a class to handle timeouts.
o   Utility.java: provides several useful functions, including the upd_send().
o   TestClient.java: a sample test client.
o   TestServer.java: a sample test server.
2.     Start by compiling and testing TestClient and TestServer. They should compile fine, but, of course, would not work properly.
3.     Implement and test the Go-Back-N protocol with no buffer at the receiver (actually with a buffer of size 1), as described in the textbook. You should test your code carefully, consider several cases such as: various packet loss rates and different send buffer sizes including one (which is the alternating-bit protocol).
4.     Implement the Selective Repeat protocol as described in the textbook.
 

Notes

1.     The code given to you as well as the comments inside it are just to help you out. You are welcome to change whatever you want, or even come up with a new design. The bottom line is: your design must provide the same interface to applications. Otherwise, we would not be able to test it.
2.     The constants defined in the code given to you are just for our own testing. You can (and should) set them to appropriate values during your testing.
3.     You do not need to implement the connection setup phase. Do it manually: (i) run the server first with the client_hostname, client_port, and server_local_port arguments, then (ii) run the client with server_hostname, server_port, and client_local_port arguments. For testing, you can run both client and server on the same machine (i.e., the host name is: localhost and the IP address is 127.0.0.1). 
4.      You may close the connection manually (see Bonus 1).
 
Bonus 1: Graceful Connection Termination  [up to 5 points]

Close the connection gracefully, use TCP-style connection teardown. You need to  implement a few test cases and demonstrate them to the TA
 

Bonus 2: Add Flow Control to the Selective Repeat Protocol [up to 5 points].  

Use the RcvWin field in the RDTSegment to implement flow control. You need to implement a few test cases and demonstrate them to the TA. You need only to implement the simple flow control method described in class: At any time, the sender can not have more than RcvWin number of sent but yet-to-be-acknowledged segments. RcvWin is set by the receiver.  (As you know, this is different from the more complex congestion control.) 

 

Bonus 3: Performance Comparison Between Go-Back-N and Selective Repeat [up to 10 points].

Intuitively, the Go-Back-N (GBN) protocol suffers performance inefficiency, because one lost packet may require a large amount of retransmissions. This indeed is one of the driving forces of Selective Repeat (SR) protocol. Since we have implemented both schemes in our RDT design, we can further examine and compare their performance.

To design a reasonable experimental study, we need to specify the performance metrics that we are interested in and define all possible input parameters that may impact them.  Common performance metrics:

·       Delay: the time difference between the transport layer accepting data from upper layer at the sender and the transport layer passing the data to upper layer at the receiver.
·       Goodput (useful throughput):  Throughput is defined as the number of bits arriving at the receiver per unit of time. One problem of using throughput as a performance measure is that it does not take  re-transmissions into account. For example: in Go-Back-N, we may retransmit many redundant packets, which are still counted toward the traditional throughput definition. These redundant bits are not useful for the application. A more meaningful measure is known as “Goodput”, which counts the number of bits passed to upper layer here. This basically means that we ignore all corrupted packets, un-necessary retransmissions, and headers.
·       Overhead: There are three aspects of overhead: communication, processing, and storage. Communication overhead has direct relationship with Goodput measurement. Processing and storage (memory) overhead comes with the additional complexity of the Selective Repeat protocol, which is its main drawback.
These performance metrics depend on several input parameters including:

·       Packet size
·       Window size
·       Packet loss rate
·       Retransmission timeout
·       Channel bandwidth
·       Round trip time
Now, we have many (input) parameters and (output) performance metrics. A good way of conducting experiments is to consider the impact of varying only one input parameter on all output metrics while keeping all other input parameters fixed at reasonable values.  For instance, we vary packet loss rate between 0.0 and 0.8 and we compare the delay, goodput, and overhead of GBN and SR. We fix the following parameters: packet size at 512 bytes, window size at 12, retransmission timeout at 500 ms, RTT at 100 msec, and channel bandwidth at infinity. You need to send enough packets to compute meaningful performance metrics.

Your task is to design a few experiments, conduct them, analyze the results (use plots), and write your findings in a 1-2 page technical report. Your grade will be determined by the nature of the experiments you conduct and the insights you can derive from them.    
 
