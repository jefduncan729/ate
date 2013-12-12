package com.axway.ate.fragment;

import com.axway.ate.Constants;
import com.axway.ate.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class EditHostDialog extends AlertDialogFragment {

	public interface EditHostListener {
		public void onHostChanged(Bundle b);
	}

	private EditHostListener listener;
	private View dlgView;
	private View ctr01;
	private EditText editName;
	private CheckBox editUseSsl;
	private int action;
//	private String key;
//	private String value;

	private static final String DEF_TITLE = "Add Host";
	
	public EditHostDialog() {
		super();
		listener = null;
		dlgView = null;
		action = 0;
	}

	public void setOnChangeListener(EditHostListener newVal) {
		listener = newVal;
	}

	private DialogInterface.OnClickListener onYes = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			final String name = editName.getText().toString();
			if (!TextUtils.isEmpty(name)) {			
				Bundle data = new Bundle();
				data.putInt(Constants.EXTRA_ACTION, action);
				data.putString(Intent.EXTRA_TEXT, name);
				if (action == R.id.action_add)
					data.putBoolean(Constants.EXTRA_USE_SSL, editUseSsl.isChecked());
				else
					data.putString(Intent.EXTRA_UID, (String)editName.getTag());
				if (listener != null && !TextUtils.isEmpty(name))
					listener.onHostChanged(data);
			}
		}		
	};
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	int layoutId = R.layout.host_dlg;
    	String id = null;
    	String name = "";
    	boolean useSsl = true;
    	Bundle args = getArguments();
    	if (args != null) {
    		if (args.containsKey(EXTRA_LAYOUT))
    			layoutId = args.getInt(EXTRA_LAYOUT);
    		if (args.containsKey(Intent.EXTRA_UID))
    			id = args.getString(Intent.EXTRA_UID);
    		if (args.containsKey(Intent.EXTRA_TEXT))
    			name = args.getString(Intent.EXTRA_TEXT);
    		if (args.containsKey(Constants.EXTRA_USE_SSL))
    			useSsl = args.getBoolean(Constants.EXTRA_USE_SSL, true);
    		if (args.containsKey(Constants.EXTRA_ACTION))
    			action = args.getInt(Constants.EXTRA_ACTION);
    	}
    	if (layoutId == 0)
    		throw new IllegalStateException("must provide layout id in fragment arguments");
    	LayoutInflater inflater = LayoutInflater.from(getActivity());
    	dlgView = inflater.inflate(layoutId, null);
    	ctr01 = (View)dlgView.findViewById(R.id.container01);
		editUseSsl = (CheckBox)dlgView.findViewById(R.id.edit_use_ssl);
		String title = "Add Host";
    	editName = (EditText)dlgView.findViewById(R.id.edit_name);
		editName.setText(name);
		if (action == R.id.action_add) {
			ctr01.setVisibility(View.VISIBLE);
			editUseSsl.setChecked(useSsl);
		}
		else {
			ctr01.setVisibility(View.GONE);
			title = "Edit Host " + id;
			editName.setTag(id);
		}
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(dlgView)
                .setPositiveButton(android.R.string.yes, onYes)
                .setNegativeButton(android.R.string.no, NOOP_LISTENER)
                .create();
    }
}