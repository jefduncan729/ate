package com.axway.ate.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpStatus;
import org.springframework.http.HttpMethod;

import android.app.Activity;
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
import com.axway.ate.fragment.EditHostDialog;
import com.axway.ate.fragment.EditHostDialog.EditHostListener;
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

public class TopologyActivity extends BaseActivity implements TopologyClient, TopologyListFragment.Listener, SelectServerDialog.Listener, EditHostListener {
	
	private static final String TAG = TopologyActivity.class.getSimpleName();
	
	private View ctr01;
	private ProgressBar prog01;
	
	private TopologyListFragment topoListFrag;
	
	private ServerInfo srvrInfo;
	private File file;
	private DomainHelper helper;
	private Topology topology;
	private Intent outstandingIntent;
	private Boolean consoleAvailable;
	private String consoleHandle;
	
	private ResultReceiver resRcvr;
	
	private Host selHost;
	private Group selGrp;
	
	public TopologyActivity() {
		super();
		topoListFrag = null;
		srvrInfo = null;
		file = null;
		helper = DomainHelper.getInstance();
		selGrp = null;
		selHost = null;
		resRcvr = null;
		outstandingIntent = null;
		consoleAvailable = null;
		consoleHandle = null;
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
		UiUtils.showToast(this, "Update successful");		
		if (HttpMethod.POST.name().equals(action)) {
			EntityType typ = EntityType.valueOf(data.getString(Constants.EXTRA_ITEM_TYPE));
			String jstr = data.getString(Constants.EXTRA_JSON_ITEM);
			if (EntityType.Host == typ) {
				Host h = helper.hostFromJson(helper.parse(jstr).getAsJsonObject());
				Host th = topology.getHostByName(h.getName());
				if (th != null) {
					th.setId(h.getId());
					updateTopologyView();
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
	}

	private void showProgress(boolean show) {
		if (show) {
			prog01.setIndeterminate(true);
			prog01.setVisibility(View.VISIBLE);
			ctr01.setVisibility(View.GONE);
		}
		else {
			prog01.setVisibility(View.GONE);
			ctr01.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.empty_frag);
		setTitle(getString(R.string.topology));
		ctr01 = (View)findViewById(R.id.container01);
		prog01 = (ProgressBar)findViewById(android.R.id.progress);
		showProgress(false);
		if (savedInstanceState != null) {
			topoListFrag = (TopologyListFragment)getFragmentManager().findFragmentByTag("topoFrag");
			String s = savedInstanceState.getString(Constants.EXTRA_JSON_TOPOLOGY);
			if (s != null)
				topology = helper.topologyFromJson(helper.parse(s).getAsJsonObject());
			consoleHandle = savedInstanceState.getString(Constants.EXTRA_CONSOLE_HANDLE);
			Bundle b = savedInstanceState.getBundle(Constants.EXTRA_SERVER_INFO);
			if (b != null)
				srvrInfo = ServerInfo.fromBundle(b);
		}
		if (topoListFrag == null) {
			topoListFrag = new TopologyListFragment();
			getFragmentManager().beginTransaction().replace(R.id.container01, topoListFrag, "topoFrag").commit();
		}
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
		boolean haveTopo = (topology != null);
		MenuItem i = menu.findItem(R.id.action_save_to_anm);
		if (i != null)
			i.setVisible(haveTopo);
		i = menu.findItem(R.id.action_save_to_disk);
		if (i != null)
			i.setVisible(haveTopo);
		i = menu.findItem(R.id.action_topo_details);
		if (i != null)
			i.setVisible(haveTopo);
		i = menu.findItem(R.id.action_compare_topo);
		if (i != null)
			i.setVisible(haveTopo);
		i = menu.findItem(R.id.action_add_host);
		if (i != null)
			i.setVisible(haveTopo);
//		if (isConsoleAvailable()) {
//			//cool, no action needed
//		}
//		else {
//			i = menu.findItem(R.id.action_ssh_to_host);
//			if (i != null)
//				i.setVisible(false);
//			i = menu.findItem(R.id.action_ssh_to_host);
//			if (i != null)
//				i.setVisible(false);
//		}
	}

	@Override
	public boolean onMenuItemSelected(MenuItem menuItem) {
		boolean rv = true;
		Intent i = null;
		switch (menuItem.getItemId()) {
			case R.id.action_settings:
				showSettings();
			break;
			case R.id.action_remove_trust:
				confirmRemoveTrust();
			break;
			case R.id.action_add_host:
//				i = new Intent();
//				i.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Host.name());
//				add(i);
				showHostDialog(null);
			break;
			case R.id.action_add_group:
				i = new Intent();
				i.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Group.name());
				add(i);
			break;
			case R.id.action_add_gateway:
				add(menuItem.getIntent());
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
				runScriptInConsole("echo 'Welcome to the APIGateway Console!'");
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
			default:
				rv = false;
		}
		return rv;
	}

	private void showHostDialog(Host h) {
		EditHostDialog dlg = new EditHostDialog();
		Bundle args = new Bundle();
		if (h == null)
			args.putInt(Constants.EXTRA_ACTION, R.id.action_add);
		else {
			args.putInt(Constants.EXTRA_ACTION, R.id.action_edit);
			args.putString(Intent.EXTRA_UID, h.getId());
			args.putString(Intent.EXTRA_TEXT, h.getName());
		}
		dlg.setOnChangeListener(this);
		dlg.setArguments(args);
		dlg.show(getFragmentManager(), "hostDlg");
	}

	@Override
	public void onHostChanged(Bundle b) {
		if (b == null)
			return;
		Host h = new Host();
		h.setName(b.getString(Intent.EXTRA_TEXT));
		switch (b.getInt(Constants.EXTRA_ACTION, 0)) {
			case  R.id.action_add:
				addHost(h, b.getBoolean(Constants.EXTRA_USE_SSL, false));
			break;
			case  R.id.action_edit:
				h.setId(b.getString(Intent.EXTRA_UID));
				updateHost(h);
			break;
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
//		else
//			service.compareTopology(si, getTopology());
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
		showProgress(true);
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
		Topology t = getTopology();
		if (i == null || t == null)
			return;
		final String id = i.getStringExtra(Constants.EXTRA_REFERRING_ITEM_ID);
		if (TextUtils.isEmpty(id))
			return;
		String typ = i.getStringExtra(Constants.EXTRA_ITEM_TYPE);
		if (TextUtils.isEmpty(typ))
			return;
		final EntityType eType = EntityType.valueOf(typ);
		boolean delFromDisk = false;
		String name = null;
		Object obj = null;
		if (eType == EntityType.Host) {
			Host h = t.getHost(id);
			if (h != null) {
				obj = h;
				name = h.getName(); 
			}
		}
		else if (eType == EntityType.Group) {
			Group g = t.getGroup(id);
			if (g != null) {
				obj = g;
				name = g.getName();
			}
			delFromDisk = i.getBooleanExtra(Constants.EXTRA_DELETE_FROM_DISK, false);
		}
		else if (eType == EntityType.Gateway) {
			String[] ids = id.split("/");
			if (ids == null || ids.length != 2)
				return;
			String gid = ids[0];
			String sid = ids[1];
			Group g = t.getGroup(gid);
			if (g == null)
				return;
			Service s = t.getService(sid);
			if (s != null) {
				obj = s;
				name = s.getName();
			}
			delFromDisk = i.getBooleanExtra(Constants.EXTRA_DELETE_FROM_DISK, false);
		}
		if (obj == null)
			return;
		confirmDelete(name, eType, id, delFromDisk);
	}

	private void performDelete(EntityType typ, String id) {
		if (EntityType.Host == typ) {
			Host h = topology.getHost(id);
			if (h != null) {
				topology.removeHost(h);
				asyncModify(createModifyIntent(HttpMethod.DELETE, typ, helper.toJson(h)));
			}
		}
		else if (EntityType.Group == typ) {
			Group g = topology.getGroup(id);
			if (g != null) {
				topology.removeGroup(g);
				asyncModify(createModifyIntent(HttpMethod.DELETE, typ, helper.toJson(g)));
			}
		}
		else if (EntityType.Gateway == typ) {
			String[] ids = id.split("/");
			if (ids.length != 2)
				return;
			Service s = topology.getService(ids[1]);
			Group g = topology.getGroup(ids[0]);
			if (s != null && g != null) {
				g.removeService(s.getId());
				Intent i = createModifyIntent(HttpMethod.DELETE, typ, helper.toJson(s));
				i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, g.getId());
				asyncModify(i);
			}
		}
		updateTopologyView();
	}
	
	private void confirmDelete(final String name, final EntityType typ, final String id, final boolean delFromDisk) {
		StringBuilder msg = new StringBuilder("Touch OK to delete ");
		msg.append(typ.name()).append(" '").append(name).append("'");
		if (delFromDisk)
			msg.append("\nand also delete the disk files associated with this ").append(typ.name());
		confirmDialog(msg.toString(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				performDelete(typ, id);
//				getHandler().sendEmptyMessageDelayed(MSG_UPDATE_TOPOLOGY, 50);
			}
		});
	}
	
	private void add(Intent i) {
		if (i == null)
			return;
		Intent ai = new Intent(this, EditActivity.class);
		String typ = i.getStringExtra(Constants.EXTRA_ITEM_TYPE);
		EntityType eType = EntityType.valueOf(typ);
		String jsonStr = null;
		switch (eType) {
			case Gateway:
				Service s = new Service();
				jsonStr = helper.toJson(s).toString();
				String ref = i.getStringExtra(Constants.EXTRA_REFERRING_ITEM_TYPE);
				String refId = i.getStringExtra(Constants.EXTRA_REFERRING_ITEM_ID);
				if (!TextUtils.isEmpty(refId)) {
					EntityType refType = EntityType.valueOf(ref);
					if (refType == EntityType.Host)
						selHost = topology.getHost(refId);
					if (refType == EntityType.Group)
						selGrp = topology.getGroup(refId);
				}
			break;
			case Group:
				Group g = new Group();
				jsonStr = helper.toJson(g).toString();
			break;
			case Host:
				Host h = new Host();
				jsonStr = helper.toJson(h).toString();
			break;
			case NodeManager:
				//node managers are not added via UI
				return;
		}
		
		if (ai != null) {
			ai.putExtra(Constants.EXTRA_ACTION, R.id.action_add);
			ai.putExtra(Constants.EXTRA_JSON_ITEM, jsonStr);
			ai.putExtra(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(topology).toString());
			ai.putExtras(i.getExtras());
			startActivityForResult(ai, R.id.action_add);
		}
	}
	
	private void updateTopologyView() {
		if (topoListFrag == null)
			return;
		topoListFrag.update(getTopology(), getTopologySource());
	}
	
	private String getTopologySource()  {
		if (srvrInfo != null)
			return srvrInfo.displayString();
		else if (file != null)
			return file.getName();
		return null;
	}
	
	private void edit(Object o) {
		if (o == null)
			return;
		Intent i = new Intent(this, EditActivity.class);
		if (o instanceof Host) {
			showHostDialog((Host)o);
			return;
//			i.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson((Host)o).toString());
//			i.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Host.name());
		}
		else if (o instanceof Group) {
			i.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson((Group)o).toString());
			i.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Group.name());
		}
		else if (o instanceof Service) {
			i.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson((Service)o).toString());			
			i.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Gateway.name());
		}
		i.putExtra(Constants.EXTRA_ACTION, R.id.action_edit);
		i.putExtra(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(topology).toString());
		startActivityForResult(i, R.id.action_edit);
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
		if (getTopology() == null)
			return;
		if (itemType == EntityType.Host) {
			edit(getTopology().getHost(id));
		}
		else if (itemType == EntityType.Group) {
			edit(getTopology().getGroup(id));
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
			Group g = getTopology().getGroup(gid);
			if (g == null)
				return;
			Service s = g.getService(sid);
			edit(s);
		}
	}

	private void modifyService(Intent data) {
		int action = data.getIntExtra(Constants.EXTRA_ACTION, 0);
		if (action == 0)
			return;
		String ref = data.getStringExtra(Constants.EXTRA_REFERRING_ITEM_TYPE);
		String refId = data.getStringExtra(Constants.EXTRA_REFERRING_ITEM_ID);
		if (action == R.id.action_edit) {
			String sid = data.getStringExtra(Constants.EXTRA_ITEM_ID);
			Service s = topology.getService(sid);
			edit(s);
		}
		else {
			Intent i = new Intent();
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_TYPE, ref);
			i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, refId);
			i.putExtra(Constants.EXTRA_ITEM_TYPE, EntityType.Gateway.name());
			add(i);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == R.id.action_console) {
			if (data != null)
				consoleHandle = data.getStringExtra("jackpal.androidterm.window_handle");
		}
		else if (requestCode == R.id.action_add || requestCode == R.id.action_edit) {
			if (resultCode == Activity.RESULT_OK) {
				if (data == null)
					throw new IllegalStateException("edit activity did not return any data");
				if (!TextUtils.isEmpty(data.getStringExtra(Constants.EXTRA_REFERRING_ITEM_ID))) {
					//this is request to add/update a Service
					modifyService(data);
					return;
				}
				String jsonStr = data.getStringExtra(Constants.EXTRA_JSON_ITEM);
				JsonElement je = helper.parse(jsonStr);
				if (je == null)
					return;
				boolean add = (requestCode == R.id.action_add);
				EntityType et = EntityType.valueOf(data.getStringExtra(Constants.EXTRA_ITEM_TYPE));
				switch (et) {
					case Host:
//						Host h = helper.hostFromJson(je.getAsJsonObject());
//						if (add)
//							addHost(h);
//						else
//							updateHost(h);
					break;
					case Group:
						Group g = (Group)helper.groupFromJson(je.getAsJsonObject());
						if (add)
							addGroup(g);
						else
							updateGroup(g);
					break;
					case Gateway:
						Service s = (Service)helper.serviceFromJson(je.getAsJsonObject());
						if (add) {
							int svcsPort = data.getIntExtra(Constants.EXTRA_SERVICES_PORT, -1);
							if (svcsPort == -1)
								throw new IllegalStateException("must provide services port for new gateway");
							addService(s, svcsPort);
						}
						else
							updateService(s);
					break;
					case NodeManager:
					break;
				}
			}
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
			showProgress(true);
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
			showProgress(true);
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
	public void addHost(Host h, boolean useSsl) throws ApiException {
		if (topology == null || h == null)
			return;
		Group g = topology.getGroupForService(topology.adminNodeManager().getId());
		topology.addHost(h);
		updateTopologyView();
		Intent i = createModifyIntent(HttpMethod.POST, EntityType.Host, helper.toJson(h));
		i.putExtra(Constants.EXTRA_USE_SSL, useSsl);
		i.putExtra(Constants.EXTRA_NODE_MGR_GROUP, helper.toJson(g).toString());
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
			throw new IllegalStateException("expecting to find group for service: " + s.getId());
		tg.addService(s);
		updateTopologyView();
		Intent i = createModifyIntent(HttpMethod.POST, EntityType.Gateway, helper.toJson(s));
		i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, tg.getId());
		i.putExtra(Constants.EXTRA_SERVICES_PORT, svcsPort);
		asyncModify(i);	//HttpMethod.POST, EntityType.Gateway, helper.toJson(s), tg);
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
			throw new IllegalStateException("expecting to find group for service: " + s.getId());
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
			asyncModify(i);	//HttpMethod.PUT, EntityType.Gateway, helper.toJson(s), g);
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
		for (Service s: svcs) {
			if (Topology.isAdminNodeManager(s)) {
				isAnm = true;
				break;
			}
		}
		if (isAnm) {
			UiUtils.showToast(this, "Cannot delete host for Admin Node Manager");
			return;
		}
		topology.removeHost(th);
		updateTopologyView();
		asyncModify(createModifyIntent(HttpMethod.DELETE, EntityType.Host, helper.toJson(th)));
	}

	@Override
	public void removeGroup(Group g) throws ApiException {
		if (topology == null || g == null)
			return;
		if (topology.getGroup(g.getId()) != null) {
			topology.removeGroup(g);
			updateTopologyView();
			asyncModify(createModifyIntent(HttpMethod.DELETE, EntityType.Group, helper.toJson(g)));
		}
	}

	@Override
	public void removeService(Service s) throws ApiException {
		if (topology == null || s == null)
			return;
		Group g = topology.getGroupForService(s.getId());
		if (g == null)
			throw new IllegalStateException("expecting to find group for service: " + s.getId());
		g.removeService(s.getId());
		updateTopologyView();
		Intent i = createModifyIntent(HttpMethod.DELETE, EntityType.Gateway, helper.toJson(s));
		i.putExtra(Constants.EXTRA_REFERRING_ITEM_TYPE, EntityType.Group.name());
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
		updateGroup(fromGrp);
		updateGroup(toGrp);
	}
	
	@Override
	public Topology getTopology() {
		return topology;
	}

	private void onCertNotTrusted(final String msg) {	//final CertPath cp) {
		Bundle args = new Bundle();
		args.putString(Intent.EXTRA_TITLE, getString(R.string.cert_not_trusted));
		StringBuilder sb = new StringBuilder();
//		int i = 1;
//	    for (Certificate c: cp.getCertificates()) {
//	    	if (c.getType() == "X.509") {
//	    		X509Certificate c509 = (X509Certificate)c;
//	    		sb.append("[").append(i++).append("]: ").append(c509.getSubjectDN().toString()).append("\n");
//	    	}
//	    }
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
		if (consoleAvailable == null) {
			consoleAvailable = new Boolean(getPackageManager().getLaunchIntentForPackage("jackpal.androidterm") != null);
		}
		return consoleAvailable.booleanValue();
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
		if (!isConsoleAvailable()) {
			UiUtils.showToast(this, "No console available");
			return;
		}
		if (h == null)
			return;
		String cmd = "ssh root@" + h.getName();
		runScriptInConsole(cmd);
	}
	
	private void startGateway(Service s) {
		if (!isConsoleAvailable()) {
			UiUtils.showToast(this, "No console available");
			return;
		}
		if (consoleHandle == null) {
			UiUtils.showToast(this, "Please SSH to a host before starting a gateway");
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
}
