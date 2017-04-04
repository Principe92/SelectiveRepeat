package model;

import java.util.*;

/**
 * Created by okori on 01-Apr-17.
 */
public class Util {

    public static int packetsReceivedFromA;
    public static int messagesReceived;
    public static int ackReceived;
    public static int packetsSent;
    public static int delivered;
    public static int timerElapsed;
    public static int ackSent;
    public static int retransmit;

    public static Map<Integer, Long> times = new HashMap<>();
    public static int timerIndex;
    public static List<Long> finalTime = new ArrayList<>();

    public static boolean isInWindow(int seq, int baseSeq, int windowSize) {

        int count = windowSize;
        while (count != 0) {
            if (baseSeq == seq) return true;
            baseSeq = move(baseSeq, windowSize);
            count--;
        }

        return false;
    }

    public static int move(int seq, int windowSize) {
        return seq == (2 * windowSize) ? 1 : seq + 1;
    }

    public static void calculateTime(int acknum, long time) {


        Iterator it = times.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = (Map.Entry) it.next();

            if (entry.getKey() <= acknum) {

                Long finalT = time - entry.getValue();
                System.out.println(String.format("pkt #%d rtt is %d", entry.getKey(), finalT));
                finalTime.add(finalT);
                it.remove();
            }
        }
    }

    public static long getRTT(int msgSize) {
        long sum = 0;
        int size = finalTime.size();
        for (int i = 0; i < size; i++) {
            sum += Util.finalTime.get(i);
        }

        System.out.println(String.format("RTT's for %d packets", size));
        return sum / msgSize;
    }
}
