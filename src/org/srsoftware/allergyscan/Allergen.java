package org.srsoftware.allergyscan;

public class Allergen {
	public String name;
	public Integer aid;
	public boolean active;
	public Allergen(int aid, String name, int active) {
		this.aid=aid;
		this.name=name;
		this.active=(active==1);
	}
	
	@Override
	public String toString() {		
		return name;
	}
}
