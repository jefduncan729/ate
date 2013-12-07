package com.axway.ate.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {

	private static final String DEF_TITLE = null;
	private static final String DEF_MSG = "Please wait...";
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String t = null;
		String m = null;
    	Bundle args = getArguments();
    	if (args != null) {
    		t = args.getString(Intent.EXTRA_TITLE);
    		m = args.getString(Intent.EXTRA_TEXT);
    	}
    	if (t == null)
    		t = DEF_TITLE;
    	if (m == null)
    		m = DEF_MSG;
		final ProgressDialog rv = new ProgressDialog(getActivity());
		if (t != null)
			rv.setTitle(t);
		rv.setCancelable(false);
		rv.setMessage(m);
		rv.setIndeterminate(true);
		return rv;
	}
}
