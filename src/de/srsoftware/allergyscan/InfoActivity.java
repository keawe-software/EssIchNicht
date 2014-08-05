package de.srsoftware.allergyscan;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.srsoftware.allergyscan.R;

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
