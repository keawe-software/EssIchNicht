package org.srsoftware.allergyscan;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AllergyScanDatabase extends SQLiteOpenHelper {
	
	
	private static final int DB_VERSION=34;
	protected static String TAG="AllergyScan";
	
	public static final String ALLERGEN_TABLE="allergens";
	public static final String CONTENT_TABLE="content";
	public static final String PRODUCT_TABLE="products";
	
	public static final String ALLERGEN_ID="aid";
	public static final String CONTENT_ID="cid";
	public static final String PRODUCT_ID="pid";
	public static final String CONTAINED="contained";
	@Override
	
	
	
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
	}

	private void createTables(SQLiteDatabase db) {
		Log.d(TAG, "AllergyScanDatabase.createTables()");
		Vector<String> queries=new Vector<String>();
		queries.add("CREATE TABLE IF NOT EXISTS "+PRODUCT_TABLE+" ("+PRODUCT_ID+" INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, barcode TEXT NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+ALLERGEN_TABLE+" ("+ALLERGEN_ID+" INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+CONTENT_TABLE+" ("+CONTENT_ID+" INTEGER NOT NULL PRIMARY KEY, "+PRODUCT_ID+" INTEGER NOT NULL, "+ALLERGEN_ID+" INTEGER NOT NULL, "+CONTAINED+" BOOL NOT NULL)");		
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
		db.execSQL("DROP TABLE IF EXISTS "+PRODUCT_TABLE);
		db.execSQL("DROP TABLE IF EXISTS "+ALLERGEN_TABLE);
		db.execSQL("DROP TABLE IF EXISTS "+CONTENT_TABLE);		
	}

	public AllergyScanDatabase(Context context) {
		super(context, "allergenDB", null, DB_VERSION);
	}

	public TreeMap<Integer, String> getAllergenList() {	  
	  TreeMap<Integer, String> result=new TreeMap<Integer, String>();
	  SQLiteDatabase database = getReadableDatabase();
		String[] fields={ALLERGEN_ID,"name"};
		Cursor cursor = database.query(ALLERGEN_TABLE, fields, null, null, null, null, null);
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
		database.delete(ALLERGEN_TABLE, null, null);
		for (Entry<Integer, String> entry:selectedAllergens.entrySet()){
			ContentValues values=new ContentValues();
			values.put(ALLERGEN_ID, entry.getKey());
			values.put("name", entry.getValue());
			database.insert(ALLERGEN_TABLE, null, values);			
		}
		database.delete(CONTENT_TABLE, null, null);
		database.close();
  }

	public int getLastPID() {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields={PRODUCT_ID};
		Cursor cursor = database.query(PRODUCT_TABLE, fields, null, null, null, null, PRODUCT_ID);
		cursor.moveToFirst();
		int result=0;
		if (!cursor.isAfterLast()) result=cursor.getInt(0); 
		database.close();
		return result;
  }

	public int getLastCID() {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields={CONTENT_ID};
		Cursor cursor = database.query(CONTENT_TABLE, fields, null, null, null, null, CONTENT_ID+" DESC");
		cursor.moveToFirst();
		int result=0;
		if (!cursor.isAfterLast()) result=cursor.getInt(0); 
		database.close();
		return result;
  }

	public void updateContent(ContentValues values) {
	  SQLiteDatabase database=getWritableDatabase();
	  database.insert(CONTENT_TABLE, null, values);
	  database.close();
  }

	public TreeSet<Integer> getAllPIDs() {
		SQLiteDatabase database=getReadableDatabase();
		TreeSet<Integer> result=new TreeSet<Integer>();
		String[] fields={PRODUCT_ID};
		Cursor cursor=database.query(PRODUCT_TABLE, fields, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			result.add(cursor.getInt(0));
			cursor.moveToNext();
		}
	  return result;
  }

	public TreeSet<Integer> getReferencedPIDs() {
		SQLiteDatabase database=getReadableDatabase();
		TreeSet<Integer> result=new TreeSet<Integer>();
		String[] fields={PRODUCT_ID};
		Cursor cursor=database.query(true, CONTENT_TABLE, fields, null, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			result.add(cursor.getInt(0));
			cursor.moveToNext();
		}
	  return result;
  }

	public void updateProducts(ContentValues values) {
	  SQLiteDatabase database=getWritableDatabase();
	  database.insert(PRODUCT_TABLE, null, values);
	  database.close();
  }

	public ProductData getProduct(String productBarcode) {
		SQLiteDatabase database=getReadableDatabase();
		String[] fields={PRODUCT_ID,"name"};
		Cursor cursor=database.query(PRODUCT_TABLE, fields, "barcode = '"+productBarcode+"'", null, null, null, null);
		cursor.moveToFirst();
		ProductData result = null;
		if (!cursor.isAfterLast()){
			int pid=cursor.getInt(0);
			String name=cursor.getString(1);
			result=new ProductData(pid, productBarcode,name);
			cursor.moveToNext();
		}
		database.close();
	  return result;
  }

	public TreeSet<Integer> getContainedAllergens(int pid, Set<Integer> limitTo) {
		SQLiteDatabase db=getReadableDatabase();
		String[] fields={ALLERGEN_ID,"contained"};
		Cursor cursor=db.query(CONTENT_TABLE, fields, CONTAINED+"=1 AND "+PRODUCT_ID+"="+pid, null, null, null, null);
		cursor.moveToFirst();
		TreeSet<Integer> result=new TreeSet<Integer>();
		while (!cursor.isAfterLast()){
			int aid=cursor.getInt(0);
			if (limitTo.contains(aid)) result.add(aid);
			cursor.moveToNext();
		}	  
		return result;
  }

	public TreeSet<Integer> getUnContainedAllergens(int pid, Set<Integer> limitTo) {
		SQLiteDatabase db=getReadableDatabase();
		String[] fields={ALLERGEN_ID,"contained"};
		Cursor cursor=db.query(CONTENT_TABLE, fields, CONTAINED+"=0 AND "+PRODUCT_ID+"="+pid, null, null, null, null);
		cursor.moveToFirst();
		TreeSet<Integer> result=new TreeSet<Integer>();
		while (!cursor.isAfterLast()){
			int aid=cursor.getInt(0);
			if (limitTo.contains(aid)) result.add(aid);
			cursor.moveToNext();
		}	  
		return result;
  }

	public int getAid(String allergen) {
		SQLiteDatabase db=getReadableDatabase();
		String[] fields={ALLERGEN_ID};
		Cursor cursor=db.query(ALLERGEN_TABLE, fields, "name='"+allergen+"'", null, null, null, null);
		cursor.moveToFirst();
		Integer aid=null;
		if (!cursor.isAfterLast()){
			aid=cursor.getInt(0);
		}	  
		return aid;
	}
}
