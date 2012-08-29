package org.srsoftware.allergyscan;

import java.util.Vector;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AllergyScanDatabase extends SQLiteOpenHelper {
	
	
	private static final int DB_VERSION=7;
	protected static String TAG="AllergyScan";
	public static final String PRODUCTS="products";
	public static final String ALLERGENS="allergens";
	public static final String CONTENT="content";
	@Override
	
	
	
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
	}

	private void createTables(SQLiteDatabase db) {
		Log.d(TAG, "AllergyScanDatabase.createTables()");
		Vector<String> queries=new Vector<String>();
		queries.add("CREATE TABLE IF NOT EXISTS "+PRODUCTS+" (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, barcode TEXT NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+ALLERGENS+" (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+CONTENT+" (a_id INTEGER NOT NULL, p_id INTEGER NOT NULL, PRIMARY KEY(a_id,p_id))");		
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

}
