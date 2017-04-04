package model;

import main.Packet;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by okori on 31-Mar-17.
 */
public class PacketAcker {
    private final CheckSumManager checkSumManager;
    private final PacketAckerListener listener;
    private final Queue<Packet> queue;
    private SequenceManager bSeqManager;
    private int windowSize;

    public PacketAcker(PacketAckerListener listener, int windowSize) {
        this.checkSumManager = new CheckSumManager();
        this.listener = listener;
        this.bSeqManager = new SequenceManager(windowSize, 0);
        this.queue = new LinkedList<>();
        this.windowSize = windowSize;
    }

    /**
     * Method to handle ack of a packet
     *
     * @param packet - A new packet from A
     */
    public void ack(Packet packet) {
        // set default response to last ack packet
        Packet response = new Packet(0, bSeqManager.get(), 0);

        if (packet != null) {
            int checkSum = checkSumManager.getCheckSum(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());

            // Check for corruption
            if (checkSum == packet.getChecksum()) {

                // Check if sequence number is next inline
                if (bSeqManager.isCorrect(packet.getSeqnum())) {

                    // send to application layer
                    System.out.println(String.format("pkt #%d in order", packet.getSeqnum()));
                    sendToLayer5(packet);

                    // generate response
                    response = new Packet(0, bSeqManager.get(), 0);
                } else {
                    // Check if it is in our current window
                    if (Util.isInWindow(packet.getSeqnum(), bSeqManager.get(), windowSize)) {

                        // buffer it
                        System.out.println(String.format("pkt #%d out of order", packet.getSeqnum()));
                        queue.add(packet);
                    } else {
                        System.out.println(String.format("pkt #%d not in window and base window is #%d", packet.getSeqnum(), bSeqManager.get()));
                    }
                }
            } else {
                //    System.out.println("Packet is corrupted");
                return;
            }
        } else {
            //  System.out.println("Packet is corrupted");
            return;
        }

        System.out.println(String.format("ack #%d sent for pkt #%d", response.getAcknum(), packet.getSeqnum()));
        listener.sendAck(response);
    }

    /**
     * Method to send a message to the application layer and other in-order messages in the queue
     *
     * @param packet - Packet containing message for the application layer
     */
    private void sendToLayer5(Packet packet) {
        bSeqManager.increment();

        System.out.println(String.format("msg #%d sent to layer5", packet.getSeqnum()));
        listener.sendToApplication(packet.getPayload());

        if (!queue.isEmpty() && bSeqManager.isCorrect(queue.peek().getSeqnum())) {
            Packet pk = queue.remove();
            sendToLayer5(pk);
        }
    }

    /**
     * Class interface
     */
    public interface PacketAckerListener {
        /**
         * Method to send an ack
         *
         * @param packet - An ack
         */
        void sendAck(Packet packet);

        /**
         * Method to send a message to the application level
         *
         * @param payload - Message
         */
        void sendToApplication(String payload);
    }
}
