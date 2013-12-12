package com.axway.ate.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.util.UiUtils;
import com.axway.ate.util.Utilities;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class ServiceFragment extends TagAwareFragment {

	private EditText editMgmtPort;
	private EditText editSvcsPort;
	private View ctrMgmtPort;
	private View ctrSvcsPort;
	private CheckBox editUseSsl;
	private AutoCompleteTextView editHost;
	private AutoCompleteTextView editGroup;

	private Host curHost;
	private Service usedBySvc;
	private Group curGroup;

	public ServiceFragment() {
		super();
		curHost = null;
		usedBySvc = null;
		curGroup = null;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = super.onCreateView(inflater, container, savedInstanceState);	//inflater.inflate(R.layout.edit_svc, null);
		editUseSsl = (CheckBox)rv.findViewById(R.id.edit_use_ssl);
		editMgmtPort = (EditText)rv.findViewById(R.id.edit_mgmt_port);
		ctrMgmtPort = (View)rv.findViewById(R.id.container01);
		editSvcsPort = (EditText)rv.findViewById(R.id.edit_svcs_port);
		ctrSvcsPort = (View)rv.findViewById(R.id.container02);
		editHost = (AutoCompleteTextView)rv.findViewById(R.id.edit_host);
		editGroup = (AutoCompleteTextView)rv.findViewById(R.id.edit_group);
		return rv;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.edit_svc;
	}

	@Override
	protected Map<String, String> getObjectTags() {
		if (itemBeingEdited == null)
			return null;
		return ((Service)itemBeingEdited).getTags();
	}

	@Override
	protected void onDisplayItem() {
		Service service = (Service)itemBeingEdited;
		if (action == R.id.action_add)
			ctrSvcsPort.setVisibility(View.VISIBLE);
		else 
			ctrSvcsPort.setVisibility(View.GONE);
		editName.setText(service.getName());
		if (service.getManagementPort() == 0)
			editMgmtPort.setText("");
		else
			editMgmtPort.setText(Integer.toString(service.getManagementPort()));
		editUseSsl.setChecked(Constants.HTTPS_SCHEME.equals(service.getScheme()));
		displayTags();
		if (editHost != null) {
			List<String> hnms = new ArrayList<String>();
			int i = 0;
			for (Host h: topology.getHosts()) {
				if (TextUtils.isEmpty(service.getHostID())) {
					if (i++ == 0)
						curHost = h;
				}
				else if (h.getId().equals(service.getHostID())) {
					curHost = h;
				}
				hnms.add(h.getName());
			}
			editHost.setEnabled(action == R.id.action_add && i > 1);
			editHost.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, hnms));
			if (curHost != null)
				editHost.setText(curHost.getName());
		}
		if (editGroup != null) {
			editGroup.setEnabled(false);	//action == R.id.action_add);
			List<String> gnms = new ArrayList<String>();
			for (Group g: topology.getGroups()) {
				if (g.getService(service.getId()) != null)
					curGroup = g;
				gnms.add(g.getName());
			}
			editGroup.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, gnms));
			if (curGroup != null)
				editGroup.setText(curGroup.getName());
		}
	}

	@Override
	protected void onPrepareItem(JsonObject json) {
		Service s;
		if (action == R.id.action_add) {	//TextUtils.isEmpty(itemId)) {
			s = new Service();
			s.setScheme(Constants.HTTPS_SCHEME);
			s.setType(ServiceType.gateway);
			if (topology != null && getArguments() != null) {
				String refId = getArguments().getString(Constants.EXTRA_REFERRING_ITEM_ID);
				String typ = getArguments().getString(Constants.EXTRA_REFERRING_ITEM_TYPE);
				EntityType eType = EntityType.valueOf(typ);
				if (eType == EntityType.Host) {
					s.setHostID(refId);
					curHost = topology.getHost(refId);
				}
				else if (eType == EntityType.Group) {
					curGroup = topology.getGroup(refId);
				}
			}
		}
		else {
			s = helper.serviceFromJson(json);	//topology.getService(itemId);
		}
		itemBeingEdited = s;
	}

	private boolean isValidPort(EditText edit, View parent) {
		if (edit == null || parent == null)
			return false;
		if (usedBySvc != null)
			return true;
		usedBySvc = null;
		String s = edit.getText().toString();
		int p = Utilities.strToIntDef(s, -1);
		if (p == -1)
			setInvalidView(edit);
		else {
//			Collection<Host>hosts = topology.getHosts();
			Collection<Group> grps = topology.getGroups();
			for (Group g: grps) {
				Collection<Service> svcs = g.getServices();
				for (Service svc: svcs) {
					if (svc.getManagementPort() == p) {
						usedBySvc = svc; 
						break;
					}
				}
				if (usedBySvc != null)
					break;
			}
			if (usedBySvc != null) {
				setInvalidView(parent);
				parent.setTag(p);
			}
		}
		return (usedBySvc == null);
	}
	
	private boolean inUse(int port) {
		boolean rv = false;
		Collection<Host>hosts = topology.getHosts();
		for (Host h: hosts) {
			if (topology.portUsedBy(h.getId(), port) != null) {
				rv = true;
				break;
			}
		}
		return rv;
	}
	
	private Service usedBy(int port) {
		Service rv = null;
		Collection<Host>hosts = topology.getHosts();
		for (Host h: hosts) {
			rv = topology.portUsedBy(h.getId(), port);
			if (rv != null) 
				break;
		}
		return rv;
	}
	
	@Override
	protected boolean isValid() {
		if (super.isValid()) {
			if (TextUtils.isEmpty(editMgmtPort.getText().toString())) {
				String s = editMgmtPort.getText().toString();
				int p = Utilities.strToIntDef(s, -1);
				if (p == -1)
					setInvalidView(editMgmtPort);
				else {
					usedBySvc = usedBy(p);
					if (usedBySvc != null) {
						ctrMgmtPort.setTag(p);
						setInvalidView(ctrMgmtPort);
					}
				}
			}
			else if (TextUtils.isEmpty(editHost.getText().toString()))
				setInvalidView(editHost);
			else if (TextUtils.isEmpty(editGroup.getText().toString()))
				setInvalidView(editGroup);
			if (action == R.id.action_add) {
				String s = editSvcsPort.getText().toString();
				int p = Utilities.strToIntDef(s, -1);
				if (p == -1)
					setInvalidView(editSvcsPort);
				else {
					usedBySvc = usedBy(p);
					if (usedBySvc != null) {
						ctrSvcsPort.setTag(p);
						setInvalidView(ctrSvcsPort);
					}
				}
//				String s = editSvcsPort.getText().toString();
//				int p = Utilities.strToIntDef(s, -1);
//				if (p == -1)
//					setInvalidView(editSvcsPort);
//				else {
//					usedBySvc = null;
//					Collection<Host>hosts = topology.getHosts();
//					for (Host h: hosts) {
//						usedBySvc = topology.portUsedBy(h.getId(), p); 
//						if (usedBySvc != null)
//							break;
//					}
//					if (usedBySvc != null) {
//						setInvalidView(ctrSvcsPort);
//					}
//				}
			}
		}
		return (getInvalidView() == null);
	}

	@Override
	protected EntityType getItemType() {
		return EntityType.Gateway;
	}

	@Override
	protected void onSaveItem(Bundle extras) {
		if (itemBeingEdited == null)
			return;
		if (action == R.id.action_add)
			extras.putInt(Intent.EXTRA_TEMPLATE, Integer.parseInt(editSvcsPort.getText().toString()));
		Service s = (Service)itemBeingEdited;
		s.setName(editName.getText().toString());
		s.setManagementPort(Integer.parseInt(editMgmtPort.getText().toString()));
		s.setHostID(topology.getHostByName(editHost.getText().toString()).getId());
		if (editUseSsl.isChecked())
			s.setScheme(Constants.HTTPS_SCHEME);
		else
			s.setScheme(Constants.HTTP_SCHEME);
		String grpName = editGroup.getText().toString();
		Group g = topology.getGroupByName(grpName);
		if (g == null) {
			g = new Group();
			g.setName(grpName);
			curGroup = g;
		}
	}

	@Override
	protected void notifyInvalid() {
		if (getInvalidView() == null)
			return;
		String msg = null;
		if (getInvalidView().getId() == R.id.edit_svcs_port)
			msg = "Please provide a services port";
		else if (usedBySvc != null) {
			int p = (Integer)getInvalidView().getTag();
			if (getInvalidView().getId() == R.id.container01)
				msg = "Management port ";
			else if (getInvalidView().getId() == R.id.container02)
				msg = "Services port ";
			msg = msg + Integer.toString(p) + " is in use by " + usedBySvc.getName();
		}
		if (msg == null)
			super.notifyInvalid();
		else
			UiUtils.showToast(getActivity(), msg);
	}
}
