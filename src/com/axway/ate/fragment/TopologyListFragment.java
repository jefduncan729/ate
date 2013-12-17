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

	protected Topology t;
	protected String src;
	protected boolean haveConsole;
	protected Listener listener;

	public interface Listener {
		public void onTopologyLoaded(Topology t);
		public void onTopologyItemSelected(EntityType itemType, String id);
		public void onDelete(Intent i);
		public void onSshToHost(Intent i);
		public void onAddGateway(Intent i);
		public void onStartGateway(Intent i);
		public void onAddHost(Intent i);
		public void onAddGroup(Intent i);
		public void onSaveToDisk(Intent i);
		public void onCompare(Intent i);
	}
	
	public TopologyListFragment() {
		super();
		t = null;
		listener = null;
		src = null;
		haveConsole = false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		Bundle args = getArguments();
		if (args != null) {
			src = args.getString(Constants.EXTRA_TOPO_SOURCE);
			haveConsole = args.getBoolean(Constants.EXTRA_HAVE_CONSOLE);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = inflater.inflate(android.R.layout.list_content, null);
		return rv;
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
		inflater.inflate(R.menu.topo, menu);
//		optsMenu = menu;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (listener == null)
			return super.onOptionsItemSelected(item);
		boolean rv = true;
		Intent i = item.getIntent();
		switch (item.getItemId()) {
			case R.id.action_add_host:
				listener.onAddHost(i);
			break;
			case R.id.action_add_group:
				listener.onAddGroup(i);
			break;
			case R.id.action_save_to_disk:
				listener.onSaveToDisk(i);
			break;
			case R.id.action_compare_topo:
				listener.onCompare(i);
			break;
			default:
				rv = false;
		}
		return rv;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (listener == null)
			return false;
		Intent i = item.getIntent();
		if (i == null)
			return false;
		boolean rv = true;
		switch (item.getItemId()) {
			case R.id.action_delete:
				listener.onDelete(i);
			break;
			case R.id.action_ssh_to_host:
				listener.onSshToHost(i);
			break;
			case R.id.action_add_gateway:
				listener.onAddGateway(i);
			break;
			case R.id.action_start_gateway:
				listener.onStartGateway(i);
			break;
			default:
				rv = false;
		}
		return rv;
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
//		Intent iMove = null;
		Intent iDel = new Intent();
		iDel.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, e.id);
		iDel.putExtra(Constants.EXTRA_ITEM_TYPE, e.itemType.name());
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
				}
				menu.add(0, R.id.action_add_gateway, p++, "Add " + EntityType.Gateway.name()).setIntent(iAdd);
			break;
			case Gateway:
//				iMove = new Intent();
//				iMove.putExtra(Constants.EXTRA_ITEM_ID, e.id);
//				menu.add(0, R.id.action_move_gateway, p++, R.string.action_move_gateway).setIntent(iMove);
				if (haveConsole) {
					iStart = new Intent();
					iStart.putExtra(Constants.EXTRA_ITEM_ID, e.id);
					menu.add(0, R.id.action_start_gateway, p++, R.string.action_start_gateway).setIntent(iStart);
				}
				menu.add(0, R.id.action_delete, p++, R.string.action_delete).setIntent(iDel);
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
		if (menu == null)
			return;
		boolean haveTopo = (t != null);
		MenuItem i = menu.findItem(R.id.action_save_to_disk);
		if (i != null)
			i.setVisible(haveTopo);
		i = menu.findItem(R.id.action_compare_topo);
		if (i != null)
			i.setVisible(haveTopo);
		i = menu.findItem(R.id.action_add_host);
		if (i != null)
			i.setVisible(haveTopo);
		i = menu.findItem(R.id.action_add_group);
		if (i != null)
			i.setVisible(haveTopo);
	}
}
