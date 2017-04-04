package main;

import model.*;

public class StudentNetworkSimulator extends NetworkSimulator {
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private final int msgSize;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    private CheckSumManager checkSumManager;
    private AckManager ackManager;
    private SequenceManager sequenceManager;
    private PacketSender packetSender;

    private PacketAcker packetAcker;

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = 2 * winsize;
        RxmtInterval = delay;
        this.msgSize = numMessages;
    }


    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {
        Util.messagesReceived++;

        // create packet
        int ackNumber = ackManager.get();
        int seqNumber = sequenceManager.get();
        int checkSum = checkSumManager.getCheckSum(seqNumber, ackNumber, message.getData());

        Packet packet = new Packet(seqNumber, ackNumber, checkSum, message.getData());
        packetSender.queuePacket(packet);

        // get sequence number and ack number ready for next packet
        ackManager.increment();
        sequenceManager.increment();
    }

    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
        Util.ackReceived++;

        if (packet != null) {
            Util.calculateTime(packet.getAcknum(), System.nanoTime());

            if (packetSender.isInWindow(packet.getAcknum())) {
                packetSender.moveWindow(packet);
            } else if (packetSender.lastAcked(packet)) {
                System.out.println(String.format("Retransmit pkt #%d due to duplicate", Util.move(packet.getAcknum(), WindowSize)));
                packetSender.retransmit(packet.getAcknum());
                stopTimer(A);
                startTimer(A, RxmtInterval);
            }
        }
    }

    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt() {
        Util.timerElapsed++;
        packetSender.onTimerElapse();
    }

    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit() {
        sequenceManager = new SequenceManager(WindowSize, 1);
        checkSumManager = new CheckSumManager();
        ackManager = new AckManager(WindowSize);
        packetSender = new PacketSender(new PacketSender.PacketListener() {
            @Override
            public void send(Packet packet) {
                Util.packetsSent++;
                StudentNetworkSimulator.this.toLayer3(A, packet);
            }

            @Override
            public void stopTimer() {
                StudentNetworkSimulator.this.stopTimer(A);
            }

            @Override
            public void startTimer() {
                StudentNetworkSimulator.this.startTimer(A, RxmtInterval);
            }

        }, WindowSize);
    }

    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet) {
        Util.packetsReceivedFromA++;
        packetAcker.ack(packet);
    }

    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit() {
        packetAcker = new PacketAcker(new PacketAcker.PacketAckerListener() {
            @Override
            public void sendAck(Packet packet) {
                Util.ackSent++;
                toLayer3(B, packet);
            }

            @Override
            public void sendToApplication(String payload) {
                Util.delivered++;
                toLayer5(payload);
            }

        }, WindowSize);
    }

    // Use to print final statistics
    protected void Simulation_done() {

        System.out.println(String.format("RTT is %d", Util.getRTT(msgSize)));

        System.out.println(String.format("Messages received layer5 %d", Util.messagesReceived));
        System.out.println(String.format("Ack received from B: %d", Util.ackReceived));
        System.out.println(String.format("Packet Sent to B: %d", Util.packetsSent));
        System.out.println(String.format("Packet received from A: %d", Util.packetsReceivedFromA));
        System.out.println(String.format("Messages delivered to layer5 at B: %d", Util.delivered));
        System.out.println(String.format("%d interrupt called", Util.timerElapsed));
        System.out.println(String.format("%d retransmitted", Util.retransmit));
    }

}
