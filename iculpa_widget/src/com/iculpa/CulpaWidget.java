package com.iculpa;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class CulpaWidget extends AppWidgetProvider {
	//uberwoot.
	private static int feedPos = 0;
	private static boolean feedStarted = false;
	private static Timer timer;
	public static String currentApology;
	public static Intent intent;
	public static AppWidgetManager appWM;
	public static RemoteViews views;
	public static PendingIntent pendingIntent;
	public static int appWidgetId;
	public static List<com.iculpa.Message> culpaFeed; 
	//private static TimerTask timerTask;
	
	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		Log.d("CulpaWidget", "Called onEnabled.");
	}
	
	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		try {
			context.stopService(new Intent(context, UpdateService.class)); //unregisterReceiver();
		} catch(Exception e) {
			Log.d("CulpaWidget", "Exception on disable", e);
		}
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		Log.d("CulpaWidgt", "Called onDeleted");
		try {
			if(feedStarted == true) {
				stopTimer();
			}
			context.stopService(new Intent(context, UpdateService.class));
		} catch(Exception e) {
			Log.d("CulpaWidget", "Exception on delete", e);
		}
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		//To prevent and ANR timeouts, perform the update in a service.
		context.startService(new Intent(context, UpdateService.class));
		
		appWidgetId = appWidgetIds[0];
		appWM = appWidgetManager;
				
		intent = new Intent(context, CulpaRSS.class);
		
		/*
		pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views = new RemoteViews(context.getPackageName(), R.layout.widget);
		views.setOnClickPendingIntent(R.id.widget_textview, pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetId, views);
		*/
		views = new RemoteViews(context.getPackageName(), R.layout.widget);
		
		Log.d("CulpaWidget", "Called onUpdate");
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		try {
			views.setOnClickPendingIntent(R.id.widget_textview, pendingIntent);
			appWM.updateAppWidget(appWidgetId, views);
		}
		catch(Exception e) {
			Log.e("CulpaWidget", "ERROR", e);
		}
		
		Log.d("CulpaWidget", "Called onReceive");
	}
	
	private static void stopTimer() {
		if(timer != null) {
			timer.cancel();
			//timerTask.cancel();
			Log.d("CulpaWidget", "Stopped Timer.");
		} else {
			Log.d("CulpaWidget", "Timer already stopped.");
		}
	}
	
	public static class UpdateService extends Service {
		/*
		@Override
		public void onCreate() {
			super.onCreate();
			startTimer();
		}
		*/
		
		private void startTimer() {
			Log.d("CulpaWidget", "Called startTimer.");
			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					RemoteViews updateViews = buildUpdate(getBaseContext());
					if(updateViews != null) {
						try {
							//Push update for this widget to home screen.
							ComponentName thisWidget = new ComponentName(getBaseContext(), CulpaWidget.class);
							if(thisWidget != null) {
								AppWidgetManager manager = AppWidgetManager.getInstance(getBaseContext());
								if(manager != null && updateViews != null) {
									manager.updateAppWidget(thisWidget, updateViews);
								}
							}
						} catch(Exception e) {
							Log.e("Widget", "Update Service Failed to Start", e);
						}
					}
					if(feedPos < 9) {
						feedPos = feedPos + 1;
					} else {
						feedPos = 0;
					}
				}
			}, 2000, 3600000); //Ten minute delay (600000), 1 hour update intervals (3600000)
		}
		
		@Override
		public void onStart(Intent intent, int startId) {
			Log.d("CulpaWidget", "Called onStart");
			RemoteViews uV = new RemoteViews(getBaseContext().getPackageName(), R.layout.widget);
			uV.setTextViewText(R.id.widget_textview, currentApology);
			
			try {
				XmlPullFeedParser xmlFeed = new XmlPullFeedParser("http://iculpa.com/rss.xml");
				culpaFeed = xmlFeed.parse();
			} catch(Exception e) {
				Log.e("CulpaWidget", "ERROR", e);
			}
			
			RemoteViews updateViews = buildUpdate(this);
			if(updateViews != null) {
				try {
					//Push update for this widget to home screen.
					ComponentName thisWidget = new ComponentName(this, CulpaWidget.class);
					if(thisWidget != null) {
						AppWidgetManager manager = AppWidgetManager.getInstance(this);
						if(manager != null && updateViews != null) {
							manager.updateAppWidget(thisWidget, updateViews);
							
							//Attempt intent onclick action
							/*
							Intent active = new Intent(getBaseContext(), CulpaWidget.class);
							active.setAction(String.valueOf(this));
							PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, active, 0);
							updateViews.setOnClickPendingIntent(R.id.widget_textview, pendingIntent);
							*/
							
							//Start RSS Timer.
							timer = new Timer();
							startTimer();
							feedStarted = true;
						}
					}
				} catch(Exception e) {
					Log.e("Widget", "Update Service Failed to Start", e);
				}
			}
			
		}
		
		public RemoteViews buildUpdate(Context context) {
			//Build an update that holds the updated widget contents
			RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget);
			try {
				String testString = culpaFeed.get(feedPos).toString();
				testString = HTMLEntities.unhtmlentities(testString);
				testString = HTMLEntities.unhtmlDoubleQuotes(testString);
				
				int stringLength = testString.length();
				//String lengthString = String.valueOf(stringLength);
				
				//Log.d("CulpaWidget", "STRINGLENGTH:"+lengthString);
				
				if(stringLength <= 80) {
					updateViews.setFloat(R.id.widget_textview, "setTextSize", 16);
					//Log.d("CulpaWidget", "Setting size to sixteen");
				} else if(stringLength <= 139) {
					updateViews.setFloat(R.id.widget_textview, "setTextSize", 12);
					//Log.d("CulpaWidget", "Setting size to twelve");
				} else if(stringLength >= 140) {
					updateViews.setFloat(R.id.widget_textview, "setTextSize", 10);
					//Log.d("CulpaWidget", "Setting size to ten");
				} else if(stringLength >= 350) {
					updateViews.setFloat(R.id.widget_textview, "setTextSize", 9);
					//Log.d("CulpaWidget", "Setting size to nine");
				}
				updateViews.setTextViewText(R.id.widget_textview, testString);
				
				currentApology = testString;
				
				intent.removeExtra("currentApology");
				intent.putExtra("currentApology", testString);
				
				try {
					pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
					views.setOnClickPendingIntent(R.id.widget_textview, pendingIntent);
					appWM.updateAppWidget(appWidgetId, views);
				} catch(Exception e) {
					Log.e("Culpa", "ERROR", e);
				}
				
				Log.d("CulpaWidget", "Called buildUpdate.");
				
			} catch(Exception e) {
				Log.e("CulpaWidget", "Error Updating Views", e);
			}
			return updateViews;
		}
		
		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}
	}
}



