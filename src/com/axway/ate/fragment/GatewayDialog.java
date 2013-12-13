package com.axway.ate.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;

import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.R;
import com.axway.ate.adapter.TagListAdapter;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology.EntityType;

public class GatewayDialog extends TagAwareDialog {

	public interface GatewayListener {
		public void onGatewayChanged(Bundle b);
	}
	
	private EditText editMgmtPort;
	private EditText editSvcsPort;
	private View ctrMgmtPort;
	private View ctrSvcsPort;
	private CheckBox editUseSsl;
	private AutoCompleteTextView editHost;
//	private AutoCompleteTextView editGroup;
	private EditText editGroup;

	private GatewayListener listener;
	private Service svc;
	private Host curHost;
	private Group curGroup;
	
	public GatewayDialog() {
		super();
		listener = null;
		svc = null;
		curHost = null;
		curGroup = null;
	}

	public void setOnChangeListener(GatewayListener newVal) {
		listener = newVal;
	}
	
	private DialogInterface.OnClickListener onYes = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String name = editName.getText().toString();
			if (isValidName(name)) {			
				Bundle data = new Bundle();
				data.putInt(Constants.EXTRA_ACTION, action);
				data.putString(Intent.EXTRA_TEXT, name);
				svc.setName(name);
				int port = Integer.parseInt(editMgmtPort.getText().toString());
				Host h = portUsedBy(port);
				if (h != null) {
					//
				}
				svc.setManagementPort(port);
				
				if (action == R.id.action_add) {
					if (editUseSsl.isChecked())
						svc.setScheme(Constants.HTTPS_SCHEME);
					else
						svc.setScheme(Constants.HTTP_SCHEME);
					port = Integer.parseInt(editSvcsPort.getText().toString());				
					h = portUsedBy(port);
					if (h != null) {
						//
					}
					String hname = editHost.getText().toString();
					h = topology.getHostByName(hname);
					if (h != null)
						svc.setHostID(h.getId());
					data.putInt(Constants.EXTRA_SERVICES_PORT, port);
				}
				else
					data.putString(Intent.EXTRA_UID, (String)editName.getTag());
				data.putString(Constants.EXTRA_JSON_ITEM, DomainHelper.getInstance().toJson(svc).toString());
				if (listener != null && !TextUtils.isEmpty(name))
					listener.onGatewayChanged(data);
			}
		}		
	};

	@Override
	protected int getLayoutId() {
//		if (action == R.id.action_add)
			return R.layout.edit_svc;
//		return R.layout.edit_grp;
	}

	@Override
	protected EntityType getItemType() {
		return EntityType.Gateway;
	}

	@Override
	protected OnClickListener createOnYes() {
		return onYes;
	}

	@Override
	protected void setupView(View view) {
		super.setupView(view);
		if (itemJson != null)
			svc = DomainHelper.getInstance().serviceFromJson(itemJson);
		editUseSsl = (CheckBox)view.findViewById(R.id.edit_use_ssl);
		editMgmtPort = (EditText)view.findViewById(R.id.edit_mgmt_port);
		ctrMgmtPort = (View)view.findViewById(R.id.container01);
		editSvcsPort = (EditText)view.findViewById(R.id.edit_svcs_port);
		ctrSvcsPort = (View)view.findViewById(R.id.container02);
		editHost = (AutoCompleteTextView)view.findViewById(R.id.edit_host);		
		List<String> hnms = new ArrayList<String>();
		Collection<Host>hosts = topology.getHosts();
		for (Host h: hosts)
			hnms.add(h.getName());
		editHost.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, hnms));
		
		editGroup = (EditText)view.findViewById(R.id.edit_group);
		editUseSsl.setChecked(Constants.HTTPS_SCHEME.equals(svc.getScheme()));
		if (svc.getManagementPort() == 0)
			editMgmtPort.setText("");
		else
			editMgmtPort.setText(Integer.toString(svc.getManagementPort()));
		if (action == R.id.action_add) {
			String refId = getArguments().getString(Constants.EXTRA_REFERRING_ITEM_ID);
			ctrSvcsPort.setVisibility(View.VISIBLE);
			curHost = null;
			curGroup = topology.getGroup(refId);
			editUseSsl.setEnabled(true);
			editHost.setEnabled(hosts.size() > 1);
		}
		else {
			editUseSsl.setEnabled(false);
			editHost.setEnabled(false);
			ctrSvcsPort.setVisibility(View.GONE);
			curHost = topology.getHost(svc.getHostID());
			curGroup = topology.getGroupForService(svc.getId());
		}
			
		if (getTagList() != null && svc != null)
			getTagList().setAdapter(new TagListAdapter(getActivity(), svc.getTags()));
		displayTags();
		
		if (curHost == null && hosts.size() == 1)
			curHost = hosts.iterator().next();
		editHost.setText(curHost.getName());
		
		editGroup.setEnabled(false);		
		if (curGroup != null)
			editGroup.setText(curGroup.getName());
	}

	@Override
	protected Map<String, String> getObjectTags() {
		if (svc == null)
			return null;
		return svc.getTags();
	}

	@Override
	protected String getTitle() {
		if (action == R.id.action_add)
			return "Add Gateway to Group " + curGroup.getName();
		return super.getTitle();
	}
}