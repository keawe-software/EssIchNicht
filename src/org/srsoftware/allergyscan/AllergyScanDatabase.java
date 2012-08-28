package org.srsoftware.allergyscan;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class AllergyScanDatabase extends SQLiteOpenHelper {
	
	
	private static final int DB_VERSION=2;
	private static final String TABLE_NAME="allergenDB";
	private static final String CREATE_QUERY="CREATE TABLE "+TABLE_NAME+" ("; 

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_QUERY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	public AllergyScanDatabase(Context context) {
		super(context, "allergenDB", null, DB_VERSION);
	}

}
