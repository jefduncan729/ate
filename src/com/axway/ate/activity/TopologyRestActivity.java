package com.axway.ate.activity;

import java.util.Collection;

import org.apache.http.HttpStatus;
import org.springframework.http.HttpMethod;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.axway.ate.ApiException;
import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.ServerInfo;
import com.axway.ate.fragment.AlertDialogFragment;
import com.axway.ate.fragment.SshUserDialog;
import com.axway.ate.fragment.TopologyLoaderFragment;
import com.axway.ate.service.BaseIntentService;
import com.axway.ate.service.RestService;
import com.axway.ate.util.UiUtils;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class TopologyRestActivity extends TopologyActivity {

	private static final String TAG = TopologyRestActivity.class.getSimpleName();
	
	private TopologyLoaderFragment topoLdrFrag;
	private ServerInfo srvrInfo;
	private Intent outstandingIntent;
	private Boolean haveConsole;
	private String consoleHandle;
	private Intent hostNodeMgrIntent;
	private ResultReceiver resRcvr;

	public TopologyRestActivity() {
		super();
		topoLdrFrag = null;
		srvrInfo = null;
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
			dismissProgressDialog();
			EntityType et = null;
			if (data != null) {
				et = EntityType.valueOf(data.getString(Constants.EXTRA_ITEM_TYPE));
			}
			switch (code) {
				case HttpStatus.SC_BAD_REQUEST: {
					String msg = getString(R.string.invalid_data);
					if (EntityType.Gateway == et)
						msg = getString(R.string.svc_port_in_use);
					else if (EntityType.Host == et)
						;
					else if (EntityType.Group == et)
						;
					networkError(msg, getString(R.string.bad_request));
				}
				break;
				case HttpStatus.SC_NOT_FOUND:
					UiUtils.showToast(this, getString(R.string.not_found));
				break;
				case HttpStatus.SC_UNAUTHORIZED:
					networkError(getString(R.string.check_conn), getString(R.string.unauthorized));
				break;
				case HttpStatus.SC_FORBIDDEN:
					networkError(getString(R.string.check_conn), getString(R.string.access_denied));
				break;
				default:
					alertDialog(getString(R.string.error), "Status Code: " + Integer.toString(code) + "\n" + data.getString(Intent.EXTRA_BUG_REPORT), null);
			}
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srvrInfo = null;
		Bundle b = null;
		setContentView(R.layout.empty_frag);
//		setTitle(getString(R.string.topology));
		if (savedInstanceState != null) {
			String s = savedInstanceState.getString(Constants.EXTRA_JSON_TOPOLOGY);
			if (s != null)
				topology = helper.topologyFromJson(helper.parse(s).getAsJsonObject());
			consoleHandle = savedInstanceState.getString(Constants.EXTRA_CONSOLE_HANDLE);
			b = savedInstanceState.getBundle(Constants.EXTRA_SERVER_INFO);
		}
		else
			b = getIntent().getBundleExtra(Constants.EXTRA_SERVER_INFO);
		if (b != null)
			srvrInfo = ServerInfo.fromBundle(b);
		if (srvrInfo == null)
			finish();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (consoleHandle != null)
			outState.putString(Constants.EXTRA_CONSOLE_HANDLE, consoleHandle);
		if (srvrInfo != null)
			outState.putBundle(Constants.EXTRA_SERVER_INFO, srvrInfo.toBundle());

	}
	
	private void processResult(Bundle data) {
		String action = data.getString(Constants.EXTRA_ACTION);
		if (RestService.ACTION_COMPARE.equals(action)) {
			dismissProgressDialog();
			infoDialog("Compare Results", data.getString(Constants.EXTRA_COMPARE_RESULT));
			return;
		}
		if (HttpMethod.POST.name().equals(action)) {
			EntityType typ = EntityType.valueOf(data.getString(Constants.EXTRA_ITEM_TYPE));
			String jstr = data.getString(Constants.EXTRA_JSON_ITEM);
			if (EntityType.Host == typ) {
				//if adding a Host, add the nodemgr service
				Host h = helper.hostFromJson(helper.parse(jstr).getAsJsonObject());
				Host th = topology.getHostByName(h.getName());
				if (th == null)
					return;
				if (hostNodeMgrIntent != null) {
//					UiUtils.showToast(this, "Adding NodeManager for host " + th.getName());
					Service nmSvc = helper.createNodeMgr(h, hostNodeMgrIntent.getBooleanExtra(Constants.EXTRA_USE_SSL, true), hostNodeMgrIntent.getIntExtra(Constants.EXTRA_MGMT_PORT, 0));	//new Service();
					hostNodeMgrIntent.putExtra(Constants.EXTRA_JSON_ITEM, helper.toJson(nmSvc).toString());
					asyncModify(hostNodeMgrIntent);
					hostNodeMgrIntent = null;
					return;
				}
			}
		}
		else if (HttpMethod.DELETE.name().equals(action)) {
			EntityType typ = EntityType.valueOf(data.getString(Constants.EXTRA_ITEM_TYPE));
			if (EntityType.NodeManager == typ) {
				//if deleting a nodemgr, delete the host
				String id = data.getString(Constants.EXTRA_HOST_ID);
				Host h = topology.getHost(id);
				if (h == null)
					return;
				//UiUtils.showToast(this, "Removing host " + h.getName());
				asyncModify(createModifyIntent(HttpMethod.DELETE, EntityType.Host, helper.toJson(h)));
				return;
			}
		}
		dismissProgressDialog();
		loadTopology();
	}

	@Override
	protected void loadTopology() {
		if (srvrInfo == null)
			return;
		topology = null;
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_TOPO_SOURCE, srvrInfo.displayString());
		args.putBoolean(Constants.EXTRA_HAVE_CONSOLE, isConsoleAvailable());
		args.putBundle(Constants.EXTRA_SERVER_INFO, srvrInfo.toBundle());
		args.putString(Constants.EXTRA_URL, srvrInfo.buildUrl("topology"));
		topoLdrFrag = new TopologyLoaderFragment();
		topoLdrFrag.setArguments(args);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.container01, topoLdrFrag, TAG_TOPOLOGY_FRAG).commit();		
	}

	private void networkError(String msg, String title) {
		AlertDialogFragment dlg = new AlertDialogFragment();
		Bundle args = new Bundle();
		
		if (TextUtils.isEmpty(title))
			title = getString(R.string.conn_error);
		args.putString(Intent.EXTRA_TITLE, title);
		args.putString(Intent.EXTRA_TEXT, msg);
		dlg.setOnNegative(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		}, R.string.action_exit);
//		dlg.setOnNeutral(new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				showConnMgr();
//			}
//		}, R.string.action_conn_mgr);
		dlg.setOnPositive(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showSelectFileDialog(R.id.action_load_from_disk);
			}
		}, R.string.action_work_local);
		dlg.setArguments(args);
		dlg.show(getFragmentManager(), TAG_ALERT_DLG);
	}
	
	private Intent createModifyIntent(HttpMethod method, EntityType etype, JsonObject json) {		
		Intent rv = new Intent(this, RestService.class);
		rv.setAction(method.name());
		if (srvrInfo != null)
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
			//UiUtils.showToast(this, "Removing Node Manager service from host " + th.getName());
			asyncModify(hostNodeMgrIntent);
		}
	}

	@Override
	public void removeGroup(Group g, boolean delFromDisk) throws ApiException {
		if (topology == null || g == null)
			return;
		if (topology.getGroup(g.getId()) != null) {
			topology.removeGroup(g);
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
		Intent i = createModifyIntent(HttpMethod.DELETE, EntityType.Gateway, helper.toJson(s));
		i.putExtra(Constants.EXTRA_DELETE_FROM_DISK, delFromDisk);
		i.putExtra(Constants.EXTRA_REFERRING_ITEM_ID, g.getId());
		asyncModify(i);
	}
	
	private void asyncModify(Intent i) {
		if (srvrInfo == null || i == null || i.getExtras() == null)
			return;
		if (!progressDialogShowing()) {
			String action = i.getAction();
			int msgId = 0;
			if (HttpMethod.POST.name().equals(action))
				msgId = R.string.adding;
			else if (HttpMethod.PUT.name().equals(action))
				msgId = R.string.updating;
			else if (HttpMethod.DELETE.name().equals(action))
				msgId = R.string.deleting;
			showProgressDialog(getString(msgId));
		}
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
			i = getPackageManager().getLaunchIntentForPackage(Constants.JACKPAL_TERMINAL_PACKAGE);
			if (i != null) {
				Log.d(TAG, "consoleHandle: " + consoleHandle);
				i = new Intent(Constants.JACKPAL_ACTION_RUN_SCRIPT);
				String cmd = "pwd";
				if (consoleHandle != null) {
					cmd = "";
					i.putExtra(Constants.JACKPAL_EXTRA_WINDOW_HANDLE, consoleHandle);
				}
				i.putExtra(Constants.JACKPAL_EXTRA_INITIAL_CMD, cmd);
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
			i = getPackageManager().getLaunchIntentForPackage(Constants.JACKPAL_TERMINAL_PACKAGE);
			if (i != null) {
				i = new Intent(Constants.JACKPAL_ACTION_RUN_SCRIPT);
				i.addCategory(Intent.CATEGORY_DEFAULT);				
				i.putExtra(Constants.JACKPAL_EXTRA_INITIAL_CMD, script);
				if (consoleHandle != null)
					i.putExtra(Constants.JACKPAL_EXTRA_WINDOW_HANDLE, consoleHandle);
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
			UiUtils.showToast(this, getString(R.string.open_console1));
			return;
		}
		String username = getSshUser();
		if (username == null)
			sshUserDialog(h.getName());
		else
			runScriptInConsole(getString(R.string.ssh_cmd, username, h.getName()));
	}
	
	private void startGateway(Service s) {
		if (consoleHandle == null) {
			UiUtils.showToast(this, getString(R.string.open_console2));
			return;
		}
		Group g = topology.getGroupForService(s.getId());
		if (g == null)
			return;
		runScriptInConsole(getString(R.string.startinstance_cmd, s.getName(), g.getName()));
	}

	@Override
	protected void onDestroy() {
		Intent i = new Intent(this, RestService.class);
		i.setAction(BaseIntentService.ACTION_KILL_RES_RCVR);
		startService(i);
		super.onDestroy();
	}

	private String getSshUser() {
		String rv = null;
		if (getPrefs().getBoolean(Constants.KEY_REMEMBER_USER, false))
			rv = getPrefs().getString(Constants.KEY_SSHUSER, null);
		return rv;
	}
	
	private void sshUserDialog(String hostname) {
		SshUserDialog dlg = new SshUserDialog();
		String username = null;
		boolean remember = getPrefs().getBoolean(Constants.KEY_REMEMBER_USER, false); 
		if (remember)
			username = getPrefs().getString(Constants.KEY_SSHUSER, null);
		Bundle args = new Bundle();
		args.putString(Constants.KEY_SSHUSER, username);
		args.putBoolean(Constants.KEY_REMEMBER_USER, remember);
		args.putString(Constants.EXTRA_HOST_ID, hostname);
		dlg.setArguments(args);
		dlg.setListener(this);
		dlg.show(getFragmentManager(), TAG_SSHUSER_DLG);
	}

	@Override
	public void onUserSelected(Bundle data) {
		if (data == null)
			return;
		String username = data.getString(Constants.KEY_SSHUSER);
		String hostname = data.getString(Constants.EXTRA_HOST_ID);
		if (TextUtils.isEmpty(username))
			return;
		if (data.getBoolean(Constants.KEY_REMEMBER_USER, false)) {
			SharedPreferences.Editor e = getPrefs().edit();
			e.putString(Constants.KEY_SSHUSER, username);
			e.putBoolean(Constants.KEY_REMEMBER_USER, true);
			e.commit();
		}
		runScriptInConsole(getString(R.string.ssh_cmd, username, hostname));
	}
	
	@Override
	public void onBackPressed() {
		if (outstandingIntent != null) {		
			confirmDialog(getString(R.string.outstanding_intent), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onExitConfirmed();
				}
			});
		}
		else
			super.onBackPressed();
	}

	protected void performCompare(ServerInfo si)  {
		if (si == null)
			return;
		showProgressDialog("Comparing...");
		Intent i = new Intent(this, RestService.class);
		i.setAction(RestService.ACTION_COMPARE);
		i.putExtra(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(topology).toString());
		i.putExtra(Constants.EXTRA_SERVER_INFO, si.toBundle());
		i.putExtra(Intent.EXTRA_RETURN_RESULT, getResultReceiver());
		startService(i);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == R.id.action_console) {
			if (resultCode != Activity.RESULT_OK)
				return;
			Log.d(TAG, "consoleActivity result: " + Integer.toString(resultCode));
			if (data != null) {
				consoleHandle = data.getStringExtra(Constants.JACKPAL_EXTRA_WINDOW_HANDLE);
			}
			Log.d(TAG, "consoleHandle:" + consoleHandle);
		}
		else if (requestCode == R.id.action_conn_mgr) {
			srvrInfo = null;
		}
		else
			super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onStartGateway(Intent i) {
		if (i == null)
			return;
		String id = i.getStringExtra(Constants.EXTRA_ITEM_ID);
		String ids[] = id.split("/");
		if (ids != null && ids.length == 2) {
			Service s = topology.getService(ids[1]);
			if (s != null)
				startGateway(s);
		}
	}

	@Override
	public void onSshToHost(Intent i) {
		if (i == null)
			return;
		Host h = topology.getHost(i.getStringExtra(Constants.EXTRA_ITEM_ID));
		sshToHost(h);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.rest, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		boolean rv = true;
		switch (menuItem.getItemId()) {
			case R.id.action_console:
				launchConsole();
			break;
			case R.id.action_forget_sshuser:
				forgetSshUser();
			break;
			default:
				rv = super.onOptionsItemSelected(menuItem);
		}
		return rv;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem i = menu.findItem(R.id.action_console);
		if (i != null)
			i.setVisible(isConsoleAvailable());
		i = menu.findItem(R.id.action_forget_sshuser);
		if (i != null)
			i.setVisible(!TextUtils.isEmpty(getPrefs().getString(Constants.KEY_SSHUSER, null)));
		return true;
	}

	private void forgetSshUser() {
		SharedPreferences.Editor e = getPrefs().edit();
		e.remove(Constants.KEY_SSHUSER);
		e.putBoolean(Constants.KEY_REMEMBER_USER, false);
		e.commit();
		UiUtils.showToast(this, R.string.sshuser_forgotten);
	}
}
