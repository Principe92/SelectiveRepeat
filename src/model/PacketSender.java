package model;

import main.Packet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by okori on 30-Mar-17.
 */
public class PacketSender {

    private final PacketListener listener;
    private Queue<Packet> queue;
    private Map<Integer, Packet> buffer;
    private int windowSize;
    private int index;
    private int maxIndex;
    private int baseSeq;
    private int lastAcked;

    public PacketSender(PacketListener listener, int windowSize) {
        this.listener = listener;
        this.windowSize = windowSize;
        this.queue = new LinkedList<>();
        this.buffer = new HashMap<>();
        this.maxIndex = windowSize;
        this.baseSeq = 1;
    }

    /**
     * Method to check if an ack is for a packet in a window
     *
     * @param seq - Sequence number of ack
     * @return True, if an ack belongs to packet in a window
     */
    public boolean isInWindow(int seq) {
        return Util.isInWindow(seq, baseSeq, windowSize);
    }

    /**
     * Method to queue a packet or send it to layer3 immediately
     *
     * @param packet - New packet
     */
    public void queuePacket(Packet packet) {

        // Send packet if queue is empty
        // Queue arriving packets, if queue is not empty
        // Queue packet if it belongs to next window

        if (queue.isEmpty()) {
            if (index < maxIndex) {

                if (index == 0) {
                    listener.startTimer();
                }

                buffer.put(packet.getSeqnum(), packet);
                System.out.println(String.format("Sent pkt #%d", packet.getSeqnum()));
                listener.send(packet);

                index++;
                System.out.println(String.format("pkt #%d time at %d", packet.getSeqnum(), System.nanoTime()));
                Util.times.put(packet.getSeqnum(), System.nanoTime());

            } else {
                System.out.println(String.format("Queued pkt #%d", packet.getSeqnum()));
                queue.add(packet);
            }
        } else {
            System.out.println(String.format("Queued pkt #%d", packet.getSeqnum()));
            queue.add(packet);
        }
    }

    /**
     * Method to move window after receiving a correct ack
     *
     * @param packet - An ack
     */
    public void moveWindow(Packet packet) {

        // remove packet from buffer
        lastAcked = packet.getAcknum();
        removePacket(packet.getAcknum());

        System.out.println(String.format("Last acked is #%d", lastAcked));

        // move the base index of the window
        int newBase = Util.move(packet.getAcknum(), windowSize);
        int diff = getNewDifference(newBase);
        maxIndex += diff;
        baseSeq = newBase;

        // send unsent packets in window
        checkQueue();

        // stop & start timer
        resetTimer();
    }

    /**
     * Method to stop and start the timer
     */
    private void resetTimer() {
        listener.stopTimer();
        listener.startTimer();
    }

    /**
     * Method to send unsent packets in window
     */
    private void checkQueue() {
        while (index < maxIndex && !queue.isEmpty()) {

            Packet next = queue.remove();
            buffer.put(next.getSeqnum(), next);

            System.out.println(String.format("Sent pkt #%d from queue", next.getSeqnum()));
            listener.send(next);
            index++;

            System.out.println(String.format("pkt #%d time at %d", next.getSeqnum(), System.nanoTime()));
            Util.times.put(next.getSeqnum(), System.nanoTime());
        }
    }

    /**
     * Method to calculate horizontal displacement of window
     *
     * @param newBaseSeq - new base index of window
     * @return Horizontal displacement of window
     */
    private int getNewDifference(int newBaseSeq) {
        int num = baseSeq;
        int count = 0;

        while (num != newBaseSeq) {
            removePacket(num);
            num = Util.move(num, windowSize);
            count++;
        }

        return count;
    }

    /**
     * Method to remove a packet from the buffer
     *
     * @param key - Sequence number of packet
     */
    private void removePacket(int key) {
        if (buffer.containsKey(key))
            buffer.remove(key);
    }

    /**
     * Method to check if an ack is a duplicate
     *
     * @param packet - Packet from B
     * @return True if an ack is a duplicate. Else, false
     */
    public boolean lastAcked(Packet packet) {
        return packet.getAcknum() == lastAcked;
    }

    /**
     * Method to retransmit a given packet
     *
     * @param seq - Sequence number of the last acked packet in B
     */
    public void retransmit(int seq) {
        int nextSeq = Util.move(seq, windowSize);

        if (buffer.containsKey(nextSeq)) {
            Packet packet = buffer.get(nextSeq);

            Util.retransmit++;
            listener.send(packet);
        }

        checkQueue();
        //resetTimer();
    }

    /**
     * Method to handle timer interrupts
     */
    public void onTimerElapse() {
        System.out.println(String.format("Retransmit pkt #%d due to time elapse", Util.move(lastAcked, windowSize)));

        retransmit(lastAcked);
        listener.startTimer();
    }

    /**
     * Class interface
     */
    public interface PacketListener {
        /**
         * Method to send a packet to layer3
         *
         * @param packet - Packet to be sent
         */
        void send(Packet packet);

        /**
         * Method to stop the timer
         */
        void stopTimer();

        /**
         * Method to start the timer
         */
        void startTimer();
    }
}
