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

import com.axway.ate.R;
import com.axway.ate.activity.DomainHelper;
import com.axway.ate.activity.MainService;
import com.axway.ate.util.UiUtils;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

abstract public class EditFragment extends Fragment implements OnClickListener {
	
	private static final String TAG = EditFragment.class.getSimpleName();
	
	protected EditText editName;
	protected View invalidView;
	protected View viewGateways;
	protected View viewTags;
	
	protected DomainHelper helper;
	protected MainService topoService;
	protected Object itemBeingEdited;
	protected String itemId;
	protected Topology topology;

	abstract protected void onPrepareItem();
	abstract protected void onDisplayItem();
	abstract protected int getLayoutId();
	abstract protected EntityType getItemType();
	abstract protected void onSaveObject();
	
	public EditFragment() {
		super();
		Log.d(TAG, "constructor");
		helper  = DomainHelper.getInstance();
		topoService = null;
		topology = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setHasOptionsMenu(true);
		itemId = null;
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Intent.EXTRA_UID))
				itemId = savedInstanceState.getString(Intent.EXTRA_UID);
		}
		else if (getArguments() != null) {
			itemId = getArguments().getString(Intent.EXTRA_UID);
		}
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
		Intent i = new Intent();
		onSaveObject();
		getActivity().setResult(Activity.RESULT_OK, i);
		getActivity().finish();
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
	
	public void onServiceAvailable(MainService s) {
		Log.d(TAG, "onServiceAvailable");
		topology = null;
		this.topoService = s;
		if (topoService != null)
			topology = topoService.getTopology();
		if (topology == null)
			Log.e(TAG, "topology is null when it shouldn't be");
		else {
			prepareItem();
			displayItem();
		}
	}

	private void prepareItem() {
		Log.d(TAG, "prepareItem");
		onPrepareItem();
	}

	private void displayItem() {
		Log.d(TAG, "displayItem");
		if (TextUtils.isEmpty(itemId))
			getActivity().setTitle("New " + getItemType().name());
		else
			getActivity().setTitle("Edit " + itemId);
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
}
