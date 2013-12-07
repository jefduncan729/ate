package com.axway.ate.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.axway.ate.R;
import com.axway.ate.adapter.TopologyAdapter;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

public class TopologyListFragment extends ListFragment implements OnItemClickListener {
	private static final String TAG = TopologyListFragment.class.getSimpleName();

	private Topology t;
	private Listener listener;

	public interface Listener {
		public void onTopologyItemSelected(EntityType itemType, String id);
		public boolean onMenuItemSelected(MenuItem item);
		public void onPrepareMenu(Menu menu);
	}
	
	public TopologyListFragment() {
		super();
		t = null;
		listener = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = inflater.inflate(android.R.layout.list_content, null);
		return rv;
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshAdapter();
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
		if (listener == null)
			return;
		TopologyAdapter.Entry e = (TopologyAdapter.Entry)getListView().getItemAtPosition(pos);
		if (e == null)
			return;
		listener.onTopologyItemSelected(e.itemType, e.id);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof Listener)
			listener = (Listener)activity;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.main, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (listener == null)
			return super.onOptionsItemSelected(item);
		return listener.onMenuItemSelected(item);
	}
	
	public void update(Topology in) {
		t = in;
		refreshAdapter();
	}
	
	private void refreshAdapter() {
		if (t == null)
			setListAdapter(null);
		else {
			getListView().setOnItemClickListener(this);
			setListAdapter(new TopologyAdapter(getActivity(), t));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (listener == null)
			return false;
		return listener.onMenuItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo cmi = (AdapterContextMenuInfo)menuInfo;
		TopologyAdapter.Entry e = (TopologyAdapter.Entry)getListView().getItemAtPosition(cmi.position);
		if (e == null)
			return;
		Intent iAdd = null;
		Intent iDel = null;
		iDel = new Intent();
		iDel.putExtra(Intent.EXTRA_ORIGINATING_URI, e.id);
		iDel.putExtra(Intent.EXTRA_SUBJECT, e.itemType.name());
		int p=0;
		switch (e.itemType) {
			case Host:
			case Group:
				iAdd = new Intent();
				iAdd.putExtra(Intent.EXTRA_REFERRER, e.itemType.name());
				iAdd.putExtra(Intent.EXTRA_LOCAL_ONLY, EntityType.Gateway.name());
				iAdd.putExtra(Intent.EXTRA_ORIGINATING_URI, e.id);
				if (e.data == 0)
					menu.add(0, R.id.action_delete, p++, "Remove");
				menu.add(0, R.id.action_add_gateway, p++, "New API Server Instance");
			break;
			case Gateway:
				menu.add(0, R.id.action_delete, p++, "Remove");
			break;
			case NodeManager:
			break;
		}
		if (menu.size() == 0)
			return;
		menu.setHeaderTitle(e.name);
		MenuItem mi = menu.findItem(R.id.action_add_gateway);
		if (mi != null)
			mi.setIntent(iAdd);
		mi = menu.findItem(R.id.action_delete);
		if (mi != null)
			mi.setIntent(iDel);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnCreateContextMenuListener(this);
		setEmptyText("No topology loaded");
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (listener == null)
			super.onPrepareOptionsMenu(menu);
		else
			listener.onPrepareMenu(menu);
	}
}
