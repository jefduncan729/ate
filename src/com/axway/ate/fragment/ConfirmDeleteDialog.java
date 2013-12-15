package com.axway.ate.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.vordel.api.topology.model.Topology.EntityType;

public class ConfirmDeleteDialog extends AlertDialogFragment {

	public interface DeleteListener {
		public void onDeleteConfirmed(Bundle data);
	}

	private DeleteListener listener;
	private View dlgView;
	private TextView txt01;
	private CheckBox chk01;
	private EntityType eType;
	private String id;

	private static final String DEF_TITLE = "Confirm Delete";
	
	public ConfirmDeleteDialog() {
		super();
		listener = null;
		dlgView = null;
		eType = null;
		id = null;
	}

	public void setListener(DeleteListener newVal) {
		listener = newVal;
	}

	private DialogInterface.OnClickListener onYes = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			final boolean delFromDisk = chk01.isChecked();
			if (listener != null) {
				Bundle b = new Bundle();
				b.putBoolean(Constants.EXTRA_DELETE_FROM_DISK, delFromDisk);
				b.putString(Constants.EXTRA_ITEM_ID, id);
				b.putString(Constants.EXTRA_ITEM_TYPE, eType.name());
				listener.onDeleteConfirmed(b);
			}
		}		
	};
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	String title = DEF_TITLE;
    	String msg = "Touch OK to delete";
    	Bundle args = getArguments();
    	if (args != null) {
    		if (args.containsKey(Intent.EXTRA_TITLE))
    			title = args.getString(Intent.EXTRA_TITLE);
    		if (args.containsKey(Intent.EXTRA_TEXT))
    			msg = args.getString(Intent.EXTRA_TEXT);
    		eType = EntityType.valueOf(args.getString(Constants.EXTRA_ITEM_TYPE));
    		id = args.getString(Constants.EXTRA_ITEM_ID);
    	}
    	LayoutInflater inflater = LayoutInflater.from(getActivity());
    	dlgView = inflater.inflate(R.layout.del_dlg, null);
//    	ctr01 = (View)dlgView.findViewById(R.id.container01);
    	txt01 = (TextView)dlgView.findViewById(android.R.id.text1);
    	chk01 = (CheckBox)dlgView.findViewById(R.id.action_delete_disk);
		txt01.setText(msg);
		chk01.setChecked(args.getBoolean(Constants.EXTRA_DELETE_FROM_DISK, false));
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(dlgView)
                .setPositiveButton(android.R.string.yes, onYes)
                .setNegativeButton(android.R.string.no, NOOP_LISTENER)
                .create();
    }
}