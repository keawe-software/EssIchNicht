package org.srsoftware.allergyscan;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.allergyscan.R;

public class AllergenSelectionActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allergen_selection);
        ListView list = (ListView)findViewById(R.id.listView1);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        String[] values={ "Android", "iPhone", "WindowsMobile","Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X","Linux", "OS/2" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        	  android.R.layout.simple_list_item_1, android.R.id.text1, values);

        	// Assign adapter to ListView
        	list.setAdapter(adapter);
        
        //getActionBar().setDisplayHomeAsUpEnabled(true);
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

}
