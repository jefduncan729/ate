package com.axway.ate.activity;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.api.ApiException;
import com.axway.ate.api.ServerInfo;
import com.axway.ate.db.DbHelper.ConnMgrColumns;
import com.axway.ate.fragment.AlertDialogFragment;
import com.axway.ate.fragment.SelectFileDialog;
import com.axway.ate.fragment.SelectServerDialog;
import com.axway.ate.fragment.SelectServerDialog.Listener;
import com.axway.ate.fragment.TopologyListFragment;
import com.axway.ate.util.TopologyCompareResults;
import com.axway.ate.util.UiUtils;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

public class TopologyActivity extends ServiceAwareActivity implements TopologyListFragment.Listener, Listener {
	
	private static final String TAG = TopologyActivity.class.getSimpleName();
	
	private static final int MSG_UPDATE_TOPOLOGY = 1;
	
	private View ctr01;
	private ProgressBar prog01;
	
	private TopologyListFragment topoFrag;
	
	int testCtr;
	
	public TopologyActivity() {
		super();
		testCtr = 0;
	}

	private class ThisHandlerCallback extends ServiceAwareActivity.HandlerCallback {
		
		public ThisHandlerCallback() {
			super();
		}

		@Override
		public boolean handleMessage(Message msg) {
			boolean rv = true;
			switch (msg.what) {
				case MainService.NOTIFY_LOADING:
					showProgress(true);
				break;
				case MainService.NOTIFY_LOADED:
					onTopologyLoaded();
				break;
				case MainService.NOTIFY_TOPOLOGY_COMPARED:
					onTopologyCompared((TopologyCompareResults)msg.obj);
				break;
				case MainService.NOTIFY_SAVED:
					showProgress(false);
					if (msg.obj != null)
						handleException((Exception)msg.obj);
				break;
				case MSG_UPDATE_TOPOLOGY:
					updateTopologyView();
				break;
				default:
					rv = false;
			}
			if (!rv)
				rv = super.handleMessage(msg);
			return rv;
		}
	}
	
	@Override
	protected HandlerCallback createHandlerCallback() {
		return new ThisHandlerCallback();
	}

	private Topology getTopology() {
		if (service == null)
			return null;
		return service.getTopology();
	}
	@Override
	protected void afterServiceConnected(boolean isConnected) {
		showProgress(false);
	}
	
	private void onTopologyCompared(TopologyCompareResults results) {
		showProgress(false);
		if (results == null)
			UiUtils.showToast(this, "no result");
		else 
			infoDialog("Compare Results", results.prettyPrint());
	}
	
	private void onTopologyLoaded() {
		showProgress(false);
		updateTopologyView();
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
		showProgress(true);
		topoFrag = new TopologyListFragment();
		getFragmentManager().beginTransaction().replace(R.id.container01, topoFrag, "topoFrag").commit();
	}

	@Override
	public void onPrepareMenu(Menu menu) {
		boolean haveTopo = (getTopology() != null);
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
				edit(null, EntityType.Host);
			break;
			case R.id.action_add_group:
				edit(null, EntityType.Group);
			break;
			case R.id.action_add_gateway:
				add(menuItem.getIntent());
			break;
			case R.id.action_load_from_disk:
			case R.id.action_save_to_disk:
				showSelectFileDialog(menuItem.getItemId());
			break;
			case R.id.action_load_from_anm:
				loadFromServer();
			break;
			case R.id.action_delete:
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
			default:
				rv = false;
		}
		return rv;
	}

	private void compareTopology() {
		if (getTopology() == null)
			return;
		showProgress(true);
		ServerInfo si = getOnlyServerInfo();
		if (si == null) {
			selectServer(R.id.action_compare_topo);
		}
		else
			service.compareTopology(si, getTopology());
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
	
	private void loadFromServer() {
		if (service == null)
			return;
		ServerInfo si = getOnlyServerInfo();
		if (si == null)
			selectServer(R.id.action_save_to_anm);
		else {
			service.loadTopology(si);
		}
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
		final String id = i.getStringExtra(Intent.EXTRA_ORIGINATING_URI);
		if (TextUtils.isEmpty(id))
			return;
		String typ = i.getStringExtra(Intent.EXTRA_SUBJECT);
		if (TextUtils.isEmpty(typ))
			return;
		final EntityType eType = EntityType.valueOf(typ);
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
		}
		if (obj == null)
			return;
		confirmDelete(name, eType, id);
	}
	
	private void confirmDelete(final String name, final EntityType typ, final String id) {
		confirmDialog("Touch OK to delete " + name, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (service != null) {
					service.deleteFromTopology(typ, id);
					getHandler().sendEmptyMessageDelayed(MSG_UPDATE_TOPOLOGY, 50);
				}
			}
		});
	}
	
	private void add(Intent i) {
		if (i == null)
			return;
		Intent ai = null;
		String typ = i.getStringExtra(Intent.EXTRA_LOCAL_ONLY);
		EntityType eType = EntityType.valueOf(typ);
		if (eType == EntityType.Gateway) {
			ai = new Intent(this, ServiceActivity.class);
			ai.putExtras(i.getExtras());
		}
		if (ai != null)
			startActivityForResult(ai, EntityType.valueOf(typ).ordinal());
	}
	
	private void updateTopologyView() {
		if (topoFrag == null)
			return;
		topoFrag.update(getTopology());
	}
	
	private void edit(Object o, EntityType typ) {
		if (service == null)
			return;
		Intent i = null;
		String id = null;
		switch (typ) {
			case Host:
				i = new Intent(this, HostActivity.class);
				if (o != null)
					id = ((Host) o).getId();
			break;
			case Group:
				i = new Intent(this, GroupActivity.class);
				if (o != null)
					id = ((Group) o).getId();
			break;
			case Gateway:
				i = new Intent(this, ServiceActivity.class);
				if (o != null)
					id = ((Service) o).getId();
			break;
			case NodeManager:
			break;
		}
		if (i != null) {
			if (!TextUtils.isEmpty(id))
				i.putExtra(Intent.EXTRA_UID, id);
			startActivityForResult(i, typ.ordinal());
		}
	}

	private void confirmRemoveTrust() {
		confirmDialog("Touch OK to remove trusted certificate store.",new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (service != null) {
					service.removeTrustStore();
				}
			}
		});
	}

	@Override
	public void onTopologyItemSelected(EntityType itemType, String id) {
		if (getTopology() == null)
			return;
		if (itemType == EntityType.Host) {
			edit(getTopology().getHost(id), itemType);
		}
		else if (itemType == EntityType.Group) {
			edit(getTopology().getGroup(id), itemType);
		}
		else if (itemType == EntityType.NodeManager || itemType == EntityType.Gateway) {
			String[] ids = id.split("/");
			if (ids == null || ids.length != 2)
				return;
			String gid = ids[0];
			String sid = ids[1];
			Group g = getTopology().getGroup(gid);
			if (g == null)
				return;
			Service s = g.getService(sid);
			edit(s, itemType);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EntityType.Host.ordinal() || requestCode == EntityType.Group.ordinal()) {
			if (resultCode == Activity.RESULT_OK) {
				getHandler().sendEmptyMessageDelayed(MSG_UPDATE_TOPOLOGY, 50);
			}
		}
		else if (requestCode == EntityType.Gateway.ordinal() || requestCode == EntityType.NodeManager.ordinal()) {
			if (resultCode == Activity.RESULT_OK) {
			}
		}
		else
			super.onActivityResult(requestCode, resultCode, data);
	}

//	@Override
//	protected void onSaveInstanceState(Bundle outState) {
//		super.onSaveInstanceState(outState);
//		if (topology != null) {
//			outState.putString(Intent.EXTRA_SUBJECT, helper.toJson(topology).toString());
//		}
//	}

	@Override
	protected void handleException(final Exception e) {
		super.handleException(e);
		if (e instanceof ApiException) {
			ApiException ae = (ApiException)e;
			if (ae.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
				networkError("Please check your connection settings.", "Access Denied");
				return;
			}
			else if (ae.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				networkError("Please check your connection settings.", "Not Authorized");
				return;
			}
			else if (ae.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				UiUtils.showToast(this, "Not found");
				return;
			}
			else if (e.getCause() instanceof ResourceAccessException) {
				if (e.getCause().getCause() instanceof ConnectException) {
					networkError(e.getCause().getCause().getLocalizedMessage());
					return;
				}
			}
		}
		alertDialog("Unhandled Exception", "", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showSettings();
			}
		});
	}
//
//	protected void accessDenied() {
//	}
//
//	protected void notAuthorized() {
//	}
//
//	protected void notFound() {
//	}

	private void networkError(String msg) {
		networkError(msg, null);
	}
	
	protected void networkError(String msg, String title) {
		AlertDialogFragment dlg = new AlertDialogFragment();
		Bundle args = new Bundle();
		
		if (TextUtils.isEmpty(title))
			title = "Connection Error";
		args.putString(Intent.EXTRA_TITLE, title);
		args.putString(Intent.EXTRA_TEXT, msg);
		dlg.setOnNegative(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showProgress(false);
				finish();
			}
		}, R.string.action_exit);
		dlg.setOnNeutral(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showProgress(false);
				showConnMgr();
			}
		}, R.string.action_conn_mgr);
		dlg.setOnPositive(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showProgress(false);
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
		args.putStringArrayList(Intent.EXTRA_TEXT, jsonFiles);
		args.putInt(Intent.EXTRA_LOCAL_ONLY, action);
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
		if (service == null || TextUtils.isEmpty(fname))
			return;
		File f = new File(getFilesDir(), fname);
		if (f.exists()) {
			showProgress(true);
			service.loadTopology(f);
		}
	}
	
	protected void saveToDisk(String fname) {
		if (service == null || TextUtils.isEmpty(fname))
			return;
		if (!fname.endsWith(Constants.TOPOLOGY_FILE_EXT))
			fname = fname + Constants.TOPOLOGY_FILE_EXT;
		final File f = new File(getFilesDir(), fname);
		if (f.exists()) {
			confirmDialog(f.getName() + " exists. Overwrite?", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					service.saveToFile(f, getTopology());
				}
			});
		}
		else
			service.saveToFile(f, getTopology());
	}
	
	private boolean isDirty() {
		if (service == null)
			return false;
		return service.isDirty();
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
		else
			super.onBackPressed();
	}

	@Override
	public void onServerSelected(ServerInfo info, int action) {
		if (service == null || info == null)
			return;
		if (action == R.id.action_load_from_anm) {
			showProgress(true);
			service.loadTopology(info);
		}
		else if (action == R.id.action_compare_topo) {
			showProgress(true);
			service.compareTopology(info, getTopology());
		}
	}

	@Override
	protected void onCertsAddedToTruststore() {
		super.onCertsAddedToTruststore();
		loadFromServer();
	}
}
