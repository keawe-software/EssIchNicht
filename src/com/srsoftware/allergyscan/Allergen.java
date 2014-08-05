package com.srsoftware.allergyscan;

public class Allergen {
	public Integer local_id;
	public String name;
	public Integer aid;
	public boolean active;
	public Allergen(int local_id, int aid, String name, int active) {
		this.local_id=local_id;
		this.aid=aid;
		this.name=name;
		this.active=(active==1);
	}
	
	@Override
	public String toString() {		
		return name;
	}
}
