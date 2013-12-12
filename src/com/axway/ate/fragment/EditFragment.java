package com.axway.ate.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.R;
import com.axway.ate.util.UiUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

abstract public class EditFragment extends Fragment implements OnClickListener {
	
	private static final String TAG = EditFragment.class.getSimpleName();
	
	public interface Listener {
		public void onSaveObject(Object h, Bundle extras);
		public void onEditService(Object from, Service s);
		public void onAddService(Object from);
	}
	
	protected EditText editName;
	protected View invalidView;
	protected View viewGateways;
	protected View viewTags;
	
	protected DomainHelper helper;
	protected Object itemBeingEdited;
	protected String itemId;
	protected Topology topology;
	protected int action;	//add or update
	protected Listener listener;
	private String jsonRep;

	abstract protected void onPrepareItem(JsonObject json);
	abstract protected void onDisplayItem();
	abstract protected void onSaveItem(Bundle extras);
	abstract protected int getLayoutId();
	abstract protected EntityType getItemType();
	
	public EditFragment() {
		super();
		Log.d(TAG, "constructor");
		helper  = DomainHelper.getInstance();
		topology = null;
	}

	public void setListener(Listener l) {
		listener = l;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setHasOptionsMenu(true);
		jsonRep = null;
		String jsonTopo = null;
		action = R.id.action_edit;
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_JSON_ITEM))
				jsonRep = savedInstanceState.getString(Constants.EXTRA_JSON_ITEM);
			if (savedInstanceState.containsKey(Constants.EXTRA_ACTION))
				action = savedInstanceState.getInt(Constants.EXTRA_ACTION);
			if (savedInstanceState.containsKey(Constants.EXTRA_JSON_TOPOLOGY))
				jsonTopo = savedInstanceState.getString(Constants.EXTRA_JSON_TOPOLOGY);
		}
		else if (getArguments() != null) {
			jsonRep = getArguments().getString(Constants.EXTRA_JSON_ITEM);
			action = getArguments().getInt(Constants.EXTRA_ACTION, R.id.action_edit);
			jsonTopo = getArguments().getString(Constants.EXTRA_JSON_TOPOLOGY);
		}
		if (jsonRep == null)
			throw new IllegalStateException("must provide JSON representation of object being edited");
		if (jsonTopo != null)
			topology = helper.topologyFromJson(helper.parse(jsonTopo).getAsJsonObject());
		prepareItem();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof Listener) 
			listener = (Listener)activity;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		View rv = inflater.inflate(getLayoutId(), null);
		editName = (EditText)rv.findViewById(R.id.edit_name);
		viewGateways = rv.findViewById(R.id.container01);
		viewTags = rv.findViewById(R.id.container02);
		return rv;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.d(TAG, "onViewCreated");
	}

	protected String getObjectHostId() {
		String rv = null;
		if (itemBeingEdited instanceof Service)
			rv = ((Service)itemBeingEdited).getHostID();
		return rv;
	}
	
	protected boolean isValid() {
		invalidView = null;
		if (TextUtils.isEmpty(editName.getText().toString()))
			setInvalidView(editName);
		return (invalidView == null);
	}
	
	protected void setInvalidView(View v) {
		invalidView = v;
	}
	
	protected View getInvalidView() {
		return invalidView;
	}

	protected void notifyInvalid() {
		String msg = "Please complete the form";
		if (invalidView != null) {
			switch (invalidView.getId()) {
				case R.id.edit_name:
					msg = "Provide a name";
				break;
				case R.id.edit_mgmt_port:
					msg = "Provide a management port";
				break;
				case R.id.edit_host:
					msg = "Specify a host";
				break;
				case R.id.edit_group:
					msg = "Specify a group";
				break;
			}
		}
		UiUtils.showToast(getActivity(), msg);
	}
	
	protected void save() {
		Log.d(TAG, "save");
		if (isValid()) {
			Bundle extras = new Bundle();
			onSaveItem(extras);
			if (listener != null)
				listener.onSaveObject(itemBeingEdited, extras);
		}
		else {
			notifyInvalid();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		Log.d(TAG, "onCreateOptionsMenu");
		inflater.inflate(R.menu.item, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean rv = true;
		switch (item.getItemId()) {
			case R.id.action_save:
				if (isValid())
					save();
				else
					notifyInvalid();
			break;
			case R.id.action_delete:
			break;
			default:
				rv = super.onOptionsItemSelected(item);
		}
		return rv;
	}

	@Override
	public void onClick(View v) {
	}

	private void prepareItem() {
		Log.d(TAG, "prepareItem");
		JsonElement e = helper.parse(jsonRep);
		if (e != null)
			onPrepareItem(e.getAsJsonObject());
	}

	private void displayItem() {
		Log.d(TAG, "displayItem");
		onDisplayItem();
	}
	
	protected void showGateways(boolean show) {
		if (viewGateways == null)
			return;
		viewGateways.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	protected void showTags(boolean show) {
		if (viewTags == null)
			return;
		viewTags.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		displayItem();
	}
	
	protected void editService(Service s) {
		if (listener == null)
			return;
		if (s == null)
			listener.onAddService(itemBeingEdited);
		else
			listener.onEditService(itemBeingEdited, s);
	}
}
