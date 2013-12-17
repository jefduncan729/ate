package com.axway.ate.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.vordel.api.topology.model.Topology.EntityType;

public class SshUserDialog extends DialogFragment {

	public interface SshUserListener {
		public void onUserSelected(Bundle data);
	}

	private SshUserListener listener;
	private View dlgView;
	private EditText txt01;
	private CheckBox chk01;

	public SshUserDialog() {
		super();
		listener = null;
		dlgView = null;
	}

	public void setListener(SshUserListener newVal) {
		listener = newVal;
	}

	private DialogInterface.OnClickListener onYes = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			final boolean remember = chk01.isChecked();
			if (listener != null) {
				Bundle b = new Bundle();
				b.putBoolean(Constants.KEY_REMEMBER_USER, remember);
				b.putString(Constants.KEY_SSHUSER, txt01.getText().toString());
				b.putString(Constants.EXTRA_HOST_ID, (String)txt01.getTag());
				listener.onUserSelected(b);
			}
		}		
	};
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	Bundle args = getArguments();
    	String username = null;
    	String hostname = null;
    	boolean remember = false;
    	if (args != null) {
    		username = args.getString(Constants.KEY_SSHUSER);
    		hostname = args.getString(Constants.EXTRA_HOST_ID);
    		remember = args.getBoolean(Constants.KEY_REMEMBER_USER, false);
    	}
    	LayoutInflater inflater = LayoutInflater.from(getActivity());
    	dlgView = inflater.inflate(R.layout.ssh_user_dlg, null);
//    	ctr01 = (View)dlgView.findViewById(R.id.container01);
    	txt01 = (EditText)dlgView.findViewById(android.R.id.text1);
    	chk01 = (CheckBox)dlgView.findViewById(R.id.edit_remember_user);
		txt01.setText(username);
		txt01.setTag(hostname);
		chk01.setChecked(remember);
        return new AlertDialog.Builder(getActivity())
                .setTitle("SSH to " + hostname)
                .setView(dlgView)
                .setPositiveButton(android.R.string.yes, onYes)
                .setNegativeButton(android.R.string.no, AlertDialogFragment.NOOP_LISTENER)
                .create();
    }
}