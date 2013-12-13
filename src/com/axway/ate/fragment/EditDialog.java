package com.axway.ate.fragment;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.util.Utilities;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

abstract public class EditDialog extends DialogFragment {

	protected View dlgView;
	protected EditText editName;
	protected int action;
	protected Topology topology;
	protected String itemId;
	protected String itemName;
	protected String itemJson;

	public EditDialog() {
		super();
		dlgView = null;
		action = 0;
		topology = null;
		itemId = null;
		itemName = null;
		itemJson = null;
	}

	public void setTopology(Topology t) {
		topology = t;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	Bundle args = getArguments();
    	if (args != null) {
    		if (args.containsKey(Intent.EXTRA_UID))
    			itemId = args.getString(Intent.EXTRA_UID);
    		if (args.containsKey(Intent.EXTRA_TEXT))
    			itemName = args.getString(Intent.EXTRA_TEXT);
    		if (args.containsKey(Constants.EXTRA_ACTION))
    			action = args.getInt(Constants.EXTRA_ACTION);
    		if (args.containsKey(Constants.EXTRA_JSON_ITEM))
    			itemJson = args.getString(Constants.EXTRA_JSON_ITEM);
    	}
    	LayoutInflater inflater = LayoutInflater.from(getActivity());
    	dlgView = inflater.inflate(getLayoutId(), null);
    	editName = (EditText)dlgView.findViewById(R.id.edit_name);
    	setupView(dlgView);
        return new AlertDialog.Builder(getActivity())
                .setTitle(getTitle())
                .setView(dlgView)
                .setPositiveButton(android.R.string.yes, createOnYes())
                .setNegativeButton(android.R.string.no, AlertDialogFragment.NOOP_LISTENER)
                .create();
    }
	
	protected String getTitle() {
		StringBuilder sb = new StringBuilder();
		if (action == R.id.action_add)
			sb.append("Add ").append(getItemType().name());
		else if (action == R.id.action_edit)
			sb.append("Edit ").append(getItemType().name()).append(" ").append(itemId);
		return sb.toString();
	}

	abstract protected EntityType getItemType();
	abstract protected DialogInterface.OnClickListener createOnYes();
	abstract protected int getLayoutId();
	
	protected void setupView(View dlgView) {
		editName.setText(itemName);
		editName.setTag(itemId);
	}
	
	protected boolean isValidName(String name) {
		if (TextUtils.isEmpty(name))
			return false;
		String invalidChars = "/'?*<>|:\"";
		String s;
		boolean rv = true;
		for (int i = 0; rv && i < invalidChars.length(); i++) {
			s = invalidChars.substring(i, i+1);
			rv = !(name.contains(s));
		}
		return rv;
	}
	
	protected Host portUsedBy(int port) {
		Host rv = null;
		if (topology != null) {
			for (Host h: topology.getHosts()) {
				if (topology.portUsedBy(h.getId(), port) != null) {
					rv = h;
					break;
				}
			}
		}
		return rv;
	}
}