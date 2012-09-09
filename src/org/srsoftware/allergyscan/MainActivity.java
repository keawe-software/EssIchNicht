package org.srsoftware.allergyscan;

import java.io.IOException;
import java.util.ArrayList;
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.allergyscan.R;

public class MainActivity extends Activity implements OnClickListener, android.content.DialogInterface.OnClickListener, OnItemClickListener {

	protected static String deviceid = null;
	protected static String TAG="AllergyScan";
	AllergyScanDatabase database=null;
	private String productCode;
	private ProductData productData;
	private ListView list;
	private ArrayList<String> listItems;
	private ArrayAdapter adapter;
	private SharedPreferences settings;
	
		/**
		 * request the internal id of the device might be unique.
		 */
		private void getDeviceId() {
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			deviceid = telephonyManager.getDeviceId();
			if (deviceid.equals("000000000000000")) deviceid="356812044161832"; // TODO: this should be removed, later on
		}
	
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDeviceId(); // request the id and store in global variable
      	database=new AllergyScanDatabase(getApplicationContext()); // create database handle
        setContentView(R.layout.activity_main); 
        settings=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
        
        /* prepare result list */
  			list=(ListView)findViewById(R.id.containmentList);
  			listItems=new ArrayList<String>(); 
  			adapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,listItems);
  			list.setAdapter(adapter);
  			list.setOnItemClickListener(this);
  					
        Button scanButton=(Button)findViewById(R.id.scanButton); // start to listen to the scan-button
        scanButton.setOnClickListener(this);
    }
    
		/* (non-Javadoc)
		 * @see android.app.Activity#onResume()
		 */
		@Override
    protected void onResume() {
    	super.onResume();
    	Log.d(TAG, "MainActivity.onResume()");
      if (database.getAllergenList().isEmpty()) { // if there are no allergens selected, yet:
      	Toast.makeText(getApplicationContext(), R.string.no_allergens_selected, Toast.LENGTH_LONG).show(); // send a waring
      	selectAllergens(); // show allergen selection view
      } else {
      	try {      		
      		if (!deviceEnabled()){ // if device has not been enabled, yet:
      			AlertDialog alert=new AlertDialog.Builder(this).create(); // show warning message. learning mode will be toggled by the message button
      			alert.setTitle(R.string.hint);
      			alert.setMessage(getString(R.string.not_enabled).replace("#count", ""+RemoteDatabase.missingCredits()));
      			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), this); // this button will toggle learning mode
      			alert.show();      		
      		} else {
      			if (autoUpdate()) doUpdate();
      			if (productCode!=null) handleResult(); // if a product has been scanned before: handle it      			
      		}
      	} catch (IOException e){
      		Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
      		goHome();
      	}
      }
    }
		
		private boolean deviceEnabled() throws IOException {
			boolean deviceEnabled=settings.getBoolean("deviceEnabled", false);
			if (deviceEnabled) return true;
			deviceEnabled=RemoteDatabase.deviceEnabled();
			if (deviceEnabled) {
				Toast.makeText(getApplicationContext(), R.string.now_enabled, Toast.LENGTH_LONG).show();
				settings.edit().putBoolean("deviceEnabled", true).commit();
				
			}
			return deviceEnabled;
    }

		private void scanCode() {
			Log.d(TAG, "Main.scanCode("+productCode+")");
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	if (scannerAvailable()){    		
    		startScanning();
    	}
    }
		
		private void handleResult() {			
  		AllergyScanDatabase database=new AllergyScanDatabase(this);
  		productData=database.getProduct(productCode);
  		if (productData==null){
  			Toast.makeText(getApplicationContext(), R.string.unknown_product, Toast.LENGTH_LONG).show();
  			LearningActivity.productBarCode=productCode;
  			Intent intent=new Intent(this,LearningActivity.class);
  			startActivity(intent); // TODO: hier wird dann der Barcode nochmal gelesen. Besser wäre den gelesenen Code mitzugeben!
  		} else {
    		Log.d(TAG, productData.toString());
  			TextView productTitle = (TextView)findViewById(R.id.productTitle);

  			productTitle.setText(productData.name());
  			int pid=productData.pid();
  			TreeMap<Integer, String> allAllergens = database.getAllergenList();
  			LearningActivity.productBarCode=productCode;
  			listItems.clear();

  			TreeSet<Integer> contained=database.getContainedAllergens(pid,allAllergens.keySet());
  			
  			if (!contained.isEmpty()){
  				listItems.add(getString(R.string.contains));  			
  				for (int aid:contained) listItems.add("+ "+allAllergens.get(aid));
  			}

  			TreeSet<Integer> uncontained=database.getUnContainedAllergens(pid,allAllergens.keySet());
  			if (!uncontained.isEmpty()){
  				listItems.add(getString(R.string.not_contained));  			
  				for (int aid:uncontained) listItems.add("- "+allAllergens.get(aid));
  			}
  			
  			Set<Integer> unclear = allAllergens.keySet();
  			unclear.removeAll(contained);
  			unclear.removeAll(uncontained);
  			if (!unclear.isEmpty()){
  				listItems.add(getString(R.string.unclear));  			
  				for (int aid:unclear) listItems.add("? "+allAllergens.get(aid));
  			}
  			
  			adapter.notifyDataSetChanged();    		
    	}
  		productCode=null;
    }

		private void startScanning() {
			Intent intent=new Intent(LearningActivity.SCANNER+".SCAN");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra("SCAN_MODE", "PRODUCT_MODE");			
			startActivityForResult(intent, 0);
		}
		
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      if (requestCode == 0) {
      		// TODO: remove random code setting and warning message for final version
      		Log.w(TAG, "abort overridden in LearningActivity.onActivityResult!");
          if (resultCode == RESULT_OK) {
          		productCode = intent.getStringExtra("SCAN_RESULT_FORMAT")+"~"+intent.getStringExtra("SCAN_RESULT");
          } else if (resultCode == RESULT_CANCELED) {
          	// 	/*
          	productCode = LearningActivity.randomCode();
          	/*/
          	Log.d(TAG, "scanning aborted");
          	finish(); //*/
          }
      	}
    }
		
		private boolean scannerAvailable() {
    	PackageManager pm = getPackageManager();
      try {
         ApplicationInfo appInfo = pm.getApplicationInfo(LearningActivity.SCANNER, 0);
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
    
    private void learnCode() {
    	startActivity(new Intent(this,LearningActivity.class));    	
    }

		private void doUpdate() {  		
  		try {
	      if (!deviceEnabled()) return;
      } catch (IOException e) {
	      e.printStackTrace();
	      return;
      } 
	    Toast.makeText(getApplicationContext(), R.string.performing_update, Toast.LENGTH_SHORT).show();
  		
	    try {
	      RemoteDatabase.update(database);
      } catch (IOException e) {
    		Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
      }
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
    		case R.id.update: doUpdate(); break;
    		case R.id.info: showInfoActivity(); break;
    	}
      return dummy;
    }

		private void showInfoActivity() {
			startActivity(new Intent(this,InfoActivity.class));
    }

		private void editPreferences() {
			startActivity(new Intent(this,PreferencesActivity.class));
    }

		public void onClick(DialogInterface arg0, int arg1) { // for clicks on "not yet enabled" dialog
			learnCode();
    }

		public void onClick(View v) { // for clicks on "scan" button
			scanCode();	    
    }
		
		@Override
		public boolean onSearchRequested() {
		  boolean dummy = super.onSearchRequested();
		  scanCode();
		  return dummy;
		}

		public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
			String allergen = ((TextView) view).getText().toString();
			if (allergen.startsWith("?")||allergen.startsWith("+")||allergen.startsWith("-")){
				allergen=allergen.substring(1).trim();
				AllergyScanDatabase asd=new AllergyScanDatabase(getApplicationContext());
				final Integer allergenId=asd.getAid(allergen);
				final Integer productId=productData.pid();
				final String productName=productData.name();
				
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				
				alert.setTitle(allergen);
				alert.setMessage(getString(R.string.contains_question).replace("#product", productName).replace("#allergen", allergen));
				
				alert.setPositiveButton(R.string.yes, new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						try {
							RemoteDatabase.storeAllergenInfo(allergenId,productId,true);
							if (autoUpdate()){
								doUpdate();
								productCode=productData.code();
								handleResult();
							}
						} catch (IOException e) {
							Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG);
							finish();
						}
					}
				});
				alert.setNegativeButton(R.string.no, new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						try {
							RemoteDatabase.storeAllergenInfo(allergenId,productId,false);
							if (autoUpdate()){
								doUpdate();
								productCode=productData.code();
								handleResult();
							}
						} catch (IOException e) {
							Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG);
							finish();
						}
					}
				});
				alert.setNeutralButton(R.string.dont_know, new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.d(TAG, "neutral");
						productCode=null;
					}
				});

				alert.show();
			}
		}

		
}
