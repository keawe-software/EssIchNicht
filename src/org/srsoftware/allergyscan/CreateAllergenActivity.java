package org.srsoftware.allergyscan;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.example.allergyscan.R;

public class CreateAllergenActivity extends Activity {
	protected static String TAG="AllergyScan";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "CreateAllergenActivity.onCreate");
        setContentView(R.layout.activity_create_allergen);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create_allergen, menu);
        return true;
    }
}
