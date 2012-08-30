package org.srsoftware.allergyscan;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import com.example.allergyscan.R;

public class PreferencesActivity extends Activity implements OnClickListener {
	
		private CheckBox autoUpdateCheckbox;
		protected static String TAG="AllergyScan";
		private SharedPreferences settings;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        preapareCheckBox();
    }

    private void preapareCheckBox() {
      autoUpdateCheckbox=(CheckBox)findViewById(R.id.autoUpdateCheckbox);        
    	settings=getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
    	autoUpdateCheckbox.setChecked(settings.getBoolean(getString(R.string.auto_update), true));
      autoUpdateCheckbox.setOnClickListener(this);      
    }

		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_prefernces, menu);
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
			settings.edit().putBoolean(getString(R.string.auto_update), autoUpdateCheckbox.isChecked()).commit();
    }

}
