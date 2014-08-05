package de.srsoftware.allergyscan;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public class AllergenList {
	private TreeMap<Integer, Allergen> allergens=new TreeMap<Integer, Allergen>();
	private TreeSet<Integer> enabledAllergens=new TreeSet<Integer>();

	public void put(int local_aid, Allergen allergen) {
		allergens.put(local_aid, allergen);
		if (allergen.active){
			enabledAllergens.add(local_aid);
		}
	}

	public boolean isEmpty() {
		return allergens.isEmpty();
	}

	public Set<Integer> keySet() {
		return allergens.keySet();
	}

	public Allergen get(int aid) {
		return allergens.get(aid);
	}

	public Vector<Allergen> values() {
		return new Vector<Allergen>(allergens.values());
	}

	public int size() {
		return allergens.size();
	}

	public Set<Entry<Integer, Allergen>> entrySet() {
		return allergens.entrySet();
	}
}
