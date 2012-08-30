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
	
	
	private static final int DB_VERSION=11;
	protected static String TAG="AllergyScan";
	public static final String PRODUCTS="products";
	public static final String ALLERGENS="allergens";
	public static final String ALLERGEN_ID="aid";
	public static final String PRODUCT_ID="pid";
	public static final String CONTENT="content";
	@Override
	
	
	
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
	}

	private void createTables(SQLiteDatabase db) {
		Log.d(TAG, "AllergyScanDatabase.createTables()");
		Vector<String> queries=new Vector<String>();
		queries.add("CREATE TABLE IF NOT EXISTS "+PRODUCTS+" ("+PRODUCT_ID+" INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, barcode TEXT NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+ALLERGENS+" ("+ALLERGEN_ID+" INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+CONTENT+" ("+ALLERGEN_ID+" INTEGER NOT NULL, "+PRODUCT_ID+" INTEGER NOT NULL, PRIMARY KEY("+ALLERGEN_ID+","+PRODUCT_ID+"))");		
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
		Log.d(TAG, "AllergyScanDatabase.create("+context+")");
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

}
