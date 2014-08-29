package com.srsoftware.allergyscan;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public class AllergenList {
	private TreeMap<Integer, Allergen> allergens=new TreeMap<Integer, Allergen>();
	private TreeSet<Integer> enabledAllergens=new TreeSet<Integer>();

	public void put(Allergen allergen) {
		allergens.put(allergen.local_id, allergen);
		if (allergen.active){
			enabledAllergens.add(allergen.local_id);
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
		Vector<Allergen> result = new Vector<Allergen>(allergens.values());
		Collections.sort(result);
		return result;
	}

	public int size() {
		return allergens.size();
	}

	public Set<Entry<Integer, Allergen>> entrySet() {
		return allergens.entrySet();
	}

	public Vector<Allergen> selectByLocalAId(TreeSet<Integer> ids) {
		Vector<Allergen> result=new Vector<Allergen>();
		for (Entry<Integer, Allergen> entry:allergens.entrySet()){
			Allergen allergen = entry.getValue();
			if (ids.contains(allergen.local_id)){
				result.add(allergen);
			}
		}
		return result;
	}

	public void removeAll(Set<Integer> aids) {
		for (Integer aid:aids){
			allergens.remove(aid);
		}
		enabledAllergens.removeAll(aids);
	}
}
