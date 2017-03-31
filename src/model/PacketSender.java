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
    private int end;

    public PacketSender(PacketListener listener, int windowSize) {
        this.listener = listener;
        this.windowSize = windowSize;
        this.queue = new LinkedList<>();
        this.object = new Object();
        this.buffer = new HashMap<>();
        this.maxIndex = windowSize;
        this.end = windowSize;
    }

    public boolean isInWindow(int seq) {
        synchronized (object) {
            return buffer.containsKey(seq);
        }
    }

    @Override
    public void run() {
        while (!stop) {
            synchronized (object) {
                if (index < maxIndex) {

                    int size = queue.size();

                    if (index < size) {
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
        if (buffer.containsKey(packet.getSeqnum())) {
            buffer.remove(packet.getSeqnum());
        }

    }

    public void moveWindow(Packet packet) {
        synchronized (object) {
            removePacket(packet);


            // If base sequence of window arrived last, move the entire window by window size
            // Else just move by 1
            if (packet.getSeqnum() == baseSeq) {
                maxIndex = buffer.isEmpty() ? end + windowSize : ++maxIndex;
                baseSeq = move(baseSeq);
                end = maxIndex;
            } else {
                maxIndex++;
            }
        }
    }

    private int move(int seq) {
        return seq == (2 * windowSize) ? 1 : ++seq;
    }

    public interface PacketListener {
        void send(Packet packet);
    }
}
