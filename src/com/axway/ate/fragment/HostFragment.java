package com.axway.ate.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;

import com.axway.ate.R;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Topology.EntityType;

public class HostFragment extends EditFragment implements OnItemClickListener {
	
//	private ListView listGateways;
	private ImageButton btnAddSvc;

	public HostFragment() {
		super();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.edit_host;
	}

	@Override
	protected void onDisplayItem() {
		editName.setText(((Host)itemBeingEdited).getName());
//		if (TextUtils.isEmpty(itemId))
//			showGateways(false);
//		else {
//			List<Service> svcs = new ArrayList<Service>();
//			svcs.addAll(topology.getServicesOnHost(itemId, ServiceType.gateway));
//			listGateways.setAdapter(new ServiceListAdapter(getActivity(), svcs));
//			showGateways(false);
//		}
	}

	@Override
	protected void onPrepareItem(JsonObject json) {
		Host h;
		if (action == R.id.action_add) {
			h = new Host();
		}
		else {
			h = helper.hostFromJson(json);
		}
		itemBeingEdited = h;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = super.onCreateView(inflater, container, savedInstanceState);	//inflater.inflate(R.layout.edit_host, null);
//		listGateways = (ListView)rv.findViewById(R.id.gateway_list);
//		listGateways.setOnItemClickListener(this);
		viewGateways.setVisibility(View.GONE);
//		btnAddSvc = (ImageButton)rv.findViewById(R.id.action_add_gateway);
//		if (btnAddSvc != null)
//			btnAddSvc.setOnClickListener(this);
		return rv;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
		if (listView.getId() == R.id.gateway_list) {
//			Service s = (Service)listGateways.getItemAtPosition(pos);
//			editService(s);
		}
	}

	@Override
	protected EntityType getItemType() {
		return EntityType.Host;
	}

	@Override
	protected void onSaveItem() {
		if (itemBeingEdited == null)
			return;
		Host h = (Host)itemBeingEdited;
		h.setName(editName.getText().toString());
	}
}
