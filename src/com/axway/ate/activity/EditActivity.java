package com.axway.ate.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.R;
import com.axway.ate.fragment.EditFragment;
import com.axway.ate.fragment.EditFragment.Listener;
import com.axway.ate.fragment.GroupFragment;
import com.axway.ate.fragment.HostFragment;
import com.axway.ate.fragment.ServiceFragment;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology.EntityType;

public class EditActivity extends SinglePaneActivity implements Listener {

	private static final String TAG = EditActivity.class.getSimpleName();
	protected EntityType eType;
	protected int action;
	private DomainHelper helper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = DomainHelper.getInstance();
		action = getIntent().getIntExtra(Constants.EXTRA_ACTION, R.id.action_edit);
	}
	
	@Override
	protected EditFragment onCreateFragment() {
		EditFragment rv = null;
		String s = getIntent().getStringExtra(Constants.EXTRA_ITEM_TYPE);
		eType = EntityType.valueOf(s);
		switch (eType) {
			case Host:
				rv = new HostFragment();
			break;
			case Group:
				rv = new GroupFragment();
			break;
			case Gateway:
				rv = new ServiceFragment();
			break;
			case NodeManager:
			break;
		}
		if (rv != null)
			rv.setListener(this);
		return rv;
	}

	@Override
	public void onSaveObject(Object o, Bundle extras) {
		Intent i = new Intent();
		if (extras != null)
			i.putExtras(extras);
		if (o instanceof Host)
			i.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson((Host)o).toString());
		else if (o instanceof Group)
			i.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson((Group)o).toString());
		else if (o instanceof Service)
			i.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson((Service)o).toString());
		else
			return;
		i.putExtra(Constants.EXTRA_ITEM_TYPE, eType.name());
		setResult(Activity.RESULT_OK, i);
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		String title = (action == R.id.action_add ? "Add " : "Edit ");
		setTitle(title + eType.name());
	}

	@Override
	public void onEditService(Object from, Service s) {
		Intent i = new Intent();
		if (from instanceof Host) {
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, ((Host)from).getId());
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_TYPE, EntityType.Host.name());
		}
		else if (from instanceof Group) {
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, ((Group)from).getId());
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_TYPE, EntityType.Group.name());
		}
		else
			return;
		i.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Gateway.name());
		i.putExtra(Constants.EXTRA_ITEM_ID, s.getId());
		i.putExtra(Constants.EXTRA_ACTION, R.id.action_edit);
		setResult(Activity.RESULT_OK, i);
		finish();
	}

	@Override
	public void onAddService(Object from) {
		Intent i = new Intent();
		if (from instanceof Host) {
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, ((Host)from).getId());
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_TYPE, EntityType.Host.name());
		}
		else if (from instanceof Group) {
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, ((Group)from).getId());
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_TYPE, EntityType.Group.name());
		}
		else
			return;
		i.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Gateway.name());
		i.putExtra(Constants.EXTRA_ACTION, R.id.action_add);
		setResult(Activity.RESULT_OK, i);
		finish();
	}
}
