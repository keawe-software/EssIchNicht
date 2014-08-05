package com.srsoftware.allergyscan;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.widget.ArrayAdapter;

public class AllergenArrayAdapter extends ArrayAdapter<String> {

	public AllergenArrayAdapter(Context context,Collection<Allergen> entries) {
		super(context,android.R.layout.simple_list_item_multiple_choice,convertToStringList(entries));
	}

	private static List<String> convertToStringList(Collection<Allergen> entries) {		
		List<String> stringList=new Vector<String>();
		for (Allergen allergen:entries){
			stringList.add(allergen.toString());
		}
		return stringList;
	}
}
