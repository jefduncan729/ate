package com.axway.ate.activity;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.axway.ate.R;
import com.axway.ate.fragment.AlertDialogFragment;
import com.axway.ate.fragment.ProgressDialogFragment;

public class BaseActivity extends Activity implements OnClickListener {
	private static final String TAG = BaseActivity.class.getSimpleName();
	
	protected static final String TAG_PROG_DLG = "progDlg";
	protected static final String TAG_INFO_DLG = "infoDlg";
	protected static final String TAG_ALERT_DLG = "alertDlg";
	protected static final String TAG_CONFIRM_DLG = "confirmDlg";
	
	private SharedPreferences prefs;
	
	public BaseActivity() {
		super();
		prefs = null;
	}
	
	protected SharedPreferences getPrefs() {
		if (prefs == null)
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs;
	}
	
	protected void showSettings() {
		Intent i = new Intent(this, SettingsActivity.class);
		i.setAction(Intent.ACTION_VIEW);
		startActivityForResult(i, R.id.action_settings);
	}

	protected void showProgressDialog() {
		showProgressDialog(null, getString(R.string.confirm_msg));
	}

	protected void showProgressDialog(String message) {
		showProgressDialog(null, message);
	}
	
	protected void showProgressDialog(String title, String message) {
		DialogFragment dlg = new ProgressDialogFragment();
		Bundle b = new Bundle();
		if (title != null)
			b.putString(Intent.EXTRA_TITLE, title);
		b.putString(Intent.EXTRA_TEXT, message);
		dlg.setArguments(b);
		FragmentManager fm = getFragmentManager();
		dlg.show(fm.beginTransaction(), TAG_PROG_DLG);
	}
	
	protected void dismissProgressDialog() {
		DialogFragment frag = (DialogFragment)getFragmentManager().findFragmentByTag(TAG_PROG_DLG);
		if (frag != null)
			frag.dismiss();
	}
	
	protected void confirmDialog(DialogInterface.OnClickListener onYes) {
		confirmDialog(getString(R.string.confirm_msg), onYes);
	}
	
	protected void confirmDialog(String msg, DialogInterface.OnClickListener onYes) {
		confirmDialog(getString(R.string.confirm), msg, onYes);
	}	
	
	protected void confirmDialog(String title, String msg, DialogInterface.OnClickListener onYes) {
		confirmDialog(getString(R.string.confirm), msg, onYes, null);
	}	
	
	protected void confirmDialog(String title, String msg, DialogInterface.OnClickListener onYes, DialogInterface.OnClickListener onNo) {
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, title);
		args.putString(Intent.EXTRA_TEXT, msg);
		args.putInt(AlertDialogFragment.EXTRA_ICON, android.R.drawable.ic_dialog_alert);
		AlertDialogFragment dlgFrag = new AlertDialogFragment();
		dlgFrag.setArguments(args);
		dlgFrag.setOnPositive(onYes);
		if (onNo == null)
			onNo = AlertDialogFragment.NOOP_LISTENER;
		dlgFrag.setOnNegative(onNo);
		dlgFrag.setCancelable(false);
		FragmentManager fm = getFragmentManager();
		dlgFrag.show(fm.beginTransaction(), TAG_CONFIRM_DLG);
	}	
	
	protected void alertDialog(String msg) {
		alertDialog(msg, null);
	}
	
	protected void alertDialog(String msg, DialogInterface.OnClickListener onYes) {
		alertDialog(getString(R.string.alert), msg, onYes);
	}	
	
	protected void alertDialog(String title, String msg, DialogInterface.OnClickListener onYes) {
		alertDialog(title, msg, onYes, null);
	}	
	
	protected void alertDialog(String title, String msg, DialogInterface.OnClickListener onYes, DialogInterface.OnClickListener onNo) {
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, title);
		args.putString(Intent.EXTRA_TEXT, msg);
		args.putInt(AlertDialogFragment.EXTRA_ICON, android.R.drawable.ic_dialog_alert);
		AlertDialogFragment dlgFrag = new AlertDialogFragment();
		dlgFrag.setArguments(args);
		if (onYes == null)
			onYes = AlertDialogFragment.NOOP_LISTENER;
		dlgFrag.setOnPositive(onYes);
		if (onNo != null)
			dlgFrag.setOnNegative(onNo);
		FragmentManager fm = getFragmentManager();
		dlgFrag.show(fm.beginTransaction(), TAG_ALERT_DLG);
	}	
	
	protected void alertDialog(String title, String msg, DialogInterface.OnClickListener onYes, DialogInterface.OnClickListener onNo, DialogInterface.OnClickListener onNeutral) {
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, title);
		args.putString(Intent.EXTRA_TEXT, msg);
		args.putInt(AlertDialogFragment.EXTRA_ICON, android.R.drawable.ic_dialog_alert);
		AlertDialogFragment dlgFrag = new AlertDialogFragment();
		dlgFrag.setArguments(args);
		if (onYes == null)
			onYes = AlertDialogFragment.NOOP_LISTENER;
		dlgFrag.setOnPositive(onYes);
		if (onNo != null)
			dlgFrag.setOnNegative(onNo);
		if (onNeutral != null)
			dlgFrag.setOnNeutral(onNeutral);
		FragmentManager fm = getFragmentManager();
		dlgFrag.show(fm.beginTransaction(), TAG_ALERT_DLG);
	}	
	
	protected void infoDialog(String msg) {
		infoDialog(getString(R.string.info), msg);
	}

	protected void infoDialog(String title, String msg) {
		infoDialog(title, msg, null);
	}
	
	protected void infoDialog(String title, String msg, DialogInterface.OnClickListener onYes) {
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, title);
		args.putString(Intent.EXTRA_TEXT, msg);
		args.putInt(AlertDialogFragment.EXTRA_ICON, android.R.drawable.ic_dialog_info);
		AlertDialogFragment dlgFrag = new AlertDialogFragment();
		dlgFrag.setArguments(args);
		if (onYes == null)
			onYes = AlertDialogFragment.NOOP_LISTENER;
		dlgFrag.setOnPositive(onYes);
		FragmentManager fm = getFragmentManager();
		dlgFrag.show(fm.beginTransaction(), TAG_INFO_DLG);
	}	
	
	protected void handleException(Exception e) {
		dismissProgressDialog();
		String msg = null;
		if (e != null)
			msg = e.getLocalizedMessage();
		if (msg == null)
			msg = "unknown exception";
		Log.e(TAG, msg, e);
		System.gc();
	}

	@Override
	public void onClick(View arg0) {
	}
}
