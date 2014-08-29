package com.srsoftware.allergyscan;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

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
    	autoUpdateCheckbox.setChecked(settings.getBoolean("autoUpdate", true));
      autoUpdateCheckbox.setOnClickListener(this);      
    }

		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_prefernces, menu);
        return true;
    }

		public void onClick(View v) {
			settings.edit().putBoolean("autoUpdate", autoUpdateCheckbox.isChecked()).commit();
    }

}
