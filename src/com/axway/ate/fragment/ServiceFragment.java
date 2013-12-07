package com.axway.ate.fragment;

import java.util.ArrayList;
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
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class ServiceFragment extends TagAwareFragment {

	private EditText editMgmtPort;
	private CheckBox editUseSsl;
	private AutoCompleteTextView editHost;
	private AutoCompleteTextView editGroup;

	private Host curHost;
	private Group curGroup;

	public ServiceFragment() {
		super();
		curHost = null;
		curGroup = null;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = super.onCreateView(inflater, container, savedInstanceState);
		editMgmtPort = (EditText)rv.findViewById(R.id.edit_mgmt_port);
		editHost = (AutoCompleteTextView)rv.findViewById(R.id.edit_host);
		editGroup = (AutoCompleteTextView)rv.findViewById(R.id.edit_group);
		editUseSsl = (CheckBox)rv.findViewById(R.id.edit_use_ssl);
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
//
//	@Override
//	protected Bundle collectResults() {
//		Bundle rv = super.collectResults();
//		Service svc = (Service)itemBeingEdited;
//		svc.setName(editName.getText().toString());
//		svc.setManagementPort(Integer.parseInt(editMgmtPort.getText().toString()));
//		svc.setHostID(topology.getHostByName(editHost.getText().toString()).getId());
//		if (editUseSsl.isChecked())
//			svc.setScheme(Constants.HTTPS_SCHEME);
//		else
//			svc.setScheme(Constants.HTTP_SCHEME);
//		rv.putString(Intent.EXTRA_SUBJECT, helper.toJson(svc).toString());
//		return rv;
//	}

	@Override
	protected void onDisplayItem() {
//		Service service;
//		if (newObj) {
//			service = new Service();
//			service.setId(topology.generateID(EntityType.Gateway));
//		}
//		else
//			service = topology.getService(itemId);
//		if (service == null) {
//			service = new Service();
//			if (TextUtils.isEmpty(itemId))
//				itemId = topology.generateID(EntityType.Gateway);
//			service.setId(itemId);
//			service.setType(ServiceType.gateway);
//		}
//		itemBeingEdited = service;
		Service service = (Service)itemBeingEdited;
		editName.setText(service.getName());
		if (service.getManagementPort() == 0)
			editMgmtPort.setText("");
		else
			editMgmtPort.setText(Integer.toString(service.getManagementPort()));
		editUseSsl.setChecked(Constants.HTTPS_SCHEME.equals(service.getScheme()));
		displayTags();
		if (editHost != null) {
			List<String> hnms = new ArrayList<String>();
			for (Host h: topology.getHosts()) {
				if (h.getId().equals(service.getHostID()))
					curHost = h;
				hnms.add(h.getName());
			}
			editHost.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, hnms));
			if (curHost != null)
				editHost.setText(curHost.getName());
		}
		if (editGroup != null) {
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
	protected void onPrepareItem() {
		Service s;
		if (TextUtils.isEmpty(itemId)) {
			s = new Service();
			s.setScheme(Constants.HTTPS_SCHEME);
			s.setType(ServiceType.gateway);
			if (getArguments() != null) {
				String refId = getArguments().getString(Intent.EXTRA_ORIGINATING_URI);
				String typ = getArguments().getString(Intent.EXTRA_REFERRER);
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
			s = topology.getService(itemId);
		}
		itemBeingEdited = s;
	}

	@Override
	protected boolean isValid() {
		if (super.isValid()) {
			if (TextUtils.isEmpty(editMgmtPort.getText().toString()))
				setInvalidView(editMgmtPort);
			else if (TextUtils.isEmpty(editHost.getText().toString()))
				setInvalidView(editHost);
			else if (TextUtils.isEmpty(editGroup.getText().toString()))
				setInvalidView(editGroup);
		}
		return (getInvalidView() == null);
	}

	@Override
	protected EntityType getItemType() {
		return EntityType.Gateway;
	}

	@Override
	protected void onSaveObject() {
		if (topoService == null || itemBeingEdited == null)
			return;
		Service s = (Service)itemBeingEdited;
		s.setName(editName.getText().toString());
		s.setName(editName.getText().toString());
		s.setManagementPort(Integer.parseInt(editMgmtPort.getText().toString()));
		s.setHostID(topology.getHostByName(editHost.getText().toString()).getId());
		if (editUseSsl.isChecked())
			s.setScheme(Constants.HTTPS_SCHEME);
		else
			s.setScheme(Constants.HTTP_SCHEME);
		topoService.saveService(s, curGroup);
	}
}
