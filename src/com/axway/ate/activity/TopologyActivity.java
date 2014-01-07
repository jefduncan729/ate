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
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.axway.ate.fragment.SshUserDialog;
import com.axway.ate.fragment.SshUserDialog.SshUserListener;
import com.axway.ate.fragment.TopologyFileFragment;
import com.axway.ate.fragment.TopologyListFragment;
import com.axway.ate.fragment.TopologyLoaderFragment;
import com.axway.ate.service.BaseIntentService;
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

abstract public class TopologyActivity extends BaseActivity 
	implements TopologyClient, 
		TopologyListFragment.Listener, 
		HostListener, 
		GroupListener, 
		GatewayListener, 
		DeleteListener, SshUserListener {
	
	private static final String TAG = TopologyActivity.class.getSimpleName();
	protected static final String TAG_HOST_DLG = "hostDlg";
	protected static final String TAG_GROUP_DLG = "grpDlg";
	protected static final String TAG_GATEWAY_DLG = "gatewayDlg";
	protected static final String TAG_DEL_DLG = "delDlg";
	protected static final String TAG_SEL_FILE_DLG = "selFileDlg";	
	protected static final String TAG_SSHUSER_DLG = "sshUserDlg";
	
	protected static final String TAG_TOPOLOGY_FRAG = "topoFrag";
	
	protected DomainHelper helper;
	protected Topology topology;
	
	protected Group selGrp;
	
	public TopologyActivity() {
		super();
		helper = DomainHelper.getInstance();
		selGrp = null;
	}

	@Override
	public void onTopologyLoaded(Topology t) {
		topology = t;
		if (t == null)
			;	//networkError("Have you trusted your AdminNodeManager's certificate via the Connection Manager?", "Error");
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.empty_frag);
		setTitle(getString(R.string.topology));
		if (savedInstanceState != null) {
			String s = savedInstanceState.getString(Constants.EXTRA_JSON_TOPOLOGY);
			if (s != null)
				topology = helper.topologyFromJson(helper.parse(s).getAsJsonObject());
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (topology != null)
			outState.putString(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(topology).toString());
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadTopology();
	}

	abstract protected void loadTopology();
	
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
		dlg.show(getFragmentManager(), TAG_HOST_DLG);
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
		dlg.show(getFragmentManager(), TAG_GROUP_DLG);
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
		dlg.show(getFragmentManager(), TAG_GATEWAY_DLG);
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
//		if (getTopology() == null)
//			return;
//		ServerInfo si = getOnlyServerInfo();
//		if (si == null) {
//			selectServer(R.id.action_compare_topo);
//		}
//		else {
//			performCompare(si);
//		}
	}

	protected void performCompare(ServerInfo si)  {
//		if (si == null)
//			return;
//		showProgressDialog("Comparing...");
//		Intent i = new Intent(this, RestService.class);
//		i.setAction(RestService.ACTION_COMPARE);
//		i.putExtra(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(topology).toString());
//		i.putExtra(Constants.EXTRA_SERVER_INFO, si.toBundle());
//		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
//		startService(i);
	}
	
	private void topologyDetails() {
		if (topology == null)
			return;
		StringBuilder sb = new StringBuilder();
		sb.append("\nID: ").append(topology.getId());
		sb.append("\nproductVersion: ").append(topology.getProductVersion());
		sb.append("\nlast updated: ").append(DomainHelper.getInstance().formatDatetime(topology.getTimestamp()));
		sb.append("\nversion: ").append(topology.getVersion());
		sb.append("\n");
		infoDialog(getString(R.string.topo_details), sb.toString());
	}
	
	private void delete(Intent i) {
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
	
	protected void performDelete(EntityType typ, String id, boolean delFromDisk) {
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
	}
	
	private void confirmDelete(final String name, final EntityType typ, final String id) {
		ConfirmDeleteDialog dlg = new ConfirmDeleteDialog();
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_ITEM_TYPE, typ.name());
		args.putString(Constants.EXTRA_ITEM_ID, id);
		args.putString(Intent.EXTRA_TEXT, getString(R.string.touch_to_del, typ.name(), name));
		dlg.setArguments(args);
		dlg.setListener(this);
		dlg.show(getFragmentManager(), TAG_DEL_DLG);
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

	protected void showSelectFileDialog(final int action) {
		File dir = getFilesDir();
		File[] files = null;
		ArrayList<String> jsonFiles = new ArrayList<String>();
		if (dir != null) {
			files = dir.listFiles();
			for (File f: files) {
				if (f.getName().endsWith(Constants.TOPOLOGY_FILE_EXT))
					jsonFiles.add(f.getName());
			}
		}
		if (jsonFiles.size() == 0) {
			if (action == R.id.action_load_from_disk) {
				UiUtils.showToast(this, getString(R.string.no_saved_files));
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
					;	//loadFromDisk(fname);
				else if (action == R.id.action_save_to_disk)
					saveToDisk(fname);
			}
		});
		dlg.show(getFragmentManager(), TAG_SEL_FILE_DLG);
	}

	protected void saveToFile(File f) {
		if (f == null || topology == null)
			return;
		helper.saveToFile(f, topology);
		if (!Constants.SAMPLE_FILE.equals(f.getName()))
			UiUtils.showToast(this, getString(R.string.saved_file, f.getName()));
	}
	
	protected void saveToDisk(String fname) {
		if (TextUtils.isEmpty(fname))
			return;
		if (!fname.endsWith(Constants.TOPOLOGY_FILE_EXT))
			fname = fname + Constants.TOPOLOGY_FILE_EXT;
		final File f = new File(getFilesDir(), fname);
		if (f.exists()) {
			confirmDialog(getString(R.string.overwrite, f.getName()), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					saveToFile(f);
				}
			});
		}
		else
			saveToFile(f);
	}

	protected void onExitConfirmed() {
		finish();
	}

	@Override
	public void moveService(Service svc, Group fromGrp, Group toGrp) throws ApiException {
		if (topology == null || svc == null || fromGrp == null || toGrp == null)
			return;
//		Service s = fromGrp.getService(svc.getId());
//		if (s == null) {
//			UiUtils.showToast(this, svc.getName() + " is not in group " + fromGrp.getName());
//			return;
//		}
//		s = toGrp.getService(svc.getId());
//		if (s != null) {
//			UiUtils.showToast(this, svc.getName() + " is already in group " + fromGrp.getName());
//			return;
//		}
//		fromGrp.removeService(svc.getId());
//		toGrp.addService(svc);
//		if (srvrInfo == null) { 
//			updateTopologyView();
//			return;
//		}
//		UiUtils.showToast(this, "Moving " + svc.getName() + " from " + fromGrp.getName() + " to " + toGrp.getName());				
//		Intent i = createModifyIntent(HttpMethod.DELETE, EntityType.Gateway, helper.toJson(svc));
//		i.setAction(RestService.ACTION_MOVE_GATEWAY);
//		i.putExtra(Constants.EXTRA_FROM_GROUP, fromGrp.getId());
//		i.putExtra(Constants.EXTRA_TO_GROUP, toGrp.getId());
//		asyncModify(i);
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

	@Override
	protected void onDestroy() {
		Intent i = new Intent(this, RestService.class);
		i.setAction(BaseIntentService.ACTION_KILL_RES_RCVR);
		startService(i);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		boolean rv = true;
		switch (menuItem.getItemId()) {
			case R.id.action_settings:
				showSettings();
			break;
			case R.id.action_test:
				test();
			break;
			default:
				rv = false;
		}
		return rv;
	}

	@Override
	public void onDelete(Intent i) {
		delete(i);
	}

	@Override
	public void onAddGateway(Intent i) {
		showGatewayDialog(null, i);
	}

	@Override
	public void onAddHost(Intent i) {
		showHostDialog(null);
	}

	@Override
	public void onAddGroup(Intent i) {
		showGroupDialog(null);
	}

	@Override
	public void onSaveToDisk(Intent i) {
		showSelectFileDialog(R.id.action_save_to_disk);
	}

	@Override
	public void onCompare(Intent i) {
		compareTopology();
	}

	@Override
	public void onStartGateway(Intent i) {
		notImplemented();
	}

	@Override
	public void onSshToHost(Intent i) {
		notImplemented();
	}

	@Override
	public void onUserSelected(Bundle data) {
		notImplemented();
	}

	@Override
	public void onRequestMonitoring(Intent i) {
		notImplemented();
	}

	@Override
	public void onManageKps(Intent i) {
		notImplemented();
	}
	
	private void test() {
		Intent i = new Intent(this, RestService.class);
		i.setAction(RestService.ACTION_CREATE_CHART);
		i.putExtra(Constants.EXTRA_ITEM_ID, "test");
		startService(i);
	}
	
	protected void notImplemented() {
		UiUtils.showToast(this, "Feature not implemented");
	}

	@Override
	public void onLoadError(ApiException e) {
		String ttl = "Error";
		String msg = getString(R.string.check_conn);
		switch (e.getStatusCode()) {
			case HttpStatus.SC_UNAUTHORIZED:
				ttl = getString(R.string.unauthorized);
			break;
			case HttpStatus.SC_NOT_FOUND:
				ttl = getString(R.string.not_found);
			break;
			case HttpStatus.SC_FORBIDDEN:
				ttl = getString(R.string.access_denied);
			break;
			default:
				msg = e.getLocalizedMessage();
		}
		if (TextUtils.isEmpty(msg))
			msg = "Unknown error";
		alertDialog(ttl, msg, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
	}
}
