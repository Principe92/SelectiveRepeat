package model;

public class CheckSumManager {

	public int getCheckSum(int seqNumber, int ackNumber, String payload) {
		return seqNumber + ackNumber + getNumber(payload);
	}

	private int getNumber(String message) {
		
		int size = message.length();
		int sum = 0;
		
		for (int i = 0; i < size; i++){
			Character c = message.charAt(i);
			sum += Character.getNumericValue(c);
		}
		
		return sum;
	}
}
