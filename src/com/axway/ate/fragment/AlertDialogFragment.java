package com.axway.ate.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class AlertDialogFragment extends DialogFragment {

	public static final String EXTRA_LAYOUT = "layoutId";
	public static final String EXTRA_ICON = "iconId";
	public static final String EXTRA_VALUE = "value";
	public static final String EXTRA_ACTION = "action";
	
	private static final String DEF_TITLE = "Alert!";
	
	private DialogInterface.OnClickListener onPositive;
	private DialogInterface.OnClickListener onNegative;
	private DialogInterface.OnClickListener onNeutral;
	
	private int posResId;
	private int negResId;
	private int neutResId;
	private int iconId;
	
	public AlertDialogFragment() {
		super();
		onPositive = null;
		onNegative = null;
		onNeutral = null;
		posResId = android.R.string.ok;
		negResId = android.R.string.cancel;
		neutResId = android.R.string.paste;
		iconId = android.R.drawable.ic_dialog_alert; 
	}
	
	public static final DialogInterface.OnClickListener NOOP_LISTENER = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		}
	};

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String t = DEF_TITLE; 
		String m = "";
		Bundle args = getArguments();
		if (args != null) {
			if (args.containsKey(Intent.EXTRA_TITLE))
				t = args.getString(Intent.EXTRA_TITLE);
			if (args.containsKey(Intent.EXTRA_TEXT))
				m = args.getString(Intent.EXTRA_TEXT);
			if (args.containsKey(EXTRA_ICON))
				iconId = args.getInt(EXTRA_ICON);
		}
		AlertDialog.Builder bldr = new AlertDialog.Builder(getActivity());
		bldr.setTitle(t)
			.setMessage(m)
			.setIcon(iconId);
		if (onPositive == null)
			onPositive = NOOP_LISTENER;
		bldr.setPositiveButton(posResId, onPositive);
		if (onNegative != null)
			bldr.setNegativeButton(negResId, onNegative);
		if (onNeutral != null)
			bldr.setNeutralButton(neutResId, onNeutral);
		return bldr.create();
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

	public DialogInterface.OnClickListener getOnNegative() {
		return onNegative;
	}

	public void setOnNegative(DialogInterface.OnClickListener onNegative) {
		this.onNegative = onNegative;
	}

	public void setOnNegative(DialogInterface.OnClickListener onNegative, int resId) {
		setOnNegative(onNegative);
		negResId = resId;
	}

	public DialogInterface.OnClickListener getOnNeutral() {
		return onNeutral;
	}

	public void setOnNeutral(DialogInterface.OnClickListener onNeutral) {
		this.onNeutral = onNeutral;
	}

	public void setOnNeutral(DialogInterface.OnClickListener onNeutral, int resId) {
		setOnNeutral(onNeutral);
		neutResId = resId;
	}
}
