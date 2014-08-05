package com.srsoftware.allergyscan;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.srsoftware.allergyscan.R;

public class SynchronizeActivity extends Activity implements OnClickListener {

    private Button alwaysOkButton;
		private Button onceOkButton;
		private Button noButton;

		@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synchronize);
        
        alwaysOkButton=(Button)findViewById(R.id.alway_ok);
        alwaysOkButton.setOnClickListener(this);
        
        onceOkButton=(Button)findViewById(R.id.once_ok);
        onceOkButton.setOnClickListener(this);

        noButton=(Button)findViewById(R.id.dont_sync);
        noButton.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_synchronize, menu);
        return true;
    }

		public void onClick(View v) {
			if (v==alwaysOkButton){
				Log.d("TEst", "ok");
			}
			if (v==onceOkButton){
				Log.d("TEst", "ok once");
			}
			if (v==noButton){
				Log.d("TEst", "not ok");
			}
		}
}
