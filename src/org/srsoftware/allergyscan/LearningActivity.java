package org.srsoftware.allergyscan;

import java.io.IOException;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.example.allergyscan.R;

public class LearningActivity extends Activity {
		protected static String TAG="AllergyScan";
		protected static String SCANNER="com.google.zxing.client.android";
		protected static Long productBarCode=null;
		private AllergenList allergens;
		private SharedPreferences settings;
		private AllergyScanDatabase localDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);
        settings=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
	    	localDatabase=new AllergyScanDatabase(getApplicationContext(),settings); // create database handle
	      allergens=localDatabase.getAllAllergens();
//        getActionBar().setDisplayHomeAsUpEnabled(true);        
    }

     @Override
    protected void onResume() {
    	super.onResume();
    	Log.d(TAG, "LearningActivity.onResume()");
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  		if (productBarCode!=null){
  			handleProductBarcode();
  		} else if (scannerAvailable()){
  			startScanning();
    	}
    }
     
     @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_learning, menu);
        return true;
    }
    
 		private void handleProductBarcode() {
			Log.d(TAG, "handleProductBarcode("+productBarCode+")");
			final ProductData product = localDatabase.getProduct(productBarCode);
			if (product==null){
				return;
			}
			if (product.name()==null) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);

				alert.setTitle(R.string.product_name);
				alert.setMessage(getString(R.string.enter_product_name).replace("#product", ""+productBarCode));

				// Set an EditText view to get user input 
				final EditText input = new EditText(this);
				alert.setView(input);

				alert.setPositiveButton("Ok", new OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						String productName=input.getText().toString();
						if (productName.length()<3){
							Toast.makeText(getApplicationContext(), "Bezeichnung zu kurz!", Toast.LENGTH_LONG).show();
							handleProductBarcode();
						} else try {
							if (!localDatabase.storeProduct(productBarCode,productName)){
								throw new IOException();
							}
							askForAllergens(0,product);
						} catch (IOException e) {
							Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
							finish();
						}
					}
				});

				alert.show();
			} else askForAllergens(0,product);
		}		

		protected void askForAllergens(final int index, final ProductData product) {
			Log.d(TAG, "AskForAllergens("+index+")");
			Entry<Integer, Allergen> entry = getAllergen(index);
			if (entry==null){
				Log.d(TAG, "resetting product infos");
				// if all allergens have been asked for
				productBarCode=null;
				finish();
			} else { // entry != null, which means we have another allergen in question
				final Allergen allergen=entry.getValue();
				final int allergenId=entry.getKey();
			
				AlertDialog.Builder alert = new AlertDialog.Builder(this);			
				alert.setTitle(allergen.name);
				alert.setMessage(getString(R.string.contains_question).replace("#product", product.name()).replace("#allergen", allergen.name));
				alert.setPositiveButton(R.string.yes, new OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						localDatabase.storeAllergenInfo(allergenId,product.barcode(),true);
						askForAllergens(index+1, product);
					}
				});
				alert.setNegativeButton(R.string.no, new OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						localDatabase.storeAllergenInfo(allergenId,product.barcode(),false);
						askForAllergens(index+1, product);
					}
				});
				alert.setNeutralButton(R.string.dont_know, new OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.d(TAG, "don't know, whether "+product.name()+" contains "+allergen);
						askForAllergens(index+1, product);
					}
				});

				alert.show(); // after execution of onClick-method we return to onResume()
			}
		}

		private Entry<Integer, Allergen> getAllergen(int index) {
			int i=0;
			for (Entry<Integer, Allergen> entry:allergens.entrySet()){
				if (index==i++) return entry;
			}
			return null;
		}

		private void startScanning() {
			if (MainActivity.deviceid.equals("000000000000000")){
				productBarCode=randomCode();
				handleProductBarcode();
			} else {
				Intent intent=new Intent(SCANNER+".SCAN"); // start zxing scanne
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.putExtra("SCAN_MODE", "PRODUCT_MODE");			
				startActivityForResult(intent, 0); // scanner calls onActivityResult
			}
		}

		@SuppressWarnings("deprecation")
		private boolean scannerAvailable() {
			if (MainActivity.deviceid.equals("000000000000000")) return true;
    	PackageManager pm = getPackageManager();
      try {
         pm.getApplicationInfo(SCANNER, 0);
         return true;
      } catch (NameNotFoundException e) {
      	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
      	dialogBuilder.setTitle(R.string.warning);
      	dialogBuilder.setMessage(R.string.no_scanner);
      	dialogBuilder.setCancelable(false);
      	AlertDialog dialog = dialogBuilder.create();
      	dialog.setButton(getString(R.string.ok), new OnClickListener() {
					
      		public void onClick(DialogInterface dialog, int which) {	
      			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.scannerUrl)));
      			startActivity(browserIntent);
          }
				});
      	dialog.show();
      	
      	return false;
      }
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
          		productBarCode = MainActivity.getBarCode(intent);
          		// here the product code is set; afterwards onResume is called again
          } else if (resultCode == RESULT_CANCELED) {
          	/*
      		  // TODO: remove random code setting and warning message for final version
      			Log.w(TAG, "abort overridden in LearningActivity.onActivityResult!");
          	productBarCode = randomCode();
          	/*/
          	Log.d(TAG, "scanning aborted");
          	finish(); //*/
          }
      	}
    }

		static Long randomCode() {
			int number=(int)(100000*Math.random());
			if (number<10) return Long.parseLong("180000"+number); 
			if (number<100) return Long.parseLong("18000"+number); 
			if (number<1000) return Long.parseLong("1800"+number); 
			if (number<10000) return Long.parseLong("180"+number);
			return Long.parseLong("18"+number);
    }
}
