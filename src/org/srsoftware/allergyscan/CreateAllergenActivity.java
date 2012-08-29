package org.srsoftware.allergyscan;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AutoCompleteTextView;

import com.example.allergyscan.R;

public class CreateAllergenActivity extends Activity {
	protected static String TAG="AllergyScan";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "CreateAllergenActivity.onCreate");
        setContentView(R.layout.activity_create_allergen);
        
        final AutoCompleteTextView text=(AutoCompleteTextView)findViewById(R.id.autoCompleteTextView1);
        OnKeyListener listener=new OnKeyListener() {
					
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						if (event.getAction()==KeyEvent.ACTION_UP && keyCode==KeyEvent.KEYCODE_ENTER){
							storeAllergen(text.getText().toString().trim());
						}
						return false;
					}
				};
				text.setOnKeyListener(listener);
    }

    protected void storeAllergen(String allergen) {
			Log.d(TAG, "here, \""+allergen+"\" should be stored");

    }

		@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create_allergen, menu);
        return true;
    }
}
