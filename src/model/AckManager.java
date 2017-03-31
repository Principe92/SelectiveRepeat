package model;

public class AckManager {

	private int ack;

	public AckManager(){
		this.ack = 0;
	}

	public int get() {
		return ack;
	}

	public void setAck(int ack) {
		this.ack = ack;
	}
	
	public void increment(){
		ack++;
	}

}
