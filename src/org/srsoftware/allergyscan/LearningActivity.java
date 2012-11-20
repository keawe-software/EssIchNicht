package org.srsoftware.allergyscan;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
		protected static String SCANNER="com.google.zxing.client.android";
		protected static String productBarCode=null;
		private String productName=null;
		private Integer productId=null;
		private TreeMap<Integer, String> allergens;
		private SharedPreferences settings;
		private AllergyScanDatabase localDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);
        settings=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
	    	localDatabase=new AllergyScanDatabase(getApplicationContext(),settings); // create database handle
	      allergens=localDatabase.getAllergenList();
//        getActionBar().setDisplayHomeAsUpEnabled(true);        
    }

     @Override
    protected void onResume() {
    	super.onResume();
    	Log.d(TAG, "LearningActivity.onResume()");
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	if (scannerAvailable()){
    		if (productBarCode!=null){
    			handleProductBarcode();
    		} else startScanning();
    	}
    }
     
     @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_learning, menu);
        return true;
    }
    
    private ProductData getProduct(String productBarcode){
    	ProductData product = localDatabase.getProduct(productBarcode);
    	if (product==null) try {
				product=RemoteDatabase.getProduct(productBarCode);
			} catch (IOException e1) {}
    	return product;
    }
    
		private void handleProductBarcode() {
			ProductData product = getProduct(productBarCode);
			if (product!=null){
				productName=product.name();
				productId=product.pid();
			}
			if (productName==null) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);

				alert.setTitle(R.string.product_name);
				alert.setMessage(getString(R.string.enter_product_name).replace("#product", productBarCode));

				// Set an EditText view to get user input 
				final EditText input = new EditText(this);
				alert.setView(input);

				alert.setPositiveButton("Ok", new OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						productName=input.getText().toString();
						if (productName.length()<3){
							Toast.makeText(getApplicationContext(), "Bezeichnung zu kurz!", Toast.LENGTH_LONG).show();
							handleProductBarcode();
						} else try {
							productId=RemoteDatabase.storeProduct(productBarCode,productName);
							if (productId==null) throw new IOException();
							askForAllergens(0);
						} catch (IOException e) {
							Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
							finish();
						}
					}
				});

				alert.show();
			} else askForAllergens(0);
		}		

		protected void askForAllergens(final int index) {
			Log.d(TAG, "AskForAllergens("+index+")");
			Entry<Integer, String> entry = getAllergen(index);
			if (entry==null){
				Log.d(TAG, "resetting product infos");
				// if all allergens have been asked for
				productBarCode=null;
				productName=null;
				productId=null;
				finish();
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
						localDatabase.storeAllergenInfo(allergenId,productId,true);
						RemoteDatabase.storeAllergenInfo(allergenId,productId,true);
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
						finish();
					}
					askForAllergens(index+1);
				}
			});
			alert.setNegativeButton(R.string.no, new OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					try {
						localDatabase.storeAllergenInfo(allergenId,productId,false);
						RemoteDatabase.storeAllergenInfo(allergenId,productId,false);
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
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

			alert.show(); // after execution of onClick-method we return to onResume()
		}

		private Entry<Integer, String> getAllergen(int index) {
			int i=0;
			for (Entry<Integer, String> entry:allergens.entrySet()){
				if (index==i++) return entry;
			}
			return null;
		}

		private void startScanning() {
			Intent intent=new Intent(SCANNER+".SCAN"); // start zxing scanne
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra("SCAN_MODE", "PRODUCT_MODE");			
			startActivityForResult(intent, 0); // scanner calls onActivityResult
		}

		@SuppressWarnings("deprecation")
		private boolean scannerAvailable() {
    	PackageManager pm = getPackageManager();
      try {
         pm.getApplicationInfo(SCANNER, 0);
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
          		productBarCode = intent.getStringExtra("SCAN_RESULT_FORMAT")+"~"+intent.getStringExtra("SCAN_RESULT");
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

		static String randomCode() {
	/*		double dummy = Math.random();
			if (dummy<0.25) return "EAN_111111";
			if (dummy<0.5) return "EAN_222222";
			if (dummy<0.75) return "EAN_333333";
			if (dummy<2) return "EAN_44444";
			
		/*/	
			int number=(int)(100000*Math.random());
			if (number<10) return "EAN_8~0000"+number; 
			if (number<100) return "EAN_8~000"+number; 
			if (number<1000) return "EAN_8~00"+number; 
			if (number<10000) return "EAN_8~0"+number;
			return "EAN_8~"+number; //*/
    }

		public void onClick(DialogInterface dialog, int which) {			
			goHome();
    }
}
