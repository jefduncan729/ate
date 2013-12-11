package com.axway.ate.activity;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.ServerInfo;
import com.axway.ate.db.DbHelper.ConnMgrColumns;
import com.axway.ate.fragment.ConnMgrDialog;
import com.axway.ate.fragment.ConnMgrFragment;
import com.axway.ate.fragment.ConnMgrFragment.Listener;
import com.axway.ate.service.RestService;
import com.axway.ate.util.UiUtils;

public class ConnMgrActivity extends BaseActivity implements Listener {
	
	private static final String TAG = ConnMgrActivity.class.getSimpleName();

	private ConnMgrFragment frag;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		Cursor c = getContentResolver().query(uri, null, null, null, null);
		if (c == null)
			return;
		ServerInfo info = null;
		if (c.moveToFirst()) {
			info = ServerInfo.from(c);
		}
		c.close();
		Intent i = new Intent(this, RestService.class);		
		i.setAction(RestService.ACTION_CHECK_CERT);
		i.putExtra(Intent.EXTRA_LOCAL_ONLY, info.toBundle());
//		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		i.putExtra(Intent.EXTRA_TEXT, info.buildUrl("topology"));
		startService(i);
	}
}
