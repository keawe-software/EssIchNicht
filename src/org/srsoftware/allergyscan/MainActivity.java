package org.srsoftware.allergyscan;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.allergyscan.R;

public class MainActivity extends Activity implements OnClickListener {

	protected static String deviceid = null;
	protected static String TAG="AllergyScan";
	AllergyScanDatabase database=null;
	
		private void getDeviceId() {
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			deviceid = telephonyManager.getDeviceId();
		}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDeviceId();
      	database=new AllergyScanDatabase(getApplicationContext());
        setContentView(R.layout.activity_main);
    }
    
    @Override
    protected void onStart() {    	
    	super.onStart();
    }

		@Override
    protected void onResume() {
    	super.onResume();
    	Log.d(TAG, "MainActivity.onResume()");
      if (database.getAllergenList().isEmpty()) {
      	Toast.makeText(getApplicationContext(), R.string.no_allergens_selected, Toast.LENGTH_LONG).show();
      	selectAllergens();
      } else {
      	try {
      		if (!RemoteDatabase.deviceEnabled()){
      			AlertDialog alert=new AlertDialog.Builder(this).create();
      			alert.setTitle(R.string.hint);
      			alert.setMessage(getString(R.string.not_enabled).replace("#count", ""+RemoteDatabase.missingCredits()));
      			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), this);
      			alert.show();      		
      		} else if (autoUpdate()) {
      			doUpdate();
      		} else {
      			Log.w(TAG, "scanning not implemented, yet.");
      		}
      	} catch (IOException e){
      		Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
      		goHome();
      	}
      }
    }
		
		private void goHome() {
    	Toast.makeText(getApplicationContext(), R.string.will_shut_down, Toast.LENGTH_LONG).show();
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		}
    
    private void learnCode() {
    	startActivity(new Intent(this,LearningActivity.class));    	
    }

		private void doUpdate() {
	    Toast.makeText(getApplicationContext(), R.string.performing_update, Toast.LENGTH_LONG).show();
	    
	    try {
	      RemoteDatabase.update(database);
      } catch (IOException e) {
    		Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
      }
    }

		private boolean autoUpdate() {
    	SharedPreferences prefs=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
    	return prefs.getBoolean(getString(R.string.auto_update), true);
    }

		private void selectAllergens() {
    	startActivity(new Intent(this,AllergenSelectionActivity.class));
    }

		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {    	
    	boolean dummy = super.onOptionsItemSelected(item);
    	switch (item.getItemId()){
    		case R.id.allergen_selection: selectAllergens(); break;
    		case R.id.menu_settings: editPreferences(); break;
    	}
      return dummy;
    }

		private void editPreferences() {
			startActivity(new Intent(this,PreferencesActivity.class));
    }

		public void onClick(DialogInterface arg0, int arg1) {
			learnCode();	    
    }
}
