package com.axway.ate.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.AdapterView.OnItemClickListener;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Topology.EntityType;

public class HostFragment extends EditFragment implements OnItemClickListener {
	
	private CheckBox editUseSsl;
	private View ctrSsl;
	
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
		if (editUseSsl != null) {
			editUseSsl.setChecked(true);
		}
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
		editUseSsl = (CheckBox)rv.findViewById(R.id.edit_use_ssl);
		ctrSsl = rv.findViewById(R.id.container03);
		if (ctrSsl != null)
			ctrSsl.setVisibility(action == R.id.action_add ? View.VISIBLE : View.GONE);
//		listGateways = (ListView)rv.findViewById(R.id.gateway_list);
//		listGateways.setOnItemClickListener(this);
		viewGateways.setVisibility(View.GONE);
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
	protected void onSaveItem(Bundle extras) {
		if (itemBeingEdited == null)
			return;
		Host h = (Host)itemBeingEdited;
		h.setName(editName.getText().toString());
		if (action == R.id.action_add && editUseSsl != null)
			extras.putBoolean(Constants.EXTRA_USE_SSL, editUseSsl.isChecked());
	}
}
