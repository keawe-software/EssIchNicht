package com.srsoftware.allergyscan;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AllergyScanDatabase extends SQLiteOpenHelper {

	private static final int DB_VERSION = 34;
	protected static String TAG = "AllergyScan";

	public static final String ALLERGEN_TABLE = "allergens";
	public static final String CONTENT_TABLE = "content";
	public static final String PRODUCT_TABLE = "products";

	private SharedPreferences settings;

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
	}

	private void createTables(SQLiteDatabase db) {
		Vector<String> queries = new Vector<String>();
		queries.add("CREATE TABLE IF NOT EXISTS " + ALLERGEN_TABLE + " (laid INTEGER NOT NULL PRIMARY KEY, aid INTEGER, name TEXT COLLATE NOCASE NOT NULL, active BOOL NOT NULL)");
		queries.add("CREATE TABLE IF NOT EXISTS " + CONTENT_TABLE + " (laid INTEGER NOT NULL, barcode INTEGER NOT NULL, contained BOOL NOT NULL, PRIMARY KEY(laid,barcode))");
		queries.add("CREATE TABLE IF NOT EXISTS " + PRODUCT_TABLE + " (barcode INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)");
		for (String query : queries) {
			db.execSQL(query);
		}

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		dropTables(db);
		createTables(db);
	}

	@SuppressWarnings("rawtypes")
	public void syncWithRemote(boolean autosync) {
		// TODO: this should be done in separate thread
		if (!autosync) {
			// TODO: ask user
		}

		JSONObject array;
		try {
			TreeSet<Long> remoteBarcodes = new TreeSet<Long>();
			array = RemoteDatabase.getNewProducts();

			if (array != null) {
				SQLiteDatabase database = getWritableDatabase();
				for (@SuppressWarnings("unchecked")
				Iterator<String> it = array.keys(); it.hasNext();) {
					String barcodeString = it.next();
					String name = array.getString(barcodeString);
					Long barcode = Long.parseLong(barcodeString);
					remoteBarcodes.add(barcode);
					ContentValues values = new ContentValues();
					values.put("barcode", barcode);
					values.put("name", name);
					try {
						database.insertOrThrow(PRODUCT_TABLE, null, values);
					} catch (SQLiteConstraintException sqlce) {} // Ignore duplicates
				}
				database.close();
			}

			RemoteDatabase.storeNewProducts(getNewProducts(remoteBarcodes));

			array = RemoteDatabase.getNewAllergens(getAllAllergens());
			if (array != null) {
				for (@SuppressWarnings("unchecked")
				Iterator<String> it = array.keys(); it.hasNext();) {
					try {
						String remoteAidString = it.next();
						String name = array.getString(remoteAidString);
						Integer localAllergenId = getLocalAllergenId(name);
						Integer remoteAid = Integer.parseInt(remoteAidString);
						ContentValues values = new ContentValues();
						values.put("aid", remoteAid);
						SQLiteDatabase database = getWritableDatabase();
						if (localAllergenId != null) {
							values.put("laid", localAllergenId);
							database.update(ALLERGEN_TABLE, values, "laid=" + localAllergenId, null);
						} else {
							values.put("name", name);
							values.put("active", false);
							database.insert(ALLERGEN_TABLE, null, values);
						}
						database.close();
					} catch (SQLiteConstraintException sqlce) {} // Ignore duplicates
				}

			}

			TreeMap<Integer, TreeMap<Long, Integer>> containments = getAllContainments();

			AllergenList activeAllergens = getActiveAllergens();
			if (activeAllergens != null && !activeAllergens.isEmpty()) {
				array = RemoteDatabase.getInfo(activeAllergens);
				for (Iterator it = array.keys(); it.hasNext();) {
					Integer aid = Integer.parseInt(it.next().toString());
					Integer laid = getLocalAllergenId(aid);
					try {
						JSONObject inner = array.getJSONObject(aid.toString());
						SQLiteDatabase db = getWritableDatabase();						
						
						TreeMap<Long, Integer> containtmentsForCurrentAid = containments.get(aid);
						
						for (Iterator it2 = inner.keys(); it2.hasNext();) {
							Long barcode = Long.parseLong(it2.next().toString());
							Integer contained = Integer.parseInt(inner.get(barcode.toString()).toString());
							ContentValues values = new ContentValues();
							values.put("laid", laid);
							values.put("barcode", barcode);
							values.put("contained", contained);
							db.insertWithOnConflict(CONTENT_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
							
							if (containtmentsForCurrentAid.get(barcode).equals(contained)){
								containtmentsForCurrentAid.remove(barcode); // remove information already known to the server
							}
						}
						if (containtmentsForCurrentAid.isEmpty()){
							containments.remove(aid);
						}
						db.close();
					} catch (JSONException je) {
						// exceptions will be thrown at empty results and can be ignored
					}
				}
			}

			if (containments != null && !containments.isEmpty()) {
				if (RemoteDatabase.setInfo(MainActivity.deviceid, containments)){
					settings.edit().putBoolean("deviceEnabled", true).commit();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private TreeMap<Integer, TreeMap<Long, Integer>> getAllContainments() {
		TreeMap<Integer, TreeMap<Long, Integer>> result = new TreeMap<Integer, TreeMap<Long, Integer>>();
		AllergenList allergens = getAllAllergens();
		SQLiteDatabase db = getReadableDatabase();

		for (Allergen allergen : allergens.values()) {
			String[] columns = { "barcode", "contained" };
			Cursor cursor = db.query(CONTENT_TABLE, columns, "laid=" + allergen.local_id, null, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				TreeMap<Long, Integer> map = result.get(allergen.aid);
				if (map == null) {
					map = new TreeMap<Long, Integer>();
					result.put(allergen.aid, map);
				}
				map.put(cursor.getLong(0), cursor.getInt(1));
				cursor.moveToNext();
			}
		}
		db.close();
		return result;
	}

	private TreeMap<Long, String> getNewProducts(TreeSet<Long> remoteBarcodes) {
		SQLiteDatabase db = getReadableDatabase();
		String[] fields = { "barcode", "name" };
		Cursor cursor = db.query(PRODUCT_TABLE, fields, null, null, null, null, null);
		cursor.moveToFirst();
		TreeMap<Long, String> newProducts = new TreeMap<Long, String>();
		while (!cursor.isAfterLast()) {
			Long barcode = cursor.getLong(0);
			if (!remoteBarcodes.contains(barcode)) {
				String name = cursor.getString(1);
				newProducts.put(barcode, name);
			}
			cursor.moveToNext();
		}
		db.close();
		return newProducts;
	}

	private void dropTables(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS " + PRODUCT_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + ALLERGEN_TABLE);
		db.execSQL("DROP TABLE IF EXISTS " + CONTENT_TABLE);
		settings.edit().putBoolean("deviceEnabled", true).commit();
	}

	public AllergyScanDatabase(Context context, SharedPreferences settings) {
		super(context, "allergenDB", null, DB_VERSION);
		this.settings = settings;
	}

	public AllergenList getAllAllergens() {
		AllergenList result = new AllergenList();
		SQLiteDatabase database = getReadableDatabase();
		String[] fields = { "laid", "aid", "name", "active" };
		Cursor cursor = database.query(ALLERGEN_TABLE, fields, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			result.put(new Allergen(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getInt(3)));
			cursor.moveToNext();
		}
		database.close();
		return result;
	}

	public void setSelectedAllergens(TreeMap<Integer, String> selectedAllergens) {
		SQLiteDatabase database = getWritableDatabase();
		database.delete(ALLERGEN_TABLE, null, null);
		for (Entry<Integer, String> entry : selectedAllergens.entrySet()) {
			ContentValues values = new ContentValues();
			values.put("aid", entry.getKey());
			values.put("name", entry.getValue());
			database.insert(ALLERGEN_TABLE, null, values);
		}
		database.delete(CONTENT_TABLE, null, null);
		database.close();
	}

	public int getLastPID() {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields = { "pid" };
		Cursor cursor = database.query(PRODUCT_TABLE, fields, null, null, null, null, "pid");
		cursor.moveToFirst();
		int result = 0;
		if (!cursor.isAfterLast()) result = cursor.getInt(0);
		database.close();
		return result;
	}

	public int getLastCID() {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields = { "cid" };
		Cursor cursor = database.query(CONTENT_TABLE, fields, null, null, null, null, "cid DESC");
		cursor.moveToFirst();
		int result = 0;
		if (!cursor.isAfterLast()) result = cursor.getInt(0);
		database.close();
		return result;
	}

	public void updateContent(ContentValues values) {
		SQLiteDatabase database = getWritableDatabase();
		database.insert(CONTENT_TABLE, null, values);
		database.close();
	}

	public TreeSet<Integer> getAllPIDs() {
		SQLiteDatabase database = getReadableDatabase();
		TreeSet<Integer> result = new TreeSet<Integer>();
		String[] fields = { "barcode" };
		Cursor cursor = database.query(PRODUCT_TABLE, fields, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			result.add(cursor.getInt(0));
			cursor.moveToNext();
		}
		database.close();
		return result;
	}

	public TreeSet<Integer> getReferencedPIDs() {
		SQLiteDatabase database = getReadableDatabase();
		TreeSet<Integer> result = new TreeSet<Integer>();
		String[] fields = { "pid" };
		Cursor cursor = database.query(true, CONTENT_TABLE, fields, null, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			result.add(cursor.getInt(0));
			cursor.moveToNext();
		}
		database.close();
		return result;
	}

	public void updateProducts(ContentValues values) {
		SQLiteDatabase database = getWritableDatabase();
		database.insert(PRODUCT_TABLE, null, values);
		database.close();
	}

	public ProductData getProduct(Barcode barcode) {
		SQLiteDatabase database = getReadableDatabase();
		String[] fields = { "name" };
		Cursor cursor = database.query(PRODUCT_TABLE, fields, "barcode = " + barcode, null, null, null, null);
		cursor.moveToFirst();
		ProductData result = null;
		if (!cursor.isAfterLast()) {
			String name = cursor.getString(0);
			result = new ProductData(barcode, name);
		}
		database.close();
		return result;
	}

	public TreeSet<Integer> getContainedAllergens(Barcode barcode, Set<Integer> limitTo) {
		SQLiteDatabase db = getReadableDatabase();
		String[] fields = { "laid", "contained" };
		Cursor cursor = db.query(CONTENT_TABLE, fields, "contained=1 AND " + "barcode=" + barcode, null, null, null, null);
		cursor.moveToFirst();
		TreeSet<Integer> result = new TreeSet<Integer>();
		while (!cursor.isAfterLast()) {
			int aid = cursor.getInt(0);
			if (limitTo.contains(aid)) result.add(aid);
			cursor.moveToNext();
		}
		db.close();
		return result;
	}

	public TreeSet<Integer> getUnContainedAllergens(Barcode barcode, Set<Integer> limitTo) {
		SQLiteDatabase db = getReadableDatabase();
		String[] fields = { "laid", "contained" };
		Cursor cursor = db.query(CONTENT_TABLE, fields, "contained=0 AND " + "barcode=" + barcode, null, null, null, null);
		cursor.moveToFirst();
		TreeSet<Integer> result = new TreeSet<Integer>();
		while (!cursor.isAfterLast()) {
			int aid = cursor.getInt(0);
			if (limitTo.contains(aid)) result.add(aid);
			cursor.moveToNext();
		}
		db.close();
		return result;
	}

	public Integer getLocalAllergenId(String allergen) {
		SQLiteDatabase db = getReadableDatabase();
		String[] fields = { "laid" };
		Cursor cursor = db.query(ALLERGEN_TABLE, fields, "name='" + allergen + "'", null, null, null, null);
		cursor.moveToFirst();
		Integer laid = null;
		if (!cursor.isAfterLast()) {
			laid = cursor.getInt(0);
		}
		db.close();
		return laid;
	}

	private Integer getLocalAllergenId(int aid) {
		SQLiteDatabase db = getReadableDatabase();
		String[] fields = { "laid" };
		Cursor cursor = db.query(ALLERGEN_TABLE, fields, "aid='" + aid + "'", null, null, null, null);
		cursor.moveToFirst();
		Integer laid = null;
		if (!cursor.isAfterLast()) {
			laid = cursor.getInt(0);
		}
		db.close();
		return laid;
	}

	public void removeContent(Integer aid, Integer pid) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(CONTENT_TABLE, "aid=" + aid + " AND pid=" + pid, null);
		db.close();
	}

	public void resetAllergenInfo(int localAllergenId, Barcode barcode) {
		SQLiteDatabase database = getWritableDatabase();
		database.delete(CONTENT_TABLE, "laid=" + localAllergenId + " AND barcode=" + barcode.get(), null);
		database.close();
	}

	public void storeAllergenInfo(int localAllergenId, Barcode barcode, boolean contained) {
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("laid", localAllergenId);
		values.put("barcode", barcode.get());
		values.put("contained", contained);
		database.insertWithOnConflict(CONTENT_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		database.close();
	}

	public void storeAllergen(String name) {
		Integer localAllergenId = getLocalAllergenId(name); // find out, whether we already have a synonym name / other upper/lowercase letters
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("name", name);
		if (localAllergenId != null) { // if we already have a synonym: update name (i.e. uppercase/lowercase letters)
			database.update(ALLERGEN_TABLE, values, "laid=" + localAllergenId, null);
		} else { // otherwise: store new allergen
			values.put("aid", 0);
			values.put("active", true);
			database.insert(ALLERGEN_TABLE, null, values);
		}
		database.close();
	}

	public ProductData storeProduct(Barcode barcode, String name) {
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("barcode", barcode.get());
		values.put("name", name);
		long rowid = database.insert(PRODUCT_TABLE, null, values);
		database.close();
		if (rowid < 0) return null;
		return new ProductData(barcode, name);
	}

	public void setEnabled(Vector<Allergen> enabledAllergens) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("active", 0);
		db.update(ALLERGEN_TABLE, values, null, null);
		for (Allergen allergen : enabledAllergens) {
			values = new ContentValues();
			values.put("active", 1);
			db.update(ALLERGEN_TABLE, values, "laid=" + allergen.local_id, null);
		}
	}

	public AllergenList getActiveAllergens() {
		AllergenList result = new AllergenList();
		SQLiteDatabase database = getReadableDatabase();
		String[] fields = { "laid", "aid", "name", "active" };
		Cursor cursor = database.query(ALLERGEN_TABLE, fields, "active=1", null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			result.put(new Allergen(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getInt(3)));
			cursor.moveToNext();
		}
		database.close();
		return result;
	}

	Stack<Allergen> allergenStack() {
		Stack<Allergen> allergenStack = new Stack<Allergen>();
		allergenStack.addAll(getActiveAllergens().values());
		return allergenStack;
	}
}
