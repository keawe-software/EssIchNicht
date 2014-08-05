package org.srsoftware.allergyscan;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.allergyscan.R;

public class AllergenSelectionActivity extends Activity implements OnClickListener {
		protected static String TAG="AllergyScan";
		private Button createButton,storeButton;
		private ListView list;
		private AllergenList availableAllergens;
		private SharedPreferences settings;
		private AllergyScanDatabase localDatabase;
		
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allergen_selection);


        settings=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
        localDatabase=new AllergyScanDatabase(getApplicationContext(),settings); // create database handle
        createButton=(Button)findViewById(R.id.createAllergenButton);
        storeButton=(Button)findViewById(R.id.storeAllergenSelection);
        list = (ListView)findViewById(R.id.listView1);
        createButton.setOnClickListener(this);
        storeButton.setOnClickListener(this);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    
    @Override
    protected void onResume() {
    	super.onResume();
      createListOfAllAllergens();

    };

    private void createListOfAllAllergens() {
      availableAllergens=localDatabase.getAllAllergens();
      if (availableAllergens==null || availableAllergens.isEmpty()){
       	Toast.makeText(getApplicationContext(), R.string.no_allergens_in_database, Toast.LENGTH_LONG).show();
       	createNewAllergen();
      } else {
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);      
       	List<Allergen> allergens = availableAllergens.values(); // make string list
       	ArrayAdapter<Allergen> adapter = new ArrayAdapter<Allergen>(this, android.R.layout.simple_list_item_multiple_choice,allergens);
       	list.setAdapter(adapter);          	// Assign adapter to ListView
       	int size=allergens.size();
       	for (int i=0; i<size; i++){
       		Object object = list.getItemAtPosition(i);
       		if (object instanceof Allergen){
       			if (((Allergen)object).active){
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

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }    

		public void onClick(View v) {
			if (v==storeButton){
				storeAllergenSelection();
			}
			if (v==createButton){
				createNewAllergen();
			}
		}

		private void storeAllergenSelection() {
     	int size=list.getCount();
     	Vector<Allergen> enabledAllergens=new Vector<Allergen>();
     	for (int i=0; i<size; i++){
     		Object object = list.getItemAtPosition(i);
     		if (object instanceof Allergen){
     			if (list.isItemChecked(i)){
     				enabledAllergens.add((Allergen) object);
     			}
       	}
      }
     	localDatabase.setEnabled(enabledAllergens);
		  finish();
    }


		private void createNewAllergen() {
			Intent intent=new Intent(this,CreateAllergenActivity.class);
			startActivity(intent);			
		}
}
