package model;

public class AckManager {

	private int ack;

	public AckManager(){
		this.ack = 0;
	}

	public int get() {
		return ack;
	}

	public void setAck(int seqnum) {
		ack = seqnum;
	}
	
	public void increment(){
		ack++;
	}

}
