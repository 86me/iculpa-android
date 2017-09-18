package com.iculpa;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


public class CulpaRSS extends Activity {
	TextView rssText;
	
	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rss);
		
		rssText = (TextView)findViewById(R.id.rss_textview);
		
		Bundle extras = getIntent().getExtras();
		
		if(extras != null) {
			Log.d("CulpaRSS", extras.toString());
			String apologyText = extras.getString("currentApology");
			rssText.setText(apologyText);
		} else {
			rssText.setText("Error retrieving apology.");
		}
		
				
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    super.onActivityResult(requestCode, resultCode, intent);
	    Log.d("CulpaRSS", "Something happened.");
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		rssText = (TextView)findViewById(R.id.rss_textview);
		
		Bundle extras = getIntent().getExtras();
		
		if(extras != null) {
			Log.d("CulpaRSS", extras.toString());
			String apologyText = extras.getString("currentApology");
			rssText.setText(apologyText);
		} else {
			rssText.setText("Error retrieving apology.");
		}
		Log.d("CulpaRSS", "Called with new intent.");
		
	}
	
}