package org.srsoftware.allergyscan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import android.app.Activity;
import android.content.Intent;
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
		private TreeMap<Integer,String> availableAllergens;
		
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allergen_selection);
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
      try {
      	availableAllergens=RemoteDatabase.getAvailableAllergens();
      	if (availableAllergens==null || availableAllergens.isEmpty()){
        	Toast.makeText(getApplicationContext(), R.string.no_allergens_in_database, Toast.LENGTH_LONG).show();
        	createNewAllergen();
        } else {
          list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);      
        	@SuppressWarnings({ "unchecked", "rawtypes" })
          List<String> entries = new ArrayList(availableAllergens.values()); // make string list
        	Collections.sort(entries,String.CASE_INSENSITIVE_ORDER); // sort case insensitive
        	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, entries);
         	list.setAdapter(adapter);          	// Assign adapter to ListView         	         	
         	AllergyScanDatabase asd=new AllergyScanDatabase(getApplicationContext());
         	Collection<String> selectedNames = asd.getAllergenList().values();
         	int size=entries.size();
         	for (int i=0; i<size; i++){
         		if (selectedNames.contains(list.getItemAtPosition(i).toString())) list.setItemChecked(i, true);
         	}
        }
      } catch (IOException e){
      	Toast.makeText(getApplicationContext(), R.string.server_not_available, Toast.LENGTH_LONG).show();
      	goHome();
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
			SparseBooleanArray positions = list.getCheckedItemPositions();
		  int size=availableAllergens.size();
		  TreeSet<String> names=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		  for (int i=0; i<=size; i++) if (positions.get(i)) names.add(list.getItemAtPosition(i).toString());
		  TreeMap<Integer,String> selectedAllergens=new TreeMap<Integer, String>();
		  for (Entry<Integer, String> entry:availableAllergens.entrySet())	if (names.contains(entry.getValue())) selectedAllergens.put(entry.getKey(), entry.getValue());
		  (new AllergyScanDatabase(getApplicationContext())).setSelectedAllergens(selectedAllergens);
		  finish();
    }


		private void createNewAllergen() {
			Intent intent=new Intent(this,CreateAllergenActivity.class);
			startActivity(intent);			
		}
}
