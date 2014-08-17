package com.srsoftware.allergyscan;

public class Barcode {
	private Long code;
	private static int step=0;

	public Barcode(Long code) {
		this.code=code;
	}
	
	public Barcode(String code) {
		this(Long.parseLong(code));
	}

	public static Barcode random(){
		return new Barcode(randomCode());
	}
	
	private static Long randomCode() {
		step++;
		switch (step){
		case 1: return 12345678L;
		case 2: return 23456789L;
		case 3: return 34567890L; 
		case 4: return 45678901L; 
		case 5: step=0; 
		}
		return 56789012L;

	}

	public Long get() {
		return code;
	}
	
	public String toString(){
		return ""+code;
	}
}
