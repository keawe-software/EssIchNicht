package com.srsoftware.allergyscan;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AutoCompleteTextView;

public class CreateAllergenActivity extends Activity implements OnKeyListener {
	private AutoCompleteTextView text;
	private AllergyScanDatabase localDatabase;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_allergen);
        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE); // create settings handle
        localDatabase=new AllergyScanDatabase(getApplicationContext(),settings); // create database handle
        text=(AutoCompleteTextView)findViewById(R.id.autoCompleteTextView1);
				text.setOnKeyListener(this);
    }

		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create_allergen, menu);
        return true;
    }

		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (event.getAction()==KeyEvent.ACTION_UP && keyCode==KeyEvent.KEYCODE_ENTER){
	      localDatabase.storeAllergen(text.getText().toString().trim());
				finish();
				return true;
			}
			return false;
		}
}
