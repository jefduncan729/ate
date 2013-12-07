package com.axway.ate.fragment;

import com.axway.ate.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class EditTagDialog extends AlertDialogFragment {

	public interface EditTagListener {
		public void onTagChanged(String key, String value, int action);
	}

	private EditTagListener listener;
	private View dlgView;
	private View ctr01;
	private EditText edit01;
	private EditText edit02;
	private int action;
//	private String key;
//	private String value;

	private static final String DEF_TITLE = "Edit";
	
	public EditTagDialog() {
		super();
		listener = null;
		dlgView = null;
		action = 0;
	}

	public void setOnChangeListener(EditTagListener newVal) {
		listener = newVal;
	}

	private DialogInterface.OnClickListener onYes = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			final String key = edit01.getText().toString();
			final String value = edit02.getText().toString();
			if (listener != null && !TextUtils.isEmpty(key)&& !TextUtils.isEmpty(value))
				listener.onTagChanged(key, value, action);
		}		
	};
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	String title = DEF_TITLE;
    	int layoutId = 0;
    	String key = "";
    	String value = "";
    	Bundle args = getArguments();
    	if (args != null) {
    		if (args.containsKey(Intent.EXTRA_TITLE))
    			title = args.getString(Intent.EXTRA_TITLE);
    		if (args.containsKey(EXTRA_LAYOUT))
    			layoutId = args.getInt(EXTRA_LAYOUT);
    		if (args.containsKey(Intent.EXTRA_UID))
    			key = args.getString(Intent.EXTRA_UID);
    		if (args.containsKey(Intent.EXTRA_SUBJECT))
    			value = args.getString(Intent.EXTRA_SUBJECT);
    		if (args.containsKey(EXTRA_ACTION))
    			action = args.getInt(EXTRA_ACTION);
    	}
    	if (layoutId == 0)
    		throw new IllegalStateException("must provide layout id in fragment arguments");
    	LayoutInflater inflater = LayoutInflater.from(getActivity());
    	dlgView = inflater.inflate(layoutId, null);
    	ctr01 = (View)dlgView.findViewById(R.id.container01);
    	edit01 = (EditText)dlgView.findViewById(android.R.id.text1);
		edit01.setText(key);
    	if (TextUtils.isEmpty(key)) {
    		ctr01.setVisibility(View.VISIBLE);
    	}
    	else {
    		ctr01.setVisibility(View.GONE);
    	}
    	edit02 = (EditText)dlgView.findViewById(android.R.id.text2);
    	edit02.setText(value);
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(dlgView)
                .setPositiveButton(android.R.string.yes, onYes)
                .setNegativeButton(android.R.string.no, NOOP_LISTENER)
                .create();
    }
}