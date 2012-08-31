package org.srsoftware.allergyscan;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AllergyScanDatabase extends SQLiteOpenHelper {
	
	
	private static final int DB_VERSION=16;
	protected static String TAG="AllergyScan";
	
	public static final String ALLERGENS="allergens";
	public static final String CONTENT="content";
	public static final String PRODUCTS="products";
	
	public static final String ALLERGEN_ID="aid";
	public static final String CONTENT_ID="cid";
	public static final String PRODUCT_ID="pid";
	@Override
	
	
	
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
	}

	private void createTables(SQLiteDatabase db) {
		Log.d(TAG, "AllergyScanDatabase.createTables()");
		Vector<String> queries=new Vector<String>();
		queries.add("CREATE TABLE IF NOT EXISTS "+PRODUCTS+" ("+PRODUCT_ID+" INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, barcode TEXT NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+ALLERGENS+" ("+ALLERGEN_ID+" INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+CONTENT+" ("+CONTENT_ID+" INTEGER NOT NULL PRIMARY KEY, "+ALLERGEN_ID+" INTEGER NOT NULL, "+PRODUCT_ID+" INTEGER NOT NULL)");		
		for (String query: queries){
			Log.d(TAG, query);
			db.execSQL(query);	
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		dropTables(db);
		createTables(db);
	}

	private void dropTables(SQLiteDatabase db) {
		Log.d(TAG, "AllergyScanDatabase.dropTables()");
		db.execSQL("DROP TABLE IF EXISTS "+PRODUCTS);
		db.execSQL("DROP TABLE IF EXISTS "+ALLERGENS);
		db.execSQL("DROP TABLE IF EXISTS "+CONTENT);		
	}

	public AllergyScanDatabase(Context context) {
		super(context, "allergenDB", null, DB_VERSION);
	}

	public TreeMap<Integer, String> getAllergenList() {	  
	  TreeMap<Integer, String> result=new TreeMap<Integer, String>();
	  SQLiteDatabase database = getReadableDatabase();
		String[] fields={ALLERGEN_ID,"name"};
		Cursor cursor = database.query(ALLERGENS, fields, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			result.put(cursor.getInt(0), cursor.getString(1));
			cursor.moveToNext();
		}
		database.close();
		return result;
  }

	public void setSelectedAllergens(TreeMap<Integer, String> selectedAllergens) {
		SQLiteDatabase database=getWritableDatabase();
		database.delete(ALLERGENS, null, null);
		for (Entry<Integer, String> entry:selectedAllergens.entrySet()){
			ContentValues values=new ContentValues();
			values.put(ALLERGEN_ID, entry.getKey());
			values.put("name", entry.getValue());
			database.insert(ALLERGENS, null, values);			
		}
		database.close();
  }

	public int getLastPID() {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields={PRODUCT_ID};
		Cursor cursor = database.query(PRODUCTS, fields, null, null, null, null, PRODUCT_ID);
		cursor.moveToFirst();
		int result=0;
		if (!cursor.isAfterLast()) result=cursor.getInt(0); 
		database.close();
		return result;
  }

	public int getLastCID() {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields={CONTENT_ID};
		Cursor cursor = database.query(CONTENT, fields, null, null, null, null, CONTENT_ID);
		cursor.moveToFirst();
		int result=0;
		if (!cursor.isAfterLast()) result=cursor.getInt(0); 
		database.close();
		return result;
  }
}
