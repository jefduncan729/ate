package com.axway.ate.service;

import com.axway.ate.util.UiUtils;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.widget.Toast;

abstract public class BaseIntentService extends IntentService {
	
	public static final String ACTION_BASE = "com.axway.ate.";

	private SharedPreferences prefs;
	private NotificationManager notificationMgr;
	private Handler handler;
	protected Context context;
	private ResultReceiver resRcvr;
	
	private class DisplayToast implements Runnable {

		String msg;
		int len;
		
		public DisplayToast(String msg) {
			this(msg, Toast.LENGTH_SHORT);
		}

		public DisplayToast(String msg, int len) {
			super();
			this.msg = msg;
			this.len = len;
		}
		
		@Override
		public void run() {
			if (len != Toast.LENGTH_LONG && len != Toast.LENGTH_SHORT)
				len = Toast.LENGTH_SHORT;
			if (len == Toast.LENGTH_SHORT)
				UiUtils.showToast(context, msg);
			else
				UiUtils.showToastLong(context, msg);
		}
	}

	protected BaseIntentService(String name) {
		super(name);
		prefs = null;
		notificationMgr = null;
		handler = null;
		context = null;
		resRcvr = null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
		handler = new Handler();
	}

	@Override
	public void onDestroy() {
		handler = null;
		notificationMgr = null;
		prefs = null;
		context = null;
		super.onDestroy();
	}

	protected void showToast(String msg) {
		handler.post(new DisplayToast(msg));
	}

	protected void showToastLong(String msg) {
		handler.post(new DisplayToast(msg, Toast.LENGTH_LONG));
	}

	protected SharedPreferences getPrefs() {
		if (prefs == null) {
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		return prefs;
	}
	
	protected NotificationManager getNotificationManager() {
		if (notificationMgr == null)
			notificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		return notificationMgr;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		resRcvr = intent.getParcelableExtra(Intent.EXTRA_RETURN_RESULT);
	}
	
	protected ResultReceiver getResultReceiver() {
		return resRcvr;
	}
}
