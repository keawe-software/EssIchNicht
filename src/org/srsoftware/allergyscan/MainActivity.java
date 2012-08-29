package org.srsoftware.allergyscan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
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
        Log.d(TAG, "MainActivity.onCreate()");        
        getDeviceId();
        setContentView(R.layout.activity_main);
    }
    
    @Override
    protected void onStart() {    	
    	super.onStart();
      AllergenList allergenList=new AllergenList(getApplicationContext());
      if (allergenList.isEmpty()) {
      	Log.w(TAG, "allergen list empty!");
      	Toast.makeText(getApplicationContext(), "Noch keine Allergene vorhanden!", Toast.LENGTH_LONG).show();
      	Intent intent=new Intent(this,AllergenSelectionActivity.class);
      	startActivity(intent);
      }
    }

		@Override
    protected void onResume() {
    	super.onResume();
    	Log.d(TAG, "onResume");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
