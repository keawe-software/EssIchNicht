package com.srsoftware.allergyscan;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
	
	private SharedPreferences settings;
	@Override
	
	
	
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
	}

	private void createTables(SQLiteDatabase db) {
		Log.d(TAG, "AllergyScanDatabase.createTables()");
		Vector<String> queries=new Vector<String>();
		queries.add("CREATE TABLE IF NOT EXISTS "+ALLERGEN_TABLE+" (laid INTEGER NOT NULL PRIMARY KEY, aid INTEGER, name TEXT NOT NULL, active BOOL NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS "+CONTENT_TABLE+" (laid INTEGER NOT NULL, barcode INTEGER NOT NULL, contained BOOL NOT NULL, PRIMARY KEY(laid,barcode))");		
		queries.add("CREATE TABLE IF NOT EXISTS "+PRODUCT_TABLE+" (barcode INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
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
	
	public void syncWithRemote(boolean autosync){
		// TODO: this should be done in separate thread
		if (!autosync){
			// TODO: ask user
		}
		
		JSONObject array;
		try {
			array = RemoteDatabase.getNewProducts(getAllBarCodes());
		  SQLiteDatabase database=getWritableDatabase();
			for (@SuppressWarnings("unchecked")
			Iterator<String> it=array.keys(); it.hasNext();){
				String barcode=it.next();
				String name=array.getString(barcode);
				ContentValues values=new ContentValues();
				values.put("barcode", barcode);
				values.put("name", name);
			  database.insert(PRODUCT_TABLE, null, values);
			}
		  database.close();		  
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private TreeSet<Long> getAllBarCodes() {
		SQLiteDatabase db = getReadableDatabase();
		String[] fields={"barcode"};
		Cursor cursor = db.query(PRODUCT_TABLE, fields, null, null, null, null, null);
		cursor.moveToFirst();
		TreeSet<Long> result=new TreeSet<Long>();
		while (!cursor.isAfterLast()){
			result.add(cursor.getLong(0));
			cursor.moveToNext();
		}
		return result;
	}

	private void dropTables(SQLiteDatabase db) {
		Log.d(TAG, "AllergyScanDatabase.dropTables()");
		db.execSQL("DROP TABLE IF EXISTS "+PRODUCT_TABLE);
		db.execSQL("DROP TABLE IF EXISTS "+ALLERGEN_TABLE);
		db.execSQL("DROP TABLE IF EXISTS "+CONTENT_TABLE);
		settings.edit().putBoolean("deviceEnabled", true).commit();
	}

	public AllergyScanDatabase(Context context, SharedPreferences settings) {
		super(context, "allergenDB", null, DB_VERSION);
		this.settings=settings;
	}

	public AllergenList getAllAllergens() {	  
		AllergenList result=new AllergenList();
	  SQLiteDatabase database = getReadableDatabase();
		String[] fields={"laid","aid","name","active"};
		Cursor cursor = database.query(ALLERGEN_TABLE, fields, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			result.put(cursor.getInt(0), new Allergen(cursor.getInt(0),cursor.getInt(1),cursor.getString(2),cursor.getInt(3)));
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
			values.put("aid", entry.getKey());
			values.put("name", entry.getValue());
			database.insert(ALLERGEN_TABLE, null, values);			
		}
		database.delete(CONTENT_TABLE, null, null);
		database.close();
  }

	public int getLastPID() {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields={"pid"};
		Cursor cursor = database.query(PRODUCT_TABLE, fields, null, null, null, null, "pid");
		cursor.moveToFirst();
		int result=0;
		if (!cursor.isAfterLast()) result=cursor.getInt(0); 
		database.close();
		return result;
  }

	public int getLastCID() {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields={"cid"};
		Cursor cursor = database.query(CONTENT_TABLE, fields, null, null, null, null, "cid DESC");
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
		String[] fields={"barcode"};
		Cursor cursor=database.query(PRODUCT_TABLE, fields, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			result.add(cursor.getInt(0));
			cursor.moveToNext();
		}
		database.close();
	  return result;
  }

	public TreeSet<Integer> getReferencedPIDs() {
		SQLiteDatabase database=getReadableDatabase();
		TreeSet<Integer> result=new TreeSet<Integer>();
		String[] fields={"pid"};
		Cursor cursor=database.query(true, CONTENT_TABLE, fields, null, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			result.add(cursor.getInt(0));
			cursor.moveToNext();
		}
		database.close();
	  return result;
  }

	public void updateProducts(ContentValues values) {
	  SQLiteDatabase database=getWritableDatabase();
	  database.insert(PRODUCT_TABLE, null, values);
	  database.close();
  }

	public ProductData getProduct(Long barcode) {
		SQLiteDatabase database=getReadableDatabase();
		String[] fields={"name"};
		Cursor cursor=database.query(PRODUCT_TABLE, fields, "barcode = "+barcode, null, null, null, null);
		cursor.moveToFirst();
		ProductData result = null;
		if (!cursor.isAfterLast()){
			String name=cursor.getString(0);
			result=new ProductData(barcode,name);
		}
		database.close();
	  return result;
  }

	public TreeSet<Integer> getContainedAllergens(Long barcode, Set<Integer> limitTo) {
		SQLiteDatabase db=getReadableDatabase();
		String[] fields={"aid","contained"};
		Cursor cursor=db.query(CONTENT_TABLE, fields, "contained=1 AND "+"pid="+barcode, null, null, null, null);
		cursor.moveToFirst();
		TreeSet<Integer> result=new TreeSet<Integer>();
		while (!cursor.isAfterLast()){
			int aid=cursor.getInt(0);
			if (limitTo.contains(aid)) result.add(aid);
			cursor.moveToNext();
		}	  
		db.close();
		return result;
  }

	public TreeSet<Integer> getUnContainedAllergens(Long barcode, Set<Integer> limitTo) {
		SQLiteDatabase db=getReadableDatabase();
		String[] fields={"aid","contained"};
		Cursor cursor=db.query(CONTENT_TABLE, fields, "contained=0 AND "+"barcode="+barcode, null, null, null, null);
		cursor.moveToFirst();
		TreeSet<Integer> result=new TreeSet<Integer>();
		while (!cursor.isAfterLast()){
			int aid=cursor.getInt(0);
			if (limitTo.contains(aid)) result.add(aid);
			cursor.moveToNext();
		}	  
		db.close();
		return result;
  }

	public int getAid(String allergen) {
		SQLiteDatabase db=getReadableDatabase();
		String[] fields={"barcode"};
		Cursor cursor=db.query(ALLERGEN_TABLE, fields, "name='"+allergen+"'", null, null, null, null);
		cursor.moveToFirst();
		Integer aid=null;
		if (!cursor.isAfterLast()){
			aid=cursor.getInt(0);
		}	  
		db.close();
		return aid;
	}

	public void removeContent(Integer aid, Integer pid) {
		SQLiteDatabase db=getWritableDatabase();
		db.delete(CONTENT_TABLE, "aid="+aid+" AND pid="+pid, null);
		db.close();
	}

	public void storeAllergenInfo(int allergenId, Long barcode, boolean b) {
		Log.d(TAG, "AllergyScanDatabse.storeAllergenInfo not implemented");
		// TODO Auto-generated method stub

	}

	public void storeAllergen(String name) {
		Log.d(TAG,"AllergyScanDatabse.storeAllergen("+name+")");
		SQLiteDatabase database=getWritableDatabase();
		ContentValues values=new ContentValues();
		values.put("aid", 0);
		values.put("name", name);
		values.put("active", 0);
		database.insert(ALLERGEN_TABLE, null, values);			
		database.close();  
	}

	public ProductData storeProduct(Long barcode, String name) {
		Log.d(TAG,"AllergyScanDatabse.storeProduct("+barcode+","+name+")");
		SQLiteDatabase database=getWritableDatabase();
		ContentValues values=new ContentValues();
		values.put("barcode", barcode);
		values.put("name", name);
		long rowid = database.insert(PRODUCT_TABLE, null, values);			
		database.close();  
		if (rowid<0) return null;
		return new ProductData(barcode, name);
	}

	public void setEnabled(Vector<Allergen> enabledAllergens) {
		SQLiteDatabase db=getWritableDatabase();
		ContentValues values=new ContentValues();
		values.put("active", 0);
		db.update(ALLERGEN_TABLE, values, null, null);
		for (Allergen allergen:enabledAllergens){
			values=new ContentValues();
			values.put("active", 1);
			db.update(ALLERGEN_TABLE, values, "laid="+allergen.local_id, null);
		}
	}

	public AllergenList getActiveAllergens() {	  
		AllergenList result=new AllergenList();
	  SQLiteDatabase database = getReadableDatabase();
		String[] fields={"laid","aid","name","active"};
		Cursor cursor = database.query(ALLERGEN_TABLE, fields, "active=1", null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()){
			result.put(cursor.getInt(0), new Allergen(cursor.getInt(0),cursor.getInt(1),cursor.getString(2),cursor.getInt(3)));
			cursor.moveToNext();
		}
		database.close();
		return result;
  }
}
