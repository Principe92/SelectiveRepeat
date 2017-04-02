package model;

/**
 * Created by okori on 01-Apr-17.
 */
public class Util {

    public static int packetsReceivedFromA;
    public static int messagesReceived;
    public static int ackReceived;
    public static int packetsSent;
    public static int delivered;

    public static boolean isInWindow(int seq, int baseSeq, int windowSize) {

        while (windowSize != 0) {
            if (baseSeq == seq) return true;
            baseSeq = move(baseSeq, windowSize);
            windowSize--;
        }

        return false;
    }

    public static int move(int seq, int windowSize) {
        return seq == (2 * windowSize) ? 1 : ++seq;
    }
}
