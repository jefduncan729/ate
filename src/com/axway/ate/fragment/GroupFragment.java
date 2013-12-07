package com.axway.ate.fragment;

import java.util.ArrayList;
import java.util.List;
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
import android.widget.ListView;

import com.axway.ate.R;
import com.axway.ate.activity.ServiceActivity;
import com.axway.ate.adapter.ServiceListAdapter;
import com.axway.ate.fragment.EditTagDialog.EditTagListener;
import com.google.gson.JsonElement;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class GroupFragment extends TagAwareFragment implements OnItemClickListener, EditTagListener {

	private ListView listGateways;
	private ImageButton btnAddSvc;

	public GroupFragment() {
		super();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = super.onCreateView(inflater, container, savedInstanceState);
		listGateways = (ListView)rv.findViewById(R.id.gateway_list);
		listGateways.setOnItemClickListener(this);
		btnAddSvc = (ImageButton)rv.findViewById(R.id.action_add_gateway);
		if (btnAddSvc != null)
			btnAddSvc.setOnClickListener(this);
		return rv;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.edit_grp;
	}
//
//	@Override
//	protected Bundle collectResults() {
//		Bundle rv = super.collectResults();
//		Group group = new Group();
//		group.setId(((Group)itemBeingEdited).getId());
//		group.setName(editName.getText().toString());
//		rv.putString(Intent.EXTRA_SUBJECT, helper.toJson(group).toString());
//		return rv;
//	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
		if (listView.getId() == R.id.gateway_list) {
			Service s = (Service)listGateways.getItemAtPosition(pos);
			editService(s);
		}
		else
			super.onItemClick(listView, view, pos, id);
	}
	
	private void editService(Service s) {
		Intent i = new Intent(getActivity(), ServiceActivity.class);
		if (s != null)
			i.putExtra(Intent.EXTRA_UID, s.getId());
		i.putExtra(Intent.EXTRA_SUBJECT, ((Group)itemBeingEdited).getName());
		startActivityForResult(i, EntityType.Gateway.ordinal());
	}
	
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
						topoService.saveService(svc, (Group)itemBeingEdited);
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
		List<Service> svcs = new ArrayList<Service>();
		svcs.addAll(((Group)itemBeingEdited).getServicesByType(ServiceType.gateway));
		listGateways.setAdapter(new ServiceListAdapter(getActivity(), svcs));
	}

	@Override
	protected void onPrepareItem() {
		Group g;
		if (TextUtils.isEmpty(itemId)) {
			g = new Group();
		}
		else {
			g = topology.getGroup(itemId);
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

	@Override
	protected void onSaveObject() {
		if (topoService == null || itemBeingEdited == null)
			return;
		Group g = (Group)itemBeingEdited;
		g.setName(editName.getText().toString());
		topoService.saveGroup(g);
	}
}
