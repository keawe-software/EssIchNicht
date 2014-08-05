package com.srsoftware.allergyscan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SynchronizeActivity extends Activity implements OnClickListener {

	private Button alwaysOkButton;
	private Button onceOkButton;
	private Button noButton;
	private View progressBar;
	private SharedPreferences settings;
	private static String TAG="AllergyScan";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_synchronize);
    settings=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
    
		alwaysOkButton = (Button) findViewById(R.id.alway_ok);
		alwaysOkButton.setOnClickListener(this);

		onceOkButton = (Button) findViewById(R.id.once_ok);
		onceOkButton.setOnClickListener(this);

		noButton = (Button) findViewById(R.id.dont_sync);
		noButton.setOnClickListener(this);
		
		progressBar = findViewById(R.id.progressBar1);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "SynchronizeActivity.onResume()");
    if (autoSyncEnabled()){
    	sync();
    } else {
  		alwaysOkButton.setEnabled(true);
  		onceOkButton.setEnabled(true);
  		noButton.setEnabled(true);
  		progressBar.setVisibility(View.INVISIBLE);
    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_synchronize, menu);
		return true;
	}

	public void onClick(View v) {
		if (v == alwaysOkButton) {
			settings.edit().putBoolean("autoUpdate", true).commit();
			sync();
		}
		if (v == onceOkButton) {
			settings.edit().putBoolean("autoUpdate", false).commit();
			sync();
		}
		if (v == noButton) {
			finish();
			goHome();
		}
	}

	private class SyncThread extends Thread {

		private SynchronizeActivity synchronizeActivity;

		public SyncThread(SynchronizeActivity synchronizeActivity) {
			this.synchronizeActivity = synchronizeActivity;
		}

		@Override
		public void run() {
			SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
			AllergyScanDatabase localDatabase = new AllergyScanDatabase(getApplicationContext(), settings); // create database handle
			localDatabase.syncWithRemote(false);
			synchronizeActivity.finish();
		}
	}
	
	/**
	 * check, whether automatic updates are enabled on this device
	 * @return true, if automatic updates are not deactivated
	 */
	private boolean autoSyncEnabled() {    	
		boolean result=settings.getBoolean("autoUpdate", false);
		System.out.println(result);
  	return result;
  }

	private void sync() {
		alwaysOkButton.setEnabled(false);
		onceOkButton.setEnabled(false);
		noButton.setEnabled(false);
		progressBar.setVisibility(View.VISIBLE);
		new SyncThread(this).start();
	}

	/**
	 * go back to the system desktop
	 */
	void goHome() {
		Toast.makeText(getApplicationContext(), R.string.will_shut_down, Toast.LENGTH_LONG).show();
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}
}
