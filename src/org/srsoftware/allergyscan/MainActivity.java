package org.srsoftware.allergyscan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.TelephonyManager;
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
        setContentView(R.layout.activity_main);
    }
    
    @Override
    protected void onStart() {    	
    	super.onStart();
    }

		@Override
    protected void onResume() {
    	super.onResume();
    	AllergyScanDatabase asd=new AllergyScanDatabase(getApplicationContext());
      if (asd.getAllergenList().isEmpty()) {
      	Toast.makeText(getApplicationContext(), R.string.no_allergens_selected, Toast.LENGTH_LONG).show();
      	selectAllergens();
      } else {
      	if (!deviceEnabled()){
      		AlertDialog alert=new AlertDialog.Builder(this).create();
      		alert.setTitle(R.string.hint);
      		alert.setMessage(getString(R.string.not_enabled));
      		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), this);
      		alert.show();      		
      	} else if (autoUpdate()) doUpdate();
      }
    }
    
    private void learnCode() {
    	startActivity(new Intent(this,LearningActivity.class));    	
    }

		private boolean deviceEnabled() {
    	// TODO: implement
	    return false;
    }

		private void doUpdate() {
	    Toast.makeText(getApplicationContext(), R.string.performing_update, Toast.LENGTH_LONG).show();
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
