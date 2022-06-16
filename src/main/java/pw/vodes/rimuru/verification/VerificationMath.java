package pw.vodes.rimuru.verification;

import pw.vodes.rimuru.Util;

public class VerificationMath {
	
	private String message;
	private int result;
	
	public VerificationMath() {
		var num1 = Util.random.nextInt(80) + 10;
		var num2 = Util.random.nextInt(8) + 1;
		var num3 = Util.random.nextInt(20) + 1;
		while(getDoubleValue(num1, num2, num3) != Double.valueOf((int)getDoubleValue(num1, num2, num3))) {
			num1 = Util.random.nextInt(80) + 10;
			num2 = Util.random.nextInt(8) + 1;
			num3 = Util.random.nextInt(20) + 1;
		}
		
		message = String.format("What is %d / %d - %d?", num1, num2, num3);
		result = (int)getDoubleValue(num1, num2, num3);
	}
	
	private double getDoubleValue(int num1, int num2, int num3){
		return ((double)num1 / (double)num2) - (double)num3;
	}
	
	public String getMessage() {
		return message;
	}
	
	public int getResult() {
		return result;
	}

}
