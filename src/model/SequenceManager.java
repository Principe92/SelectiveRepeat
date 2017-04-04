package model;

public class SequenceManager {

    private int seq;
    private int windowSize;

    public SequenceManager(int windowSize, int start) {
        this.seq = start;
        this.windowSize = windowSize;
    }

    public int get() {
        return seq;
    }

    public void increment() {
        seq = Util.move(seq, windowSize);
    }

    public boolean isCorrect(int seqnum) {
        return Util.move(seq, windowSize) == seqnum;
    }
}
