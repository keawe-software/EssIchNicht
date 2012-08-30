package org.srsoftware.allergyscan;

import java.util.TreeMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.allergyscan.R;

public class MainActivity extends Activity {

	protected static String deviceid = null;
	protected Object allergenList = null;
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
      TreeMap<Integer,String> allergenList=asd.getAllergenList();

      if (allergenList.isEmpty()) {
      	Log.w(TAG, "allergen list empty!");
      	Toast.makeText(getApplicationContext(), R.string.no_allergens_selected, Toast.LENGTH_LONG).show();
      	selectAllergens();
      } else {
      	Log.d(TAG, "selected allergens: "+allergenList.toString());
      }
    }
    
    private void selectAllergens() {
    	Intent intent=new Intent(this,AllergenSelectionActivity.class);
    	startActivity(intent);
    }

		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	boolean dummy = super.onOptionsItemSelected(item);
    	selectAllergens();
      return dummy;
    }
}
