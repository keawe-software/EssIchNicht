package com.srsoftware.allergyscan;

import java.io.IOException;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LearningActivity extends Activity implements android.view.View.OnClickListener {
		protected static String TAG="AllergyScan";
		protected static String SCANNER="com.google.zxing.client.android";
		protected static Barcode productBarCode=null;
		private SharedPreferences settings;
		private AllergyScanDatabase localDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_learning);
      settings=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
    	localDatabase=new AllergyScanDatabase(getApplicationContext(),settings); // create database handle
  		Button backButton = (Button) findViewById(R.id.backButton); // start to listen to the scan-button
  		backButton.setOnClickListener(this);
    }

     @Override
    protected void onResume() {
    	super.onResume();
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
  		if (productBarCode!=null){
  			learnProductBarcode(localDatabase.allergenStack());
  		} else if (MainActivity.scannerAvailable(this)){
  			startScanning();
    	}
    }
     
     @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_learning, menu);
        return true;
    }
 
 		private void learnProductBarcode(final Stack<Allergen> allergenStack) {
			ProductData product = localDatabase.getProduct(productBarCode);
			if (product==null){
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
							learnProductBarcode(allergenStack);
						} else try {
							ProductData newProduct = localDatabase.storeProduct(productBarCode,productName);
							if (newProduct==null){
								throw new IOException();
							}
							askForAllergens(allergenStack,newProduct);
						} catch (IOException e) {
							Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
							finish();
						}
					}
				});

				alert.show();
			} else askForAllergens(allergenStack,product);
		}		
 		
		protected void askForAllergens(final Stack<Allergen> allergens, final ProductData product) {
			if (allergens.isEmpty()){// if all allergens have been asked for
				productBarCode=null;
				finish();
			} else { // entry != null, which means we have another allergen in question
				final Allergen allergen=allergens.pop();
			
				AlertDialog.Builder alert = new AlertDialog.Builder(this);			
				alert.setTitle(allergen.name);
				alert.setMessage(getString(R.string.contains_question).replace("#product", product.name()).replace("#allergen", allergen.name));
				alert.setPositiveButton(R.string.yes, new OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						localDatabase.storeAllergenInfo(allergen.local_id,product.barcode(),true);
						askForAllergens(allergens, product);
					}
				});
				alert.setNegativeButton(R.string.no, new OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						localDatabase.storeAllergenInfo(allergen.local_id,product.barcode(),false);
						askForAllergens(allergens, product);
					}
				});
				alert.setNeutralButton(R.string.dont_know, new OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						askForAllergens(allergens, product);
					}
				});

				alert.show(); // after execution of onClick-method we return to onResume()
			}
		}

		void startScanning() {
			if (MainActivity.deviceid.equals("000000000000000")){
				productBarCode=Barcode.random();
				learnProductBarcode(localDatabase.allergenStack());
			} else {
				Intent intent=new Intent(SCANNER+".SCAN"); // start zxing scanne
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				intent.putExtra("SCAN_MODE", "PRODUCT_MODE");			
				startActivityForResult(intent, 0); // scanner calls onActivityResult
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
          	finish();
          }
      	}
    }

		public void onClick(View v) {
			MainActivity.productCode=null;
			finish();
		}
}
