package com.iculpa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

//TODO: Clean up/remove commented code blocks

public class CulpaApp extends Activity {
	public static final int SUBMIT_APOLOGY = Menu.FIRST;
	public static final int SUBMIT_PREFS = Menu.CATEGORY_SYSTEM;
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	EditText apologyText;
	Button submitButton;
	HttpResponse response;
	String encodedMsg = "";
	LocationManager locationManager;
	LocationListener locationListener;
	double currentLat;
	double currentLon;
	
	/* Handler for threaded apologySubmit() calls. */
	final Handler mHandler = new Handler();
	
	/* Wrapper function for toast notifications. */
	private void toast(String text, String length) {
		if(length == "long") {
			Toast.makeText(this, text, Toast.LENGTH_LONG).show();
		} else if(length == "short") {
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
		}
	}
	
	private OnClickListener submitListener = new OnClickListener() {
		public void onClick(View v) {
			//Submit apology
			apologySubmit();
		}
	};
	
	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		submitButton = (Button)findViewById(R.id.apology_submit);
		apologyText = (EditText)findViewById(R.id.apology_input);
		
		submitButton.setOnClickListener(submitListener);
		
		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
			// Called when a new location is found by the network location provider.
			public void onLocationChanged(Location location) {
		      currentLat = location.getLatitude();
		      currentLon = location.getLongitude();
		      Log.d("EGON", "Lat: " + Double.toString(location.getLatitude()) +
		    		  		" Lon: " + Double.toString(location.getLongitude()));
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {}

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		};

		// Acquire a reference to the system Location Manager
		try {
			locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		} catch(IllegalArgumentException err) {
			//Error. Do nothing.
		}

		// Check to see if GPS is on.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		Boolean locationPref = prefs.getBoolean("locationPref", false);
		if(locationPref == true) {
			if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				createGpsDisabledAlert();
			}
		}
		
		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TWO_MINUTES, 20, locationListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TWO_MINUTES, 20, locationListener);


		if(savedInstanceState != null) {
			//String statusString = savedInstanceState.getString("apology_text");
			//Toast.makeText(this, statusString, Toast.LENGTH_LONG).show();
		} else {
			//Do nothing.
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, SUBMIT_APOLOGY, 0, R.string.apology_submit_text);
		menu.add(0, SUBMIT_PREFS, 0, R.string.prefs_submit_text);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case SUBMIT_APOLOGY:
			apologySubmit();
			return true;
		case SUBMIT_PREFS:
			//Open preferences
			Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
			startActivity(settingsActivity);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	/* URL encode string and post to iCulpa via API. */
	private void apologySubmit() {
		
		//Called after successful post.
		final Runnable mUpdateResults = new Runnable() {
			public void run() {
				//Submission complete
				apologyText.setText("");
				toast("Apology Submitted", "long");
			}
		};
		
		final Runnable mErrorMessage = new Runnable() {
			public void run() {
				//Not submitted
				toast("Apology cannot be blank", "short");
			}
		};
		
		//Fire off a thread to do some work in the background.
		Thread t = new Thread() {
			public void run() {
				//URLEncode apology string.
				try {
					encodedMsg = URLEncoder.encode(apologyText.getText().toString(), "UTF-8");
				} catch(UnsupportedEncodingException uee) {
					//Error.
				}
				
				//Check location preference.
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				Boolean locationPref = prefs.getBoolean("locationPref", false);
				Log.d("EGON", "Location Preference: " + Boolean.toString(locationPref));
				
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget;
				
				if(locationPref == false) {
					httpget = new HttpGet("http://iculpa.com/api/apology/" + encodedMsg);
					Log.d("EGON", "No LATLON");
				} else {
					httpget = new HttpGet("http://iculpa.com/api/apology/" + encodedMsg + "/" + currentLat + "/" + currentLon);
					Log.d("EGON", "With LATLON: lat:"+currentLat+" lon:"+currentLon);
				}
				
				try {
					//If apology field is non-empty, trigger HTTP request.
					if(apologyText.getText().toString().length() != 0) {
						//Execute HTTP Post Request
						response = httpclient.execute(httpget);
					
						//Success
						mHandler.post(mUpdateResults);
					} else {
						//Failure
						mHandler.post(mErrorMessage);
					}
					
				} catch(ClientProtocolException e) {
					apologyText.setText("ERROR #440: NOTIFY PROVIDER");
				} catch(IOException e) {
					//IO Error
				}
			}
		};
		t.start();
		
	}
	/**
	 * @see android.app.Activity@onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		Log.d("EGON", "Called onSaveInstanceState().");
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Log.d("EGON", "Called onRestoreInstanceState().");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		//Turn off GPS listener.
		locationManager.removeUpdates(locationListener);
		locationManager = null;
		Log.d("EGON", "Called onPause().");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(locationManager == null) {
			locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TWO_MINUTES, 20, locationListener);
		}
		Log.d("EGON", "Called onResume().");
	}
	
	/**
	 * GPS alert dialog.
	 */
	private void createGpsDisabledAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Your GPS is disabled. Would you like to enable it?")
			.setCancelable(false)
			.setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					showGpsOptions();
				}

				private void showGpsOptions() {
					Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(gpsOptionsIntent);
				}
			});
		builder.setNegativeButton("Do nothing", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("EGON", "Called onDestroy().");
	}
}
