package com.srsoftware.allergyscan;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class AllergenSelectionActivity extends Activity implements OnClickListener {
	private Button createButton, storeButton;
	private ListView list;
	private AllergenList availableAllergens;
	private SharedPreferences settings;
	private AllergyScanDatabase localDatabase;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_allergen_selection);

		settings = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
		localDatabase = new AllergyScanDatabase(getApplicationContext(), settings); // create database handle
		createButton = (Button) findViewById(R.id.createAllergenButton);
		storeButton = (Button) findViewById(R.id.storeAllergenSelection);
		list = (ListView) findViewById(R.id.listView1);
		createButton.setOnClickListener(this);
		storeButton.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (MainActivity.network_status != MainActivity.GOOD){
			finish();
			return;
		}
		createListOfAllAllergens();
	};
	
	private void startSynchronizeActivity() {
		startSynchronizeActivity(false);
	}

	private void startSynchronizeActivity(boolean urgent) {
		SynchronizeActivity.urgent=urgent;
		startActivity(new Intent(this, SynchronizeActivity.class)); // start the learning activity
	}

	private void createListOfAllAllergens() {
		availableAllergens = localDatabase.getAllAllergens();
		if (availableAllergens == null || availableAllergens.isEmpty()) {
			Toast.makeText(getApplicationContext(), R.string.no_allergens_in_database, Toast.LENGTH_LONG).show();
			startSynchronizeActivity(true);
		} else {
			list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			List<Allergen> allergens = availableAllergens.values(); // make string list
			ArrayAdapter<Allergen> adapter = new ArrayAdapter<Allergen>(this, android.R.layout.simple_list_item_multiple_choice, allergens);
			list.setAdapter(adapter); // Assign adapter to ListView
			int size = allergens.size();
			for (int i = 0; i < size; i++) {
				Object object = list.getItemAtPosition(i);
				if (object instanceof Allergen) {
					if (((Allergen) object).active) {
						list.setItemChecked(i, true);
					}
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_allergen_selection, menu);
		return true;
	}

	public void onClick(View v) {
		if (v == storeButton) {
			storeAllergenSelection();
		}
		if (v == createButton) {
			storeAllergenSelection(false);
			createNewAllergen();
		}
	}

	private void storeAllergenSelection() {
		storeAllergenSelection(true);
	}

	private void storeAllergenSelection(boolean updateAfter) {
		int size = list.getCount();
		Vector<Allergen> enabledAllergens = new Vector<Allergen>();
		for (int i = 0; i < size; i++) {
			Object object = list.getItemAtPosition(i);
			if (object instanceof Allergen) {
				if (list.isItemChecked(i)) {
					enabledAllergens.add((Allergen) object);
				}
			}
		}
		if (!showFreeVersionHint(enabledAllergens)) {
			localDatabase.setEnabled(enabledAllergens);
			if (updateAfter){
				finish();
				startSynchronizeActivity();
			}
		}
	}

	private boolean showFreeVersionHint(Vector<Allergen> enabledAllergens) {
		if (enabledAllergens.size() > 2) {
			AlertDialog ad = new AlertDialog.Builder(this).create();
			ad.setCancelable(false); // disable Back button
			ad.setMessage(getString(R.string.free_version_hint));
			ad.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			ad.show();
			return true;
		}
		return false;
	}

	private void createNewAllergen() {
		Intent intent = new Intent(this, CreateAllergenActivity.class);
		startActivity(intent);
	}
}
