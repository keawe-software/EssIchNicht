package org.srsoftware.allergyscan;

import java.util.TreeMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AllergenList {
	private TreeMap<Integer,String> allergens=null;

	public AllergenList (Context context) {
		allergens=new TreeMap<Integer, String>();
		SQLiteDatabase database = (new AllergyScanDatabase(context)).getReadableDatabase();
		String[] fields={"id","name"};
		Cursor cursor = database.query(AllergyScanDatabase.ALLERGENS, fields, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			allergens.put(cursor.getInt(0), cursor.getString(1));
			cursor.moveToNext();
		}
	}
	
	public boolean isEmpty(){
		return allergens.isEmpty();
	}

}
