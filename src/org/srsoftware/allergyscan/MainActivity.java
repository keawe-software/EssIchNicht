package org.srsoftware.allergyscan;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.widget.Toast;

import com.example.allergyscan.R;

public class MainActivity extends Activity {

	protected static String deviceid = null;
	protected Object allergenList = null;
	
		private void getDeviceId() {
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			deviceid = telephonyManager.getDeviceId();
		}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(getApplicationContext(), "onCreate", 5).show();
        getDeviceId();
        setContentView(R.layout.activity_main);
    }
    
    @Override
    protected void onStart() {    	
    	super.onStart();
    	allergenList=readAllergenList();
    	Toast.makeText(getApplicationContext(), "onStart", 5).show();
    }

    private Object readAllergenList() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
    protected void onResume() {
    	super.onResume();
    	Toast.makeText(getApplicationContext(), "onResume", 5).show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
