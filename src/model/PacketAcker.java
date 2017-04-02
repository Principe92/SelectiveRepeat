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
    private Object object;
    private int windowSize;

    public PacketAcker(PacketAckerListener listener, int windowSize) {
        this.checkSumManager = new CheckSumManager();
        this.listener = listener;
        this.bSeqManager = new SequenceManager(windowSize);
        this.queue = new LinkedList<>();
        this.object = new Object();
        this.windowSize = windowSize;
    }

    public void ack(Packet packet) {
        Packet response = new Packet(0, bSeqManager.get(), 0);

        if (packet != null) {
            int checkSum = checkSumManager.getCheckSum(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());

            if (checkSum == packet.getChecksum()) {
                // to layer 5 while we respond with an ACK

                // if we have the correct sequence number
                if (bSeqManager.isCorrect(packet.getSeqnum())) {

                    sendToLayer5(packet);

                    response = new Packet(0, packet.getSeqnum(), 0);
                } else {
                    if (Util.isInWindow(packet.getSeqnum(), bSeqManager.get(), windowSize)) {

                        // buffer
                        queue.add(packet);
                    }
                }
            }
        }

        listener.sendAck(response);
    }

    private void sendToLayer5(Packet packet) {
        listener.sendToApplication(packet.getPayload());
        bSeqManager.increment();

        if (!queue.isEmpty() && bSeqManager.isCorrect(queue.peek().getSeqnum())) {
            Packet pk = queue.remove();
            sendToLayer5(pk);
        }
    }

    public interface PacketAckerListener {
        void sendAck(Packet packet);

        void sendToApplication(String payload);
    }
}
