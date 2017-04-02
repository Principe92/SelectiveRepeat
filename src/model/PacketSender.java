package model;

import main.Packet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by okori on 30-Mar-17.
 */
public class PacketSender extends Thread {

    private final PacketListener listener;
    private final Object object;
    private Queue<Packet> queue;
    private Map<Integer, Packet> buffer;
    private boolean stop;
    private int windowSize;
    private int index;
    private int maxIndex;
    private int baseSeq;
    private int lastAcked;

    public PacketSender(PacketListener listener, int windowSize) {
        this.listener = listener;
        this.windowSize = windowSize;
        this.queue = new LinkedList<>();
        this.object = new Object();
        this.buffer = new HashMap<>();
        this.maxIndex = windowSize;
    }

    public boolean isInWindow(int seq) {
        return Util.isInWindow(seq, baseSeq, windowSize);
    }

    @Override
    public void run() {
        while (!stop) {
            synchronized (object) {
                if (index < maxIndex) {

                    int size = queue.size();

                    if (index < size) {

                        if (index == 0) {
                            listener.startTimer();
                        }

                        Packet packet = queue.remove();
                        buffer.put(packet.getSeqnum(), packet);
                        listener.send(packet);

                        index++;
                    }
                }
            }
        }
    }

    public void finish() {
        stop = true;
    }

    public void queuePacket(Packet packet) {
        synchronized (object) {
            this.queue.add(packet);
        }
    }

    private void removePacket(Packet packet) {
        removePacket(packet.getSeqnum());
    }

    public void moveWindow(Packet packet) {
        synchronized (object) {
            lastAcked = packet.getAcknum();
            removePacket(packet);

            int newBase = Util.move(packet.getAcknum(), windowSize);
            int diff = getNewDifference(newBase);
            maxIndex += diff;
            baseSeq = newBase;

            listener.stopTimer();
            listener.startTimer();
        }
    }

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

    private void removePacket(int key) {
        if (buffer.containsKey(key)) {
            buffer.remove(key);
        }
    }

    public boolean lastAcked(Packet packet) {
        return packet.getAcknum() == lastAcked;
    }

    public void retransmit(int acknum) {
        int next = Util.move(acknum, windowSize);

        if (buffer.containsKey(next)) {
            Packet packet = buffer.get(next);
            listener.send(packet);
        }
    }

    public void onTimerElapse() {
        retransmit(lastAcked);
    }

    public interface PacketListener {
        void send(Packet packet);

        void stopTimer();

        void startTimer();
    }
}
