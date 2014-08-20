package com.srsoftware.allergyscan;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SynchronizeActivity extends Activity implements OnClickListener, android.content.DialogInterface.OnClickListener {

	private Button alwaysOkButton,onceOkButton,noButton,infoOkButton;
	private View progressBar;
	private SharedPreferences settings;
	static boolean urgent = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_synchronize);
		settings = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle

		infoOkButton = (Button) findViewById(R.id.moreInfo);
		infoOkButton.setOnClickListener(this);

		alwaysOkButton = (Button) findViewById(R.id.alway_ok);
		alwaysOkButton.setOnClickListener(this);

		onceOkButton = (Button) findViewById(R.id.once_ok);
		onceOkButton.setOnClickListener(this);

		noButton = (Button) findViewById(R.id.dont_sync);
		if (urgent) {
			noButton.setText(R.string.dont_sync_shutdown);
		} else {
			noButton.setText(R.string.dont_sync);
		}
		noButton.setOnClickListener(this);

		progressBar = findViewById(R.id.progressBar1);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (autoSyncEnabled()) {
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
		if (v==infoOkButton){
			AlertDialog infoDialog = new AlertDialog.Builder(this).create(); // show warning message. learning mode will be toggled by the message button
			infoDialog.setTitle(R.string.hint);
			infoDialog.setMessage(getString(R.string.update_info));
			infoDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), this); // this button will toggle learning mode
			infoDialog.show(); // Pressing "OK" calls learnCode()

		}
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
			if (urgent) {
				goHome();
			}
			urgent = false;
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
			try {
				if (autoSyncEnabled()) {
					synchronizeActivity.finish();
					localDatabase.syncWithRemote();
				} else {
					localDatabase.syncWithRemote();
					synchronizeActivity.finish();
				}
			} catch (NetworkErrorException e) {
				if (urgent){
					MainActivity.network_status=MainActivity.FATAL;
					urgent=false;
				} else {
					MainActivity.network_status=MainActivity.FAIL;
				}
				synchronizeActivity.finish();
			}
		}		
	}
	


	/**
	 * check, whether automatic updates are enabled on this device
	 * 
	 * @return true, if automatic updates are not deactivated
	 */
	private boolean autoSyncEnabled() {
		boolean result = settings.getBoolean("autoUpdate", false);
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

	public void onClick(DialogInterface dialog, int which) {
		finish();
	}
}
