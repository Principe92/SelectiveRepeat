package model;

public class AckManager {

	private final int windowSize;
	private int ack;

	public AckManager(int windowSize) {
		this.windowSize = windowSize;
		this.ack = 1;
	}

	public int get() {
		return ack;
	}
	
	public void increment(){
		ack = Util.move(ack, windowSize);
	}

}
