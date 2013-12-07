package com.axway.ate.fragment;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

import com.axway.ate.R;
import com.axway.ate.activity.ServiceActivity;
import com.axway.ate.adapter.ServiceListAdapter;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class HostFragment extends EditFragment implements OnItemClickListener {
	
	private ListView listGateways;
	private ImageButton btnAddSvc;

	public HostFragment() {
		super();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.edit_host;
	}
//
//	@Override
//	protected Bundle collectResults() {
//		Bundle rv = super.collectResults();
//		Host h = new Host();
//		h.setId(((Host)itemBeingEdited).getId());
//		h.setName(editName.getText().toString());
//		rv.putString(Intent.EXTRA_SUBJECT, helper.toJson(h).toString());
//		return rv;
//	}

	@Override
	protected void onDisplayItem() {
		editName.setText(((Host)itemBeingEdited).getName());
		if (TextUtils.isEmpty(itemId))
			showGateways(false);
		else {
			List<Service> svcs = new ArrayList<Service>();
			svcs.addAll(topology.getServicesOnHost(itemId, ServiceType.gateway));
			listGateways.setAdapter(new ServiceListAdapter(getActivity(), svcs));
			showGateways(true);
		}
	}

	@Override
	protected void onPrepareItem() {
		Host h;
		if (TextUtils.isEmpty(itemId)) {
			h = new Host();
		}
		else {
			h = topology.getHost(itemId);
		}
		itemBeingEdited = h;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = super.onCreateView(inflater, container, savedInstanceState);
		listGateways = (ListView)rv.findViewById(R.id.gateway_list);
		listGateways.setOnItemClickListener(this);
		viewGateways.setVisibility(View.GONE);
		btnAddSvc = (ImageButton)rv.findViewById(R.id.action_add_gateway);
		if (btnAddSvc != null)
			btnAddSvc.setOnClickListener(this);
		return rv;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
		if (listView.getId() == R.id.gateway_list) {
			Service s = (Service)listGateways.getItemAtPosition(pos);
			editService(s);
		}
//		else
//			super.onItemClick(listView, view, pos, id);
	}
	
	private void editService(Service s) {
		Intent i = new Intent(getActivity(), ServiceActivity.class);
		if (s != null)
			i.putExtra(Intent.EXTRA_UID, s.getId());
		i.putExtra(Intent.EXTRA_REFERRER, EntityType.Host.name());
		i.putExtra(Intent.EXTRA_LOCAL_ONLY, EntityType.Gateway.name());
		i.putExtra(Intent.EXTRA_ORIGINATING_URI, ((Host)itemBeingEdited).getId());
		startActivityForResult(i, EntityType.Gateway.ordinal());		
	}

	@Override
	protected EntityType getItemType() {
		return EntityType.Host;
	}

	@Override
	protected void onSaveObject() {
		if (topoService == null || itemBeingEdited == null)
			return;
		Host h = (Host)itemBeingEdited;
		h.setName(editName.getText().toString());
		topoService.saveHost(h);
	}
}
