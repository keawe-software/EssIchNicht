package org.srsoftware.allergyscan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.allergyscan.R;

public class LearningActivity extends Activity implements OnClickListener {
		protected static String TAG="AllergyScan";
		private static String SCANNER="com.google.zxing.client.android";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);
//        getActionBar().setDisplayHomeAsUpEnabled(true);        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_learning, menu);
        return true;
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (scannerAvailable()){

    		Intent intent=new Intent(SCANNER+".SCAN");
    		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    		intent.putExtra("SCAN_MODE", "ONE_D_MODE");
    		startActivityForResult(intent, 0);

    		Log.d(TAG, "learnCode");
    	} else {
    		Log.w(TAG, "scanner not available");
    	}
    }
    
    private boolean scannerAvailable() {
    	PackageManager pm = getPackageManager();
      try {
         ApplicationInfo appInfo = pm.getApplicationInfo(SCANNER, 0);
         return true;
      } catch (NameNotFoundException e) {
      	AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(R.string.warning)
        .setMessage(R.string.no_scanner)
        .setCancelable(false).create();
      	
      	dialog.setButton(getString(R.string.ok), this);
        dialog.show();

      	
      	return false;
      }
    }
    
		private void goHome() {
    	Toast.makeText(getApplicationContext(), R.string.will_shut_down, Toast.LENGTH_LONG).show();
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		}

		@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      if (requestCode == 0) {
          if (resultCode == RESULT_OK) {
              //  The Intents Fairy has delivered us some data!
              String contents = intent.getStringExtra("SCAN_RESULT");
              String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
              // Handle successful scan
              Log.d(TAG, contents);
              Log.d(TAG, format);
          } else if (resultCode == RESULT_CANCELED) {          	
              // Handle cancel
          	Log.d(TAG, "scanning aborted");
          }
          finish();
      }
  }

		@Override
    public void onClick(DialogInterface dialog, int which) {			
			goHome();
    }
}
