package com.srsoftware.allergyscan;

import java.util.Calendar;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, android.content.DialogInterface.OnClickListener {

	static final int GOOD = 1;
	static final int FAIL = 0;
	static final int FATAL = -1;
	protected static String deviceid = null;
	private static SharedPreferences settings = null;
	private static AllergyScanDatabase localDatabase = null;
	static String TAG = "AllergyScan";
	public static int network_status = GOOD;
	static Barcode productCode;
	private ProductData product;
	private AlertDialog networkFailDialog = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		deviceid = getDeviceId(); // request the id and store in global variable
		setContentView(R.layout.activity_main);
		settings = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
		localDatabase = new AllergyScanDatabase(getApplicationContext(), settings); // create database handle


		Button scanButton = (Button) findViewById(R.id.scanButton); // start to listen to the scan-button
		scanButton.setOnClickListener(this);
		if (autoSyncEnabled()) { // if automatic updates are allowed, we will do so
			startSynchronizeActivity();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "MainActivity.onResume()");
		if (network_status != GOOD) {
			reportNetworkFail();
			return;
		}
		if (expired()) {
			goHome();
			return;
		}
		LearningActivity.productBarCode = null; // reset learning activity
		AllergenList chosenAllergens = localDatabase.getActiveAllergens();
		if (chosenAllergens.isEmpty()) { // if there are no allergens selected, yet:
			Toast.makeText(getApplicationContext(), R.string.no_allergens_selected, Toast.LENGTH_LONG).show(); // send a waring
			selectAllergens();
		} else {
			if (productCode != null) handleProductBarcode(productCode); // if a product has been scanned before: handle it
		}
	}

	public void reportNetworkFail() {
		Log.d(MainActivity.TAG, "MainActivity.reportNetworkFail()");
		if (network_status==FAIL){
			Toast.makeText(getApplicationContext(), R.string.network_problem, Toast.LENGTH_LONG).show();
		} else {
			networkFailDialog = new AlertDialog.Builder(this).create(); // show warning message. learning mode will be toggled by the message button
			networkFailDialog.setTitle(R.string.hint);		
			networkFailDialog.setMessage(getString(R.string.network_problem)+ " " + getString(R.string.will_shut_down));
			networkFailDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), this); // this button will toggle learning mode
			networkFailDialog.show();
		}
	}

	/**
	 * request the internal id of the device. this id should be unique.
	 * 
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
		Calendar expirationDate = Calendar.getInstance();
		expirationDate.set(2014, 12, 15);
		if (currentDate.after(expirationDate)) {
			Toast.makeText(getApplicationContext(), R.string.expired, Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	/**
	 * handle a barcode aquired before
	 * 
	 * @param barcode
	 */
	private void handleProductBarcode(Barcode barcode) {
		// AllergyScanDatabase database=new AllergyScanDatabase(this);
		product = localDatabase.getProduct(barcode); // check, if there already is information about the corrosponding product in the local db
		LearningActivity.productBarCode = barcode; // hand the product code to the learning activity
		// this is for the case, that the product is unknown, or data for a unclassified allergen shall be added

		if (product == null) { // product not known, yet. this means, that no information for this product in the context of the current allergens is available
			Toast.makeText(getApplicationContext(), R.string.unknown_product, Toast.LENGTH_LONG).show();
			startLearningActivity();
		} else { // we already have information about this product
			Log.d(TAG, product.toString());
			TextView productTitle = (TextView) findViewById(R.id.productTitle); // display the product title
			TextView changeHint = (TextView) findViewById(R.id.changeHint);
			productTitle.setText(product.name());
			changeHint.setText(R.string.change_hint);
			AllergenList allAllergens = localDatabase.getActiveAllergens(); // get the list of activated allergens
			LinearLayout list = (LinearLayout) findViewById(R.id.containmentList);
			list.removeAllViews();

			TreeSet<Integer> aidsOfContainedAllergens = localDatabase.getContainedAllergens(product.barcode(), allAllergens.keySet()); // get the list of contained allergens
			Vector<Allergen>contained=allAllergens.selectByLocalAId(aidsOfContainedAllergens);
			Button button;
			if (!contained.isEmpty()) { // add the contained allergens to the list dispayed
				TextView hint=new TextView(this);
				hint.setText(R.string.contains);
				list.addView(hint);
				for (Allergen allergen : contained){
					button=new Button(this);
					button.setText("+ " + allergen.name);
					button.setGravity(Gravity.LEFT);
					button.setBackgroundColor(Color.MAGENTA);
					button.setTag(allergen);
					button.setOnClickListener(this);
					list.addView(button);
				}
			}

			TreeSet<Integer> aidsOfUncontainedAllergens = localDatabase.getUnContainedAllergens(product.barcode(), allAllergens.keySet()); // get the list of allergens, which are not contained
			Vector<Allergen>uncontained=allAllergens.selectByLocalAId(aidsOfUncontainedAllergens);
			if (!uncontained.isEmpty()) { // add the set of allergens, which are not contained ti the displayed list
				TextView hint=new TextView(this);
				hint.setText(R.string.not_contained);
				list.addView(hint);
				for (Allergen allergen : uncontained){
					button=new Button(this);
					button.setText("- " + allergen.name);
					button.setGravity(Gravity.LEFT);		
					button.setBackgroundColor(Color.GREEN);
					button.setTag(allergen);
					button.setOnClickListener(this);
					list.addView(button);
				}
			}

			AllergenList unclear = allAllergens; // construct the list, of unclassified allergens
			unclear.removeAll(aidsOfContainedAllergens);
			unclear.removeAll(aidsOfUncontainedAllergens);

			if (!unclear.isEmpty()) {
				TextView hint=new TextView(this);
				hint.setText(R.string.unclear);
				list.addView(hint);
				for (Allergen allergen : unclear.values()){
					button=new Button(this);
					button.setText("? " + allergen.name);
					button.setGravity(Gravity.LEFT);
					button.setBackgroundColor(Color.YELLOW);
					button.setOnClickListener(this);
					button.setTag(allergen);
					list.addView(button);
				}
			}
		}
		// productCode=null;
	}

	private void startLearningActivity() {
		Intent intent = new Intent(this, LearningActivity.class); // start the learning activity
		startActivity(intent);
	}

	/**
	 * start the scanning activity
	 */
	private void startScanning() {
		if (MainActivity.deviceid.equals("000000000000000")) {
			productCode = Barcode.random();
			handleProductBarcode(productCode);
		} else if (scannerAvailable(this)) {
			Intent intent = new Intent(LearningActivity.SCANNER + ".SCAN");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
			startActivityForResult(intent, 0);
		}
	}

	/**
	 * check, whether the barcode scanning library is available
	 * 
	 * @return
	 */
	static boolean scannerAvailable(final Context c) {
		if (deviceid.equals("000000000000000")) return true;
		PackageManager pm = c.getPackageManager();
		try {
			pm.getApplicationInfo(LearningActivity.SCANNER, 0);
			return true;
		} catch (Exception e) { // if some exception occurs, this will be most likely caused by the missing scanner library
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(c);
			dialogBuilder.setTitle(R.string.warning);
			dialogBuilder.setMessage(R.string.no_scanner);
			dialogBuilder.setCancelable(false);
			AlertDialog dialog = dialogBuilder.create();
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, c.getString(R.string.cancel), new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					
				}
			});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, c.getString(R.string.play), new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					Log.d(TAG, "should start browser");
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(c.getString(R.string.playUrl)));
					c.startActivity(browserIntent);
				}
			});
			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, c.getString(R.string.fdroid), new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					Log.d(TAG, "should start browser");
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(c.getString(R.string.fdroidUrl)));
					c.startActivity(browserIntent);
				}
			});
			dialog.show();

			return false;
		}
	}

	static Barcode getBarCode(Intent intent) {
		Integer fb = formatBytes(intent.getStringExtra("SCAN_RESULT_FORMAT"));
		if (fb == null) {
			Log.d(TAG, "unknown SCAN_RESULT_FORMAT: " + intent.getStringExtra("SCAN_RESULT_FORMAT"));
			return null;
		}
		String code = fb + intent.getStringExtra("SCAN_RESULT");
		return new Barcode(code);
	}

	private static Integer formatBytes(String format) {
		format = format.toUpperCase(Locale.getDefault());
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
			Log.w(TAG, "abort overridden in LearningActivity.onActivityResult!");
			if (resultCode == RESULT_OK) {
				productCode = getBarCode(intent);
			} else if (resultCode == RESULT_CANCELED) {
				Log.d(TAG, "scanning aborted");
			}
		}
	}

	/**
	 * go back to the system desktop
	 */
	void goHome() {
		network_status=GOOD;
		Toast.makeText(getApplicationContext(), R.string.will_shut_down, Toast.LENGTH_LONG).show();
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}

	/**
	 * start the activity, which lets the user select allergens
	 */
	private void selectAllergens() {
		startActivity(new Intent(this, AllergenSelectionActivity.class));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean dummy = super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.allergen_selection:
			selectAllergens();
			break;
		case R.id.menu_settings:
			editPreferences();
			break;
		case R.id.update:
			startSynchronizeActivity();
			break;
		case R.id.info:
			showInfoActivity();
			break;
		}
		return dummy;
	}

	/**
	 * show the synchronization activity
	 */
	private void startSynchronizeActivity() {
		startActivity(new Intent(this, SynchronizeActivity.class)); // start the learning activity
	}

	/**
	 * show the information activity
	 */
	private void showInfoActivity() {
		startActivity(new Intent(this, InfoActivity.class));
	}

	/**
	 * show the preferences activity
	 */
	private void editPreferences() {
		startActivity(new Intent(this, PreferencesActivity.class));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
	 */
	public void onClick(DialogInterface dialog, int arg1) { // for clicks on "not yet enabled" dialog
		Log.d(TAG, "onClick(DialogInterface arg0, int arg1)");
		if (dialog == networkFailDialog) if (network_status == FATAL) {
			goHome();
		} else {
			network_status = GOOD;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) { // for clicks on "scan" button
		Log.d(TAG, "onClick(View v)");
		Object tag=v.getTag();
		if (tag==null){
			startScanning();
		} else {
			if (tag instanceof Allergen) {// respond only to clicks on actual allergens			
				final Allergen allergen = (Allergen) tag;
				final Barcode barcode = product.barcode(); // get the product id
				final String productName = product.name(); // get the product name

				/* here a dialog is built, which asks, whether the selected allergen is contained in the current product */
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(allergen.name);
				alert.setMessage(getString(R.string.contains_question).replace("#product", productName).replace("#allergen", allergen.name));
				alert.setPositiveButton(R.string.yes, new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) { // if "contained" clicked: store
						localDatabase.storeAllergenInfo(allergen, barcode, true);
						handleProductBarcode(barcode);
					}
				});
				alert.setNegativeButton(R.string.no, new android.content.DialogInterface.OnClickListener() { // if "not contained" clicked: store
					public void onClick(DialogInterface dialog, int whichButton) {
						localDatabase.storeAllergenInfo(allergen, barcode, false);
						handleProductBarcode(barcode);
					}
				});
				alert.setNeutralButton(R.string.dont_know, new android.content.DialogInterface.OnClickListener() { // if "don't know" clicked: ignore
					public void onClick(DialogInterface dialog, int whichButton) {
						localDatabase.resetAllergenInfo(allergen, barcode);
						handleProductBarcode(barcode);
					}
				});
				alert.show();							
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSearchRequested()
	 */
	@Override
	public boolean onSearchRequested() {
		boolean dummy = super.onSearchRequested();
		startScanning(); // if the search button is pressed: scan barcode
		return dummy;
	}

	/**
	 * check, whether automatic updates are enabled on this device
	 * 
	 * @return true, if automatic updates are not deactivated
	 */
	private boolean autoSyncEnabled() {
		return settings.getBoolean("autoUpdate", false);
	}
}
