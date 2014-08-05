package com.srsoftware.allergyscan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

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
				finish();
				goHome();
			}
		}
		
		/**
		 * go back to the system desktop
		 */
		void goHome() {
    	Toast.makeText(getApplicationContext(), R.string.will_shut_down, Toast.LENGTH_LONG).show();
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		}
}
