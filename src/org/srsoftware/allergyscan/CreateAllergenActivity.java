package org.srsoftware.allergyscan;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AutoCompleteTextView;

import com.example.allergyscan.R;

public class CreateAllergenActivity extends Activity implements OnKeyListener {
	protected static String TAG="AllergyScan";
	private AutoCompleteTextView text;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "CreateAllergenActivity.onCreate");
        setContentView(R.layout.activity_create_allergen);
        
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
				try {
	        RemoteDatabase.storeAllergen(text.getText().toString().trim());
        } catch (IOException e) {
	        Log.e(TAG, "Failed to store allergen in online database");
        }
				finish();
				return true;
			}
			return false;
		}
}
