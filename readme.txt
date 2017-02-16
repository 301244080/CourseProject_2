/* Name: Yang Yang, yya123 */

In this project I almost completed Go-back-n protocol.

The issues in this program are:

1. My code could only run when lossRate for both client and server is 0.0 beacause I failed to implement the timeout scheduling and retransmission.
The client can send data packages and receive acks normally, and in the meantime
the server can receive and read data packages and send acks as long as we have 0.0 lossRate.

2. Use GBN method only beacause I failed to implement SR method.


I developed and tested my code on IntelliJ IDE.

I did not change the argument of existing funcion so testing my code on the command line should be the same as others.



//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
(you can ignore the paragraph below)
(To be honest I spent like over 40 hours on this project, it is an interesting project actually, I just realized to
write the readme when there is only 1 hour left before the due time...I was still trying to debugg..
I'm pretty sure I can successfully implement the complete GBN protocol but time does not allow me to do so...
Finally I do learn a lot from this project although I did not finish the requirements of the project.  )

Thank you for your time and patience.
