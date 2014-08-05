package org.srsoftware.allergyscan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.allergyscan.R;

public class MainActivity extends Activity implements OnClickListener, android.content.DialogInterface.OnClickListener, OnItemClickListener {

	protected static String deviceid = null;
	private static SharedPreferences settings = null;
	private static AllergyScanDatabase localDatabase=null;
	private static String TAG="AllergyScan";
	private String productCode;
	private ProductData productData;
	private ListView list;
	private ArrayList<String> listItems;
	@SuppressWarnings("rawtypes")
	private ArrayAdapter adapter;
	
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        deviceid = getDeviceId(); // request the id and store in global variable
        setContentView(R.layout.activity_main); 
        settings=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
      	localDatabase=new AllergyScanDatabase(getApplicationContext(),settings); // create database handle
      	
        /* prepare result list */
  			list=(ListView)findViewById(R.id.containmentList);
  			listItems=new ArrayList<String>(); 
  			adapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,listItems);
  			list.setAdapter(adapter);
  			list.setOnItemClickListener(this);
  					
        Button scanButton=(Button)findViewById(R.id.scanButton); // start to listen to the scan-button
        scanButton.setOnClickListener(this);
  			if (autoSyncEnabled()){ // if automatic updates are allowed, we will do so
					localDatabase.syncWithRemote(true);
				}
    }
    
		/* (non-Javadoc)
		 * @see android.app.Activity#onResume()
		 */
		@Override
    protected void onResume() {
    	super.onResume();
    	Log.d(TAG, "MainActivity.onResume()");
    	if (expired()) {
    		goHome();
    		return;
    	}
    	LearningActivity.productBarCode=null; // reset learning activity
    	AllergenList allAllergens = localDatabase.getAllAllergens();
      if (allAllergens.isEmpty()) { // if there are no allergens selected, yet:
      	Toast.makeText(getApplicationContext(), R.string.no_allergens_selected, Toast.LENGTH_LONG).show(); // send a waring
      	selectAllergens(); // show allergen selection view
      } else if (!deviceEnabled()){ // if device has not been enabled, yet:
      	AlertDialog alert=new AlertDialog.Builder(this).create(); // show warning message. learning mode will be toggled by the message button
      	alert.setTitle(R.string.hint);
      	alert.setMessage(getString(R.string.not_enabled).replace("#count", ""+missingCredits()));
      	alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), this); // this button will toggle learning mode 
      	alert.show(); // Pressing "OK" calls learnCode()      		
     	} else {
     		if (productCode!=null) handleProductCode(productCode); // if a product has been scanned before: handle it      			
      }
    }
		
    private int missingCredits() {
			return settings.getInt("missingCredits", 10);
		}

		/**
     * start the learning activity
     */
    private void learnCode() {
    	Log.d(TAG, "MainActivity.learnCode");
    	startActivity(new Intent(this,LearningActivity.class));    	
    }
      	
		/**
		 * request the internal id of the device. this id should be unique.
		 * @return 
		 */
		private String getDeviceId() {
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			return telephonyManager.getDeviceId();
		}
		
		/**
		 * for testversions: check, whether expiration date has been reached
		 */
		private boolean expired() {
    	Calendar currentDate = Calendar.getInstance();
    	Calendar expirationDate=Calendar.getInstance();
    	expirationDate.set(2014, 12, 15);
    	if (currentDate.after(expirationDate)){
    		Toast.makeText(getApplicationContext(), R.string.expired, Toast.LENGTH_LONG).show();
  			return true;
    	}
    	return false;
		}
		
		/**
		 * check, whether the device is enabled or has been enabled before
		 * @return true, only if the device has been enabled by the server once in the past
		 * @throws IOException
		 */
		private boolean deviceEnabled() {
			return settings.getBoolean("deviceEnabled", false); // if already enabled: skip checking and return true
    }

		/**
		 * check, whether the scanning library is available. if so: start the scanning activity
		 */
		private void scanCode() {
			Log.d(TAG, "Main.scanCode("+productCode+")");
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	if (scannerAvailable()){     		
    		startScanning();
    	}
    }
		
		/**
		 * handle a barcode aquired before
		 * @param code 
		 */
		private void handleProductCode(String code) {			
//  		AllergyScanDatabase database=new AllergyScanDatabase(this);
  		productData=localDatabase.getProduct(code); // check, if there already is information about the corrosponding product in the local db
			LearningActivity.productBarCode=code; // hand the product code to the learning activity
			// this is for the case, that the product is unknown, or data for a unclassified allergen shall be added
			
			
  		if (productData==null){ // product not known, yet. this means, that no information for this product in the context of the current allergens is available
  			Toast.makeText(getApplicationContext(), R.string.unknown_product, Toast.LENGTH_LONG).show();
  			Intent intent=new Intent(this,LearningActivity.class); // start the learning activity
  			startActivity(intent);
  		} else { // we already have information about this product
    		Log.d(TAG, productData.toString());
  			TextView productTitle = (TextView)findViewById(R.id.productTitle); // display the product title

  			productTitle.setText(productData.name());
  			int pid=productData.pid(); // get the product id
  			AllergenList allAllergens = localDatabase.getAllAllergens(); // get the list of activated allergens
  			listItems.clear(); // clear the display list

  			TreeSet<Integer> contained=localDatabase.getContainedAllergens(pid,allAllergens.keySet()); // get the list of contained allergens
  			
  			if (!contained.isEmpty()){ // add the contained allergens to the list dispayed
  				listItems.add(getString(R.string.contains));  			
  				for (int aid:contained) listItems.add("+ "+allAllergens.get(aid));
  			}

  			TreeSet<Integer> uncontained=localDatabase.getUnContainedAllergens(pid,allAllergens.keySet()); // get the list of allergens, which are not contained
  			if (!uncontained.isEmpty()){ // add the set of allergens, which are not contained ti the displayed list
  				listItems.add(getString(R.string.not_contained));  			
  				for (int aid:uncontained) listItems.add("- "+allAllergens.get(aid));
  			}
  			
  			Set<Integer> unclear = allAllergens.keySet(); // construct the list, of unclassified allergens
  			unclear.removeAll(contained);
  			unclear.removeAll(uncontained);
  			
  			if (!unclear.isEmpty()){
  				listItems.add(getString(R.string.unclear));  			
  				for (int aid:unclear) listItems.add("? "+allAllergens.get(aid)); // add the unclassified allergens to the displayed list
  			}
  			
  			adapter.notifyDataSetChanged(); // actually change the display    		
    	}
  		productCode=null;
    }

		/**
		 * start the scanning activity
		 */
		private void startScanning() {
			Intent intent=new Intent(LearningActivity.SCANNER+".SCAN");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra("SCAN_MODE", "PRODUCT_MODE");			
			startActivityForResult(intent, 0);
		}
		
		private Integer formatBytes(String format){
			format=format.toUpperCase(Locale.getDefault());
			if (format.equals("UPC_A")) return 10;
			if (format.equals("UPC_E")) return 11;
			if (format.equals("ITF")) return 12; 
			if (format.equals("EAN_13")) return 13;
			if (format.equals("RSS_14")) return 14; 
			if (format.equals("RSS_EXPANDED")) return 15; 
			if (format.equals("CODABAR")) return 16; 
			if (format.equals("EAN_8")) return 18;
			if (format.equals("CODE_39")) return 39;
			if (format.equals("CODE_93")) return 93;
			if (format.equals("CODE_128")) return 28;
			return null;
		}
		
    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      if (requestCode == 0) {
      		Log.w(TAG, "abort overridden in LearningActivity.onActivityResult!");
          if (resultCode == RESULT_OK) {
          	Integer fb=formatBytes(intent.getStringExtra("SCAN_RESULT_FORMAT"));
          	if (fb==null){
            	Log.d(TAG, "unknown SCAN_RESULT_FORMAT: "+intent.getStringExtra("SCAN_RESULT_FORMAT"));
          	} else {
          		productCode = formatBytes(intent.getStringExtra("SCAN_RESULT_FORMAT"))+intent.getStringExtra("SCAN_RESULT");
          	}
          } else if (resultCode == RESULT_CANCELED) {
          	Log.d(TAG, "scanning aborted");
          }
      	}
    }
		
		/**
		 * check, whether the barcode scanning library is available
		 * @return
		 */
		private boolean scannerAvailable() {
    	PackageManager pm = getPackageManager();
      try {
         pm.getApplicationInfo(LearningActivity.SCANNER, 0);
         return true;
      } catch (Exception e) { // if some exception occurs, this will be most likely caused by the missing scanner library
      	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
      	dialogBuilder.setTitle(R.string.warning);
        dialogBuilder.setMessage(R.string.no_scanner);
        dialogBuilder.setCancelable(false);
        AlertDialog dialog = dialogBuilder.create();
      	
      	dialog.setButton(DialogInterface.BUTTON_POSITIVE,getString(R.string.ok), new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						Log.d(TAG, "should start browser");
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.scannerUrl)));
						startActivity(browserIntent);
					}
				});
        dialog.show();

      	
      	return false;
      }
    }

		/**
		 * go back to the system desktop
		 */
		private void goHome() {
    	Toast.makeText(getApplicationContext(), R.string.will_shut_down, Toast.LENGTH_LONG).show();
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		}
    
		/**
		 * check, whether automatic updates are enabled on this device
		 * @return true, if automatic updates are not deactivated
		 */
		private boolean autoSyncEnabled() {    	
    	return settings.getBoolean(getString(R.string.auto_update), false);
    }

		/**
		 * start the activity, which lets the user select allergens
		 */
		private void selectAllergens() {
    	startActivity(new Intent(this,AllergenSelectionActivity.class));
    }

		/* (non-Javadoc)
		 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
		 */
		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {    	
    	boolean dummy = super.onOptionsItemSelected(item);
    	switch (item.getItemId()){
    		case R.id.allergen_selection: selectAllergens(); break;
    		case R.id.menu_settings: editPreferences(); break;
    		case R.id.update: localDatabase.syncWithRemote(true); break;
    		case R.id.info: showInfoActivity(); break;
    	}
      return dummy;
    }

		/**
		 * show the information activity
		 */
		private void showInfoActivity() {
			startActivity(new Intent(this,InfoActivity.class));
    }

		/**
		 * show the preferences activity
		 */
		private void editPreferences() {
			startActivity(new Intent(this,PreferencesActivity.class));
    }

		/* (non-Javadoc)
		 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
		 */
		public void onClick(DialogInterface arg0, int arg1) { // for clicks on "not yet enabled" dialog
			learnCode();
    }

		/* (non-Javadoc)
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		public void onClick(View v) { // for clicks on "scan" button
			scanCode();	    
    }
		
		/* (non-Javadoc)
		 * @see android.app.Activity#onSearchRequested()
		 */
		@Override
		public boolean onSearchRequested() {
		  boolean dummy = super.onSearchRequested();
		  scanCode(); // if the search button is pressed: scan barcode
		  return dummy;
		}

		/**
		 * if item in the allergen list is clicked
		 */
		public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
			String allergen = ((TextView) view).getText().toString();
			if (allergen.startsWith("?")||allergen.startsWith("+")||allergen.startsWith("-")){ // respond only to clicks on actual allergens
				allergen=allergen.substring(1).trim(); // get the name of the allergen, should be unique
				final Integer allergenId=localDatabase.getAid(allergen); // get the allergen id
				final Integer productId=productData.pid(); // get the product id
				final String productName=productData.name(); // get the product name
				
				/* here a dialog is built, which asks, whether the selected allergen is contained in the current product */
				AlertDialog.Builder alert = new AlertDialog.Builder(this);				
				alert.setTitle(allergen);
				alert.setMessage(getString(R.string.contains_question).replace("#product", productName).replace("#allergen", allergen));				
				alert.setPositiveButton(R.string.yes, new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) { // if "contained" clicked: store
						localDatabase.storeAllergenInfo(allergenId,productId,true);
						if (autoSyncEnabled()){
							localDatabase.syncWithRemote(true);
							handleProductCode(productData.code());
						}
					}
				});
				alert.setNegativeButton(R.string.no, new android.content.DialogInterface.OnClickListener() { // if "not contained" clicked: store
					public void onClick(DialogInterface dialog, int whichButton) {
						localDatabase.storeAllergenInfo(allergenId,productId,false);
						if (autoSyncEnabled()){
							localDatabase.syncWithRemote(true);
							handleProductCode(productData.code());
						}
					}
				});
				alert.setNeutralButton(R.string.dont_know, new android.content.DialogInterface.OnClickListener() { // if "don't know" clicked: ignore
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.d(TAG, "neutral");
						productCode=null;
					}
				});

				alert.show();
			}
		}

		
}
