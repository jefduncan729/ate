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

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.adapter.TopologyAdapter;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

public class TopologyListFragment extends ListFragment implements OnItemClickListener {
	private static final String TAG = TopologyListFragment.class.getSimpleName();

	private Topology t;
	private String src;
	private boolean haveConsole;
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
	
	public void update(Topology in, String src, boolean haveConsole) {
		t = in;
		this.src = src;
		this.haveConsole = haveConsole;
		refreshAdapter();
	}
	
	private void refreshAdapter() {
		if (t == null)
			setListAdapter(null);
		else {
			getListView().setOnItemClickListener(this);
			setListAdapter(new TopologyAdapter(getActivity(), t, src));
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
		Intent iSsh = null;
		Intent iStart = null;
		Intent iMove = null;
		Intent iDel = new Intent();
		iDel.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, e.id);
		iDel.putExtra(Constants.EXTRA_ITEM_TYPE, e.itemType.name());
		Intent iDelDisk = new Intent();
		iDelDisk.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, e.id);
		iDelDisk.putExtra(Constants.EXTRA_ITEM_TYPE, e.itemType.name());
		iDelDisk.putExtra(Intent.EXTRA_DATA_REMOVED, true);
		int p=0;
		switch (e.itemType) {
			case Host:
				menu.add(0, R.id.action_delete, p++, R.string.action_delete).setIntent(iDel);
				if (haveConsole) {
					iSsh = new Intent();
					iSsh.putExtra(Constants.EXTRA_ITEM_ID, e.id);
					iSsh.putExtra(Constants.EXTRA_ITEM_NAME, e.name);
					menu.add(0, R.id.action_ssh_to_host, p++, R.string.action_ssh_to_host).setIntent(iSsh);
				}
			break;
			case Group:
				iAdd = new Intent();
				iAdd.putExtra(Constants.EXTRA_REFERRING_ITEM_TYPE, e.itemType.name());
				iAdd.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Gateway.name());
				iAdd.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, e.id);
				if (e.data == 0) {
					menu.add(0, R.id.action_delete, p++, R.string.action_delete).setIntent(iDel);
					menu.add(0, R.id.action_delete_disk, p++, R.string.action_delete_disk).setIntent(iDelDisk);
				}
				menu.add(0, R.id.action_add_gateway, p++, "Add " + EntityType.Gateway.name()).setIntent(iAdd);
			break;
			case Gateway:
				iMove = new Intent();
				iMove.putExtra(Constants.EXTRA_ITEM_ID, e.id);
				menu.add(0, R.id.action_move_gateway, p++, R.string.action_move_gateway).setIntent(iMove);
				if (haveConsole) {
					iStart = new Intent();
					iStart.putExtra(Constants.EXTRA_ITEM_ID, e.id);
					menu.add(0, R.id.action_start_gateway, p++, R.string.action_start_gateway).setIntent(iStart);
				}
				menu.add(0, R.id.action_delete, p++, R.string.action_delete).setIntent(iDel);
				menu.add(0, R.id.action_delete_disk, p++, R.string.action_delete_disk).setIntent(iDelDisk);
			break;
			case NodeManager:
			break;
		}
		if (menu.size() == 0)
			return;
		menu.setHeaderTitle(e.name);
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
