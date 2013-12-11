package com.axway.ate.fragment;

import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;

import com.axway.ate.R;
import com.axway.ate.fragment.EditTagDialog.EditTagListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology.EntityType;

public class GroupFragment extends TagAwareFragment implements OnItemClickListener, EditTagListener {

//	private ListView listGateways;
	private ImageButton btnAddSvc;

	public GroupFragment() {
		super();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = super.onCreateView(inflater, container, savedInstanceState);	//inflater.inflate(R.layout.edit_grp, null);
		viewGateways.setVisibility(View.GONE);		
//		listGateways = (ListView)rv.findViewById(R.id.gateway_list);
//		listGateways.setOnItemClickListener(this);
//		btnAddSvc = (ImageButton)rv.findViewById(R.id.action_add_gateway);
//		if (btnAddSvc != null)
//			btnAddSvc.setOnClickListener(this);
		return rv;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.edit_grp;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
//		if (listView.getId() == R.id.gateway_list) {
//			Service s = (Service)listGateways.getItemAtPosition(pos);
//			editService(s);
//		}
//		else
			super.onItemClick(listView, view, pos, id);
	}
//	
//	private void editService(Service s) {
//		Intent i = new Intent(getActivity(), ServiceActivity.class);
//		if (s != null)
//			i.putExtra(Intent.EXTRA_UID, s.getId());
//		i.putExtra(Intent.EXTRA_SUBJECT, ((Group)itemBeingEdited).getName());
//		startActivityForResult(i, EntityType.Gateway.ordinal());
//	}
	
	@Override
	protected Map<String, String> getObjectTags() {
		if (itemBeingEdited == null)
			return null;
		return ((Group)itemBeingEdited).getTags();
	}
//
//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		super.onCreateOptionsMenu(menu, inflater);
////		inflater.inflate(R.menu.grp_menu, menu);
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		boolean rv = true;
//		switch (item.getItemId()) {
//			case R.id.action_add_gateway:
//				if (getTopologyEditor() != null) {
//					Service s = getTopologyEditor().onNewService(group);
//					editService(s);
//				}
//			break;
//			default:
//				rv = super.onOptionsItemSelected(item);
//		}
//		return rv;
//	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == EntityType.Group.ordinal()) || (requestCode == EntityType.Gateway.ordinal()) || (requestCode == EntityType.NodeManager.ordinal())) {
			if (resultCode == Activity.RESULT_OK) {
				String s = data.getStringExtra(Intent.EXTRA_SUBJECT);
				if (TextUtils.isEmpty(s))
					return;
				JsonElement json = helper.parse(s);
				if (json != null) {
					Service svc = helper.serviceFromJson(json.getAsJsonObject());
					if (svc != null) {
//						topoService.updateService(svc);	//, (Group)itemBeingEdited);
					}
				}
			}
		}
		else
			super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.action_add_gateway) {
			if (itemBeingEdited != null) {
//				Service s = topoService.createGateway((Group)itemBeingEdited);
				editService(null);
			}
		}
		else
			super.onClick(v);
	}

	@Override
	protected void onDisplayItem() {
//		Group group;
//		if (newObj) {
//			group = new Group();
//			group.setId(topology.generateID(EntityType.Group));
//		}
//		else
//			group = topology.getGroup(itemId);
//		if (group == null)
//			return;
//		itemBeingEdited = group;
		
		editName.setText(((Group)itemBeingEdited).getName());
		displayTags();
//		List<Service> svcs = new ArrayList<Service>();
//		svcs.addAll(((Group)itemBeingEdited).getServicesByType(ServiceType.gateway));
//		listGateways.setAdapter(new ServiceListAdapter(getActivity(), svcs));
	}

	@Override
	protected void onPrepareItem(JsonObject json) {
		Group g;
		if (action == R.id.action_add) {	//TextUtils.isEmpty(itemId)) {
			g = new Group();
		}
		else {
			g = helper.groupFromJson(json);	//topology.getGroup(itemId);
		}
		itemBeingEdited = g;
	}

	@Override
	protected boolean isValid() {
		boolean rv = super.isValid(); 
		if (rv) {
			
		}
		return rv;
	}

	@Override
	protected EntityType getItemType() {
		return EntityType.Group;
	}
//
//	@Override
//	protected void onSaveObject() {
//		if (topoService == null || itemBeingEdited == null)
//			return;
//		Group g = (Group)itemBeingEdited;
//		g.setName(editName.getText().toString());
//		if (action == R.id.action_add)
//			topoService.addGroup(g);
//		else if (action == R.id.action_edit)
//			topoService.updateGroup(g);
//	}

	@Override
	protected void onSaveItem() {
		if (itemBeingEdited == null)
			return;
		Group g = (Group)itemBeingEdited;
		g.setName(editName.getText().toString());
	}
}
