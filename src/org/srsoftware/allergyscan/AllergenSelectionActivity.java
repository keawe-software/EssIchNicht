package org.srsoftware.allergyscan;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
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
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allergen_selection);
        Button btn=(Button)findViewById(R.id.createAllergenButton);
        btn.setOnClickListener(this);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    
    @Override
    protected void onResume() {
    	super.onResume();
      addAllergensToList();

    };

    private void addAllergensToList() {
      ListView list = (ListView)findViewById(R.id.listView1);
      list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
      TreeMap<Integer,String> availableAllergens=RemoteDatabase.getAvailableAllergens();
      if (availableAllergens.isEmpty()){
      	Toast.makeText(getApplicationContext(), R.string.no_allergens_in_database, Toast.LENGTH_LONG).show();
      	createNewAllergen();
      } else {
      	Set<Entry<Integer, String>> entries = availableAllergens.entrySet();
      	String[] values=new String[entries.size()];
      	int index=0;
      	for (Entry<Integer, String> entry: entries)	values[index++]=entry.getValue();
      	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, values);
        	// Assign adapter to ListView
       	list.setAdapter(adapter);
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
			createNewAllergen();
		}

		private void createNewAllergen() {
			Intent intent=new Intent(this,CreateAllergenActivity.class);
			startActivity(intent);			
		}

}
