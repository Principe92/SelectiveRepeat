package model;

public class SequenceManager {

    private int seq;
    private int limit;

    public SequenceManager(int limit) {
        this.seq = 1;
        this.limit = limit;
    }

    public int get() {
        return seq;
    }

    public void increment() {
        seq = seq == limit ? 1 : ++seq;
    }

    public boolean isCorrect(int seqnum) {
        return seq == seqnum;
    }
}
