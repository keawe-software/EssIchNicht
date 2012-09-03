package org.srsoftware.allergyscan;

import com.example.allergyscan.R;
import com.example.allergyscan.R.layout;
import com.example.allergyscan.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class InfoActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_info, menu);
        return true;
    }
}
