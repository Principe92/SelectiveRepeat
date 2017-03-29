package model;

public class SequenceManager {
	
	private int number;
	private int limit;

	public SequenceManager(int limit){
		this.number = 1;
		this.limit = limit;
	}

	public int get() {
		return number;
	}
	
	public void increment(){
		number = number == limit ? 1 : number++;
	}

}
