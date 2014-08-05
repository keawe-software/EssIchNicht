package com.srsoftware.allergyscan;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.srsoftware.allergyscan.R;

public class SynchronizeActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synchronize);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_synchronize, menu);
        return true;
    }
}
