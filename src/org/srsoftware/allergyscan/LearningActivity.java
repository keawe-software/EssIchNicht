package org.srsoftware.allergyscan;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;

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
import android.widget.EditText;
import android.widget.Toast;

import com.example.allergyscan.R;

public class LearningActivity extends Activity implements OnClickListener {
		protected static String TAG="AllergyScan";
		private static String SCANNER="com.google.zxing.client.android";
		private String productCode=null;
		private String productName=null;
		private Integer productId=null;
		private TreeMap<Integer, String> allergens;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);
	    	AllergyScanDatabase asd=new AllergyScanDatabase(getApplicationContext());
	      allergens=asd.getAllergenList();

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
    		if (productCode!=null){
    			askForProductName();
    		} else startSCanning();
    	}
    }
    
		private void askForProductName() {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.product_name);
			alert.setMessage(getString(R.string.enter_product_name).replace("#product", productCode));

			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Ok", new OnClickListener() {

				public void onClick(DialogInterface dialog, int whichButton) {
					productName=input.getText().toString();
					try {
						productId=RemoteDatabase.storeProduct(productCode,productName);
						if (productId==null) throw new IOException();
						askForAllergens(0);
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG);
						finish();
					}

			  }
			});

			alert.show();
		}		

		protected void askForAllergens(final int index) {
			Entry<Integer, String> entry = getAllergen(index);
			if (entry==null){
				
				// if all allergens have been asked for
				Log.w(TAG, "done");
				productCode=null;
				productName=null;
				productId=null;
				return;
			}
			final String allergen=entry.getValue();
			final int allergenId=entry.getKey();
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
			alert.setTitle(allergen);
			alert.setMessage(getString(R.string.contains_question).replace("#product", productName).replace("#allergen", allergen));
			
			alert.setPositiveButton(R.string.yes, new OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					try {
						RemoteDatabase.storeAllergenInfo(allergenId,productId,true);
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG);
						finish();
					}
					askForAllergens(index+1);
				}
			});
			alert.setNegativeButton(R.string.no, new OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					try {
						RemoteDatabase.storeAllergenInfo(allergenId,productId,false);
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG);
						finish();
					}
					askForAllergens(index+1);
				}
			});
			alert.setNeutralButton(R.string.dont_know, new OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Log.d(TAG, "don't know, whether "+productName+" contains "+allergen);
					askForAllergens(index+1);
				}
			});

			alert.show();
		}

		private Entry<Integer, String> getAllergen(int index) {
			int i=0;
			for (Entry<Integer, String> entry:allergens.entrySet()){
				if (index==i++) return entry;
			}
			return null;
		}

		private void startSCanning() {
			Intent intent=new Intent(SCANNER+".SCAN");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
			startActivityForResult(intent, 0);
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
          		productCode = intent.getStringExtra("SCAN_RESULT_FORMAT")+"~"+intent.getStringExtra("SCAN_RESULT");
          } else if (resultCode == RESULT_CANCELED) {
          	Log.d(TAG, "scanning aborted");
          	finish();
          }
      }
  }

		public void onClick(DialogInterface dialog, int which) {			
			goHome();
    }
}
