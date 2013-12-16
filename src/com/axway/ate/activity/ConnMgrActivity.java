package com.axway.ate.activity;

import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.ServerInfo;
import com.axway.ate.db.DbHelper.ConnMgrColumns;
import com.axway.ate.fragment.AlertDialogFragment;
import com.axway.ate.fragment.ConnMgrDialog;
import com.axway.ate.fragment.ConnMgrFragment;
import com.axway.ate.fragment.ConnMgrFragment.ConnMgrListener;
import com.axway.ate.service.CertificateService;
import com.axway.ate.util.UiUtils;

public class ConnMgrActivity extends BaseActivity implements ConnMgrListener {
	
	private static final String TAG = ConnMgrActivity.class.getSimpleName();

	private ConnMgrFragment frag;
	private ResultReceiver resRcvr;
	private Uri checkingUri;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		checkingUri = null;
		setContentView(R.layout.single_pane);
        if (savedInstanceState == null) {
            frag = new ConnMgrFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.container01, frag, "single_pane")
                    .commit();
        } 
        else {
            frag = (ConnMgrFragment)getFragmentManager().findFragmentByTag("single_pane");
        }
	}

	private ResultReceiver getResultReceiver() {
		if (resRcvr == null) {
			resRcvr = new ResultReceiver(new Handler()) {
				@Override
				protected void onReceiveResult(int resultCode, Bundle resultData) {
					switch (resultCode) {
						case Constants.CERT_NOT_TRUSTED:
							onCertNotTrusted(resultData.getString(Intent.EXTRA_BUG_REPORT));
						break;
						case Constants.CERT_TRUSTED:
							onCertTrusted(resultData);
						break;
						case Constants.TRUST_STORE_REMOVED:
							onTrustStoreRemoved();
						break;
					}
				}
			};
		}
		return resRcvr;
	}

	private void onTrustStoreRemoved() {
		refreshFrag();
	}
	
	private void onCertTrusted(Bundle resultData) {
		if (resultData == null)
			return;
		Uri uri = resultData.getParcelable(Intent.EXTRA_UID);
		setCertTrusted(uri, true);
		refreshFrag();
	}

	private void setCertTrusted(Uri uri, boolean trusted) {
		if (uri == null)
			return;
		ContentValues values = new ContentValues();
		values.put(ConnMgrColumns.FLAG, trusted ? Constants.FLAG_CERT_TRUSTED : Constants.FLAG_CERT_NOT_TRUSTED);
		getContentResolver().update(uri, values, null, null);
	}
	
	private void onCertNotTrusted(final String msg) {	//final CertPath cp) {
		setCertTrusted(checkingUri, false);
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, getString(R.string.cert_not_trusted));
		StringBuilder sb = new StringBuilder();
	    sb.append(msg).append("\n").append(getString(R.string.add_to_truststore));
		args.putString(Intent.EXTRA_TEXT,  sb.toString());
		AlertDialogFragment dlgFrag = new AlertDialogFragment();
		dlgFrag.setOnPositive(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {				
				addCertsToTrustStore(checkingUri);
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

	private void addCertsToTrustStore(Uri uri) {
		ServerInfo info = getFor(uri);
		if (info == null) {
			Log.e(TAG, "no server info found: " + uri.toString());
			return;
		}
		Intent i = new Intent(this, CertificateService.class);
		i.setAction(CertificateService.ACTION_CHECK_CERT);
		i.putExtra(Constants.EXTRA_SERVER_INFO, info.toBundle());
		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		i.putExtra(Constants.EXTRA_TRUST_CERT, true);
		i.putExtra(Intent.EXTRA_UID, uri);
		startService(i);
	}

	private void performRemoveTrust() {
		Intent i = new Intent(this, CertificateService.class);
		i.setAction(CertificateService.ACTION_REMOVE_TRUST_STORE);
		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
//		outstandingIntent = i;
		setCertTrusted(ConnMgrColumns.CONTENT_URI, false);
		startService(i);		
	}

	private void confirmRemoveTrust() {
		confirmDialog("Touch OK to remove trusted certificate store.",new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				performRemoveTrust();
			}
		});
	}

	private void performDelete(Uri uri, String name) {
		getContentResolver().delete(uri, null, null);
		UiUtils.showToast(this, name + " deleted");
		refreshFrag();
	}

	private void refreshFrag() {
		if (frag != null)
			frag.refresh();
	}
	
	@Override
	public void onDelete(final Uri uri, final String name) {
		if (uri == null)
			return;
		String msg = "Touch OK to delete " + (name == null ? "": name);
		confirmDialog(msg, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				performDelete(uri, name);
			}
		});
	}

	protected void editDialog(final ServerInfo si) {
		ConnMgrDialog dlg = new ConnMgrDialog();
		Bundle args = si.toBundle();
		if (si.getId() == 0)
			args.putString(Intent.EXTRA_TITLE, "New Connection");
		else {
			args.putString(Intent.EXTRA_TITLE, "Edit Connection");
		}
		dlg.setArguments(args);
		dlg.setCancelable(false);
		dlg.setListener(new ConnMgrDialog.Listener() {

			@Override
			public void onServerSaved(ServerInfo info) {
				saveServer(info);
			}
		});
		dlg.show(getFragmentManager(), "selSrvrDlg");
	}

	private void saveServer(ServerInfo info) {
		ContentValues values = new ContentValues();
		values.put(ConnMgrColumns.HOST, info.getHost());
		values.put(ConnMgrColumns.PORT, info.getPort());
		values.put(ConnMgrColumns.USE_SSL, info.isSsl());
		values.put(ConnMgrColumns.USER, info.getUser());
		values.put(ConnMgrColumns.PASS, info.getPasswd());
		values.put(ConnMgrColumns.STATUS, info.getStatus());
		if (info.getId() == 0)
			getContentResolver().insert(ConnMgrColumns.CONTENT_URI, values);
		else {
			Uri uri = ContentUris.withAppendedId(ConnMgrColumns.CONTENT_URI, info.getId());
			getContentResolver().update(uri, values, null, null);
		}
		refreshFrag();
	}

	@Override
	public void onAdd() {
		editDialog(new ServerInfo());
	}

	@Override
	public void onEdit(Uri uri) {
		Cursor c = getContentResolver().query(uri, null, null, null, null);
		if (c == null)
			return;
		if (c.moveToFirst()) {
			ServerInfo si = ServerInfo.from(c);
			c.close();
			editDialog(si);
			return;
		}
		c.close();
	}

	@Override
	public void onStatusChange(Uri uri, int newStatus) {
		if (newStatus == Constants.STATUS_ACTIVE || newStatus == Constants.STATUS_INACTIVE) {
			ContentValues values = new ContentValues();
			values.put(ConnMgrColumns.STATUS, newStatus);
			getContentResolver().update(uri, values, null, null);
			refreshFrag();
		}
	}

	@Override
	public void onCheckCert(Uri uri) {
		ServerInfo info = getFor(uri);
		if (info == null) {
			Log.e(TAG, "no server info found: " + uri.toString());
			return;
		}
		checkingUri = uri;
		Intent i = new Intent(this, CertificateService.class);		
		i.setAction(CertificateService.ACTION_CHECK_CERT);
		i.putExtra(Constants.EXTRA_SERVER_INFO, info.toBundle());
		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		startService(i);
	}
	
	private ServerInfo getFor(Uri uri) {
		ServerInfo rv = null;
		Cursor c = getContentResolver().query(uri, null, null, null, null);
		if (c == null)
			return rv;
		if (c.moveToFirst()) {
			rv = ServerInfo.from(c);
		}
		c.close();
		return rv;
	}

	@Override
	public void onRemoveTrustStore() {
		confirmRemoveTrust();
	}
}
