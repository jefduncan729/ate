package com.axway.ate.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.axway.ate.R;
import com.axway.ate.api.ServerInfo;
import com.axway.ate.db.DbHelper.ConnMgrColumns;
import com.axway.ate.util.UiUtils;

public class ConnMgrDialog extends DialogFragment {
	private static final String TAG = ConnMgrDialog.class.getSimpleName();
	
	public static final String EXTRA_ACTION = "action";
	
	private static final String DEF_TITLE = "Edit Connection";
	
	private DialogInterface.OnClickListener onPositive;
	
	public interface Listener {
		public void onServerSaved(ServerInfo info);
	}
	
	private int posResId;
	private int negResId;
	private Listener listener;
	private EditText editUser;
	private EditText editPass;
	private EditText editHost;
	private EditText editPort;
	private CheckBox editSsl;
	private CheckBox editEnabled;
	
	private View invalidView;
	
	public ConnMgrDialog() {
		super();
		Log.d(TAG, "constructor");
		onPositive = null;
		posResId = android.R.string.ok;
		negResId = android.R.string.cancel;
		listener = null;
		invalidView = null;
	}
	
	public void setListener(Listener l) {
		this.listener = l;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.d(TAG, "onCreateDialog");
		String t = DEF_TITLE;
		Bundle args = getArguments();
		if (args != null) {
			if (args.containsKey(Intent.EXTRA_TITLE))
				t = args.getString(Intent.EXTRA_TITLE);
		}
		AlertDialog.Builder bldr = new AlertDialog.Builder(getActivity());
    	Activity a = getActivity();
    	LayoutInflater inflater = LayoutInflater.from(a);
    	View dlgView = inflater.inflate(R.layout.server_dlg, null);
		editHost = (EditText)dlgView.findViewById(R.id.edit_host);
		editUser = (EditText)dlgView.findViewById(R.id.edit_user);
		editPort = (EditText)dlgView.findViewById(R.id.edit_port);
		editPass = (EditText)dlgView.findViewById(R.id.edit_passwd);
		editSsl = (CheckBox)dlgView.findViewById(R.id.edit_use_ssl);
		editEnabled = (CheckBox)dlgView.findViewById(R.id.edit_enabled);

		displayData(args);
		
		bldr.setTitle(t)
			.setView(dlgView);
		onPositive = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.d(TAG, "onPositiveClicked");
				validate();
				if (invalidView == null) {
					if (listener != null) {
						listener.onServerSaved(collectInfo());
					}
				}
				else {
					notifyInvalid();
				}
			}
		};
		bldr.setPositiveButton(posResId, onPositive);
		bldr.setNegativeButton(negResId, AlertDialogFragment.NOOP_LISTENER);
		return bldr.create();
	}

	private void notifyInvalid() {
		String msg = null;
		switch (invalidView.getId()) {
			case R.id.edit_host:
				msg = "Please provide a host";
			break;
			case R.id.edit_port:
				msg = "Please provide a port";
			break;
		}
		if (msg != null)
			UiUtils.showToast(getActivity(), msg);
	}
	
	private ServerInfo collectInfo() {
		ServerInfo rv = new ServerInfo();
		Log.d(TAG, "collectInfo");
		rv.setStatus(editEnabled.isChecked() ? 1 : 0);
		rv.setId((Long)editHost.getTag());
		rv.setHost(editHost.getText().toString());
		rv.setUser(editUser.getText().toString());
		rv.setPasswd(editPass.getText().toString());
		rv.setPort(Integer.parseInt(editPort.getText().toString()));
		rv.setSsl(editSsl.isChecked());
		return rv;
	}

	private void displayData(Bundle args) {
		Log.d(TAG, "displayData");
		if (args == null)
			return;
		editHost.setText(args.getString(ConnMgrColumns.HOST));
		editHost.setTag(args.getLong(ConnMgrColumns._ID));
		editUser.setText(args.getString(ConnMgrColumns.USER));
		editPass.setText(args.getString(ConnMgrColumns.PASS));
		int i = args.getInt(ConnMgrColumns.PORT);
		if (i == 0)
			editPort.setText("");
		else
			editPort.setText(Integer.toString(i));
		editSsl.setChecked(args.getBoolean(ConnMgrColumns.USE_SSL));
		editEnabled.setChecked(args.getInt(ConnMgrColumns.STATUS) == 1);
	}
	
	public DialogInterface.OnClickListener getOnPositive() {
		return onPositive;
	}

	public void setOnPositive(DialogInterface.OnClickListener onPositive) {
		this.onPositive = onPositive;
	}

	public void setOnPositive(DialogInterface.OnClickListener onPositive, int resId) {
		setOnPositive(onPositive);
		posResId = resId;
	}
	
	private void validate() {
		invalidView = null;
		String s = editHost.getText().toString();
		if (TextUtils.isEmpty(s)) {
			invalidView = editHost;
			return;
		}
		s = editPort.getText().toString();
		if (TextUtils.isEmpty(s)) {
			invalidView = editPort;
			return;
		}
	}
}
