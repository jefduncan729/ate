package com.axway.ate.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpStatus;
import org.springframework.http.HttpMethod;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.axway.ate.ApiException;
import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.R;
import com.axway.ate.ServerInfo;
import com.axway.ate.TopologyClient;
import com.axway.ate.db.DbHelper.ConnMgrColumns;
import com.axway.ate.fragment.AlertDialogFragment;
import com.axway.ate.fragment.ConfirmDeleteDialog;
import com.axway.ate.fragment.ConfirmDeleteDialog.DeleteListener;
import com.axway.ate.fragment.GatewayDialog;
import com.axway.ate.fragment.GatewayDialog.GatewayListener;
import com.axway.ate.fragment.GroupDialog;
import com.axway.ate.fragment.GroupDialog.GroupListener;
import com.axway.ate.fragment.HostDialog;
import com.axway.ate.fragment.HostDialog.HostListener;
import com.axway.ate.fragment.SelectFileDialog;
import com.axway.ate.fragment.SelectServerDialog;
import com.axway.ate.fragment.TopologyListFragment;
import com.axway.ate.service.RestService;
import com.axway.ate.util.UiUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class TopologyActivity extends BaseActivity implements TopologyClient, TopologyListFragment.Listener, SelectServerDialog.Listener, HostListener, GroupListener, GatewayListener, DeleteListener {
	
	private static final String TAG = TopologyActivity.class.getSimpleName();
	
	private View ctr01;
	private ProgressBar prog01;
	
	private TopologyListFragment topoListFrag;
	
	private ServerInfo srvrInfo;
	private File file;
	private DomainHelper helper;
	private Topology topology;
	private Intent outstandingIntent;
	private Boolean haveConsole;
	private String consoleHandle;
	private Intent hostNodeMgrIntent;
	
	private ResultReceiver resRcvr;
	
	private Group selGrp;
	
	public TopologyActivity() {
		super();
		topoListFrag = null;
		srvrInfo = null;
		file = null;
		helper = DomainHelper.getInstance();
		selGrp = null;
		resRcvr = null;
		outstandingIntent = null;
		haveConsole = null;
		consoleHandle = null;
		hostNodeMgrIntent = null;
	}

	private ResultReceiver getResultReceiver() {
		if (resRcvr == null) {
			resRcvr = new ResultReceiver(new Handler()) {
				@Override
				protected void onReceiveResult(int resultCode, Bundle resultData) {
					onServiceResult(resultCode, resultData);
				}
			};
		}
		return resRcvr;
	}
	
	private void onServiceResult(int code, Bundle data) {
		outstandingIntent = null;
		if (code == HttpStatus.SC_OK) {
			processResult(data);
		}
		else {
			EntityType et = null;
			if (data != null) {
				et = EntityType.valueOf(data.getString(Constants.EXTRA_ITEM_TYPE));
			}
			switch (code) {
				case Constants.CERT_NOT_TRUSTED:
					onCertNotTrusted(data.getString(Intent.EXTRA_BUG_REPORT));
				break;
				case HttpStatus.SC_BAD_REQUEST: {
					String msg = "Invalid data passed to server";
					if (EntityType.Gateway == et)
						msg = "Most likely, the services port is in use";
					else if (EntityType.Host == et)
						;
					else if (EntityType.Group == et)
						;
					networkError(msg, "Bad Request");
				}
				break;
				case HttpStatus.SC_NOT_FOUND:
					UiUtils.showToast(this, "Not found");
				break;
				case HttpStatus.SC_UNAUTHORIZED:
					networkError("Please check your connection settings.", "Not Authorized");
				break;
				case HttpStatus.SC_FORBIDDEN:
					networkError("Please check your connection settings.", "Access Denied");
				break;
				default:
					alertDialog("Error", "Status Code: " + Integer.toString(code) + "\n" + data.getString(Intent.EXTRA_BUG_REPORT), null);
			}
		}
	}

	private void processResult(Bundle data) {
		String action = data.getString(Constants.EXTRA_ACTION);
		if (HttpMethod.GET.name().equals(action)) {
			String jstr = data.getString(Constants.EXTRA_JSON_ITEM);
			topology = helper.topologyFromJson(helper.parse(jstr).getAsJsonObject());
			showProgress(false);
			updateTopologyView();
			return;
		}
		else if (RestService.ACTION_CHECK_CERT.equals(action)) {
			loadFromServer();
			return;
		}
		else if (RestService.ACTION_COMPARE.equals(action)) {
			showProgress(false);
			infoDialog("Compare Results", data.getString(Constants.EXTRA_COMPARE_RESULT));
			return;
		}
		UiUtils.showToast(this, "Update successful");
		if (getPrefs().getBoolean(Constants.KEY_RELOAD_AFTER_UPD, true)) {
			loadFromServer();
			return;
		}
		if (HttpMethod.POST.name().equals(action)) {
			//for POSTs we need to update the id of the new item; for PUT/DELETE the topology view has already been updated
			EntityType typ = EntityType.valueOf(data.getString(Constants.EXTRA_ITEM_TYPE));
			String jstr = data.getString(Constants.EXTRA_JSON_ITEM);
			if (EntityType.Host == typ) {
				Host h = helper.hostFromJson(helper.parse(jstr).getAsJsonObject());
				Host th = topology.getHostByName(h.getName());
				if (th != null) {
					th.setId(h.getId());
					updateTopologyView();
				}
				if (hostNodeMgrIntent != null) {
					UiUtils.showToast(this, "Adding NodeManager for host " + h.getName());
					Service nmSvc = new Service();
					nmSvc.setHostID(h.getId());
					nmSvc.setType(ServiceType.nodemanager);
					nmSvc.setEnabled(true);
					nmSvc.setScheme(hostNodeMgrIntent.getBooleanExtra(Constants.EXTRA_USE_SSL, true) ? Constants.HTTPS_SCHEME : Constants.HTTP_SCHEME);
					nmSvc.setManagementPort(hostNodeMgrIntent.getIntExtra(Constants.EXTRA_MGMT_PORT, 0));
					nmSvc.setName(EntityType.NodeManager.name() + "-" + Integer.toString(nmSvc.getManagementPort()));
					hostNodeMgrIntent.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson(nmSvc).toString());
					asyncModify(hostNodeMgrIntent);
					hostNodeMgrIntent = null;
				}
			}
			else if (EntityType.Group == typ) {
				Group g = helper.groupFromJson(helper.parse(jstr).getAsJsonObject());
				Group tg = topology.getGroupByName(g.getName());
				if (tg != null) {
					tg.setId(g.getId());
					tg.setTags(g.getTags());
					updateTopologyView();
				}
			}
			else if (EntityType.Gateway == typ) {
				Service s = helper.serviceFromJson(helper.parse(jstr).getAsJsonObject());
				Group g = topology.getGroup(data.getString(Constants.EXTRA_REFERRING_ITEM_ID));
				if (g == null) {
					Log.e(TAG, "expecting group for service " + s.getId());
					return;
				}
				Service ts = g.getServiceByName(s.getName());
				if (ts != null) {
					ts.setId(s.getId());
					updateTopologyView();
				}
			}
		}
		else if (HttpMethod.DELETE.name().equals(action)) {
			EntityType typ = EntityType.valueOf(data.getString(Constants.EXTRA_ITEM_TYPE));
			if (EntityType.NodeManager == typ) {
				String id = data.getString(Constants.EXTRA_HOST_ID);
				Host h = topology.getHost(id);
				if (h == null)
					return;
				UiUtils.showToast(this, "Removing host " + h.getName());
				asyncModify(createModifyIntent(HttpMethod.DELETE, EntityType.Host, helper.toJson(h)));
				topology.removeHost(h);
				updateTopologyView();
			}
		}
	}

	private void showProgress(boolean show) {
//		if (show) {
//			prog01.setIndeterminate(true);
//			prog01.setVisibility(View.VISIBLE);
//			ctr01.setVisibility(View.GONE);
//		}
//		else {
//			prog01.setVisibility(View.GONE);
//			ctr01.setVisibility(View.VISIBLE);
//		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.empty_frag);
		setTitle(getString(R.string.topology));
		ctr01 = (View)findViewById(R.id.container01);
		prog01 = (ProgressBar)findViewById(android.R.id.progress);
		ctr01.setVisibility(View.VISIBLE);
		prog01.setVisibility(View.GONE);
		showProgress(false);
		if (savedInstanceState != null) {
//			topoListFrag = (TopologyListFragment)getFragmentManager().findFragmentByTag("topoFrag");
			String s = savedInstanceState.getString(Constants.EXTRA_JSON_TOPOLOGY);
			if (s != null)
				topology = helper.topologyFromJson(helper.parse(s).getAsJsonObject());
			consoleHandle = savedInstanceState.getString(Constants.EXTRA_CONSOLE_HANDLE);
			Bundle b = savedInstanceState.getBundle(Constants.EXTRA_SERVER_INFO);
			if (b != null)
				srvrInfo = ServerInfo.fromBundle(b);
		}
//		if (topoListFrag == null) {
//			topoListFrag = new TopologyListFragment();
//			getFragmentManager().beginTransaction().replace(R.id.container01, topoListFrag, "topoFrag").commit();
//		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (topology != null)
			outState.putString(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(topology).toString());
		if (srvrInfo != null)
			outState.putBundle(Constants.EXTRA_SERVER_INFO, srvrInfo.toBundle());
		if (consoleHandle != null)
			outState.putString(Constants.EXTRA_CONSOLE_HANDLE, consoleHandle);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (topology == null) {
			loadTopology();
		}
		else
			updateTopologyView();
	}

	@Override
	public void onPrepareMenu(Menu menu) {
//		boolean haveTopo = (topology != null);
//		MenuItem i = menu.findItem(R.id.action_save_to_anm);
//		if (i != null)
//			i.setVisible(haveTopo);
//		i = menu.findItem(R.id.action_save_to_disk);
//		if (i != null)
//			i.setVisible(haveTopo);
//		i = menu.findItem(R.id.action_compare_topo);
//		if (i != null)
//			i.setVisible(haveTopo);
//		i = menu.findItem(R.id.action_add_host);
//		if (i != null)
//			i.setVisible(haveTopo);
//		i = menu.findItem(R.id.action_add_group);
//		if (i != null)
//			i.setVisible(haveTopo);
//		i = menu.findItem(R.id.action_console);
//		if (i != null)
//			i.setVisible(isConsoleAvailable());
	}

	@Override
	public boolean onMenuItemSelected(MenuItem menuItem) {
		boolean rv = true;
		switch (menuItem.getItemId()) {
			case R.id.action_settings:
				showSettings();
			break;
			case R.id.action_remove_trust:
				confirmRemoveTrust();
			break;
			case R.id.action_add_host:
				showHostDialog(null);
			break;
			case R.id.action_add_group:
				showGroupDialog(null);
			break;
			case R.id.action_add_gateway:
				showGatewayDialog(null, menuItem.getIntent());
			break;
			case R.id.action_load_from_disk:
			case R.id.action_save_to_disk:
				showSelectFileDialog(menuItem.getItemId());
			break;
			case R.id.action_load_from_anm:
				file = null;
				loadTopology();
			break;
			case R.id.action_delete:
			case R.id.action_delete_disk:
				delete(menuItem.getIntent());
			break;
			case R.id.action_topo_details:
				topologyDetails();
			break;
			case R.id.action_conn_mgr:
				showConnMgr();
			break;
			case R.id.action_compare_topo:
				compareTopology();
			break;
			case R.id.action_console:
				launchConsole();
			break;
			case R.id.action_ssh_to_host:
				if (menuItem.getIntent() != null) {
					Host h = topology.getHost(menuItem.getIntent().getStringExtra(Constants.EXTRA_ITEM_ID));
					sshToHost(h);
				}
			break;
			case R.id.action_start_gateway:
				if (menuItem.getIntent() != null) {
					String id = menuItem.getIntent().getStringExtra(Constants.EXTRA_ITEM_ID);
					String ids[] = id.split("/");
					if (ids != null && ids.length == 2) {
						Service s = topology.getService(ids[1]);
						if (s != null)
							startGateway(s);
					}
				}
			break;
			case R.id.action_move_gateway:
				if (menuItem.getIntent() != null) {
					String id = menuItem.getIntent().getStringExtra(Constants.EXTRA_ITEM_ID);
					String ids[] = id.split("/");
					if (ids != null && ids.length == 2) {
						Service s = topology.getService(ids[1]);
						if (s != null)
							moveGateway(s);
					}
				}
			break;
			default:
				rv = false;
		}
		return rv;
	}

	private void showHostDialog(Host h) {
		HostDialog dlg = new HostDialog();
		Bundle args = new Bundle();
		if (h == null) {
			h = new Host();
			args.putInt(Constants.EXTRA_ACTION, R.id.action_add);
		}
		else {
			args.putInt(Constants.EXTRA_ACTION, R.id.action_edit);
			args.putString(Intent.EXTRA_UID, h.getId());
			args.putString(Intent.EXTRA_TEXT, h.getName());
		}
		args.putString(Constants.EXTRA_JSON_ITEM, helper.toJson(h).toString());
		dlg.setOnChangeListener(this);
		dlg.setArguments(args);
		dlg.setTopology(topology);
		dlg.show(getFragmentManager(), "hostDlg");
	}

	@Override
	public void onHostChanged(Bundle b) {
		if (b == null)
			return;
		Host h = new Host();
		h.setName(b.getString(Intent.EXTRA_TEXT));
		int act = b.getInt(Constants.EXTRA_ACTION, 0);
		if (act == R.id.action_add) {
			boolean ssl = b.getBoolean(Constants.EXTRA_USE_SSL, false);
			int mp = b.getInt(Constants.EXTRA_MGMT_PORT, 0);
			if (mp == 0)
				return;
			addHost(h, mp, ssl);
		}
		else if (act == R.id.action_edit) {
			h.setId(b.getString(Intent.EXTRA_UID));
			updateHost(h);
		}
	}

	private void showGroupDialog(Group g) {
		GroupDialog dlg = new GroupDialog();
		Bundle args = new Bundle();
		if (g == null) {
			g = new Group();
			args.putInt(Constants.EXTRA_ACTION, R.id.action_add);
		}
		else {
			args.putInt(Constants.EXTRA_ACTION, R.id.action_edit);
			args.putString(Intent.EXTRA_UID, g.getId());
			args.putString(Intent.EXTRA_TEXT, g.getName());
		}
		args.putString(Constants.EXTRA_JSON_ITEM, helper.toJson(g).toString());
		dlg.setOnChangeListener(this);
		dlg.setArguments(args);
		dlg.setTopology(topology);
		dlg.show(getFragmentManager(), "grpDlg");
	}

	@Override
	public void onGroupChanged(Bundle b) {
		if (b == null)
			return;
		Group g = helper.groupFromJson(b.getString(Constants.EXTRA_JSON_ITEM));
		int act = b.getInt(Constants.EXTRA_ACTION, 0);
		if (act == R.id.action_add) {
			addGroup(g);
		}
		else if (act == R.id.action_edit) {
			updateGroup(g);
		}
	}

	private void showGatewayDialog(Service s, Intent i) {
		GatewayDialog dlg = new GatewayDialog();
		Group g = null;
		Bundle args = new Bundle();
		if (s == null) {
			String grpId = i.getStringExtra(Constants.EXTRA_REFERRING_ITEM_ID);
			g = topology.getGroup(grpId);
			s = new Service();
			s.setType(ServiceType.gateway);
			args.putInt(Constants.EXTRA_ACTION, R.id.action_add);
		}
		else {
			g = topology.getGroupForService(s.getId());
			args.putInt(Constants.EXTRA_ACTION, R.id.action_edit);
			args.putString(Intent.EXTRA_UID, s.getId());
			args.putString(Intent.EXTRA_TEXT, s.getName());
		}
		if (g == null) {
			Log.e(TAG, "no group provided");
			return;
		}
		selGrp = g;
		args.putString(Constants.EXTRA_REFERRING_ITEM_ID, g.getId());
		args.putString(Constants.EXTRA_JSON_ITEM, helper.toJson(s).toString());
		dlg.setOnChangeListener(this);
		dlg.setArguments(args);
		dlg.setTopology(topology);
		dlg.show(getFragmentManager(), "gtwyDlg");
	}

	@Override
	public void onGatewayChanged(Bundle b) {
		if (b == null)
			return;
		Service s = helper.serviceFromJson(b.getString(Constants.EXTRA_JSON_ITEM));
		int act = b.getInt(Constants.EXTRA_ACTION, 0);
		if (act == R.id.action_add) {
			addService(s, b.getInt(Constants.EXTRA_SERVICES_PORT, 0));
		}
		else if (act == R.id.action_edit) {
			updateService(s);
		}
	}
	
	private void compareTopology() {
		if (getTopology() == null)
			return;
		showProgress(true);
		ServerInfo si = getOnlyServerInfo();
		if (si == null) {
			selectServer(R.id.action_compare_topo);
		}
		else {
			Intent i = new Intent(this, RestService.class);
			i.setAction(RestService.ACTION_COMPARE);
			i.putExtra(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(topology).toString());
			i.putExtra(Constants.EXTRA_SERVER_INFO, si.toBundle());
			i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
			startService(i);
		}
	}
	
	private void showConnMgr() {
		Intent i = new Intent(this, ConnMgrActivity.class);
		startActivityForResult(i, R.id.action_conn_mgr);
	}

	private ServerInfo getOnlyServerInfo() {
		ServerInfo rv = null;
		String where = ConnMgrColumns.STATUS + " = ?";
		String[] whereArgs = new String[] { Integer.toString(Constants.STATUS_ACTIVE) };
		Cursor c = getContentResolver().query(ConnMgrColumns.CONTENT_URI, null, where, whereArgs, null);
		if (c.getCount() == 1) {
			if (c.moveToFirst()) {
				rv = ServerInfo.from(c);
			}
		}
		c.close();
		if (c == null || c.getCount() == 0)
			alertDialog("Please add or enable a connection via the Connection Manager");
		return rv;
	}

	@Override
	public void loadTopology() throws ApiException {
		if (file != null) {
			loadFromFile(file);
			return;
		}
		srvrInfo = getOnlyServerInfo();
		if (srvrInfo == null)
			selectServer(R.id.action_save_to_anm);
		else
			loadFromServer();
	}

	private void loadFromServer() {
		if (srvrInfo == null)
			return;
		file = null;
		topology = null;
//		showProgress(true);
		Intent i = new Intent(this, RestService.class);
		i.setAction(HttpMethod.GET.name());
		i.putExtra(Constants.EXTRA_SERVER_INFO, srvrInfo.toBundle());
		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		i.putExtra(Constants.EXTRA_URL, srvrInfo.buildUrl("topology"));
		outstandingIntent = i;
		startService(i);
	}
	
	private void selectServer(int action) {
		String where = ConnMgrColumns.STATUS + " = ?";
		String[] whereArgs = new String[] { Integer.toString(Constants.STATUS_ACTIVE) };
		Cursor c = getContentResolver().query(ConnMgrColumns.CONTENT_URI, null, where, whereArgs, null);
		if (c == null)
			return;
		List<ServerInfo> list = new ArrayList<ServerInfo>();
		while (c.moveToNext()) {
			list.add(ServerInfo.from(c));
		}
		c.close();
		if (list.size() == 0)
			return;
		SelectServerDialog dlg = new SelectServerDialog();
		dlg.setAction(action);
		dlg.setListener(this);
		dlg.setServerInfoList(list);
		dlg.show(getFragmentManager(), "selSrvrDlg");
	}
	
	private void topologyDetails() {
		Topology t = getTopology();
		if (t == null)
			return;
		StringBuilder sb = new StringBuilder();
		sb.append("\nID: ").append(t.getId());
		sb.append("\nproductVersion: ").append(t.getProductVersion());
		sb.append("\nlast updated: ").append(DomainHelper.getInstance().formatDatetime(t.getTimestamp()));
		sb.append("\nversion: ").append(t.getVersion());
		sb.append("\n");
		infoDialog("Topology Details", sb.toString());
	}
	
	private void delete(Intent i) {
//		Topology t = getTopology();
		if (i == null || topology == null)
			return;
		final String id = i.getStringExtra(Constants.EXTRA_REFERRING_ITEM_ID);
		if (TextUtils.isEmpty(id))
			return;
		String typ = i.getStringExtra(Constants.EXTRA_ITEM_TYPE);
		if (TextUtils.isEmpty(typ))
			return;
		final EntityType eType = EntityType.valueOf(typ);
		String name = null;
		Object obj = null;
		if (eType == EntityType.Host) {
			Host h = topology.getHost(id);
			if (h != null) {
				obj = h;
				name = h.getName(); 
			}
		}
		else if (eType == EntityType.Group) {
			Group g = topology.getGroup(id);
			if (g != null) {
				obj = g;
				name = g.getName();
			}
		}
		else if (eType == EntityType.Gateway) {
			String[] ids = id.split("/");
			if (ids == null || ids.length != 2)
				return;
			String gid = ids[0];
			String sid = ids[1];
			Group g = topology.getGroup(gid);
			if (g == null)
				return;
			Service s = topology.getService(sid);
			if (s != null) {
				obj = s;
				name = s.getName();
			}
		}
		if (obj == null)
			return;
		confirmDelete(name, eType, id);
	}
	
	private void performDelete(EntityType typ, String id, boolean delFromDisk) {
		if (EntityType.Host == typ) {
			Host h = topology.getHost(id);
			if (h != null) {
				removeHost(h);
			}
		}
		else if (EntityType.Group == typ) {
			Group g = topology.getGroup(id);
			if (g != null) {
				removeGroup(g, delFromDisk);
			}
		}
		else if (EntityType.Gateway == typ) {
			String[] ids = id.split("/");
			if (ids.length != 2)
				return;
			Service s = topology.getService(ids[1]);
			Group g = topology.getGroup(ids[0]);
			if (s != null && g != null) {
				removeService(s, delFromDisk);
				if (g.getServices().size() == 0)
					topology.removeGroup(g);
			}
		}
		updateTopologyView();
	}
	
	private void confirmDelete(final String name, final EntityType typ, final String id) {
		ConfirmDeleteDialog dlg = new ConfirmDeleteDialog();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_ITEM_TYPE, typ.name());
		args.putString(Constants.EXTRA_ITEM_ID, id);
		StringBuilder msg = new StringBuilder("Touch OK to delete ");
		msg.append(typ.name()).append(" '").append(name).append("'");
		args.putString(Intent.EXTRA_TEXT, msg.toString());
		dlg.setArguments(args);
		dlg.setListener(this);
		dlg.show(getFragmentManager(), "delDlg");
	}
	
	private void updateTopologyView() {
		topoListFrag = new TopologyListFragment();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(topology).toString());
		args.putString(Constants.EXTRA_TOPO_SOURCE, getTopologySource());
		args.putBoolean(Constants.EXTRA_HAVE_CONSOLE, isConsoleAvailable());
		topoListFrag.setArguments(args);
		getFragmentManager().beginTransaction().replace(R.id.container01, topoListFrag, "topoFrag").commit();
		
//		topoListFrag.update(getTopology(), getTopologySource(), isConsoleAvailable());
	}
	
	private String getTopologySource()  {
		if (srvrInfo != null)
			return srvrInfo.displayString();
		else if (file != null)
			return file.getName();
		return null;
	}

	private void confirmRemoveTrust() {
		confirmDialog("Touch OK to remove trusted certificate store.",new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				removeTrustStore();
			}
		});
	}

	private void removeTrustStore() {
		Intent i = new Intent(this, RestService.class);
		i.setAction(RestService.ACTION_REMOVE_TRUST_STORE);
		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		outstandingIntent = i;
		startService(i);		
	}
	
	@Override
	public void onTopologyItemSelected(EntityType itemType, String id) {
		if (itemType == EntityType.Host) {
			showHostDialog(topology.getHost(id));
		}
		else if (itemType == EntityType.Group) {
			showGroupDialog(topology.getGroup(id));
		}
		else if (itemType == EntityType.NodeManager) {
			topologyDetails();
		}
		else if (itemType == EntityType.Gateway) {
			String[] ids = id.split("/");
			if (ids == null || ids.length != 2)
				return;
			String gid = ids[0];
			String sid = ids[1];
			selGrp = topology.getGroup(gid);
			if (selGrp == null)
				return;
			Service s = selGrp.getService(sid);
			showGatewayDialog(s, null);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == R.id.action_console) {
			if (data != null)
				consoleHandle = data.getStringExtra("jackpal.androidterm.window_handle");
		}
		else
			super.onActivityResult(requestCode, resultCode, data);
	}

	private void networkError(String msg, String title) {
		AlertDialogFragment dlg = new AlertDialogFragment();
		Bundle args = new Bundle();
		
		if (TextUtils.isEmpty(title))
			title = "Connection Error";
		args.putString(Intent.EXTRA_TITLE, title);
		args.putString(Intent.EXTRA_TEXT, msg);
		dlg.setOnNegative(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		}, R.string.action_exit);
		dlg.setOnNeutral(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showConnMgr();
			}
		}, R.string.action_conn_mgr);
		dlg.setOnPositive(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showSelectFileDialog(R.id.action_load_from_disk);
			}
		}, R.string.action_work_local);
		dlg.setArguments(args);
		dlg.show(getFragmentManager(), TAG_ALERT_DLG);
	}

	protected void showSelectFileDialog(final int action) {
		File dir = getFilesDir();
		File[] files = null;
		ArrayList<String> jsonFiles = new ArrayList<String>();
		if (dir != null) {
			files = dir.listFiles();
			for (File f: files) {
				if (f.getName().endsWith(Constants.TOPOLOGY_FILE_EXT))
					jsonFiles.add(f.getName());
				else if (f.getName().endsWith(".json"))
					f.delete();
			}
		}
		if (jsonFiles.size() == 0) {
			if (action == R.id.action_load_from_disk) {
				UiUtils.showToast(this, "No saved topology files");
				return;
			}
		}
		SelectFileDialog dlg = new SelectFileDialog();
		Bundle args = new Bundle();
		args.putStringArrayList(Constants.EXTRA_JSON_ITEM, jsonFiles);
		args.putInt(Constants.EXTRA_ACTION, action);
		dlg.setArguments(args);
		dlg.setCancelable(false);
		dlg.setListener(new SelectFileDialog.Listener() {
			
			@Override
			public void onFileSelected(String fname) {
				if (action == R.id.action_load_from_disk)
					loadFromDisk(fname);
				else if (action == R.id.action_save_to_disk)
					saveToDisk(fname);
			}
		});
		dlg.show(getFragmentManager(), "selFileDlg");
	}
	
	protected void loadFromDisk(String fname) {
		if (TextUtils.isEmpty(fname))
			return;
		File f = new File(getFilesDir(), fname);
		if (f.exists()) {
//			showProgress(true);
			loadFromFile(f);
		}
	}

	private void loadFromFile(File f) {
		if (f == null)
			return;
		if (!f.exists()) {
			UiUtils.showToast(this, "File not found");
			return;
		}
		srvrInfo = null;
		file = f;
		topology = helper.loadFromFile(f);
		showProgress(false);
		updateTopologyView();
	}

	private void saveToFile(File f) {
		if (f == null || topology == null)
			return;
		helper.saveToFile(f, topology);
		if (!Constants.SAMPLE_FILE.equals(f.getName()))
			UiUtils.showToast(this, "Saved to file: " + f.getName());
	}
	
	protected void saveToDisk(String fname) {
		if (TextUtils.isEmpty(fname))
			return;
		if (!fname.endsWith(Constants.TOPOLOGY_FILE_EXT))
			fname = fname + Constants.TOPOLOGY_FILE_EXT;
		final File f = new File(getFilesDir(), fname);
		if (f.exists()) {
			confirmDialog(f.getName() + " exists. Overwrite?", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					saveToFile(f);
				}
			});
		}
		else
			saveToFile(f);
	}
	
	private boolean isDirty() {
		return false;
	}

	private void onExitConfirmed() {
		finish();
	}
	
	@Override
	public void onBackPressed() {
		if (isDirty()) {
			confirmDialog("Touch OK to discard unsaved changes.", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onExitConfirmed();
				}
			});
		}
		else if (outstandingIntent != null) {		
			confirmDialog("There is currently a background task running which will be killed if you exit. Touch OK to exit anyway.", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onExitConfirmed();
				}
			});
		}
		else
			super.onBackPressed();
	}

	@Override
	public void onServerSelected(ServerInfo info, int action) {
		if (info == null)
			return;
		if (action == R.id.action_load_from_anm) {
//			showProgress(true);
			srvrInfo = info;
			loadFromServer();
		}
		else if (action == R.id.action_compare_topo) {
//			showProgress(true);
//			service.compareTopology(info, getTopology());
		}
	}
	
	private Intent createModifyIntent(HttpMethod method, EntityType etype, JsonObject json) {		
		Intent rv = new Intent(this, RestService.class);
		rv.setAction(method.name());
		rv.putExtra(Constants.EXTRA_SERVER_INFO, srvrInfo.toBundle());
		rv.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		rv.putExtra(Constants.EXTRA_ITEM_TYPE, etype.name());
		rv.putExtra(Constants.EXTRA_JSON_ITEM, json.toString());
		return rv;
	}
	
	@Override
	public void addHost(Host h, int mgmtPort, boolean useSsl) throws ApiException {
		if (topology == null || h == null)
			return;
		Group g = topology.getGroupForService(topology.adminNodeManager().getId());
		topology.addHost(h);
		updateTopologyView();
		Intent i = createModifyIntent(HttpMethod.POST, EntityType.Host, helper.toJson(h));
		hostNodeMgrIntent = new Intent(this, RestService.class);
		hostNodeMgrIntent.setAction(HttpMethod.POST.name());
		hostNodeMgrIntent.putExtra(Constants.EXTRA_SERVER_INFO, srvrInfo.toBundle());
		hostNodeMgrIntent.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		hostNodeMgrIntent.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.NodeManager.name());
		hostNodeMgrIntent.putExtra(Constants.EXTRA_USE_SSL, useSsl);
		hostNodeMgrIntent.putExtra(Constants.EXTRA_MGMT_PORT, mgmtPort);
		hostNodeMgrIntent.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, g.getId());
		asyncModify(i);
	}

	@Override
	public void addGroup(Group g) throws ApiException {
		if (topology == null || g == null)
			return;
		topology.addGroup(g);
		updateTopologyView();
		asyncModify(createModifyIntent(HttpMethod.POST, EntityType.Group, helper.toJson(g)));
	}

	@Override
	public void addService(Service s, int svcsPort) throws ApiException {
		if (topology == null || selGrp == null || s == null)
			return;
		Group tg = topology.getGroup(selGrp.getId());
		if (tg == null)
			throw new ApiException("expecting to find group for service: " + s.getId());
		tg.addService(s);
		updateTopologyView();
		Intent i = createModifyIntent(HttpMethod.POST, EntityType.Gateway, helper.toJson(s));
		i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, tg.getId());
		i.putExtra(Constants.EXTRA_SERVICES_PORT, svcsPort);
		asyncModify(i);
	}

	@Override
	public void updateHost(Host h) throws ApiException {
		if (topology == null || h == null)
			return;
		Host th = topology.getHost(h.getId());
		if (th != null) {
			th.setName(h.getName());
			updateTopologyView();
			asyncModify(createModifyIntent(HttpMethod.PUT, EntityType.Host, helper.toJson(h)));
		}
	}

	@Override
	public void updateGroup(Group g) throws ApiException {
		if (topology == null || g == null)
			return;
		Group tg = topology.getGroup(g.getId());
		if (tg != null) {
			tg.setName(g.getName());
			tg.setTags(g.getTags());
			updateTopologyView();
			asyncModify(createModifyIntent(HttpMethod.PUT, EntityType.Group, helper.toJson(g)));
		}
	}

	@Override
	public void updateService(Service s) throws ApiException {
		if (topology == null || s == null)
			return;
		Group g = topology.getGroupForService(s.getId());
		if (g == null)
			throw new ApiException("expecting to find group for service: " + s.getId());
		Service ts = topology.getService(s.getId());
		if (ts != null) {
			ts.setName(s.getName());
			ts.setHostID(s.getHostID());
			ts.setManagementPort(s.getManagementPort());
			ts.setScheme(s.getScheme());
			ts.setTags(s.getTags());
			ts.setEnabled(s.getEnabled());
			updateTopologyView();
			Intent i = createModifyIntent(HttpMethod.PUT, EntityType.Gateway, helper.toJson(s));
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_TYPE, EntityType.Group.name());
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, g.getId());
			asyncModify(i);
		}
	}

	@Override
	public void removeHost(Host h) throws ApiException {
		if (topology == null || h == null)
			return;
		Host th = topology.getHost(h.getId());
		if (th == null)
			return;
		Collection<Service> svcs = topology.getServicesOnHost(th.getId(), ServiceType.nodemanager);
		boolean isAnm = false;
		Service nmSvc = null;
		for (Service s: svcs) {
			if (Topology.isAdminNodeManager(s))
				isAnm = true;
			else
				nmSvc = s;
		}
		if (isAnm) {
			UiUtils.showToast(this, "Cannot delete host for Admin Node Manager");
			return;
		}
		if (nmSvc == null) {
			Log.e(TAG, "no nodemanger service found for host: " + th.getName());
			asyncModify(createModifyIntent(HttpMethod.DELETE, EntityType.Host, helper.toJson(th)));
		}
		else {
			Group g = topology.getGroupForService(nmSvc.getId());
			hostNodeMgrIntent = new Intent(this, RestService.class);
			hostNodeMgrIntent.setAction(HttpMethod.DELETE.name());
			hostNodeMgrIntent.putExtra(Constants.EXTRA_SERVER_INFO, srvrInfo.toBundle());
			hostNodeMgrIntent.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
			hostNodeMgrIntent.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.NodeManager.name());
			hostNodeMgrIntent.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, g.getId());
			hostNodeMgrIntent.putExtra(Constants.EXTRA_ITEM_ID, nmSvc.getId());
			hostNodeMgrIntent.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson(nmSvc).toString());
			hostNodeMgrIntent.putExtra(Constants.EXTRA_HOST_ID, th.getId());
			UiUtils.showToast(this, "Removing Node Manager service from host " + th.getName());
			asyncModify(hostNodeMgrIntent);
		}
	}

	@Override
	public void removeGroup(Group g, boolean delFromDisk) throws ApiException {
		if (topology == null || g == null)
			return;
		if (topology.getGroup(g.getId()) != null) {
			topology.removeGroup(g);
			updateTopologyView();
			Intent i = createModifyIntent(HttpMethod.DELETE, EntityType.Group, helper.toJson(g));
			i.putExtra(Constants.EXTRA_DELETE_FROM_DISK, delFromDisk);
			asyncModify(i);
		}
	}

	@Override
	public void removeService(Service s, boolean delFromDisk) throws ApiException {
		if (topology == null || s == null)
			return;
		Group g = topology.getGroupForService(s.getId());
		if (g == null)
			throw new ApiException("expecting to find group for service: " + s.getId());
		g.removeService(s.getId());
		updateTopologyView();
		Intent i = createModifyIntent(HttpMethod.DELETE, EntityType.Gateway, helper.toJson(s));
		i.putExtra(Constants.EXTRA_DELETE_FROM_DISK, delFromDisk);
		i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, g.getId());
		asyncModify(i);
	}

	@Override
	public void moveService(Service svc, Group fromGrp, Group toGrp) throws ApiException {
		if (topology == null || svc == null || fromGrp == null || toGrp == null)
			return;
		Service s = fromGrp.getService(svc.getId());
		if (s == null) {
			UiUtils.showToast(this, svc.getName() + " is not in group " + fromGrp.getName());
			return;
		}
		s = toGrp.getService(svc.getId());
		if (s != null) {
			UiUtils.showToast(this, svc.getName() + " is already in group " + fromGrp.getName());
			return;
		}
		fromGrp.removeService(svc.getId());
		toGrp.addService(svc);
		UiUtils.showToast(this, "Moving " + svc.getName() + " from " + fromGrp.getName() + " to " + toGrp.getName());		
		updateTopologyView();
		Intent i = createModifyIntent(HttpMethod.DELETE, EntityType.Gateway, helper.toJson(svc));
		i.setAction(RestService.ACTION_MOVE_GATEWAY);
		i.putExtra(Constants.EXTRA_FROM_GROUP, fromGrp.getId());
		i.putExtra(Constants.EXTRA_TO_GROUP, toGrp.getId());
		asyncModify(i);
	}
	
	@Override
	public Topology getTopology() {
		return topology;
	}

	private void onCertNotTrusted(final String msg) {	//final CertPath cp) {
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, getString(R.string.cert_not_trusted));
		StringBuilder sb = new StringBuilder();
	    sb.append(msg).append("\n").append(getString(R.string.add_to_truststore));
		args.putString(Intent.EXTRA_TEXT,  sb.toString());
		AlertDialogFragment dlgFrag = new AlertDialogFragment();
		dlgFrag.setOnPositive(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {				
				addCertsToTrustStore();
			}
		});
		dlgFrag.setOnNegative(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		}, R.string.action_exit);
		dlgFrag.setArguments(args);
		FragmentManager fm = getFragmentManager();
		dlgFrag.show(fm.beginTransaction(), "yesnoDlg");
	}

	private void addCertsToTrustStore() {
		Intent i = new Intent(this, RestService.class);
		i.setAction(RestService.ACTION_CHECK_CERT);
		i.putExtra(Constants.EXTRA_SERVER_INFO, srvrInfo.toBundle());
		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		i.putExtra(Constants.EXTRA_URL, srvrInfo.buildUrl("topology"));
		i.putExtra(Constants.EXTRA_TRUST_CERT, true);
		outstandingIntent = i;
		startService(i);
	}
	
	private void asyncModify(Intent i) {
		if (i == null || i.getExtras() == null)
			return;
		outstandingIntent = i;
		startService(i);
	}

	private boolean isConsoleAvailable() {
		if (haveConsole == null) {
			haveConsole = Boolean.valueOf(getPackageManager().getLaunchIntentForPackage("jackpal.androidterm") != null);
		}
		return haveConsole.booleanValue();
	}
	
	private void launchConsole() {
		Intent i = null;
		try {
			i = getPackageManager().getLaunchIntentForPackage("jackpal.androidterm");
			if (i != null) {
				if (consoleHandle != null)
					i.putExtra("jackpal.androidterm.window_handle", consoleHandle);
				startActivityForResult(i, R.id.action_console);
			}
		} 
		catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
	}
	
	private void runScriptInConsole(String script) {
		Intent i = null;
		try {
			i = getPackageManager().getLaunchIntentForPackage("jackpal.androidterm");
			if (i != null) {
				i = new Intent("jackpal.androidterm.RUN_SCRIPT");
				i.addCategory(Intent.CATEGORY_DEFAULT);				
				i.putExtra("jackpal.androidterm.iInitialCommand", script);
				if (consoleHandle != null)
					i.putExtra("jackpal.androidterm.window_handle", consoleHandle);
				startActivityForResult(i, R.id.action_console);
			}
		} 
		catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
	}
	
	private void sshToHost(Host h) {
		if (h == null)
			return;
		if (consoleHandle == null) {
			UiUtils.showToast(this, "Please open a console before SSHing to a host");
			return;
		}
		String cmd = "ssh root@" + h.getName();
		runScriptInConsole(cmd);
	}
	
	private void startGateway(Service s) {
		if (consoleHandle == null) {
			UiUtils.showToast(this, "Please open a console and SSH to a host before starting a gateway");
			return;
		}
		Group g = topology.getGroupForService(s.getId());
		if (g == null)
			return;
		StringBuilder cmd = new StringBuilder("startinstance -n ");
		cmd.append("\"").append(s.getName()).append("\"");
		cmd.append(" -g \"").append(g.getName()).append("\"");
		cmd.append(" -d");
		runScriptInConsole(cmd.toString());
	}
	
	private void moveGateway(Service s) {
		Collection<Group> grps = topology.getGroups();
		Group fromGrp = topology.getGroupForService(s.getId());
		Group nmGrp = topology.getGroupForService(topology.adminNodeManager().getId());
		Group toGrp = null;
		if (grps.size() == 3) {
			for (Group g: grps) {
				if (!g.getId().equals(fromGrp.getId()) && !g.getId().equals(nmGrp.getId())) {
					toGrp = g;
					break;
				}
			}
			if (fromGrp == null || toGrp == null)
				return;
			moveService(s, fromGrp, toGrp);
		}
		else {
			//showSelectGroupDialog();
		}
	}

	@Override
	public void onDeleteConfirmed(Bundle data) {
		if (data == null)
			return;
		String id = data.getString(Constants.EXTRA_ITEM_ID);
		EntityType typ = EntityType.valueOf(data.getString(Constants.EXTRA_ITEM_TYPE));
		boolean delFromDisk = data.getBoolean(Constants.EXTRA_DELETE_FROM_DISK, false);
		performDelete(typ, id, delFromDisk);
	}
}
