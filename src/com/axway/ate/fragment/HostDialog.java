package com.axway.ate.fragment;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.util.Utilities;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Topology.EntityType;

public class HostDialog extends EditDialog {

	public interface HostListener {
		public void onHostChanged(Bundle b);
//		public void onPortInUse(Host h, int port);
	}

	private HostListener listener;
	
	private View ctr01;
	private EditText editMgmtPort;
	private CheckBox editUseSsl;

	public HostDialog() {
		super();
		listener = null;
	}

	public void setOnChangeListener(HostListener newVal) {
		listener = newVal;
	}
	
	private DialogInterface.OnClickListener onYes = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String name = editName.getText().toString();
			if (isValidName(name)) {
				Bundle data = new Bundle();
				data.putInt(Constants.EXTRA_ACTION, action);
				data.putString(Intent.EXTRA_TEXT, name);
				if (action == R.id.action_add) {
					int port = Utilities.strToIntDef(editMgmtPort.getText().toString(), 0);
					final Host h = portUsedBy(port);
					if (h != null) {	// && listener != null) {
//						listener.onPortInUse(h, mport);
						return;
					}
					data.putInt(Constants.EXTRA_MGMT_PORT, port);
					data.putBoolean(Constants.EXTRA_USE_SSL, editUseSsl.isChecked());
				}
				else
					data.putString(Intent.EXTRA_UID, (String)editName.getTag());
				if (listener != null && !TextUtils.isEmpty(name))
					listener.onHostChanged(data);
			}
		}		
	};

	@Override
	protected void setupView(View dlgView) {
		super.setupView(dlgView);
    	int mgmtPort = 0;
    	boolean useSsl = true;
    	Bundle args = getArguments();
		if (args.containsKey(Constants.EXTRA_USE_SSL))
			useSsl = args.getBoolean(Constants.EXTRA_USE_SSL, true);
		if (args.containsKey(Constants.EXTRA_MGMT_PORT))
			mgmtPort = args.getInt(Constants.EXTRA_MGMT_PORT);
    	ctr01 = (View)dlgView.findViewById(R.id.container01);
		editUseSsl = (CheckBox)dlgView.findViewById(R.id.edit_use_ssl);
		editMgmtPort = (EditText)dlgView.findViewById(R.id.edit_mgmt_port);
		if (action == R.id.action_add) {
			ctr01.setVisibility(View.VISIBLE);
			editUseSsl.setChecked(useSsl);
			if (mgmtPort != 0)
				editMgmtPort.setText(Integer.toString(mgmtPort));
		}
		else {
			ctr01.setVisibility(View.GONE);
		}
	}
//	
//	@Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//    	int layoutId = R.layout.host_dlg;
//    	String id = null;
//    	String name = "";
//    	int mgmtPort = 0;
//    	boolean useSsl = true;
//    	Bundle args = getArguments();
//    	if (args != null) {
//    		if (args.containsKey(Constants.EXTRA_LAYOUT))
//    			layoutId = args.getInt(Constants.EXTRA_LAYOUT);
//    		if (args.containsKey(Intent.EXTRA_UID))
//    			id = args.getString(Intent.EXTRA_UID);
//    		if (args.containsKey(Intent.EXTRA_TEXT))
//    			name = args.getString(Intent.EXTRA_TEXT);
//    		if (args.containsKey(Constants.EXTRA_ACTION))
//    			action = args.getInt(Constants.EXTRA_ACTION);
//    	}
//    	if (layoutId == 0)
//    		throw new IllegalStateException("must provide layout id in fragment arguments");
//    	LayoutInflater inflater = LayoutInflater.from(getActivity());
//    	dlgView = inflater.inflate(layoutId, null);
//    	ctr01 = (View)dlgView.findViewById(R.id.container01);
//		editUseSsl = (CheckBox)dlgView.findViewById(R.id.edit_use_ssl);
//		String title = DEF_TITLE;
//    	editName = (EditText)dlgView.findViewById(R.id.edit_name);
//		editName.setText(name);
//		editMgmtPort = (EditText)dlgView.findViewById(R.id.edit_mgmt_port);
//		if (action == R.id.action_add) {
//			ctr01.setVisibility(View.VISIBLE);
//			editUseSsl.setChecked(useSsl);
//			if (mgmtPort != 0)
//				editMgmtPort.setText(Integer.toString(mgmtPort));
//		}
//		else {
//			ctr01.setVisibility(View.GONE);
//			title = "Edit Host " + id;
//			editName.setTag(id);
//		}
//        return new AlertDialog.Builder(getActivity())
//                .setTitle(title)
//                .setView(dlgView)
//                .setPositiveButton(android.R.string.yes, onYes)
//                .setNegativeButton(android.R.string.no, AlertDialogFragment.NOOP_LISTENER)
//                .create();
//    }

	@Override
	protected EntityType getItemType() {
		return EntityType.Host;
	}

	@Override
	protected OnClickListener createOnYes() {
		return onYes;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.host_dlg;
	}
	
}