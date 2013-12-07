package com.axway.ate.activity;

import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import com.axway.ate.R;
import com.axway.ate.fragment.AlertDialogFragment;
import com.axway.ate.util.UiUtils;

import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class ServiceAwareActivity extends BaseActivity {
	
	private static final String TAG = ServiceAwareActivity.class.getSimpleName();

	protected MainService service;
	private Handler handler;
	
	protected ServiceAwareActivity() {
		super();
		service = null;
		handler = null;
	}
	
	/*
	 * Override this to return a subclass of this activity's HandlerCallback
	 */
	protected HandlerCallback createHandlerCallback() {
		return new ServiceAwareActivity.HandlerCallback();
	}
	
	public class HandlerCallback implements Handler.Callback {

		@Override
		public boolean handleMessage(Message msg) {
			boolean rv = true;
			switch (msg.what) {
				case MainService.NOTIFY_EXCEPTION:
					handleException((Exception)msg.obj);
				break;
				case MainService.NOTIFY_CERT_NOT_TRUSTED:
					onCertNotTrusted((CertPath)msg.obj);
				break;
				case MainService.NOTIFY_CERTS_ADDED:
					if ((Boolean)msg.obj) {
						UiUtils.showToast(ServiceAwareActivity.this, R.string.cert_trusted);
						onCertsAddedToTruststore();
					}
					else
						UiUtils.showToast(ServiceAwareActivity.this, R.string.truststore_not_loaded);
				break;
				default:
					rv = false;
			}
			return rv;
		}
	}
	
	protected Handler getHandler() {
		if (handler == null) {
			handler = new Handler(createHandlerCallback());
		}
		return handler;
	}

	protected void afterServiceConnected(boolean isConnected) {
		//implement this to load content when service starts or destroy resources when disconnected
	}
	
    private ServiceConnection osc = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
        	Log.d(TAG, "onServiceConnected");
            service = ((MainService.LocalBinder)obj).getService();
            service.setHandler(getHandler());
            afterServiceConnected(true);
        }
        
        public void onServiceDisconnected(ComponentName classname) {
        	Log.d(TAG, "onServiceDisconnected");
            service = null;
            afterServiceConnected(false);
        }
    };

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "binding to service");
		bindService(new Intent(this, MainService.class), osc, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "unbinding from service");
		unbindService(osc);
	}

	protected void onCertNotTrusted(final CertPath cp) {
		Log.d(TAG, "certPath not trusted: " + cp.toString());
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, getString(R.string.cert_not_trusted));
		StringBuilder sb = new StringBuilder();
		int i = 1;
	    for (Certificate c: cp.getCertificates()) {
	    	if (c.getType() == "X.509") {
	    		X509Certificate c509 = (X509Certificate)c;
	    		sb.append("[").append(i++).append("]: ").append(c509.getSubjectDN().toString()).append("\n");
	    	}
	    }
	    sb.append("\n").append(getString(R.string.add_to_truststore));
		args.putString(Intent.EXTRA_TEXT,  sb.toString());
		AlertDialogFragment dlgFrag = new AlertDialogFragment();
		dlgFrag.setOnPositive(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (service != null) {
					service.addCertsToTrustStore(cp);
				}
			}
		});
		dlgFrag.setOnNegative(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		}, R.string.action_exit);
		dlgFrag.setArguments(args);
		FragmentManager fm = getFragmentManager();
		dlgFrag.show(fm.beginTransaction(), "yesnoDlg");
	}
	
	protected void onCertsAddedToTruststore() {
		Log.d(TAG, "certs added to truststore");
	}
}
