package com.axway.ate.activity;

import java.io.File;
import java.util.Collection;

import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.axway.ate.ApiException;
import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.fragment.TopologyFileFragment;
import com.axway.ate.util.UiUtils;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class TopologyFileActivity extends TopologyActivity {

	private static final String TAG = TopologyFileActivity.class.getSimpleName();
	
	private TopologyFileFragment topoFileFrag;	
	private File file;
	private boolean dirty;

	public TopologyFileActivity() {
		super();
		file = null;
		dirty = false;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		file = null;
		String fname = null;
		setContentView(R.layout.empty_frag);
		setTitle(getString(R.string.topology));
		if (savedInstanceState != null) {
			if (!TextUtils.isEmpty(savedInstanceState.getString(Constants.EXTRA_FILENAME)))
				fname = savedInstanceState.getString(Constants.EXTRA_FILENAME);
		}
		else
			fname = getIntent().getStringExtra(Constants.EXTRA_FILENAME);
		if (!TextUtils.isEmpty(fname))
			file = new File(fname);
	}
	
	@Override
	protected void loadTopology() {
		if (file == null)
			finish();
		if (isFinishing())
			return;
		Topology t = helper.loadFromFile(file);
		onTopologyLoaded(t);
		Bundle args = new Bundle();
		args.putString(Constants.EXTRA_TOPO_SOURCE, file.getName());
		args.putBoolean(Constants.EXTRA_HAVE_CONSOLE, false);
		args.putString(Constants.EXTRA_JSON_TOPOLOGY, helper.toJson(t).toString());
		topoFileFrag = new TopologyFileFragment();
		topoFileFrag.setArguments(args);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.container01, topoFileFrag, TAG_TOPOLOGY_FRAG).commit();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (file != null)
			outState.putString(Constants.EXTRA_FILENAME, file.getAbsolutePath());
	}
	
	private void updateTopologyView() {
		if (topoFileFrag != null) {
			dirty = true;
			topoFileFrag.update(topology, file.getName());
		}
	}
	
	private boolean isDirty() {
		return dirty;
	}
	
	@Override
	public void onBackPressed() {
		if (isDirty()) {
			confirmDialog(getString(R.string.confirm_discard), new DialogInterface.OnClickListener() {
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
	public void addHost(Host h, int mgmtPort, boolean useSsl) throws ApiException {
		if (topology == null || h == null)
			return;
		Group g = topology.getGroupForService(topology.adminNodeManager().getId());
		topology.addHost(h);
		h.setId(topology.generateID(EntityType.Host));
		Service nmSvc = helper.createNodeMgr(h, useSsl, mgmtPort);
		nmSvc.setId(topology.generateID(EntityType.NodeManager));
		g.addService(nmSvc);
		updateTopologyView();
	}

	@Override
	public void addGroup(Group g) throws ApiException {
		if (topology == null || g == null)
			return;
		topology.addGroup(g);
		g.setId(topology.generateID(EntityType.Group));
		updateTopologyView();
	}

	@Override
	public void addService(Service s, int svcsPort) throws ApiException {
		if (topology == null || selGrp == null || s == null)
			return;
		Group tg = topology.getGroup(selGrp.getId());
		if (tg == null)
			throw new ApiException("expecting to find group for service: " + s.getId());
		tg.addService(s);
		s.setId(topology.generateID(EntityType.Gateway));
		updateTopologyView();
	}

	@Override
	public void updateHost(Host h) throws ApiException {
		if (topology == null || h == null)
			return;
		Host th = topology.getHost(h.getId());
		if (th != null) {
			th.setName(h.getName());
			updateTopologyView();
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
		if (nmSvc != null) {
			Group g = topology.getGroupForService(nmSvc.getId());
			if (g != null)
				g.removeService(nmSvc.getId());
		}
		topology.removeHost(th);
		updateTopologyView();
	}

	@Override
	public void removeGroup(Group g, boolean delFromDisk) throws ApiException {
		if (topology == null || g == null)
			return;
		if (topology.getGroup(g.getId()) != null) {
			topology.removeGroup(g);
			updateTopologyView();
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
		if (g.getServices().size() == 0)
			topology.removeGroup(g);
		updateTopologyView();
	}

	@Override
	protected void performDelete(EntityType typ, String id, boolean delFromDisk) {
		super.performDelete(typ, id, delFromDisk);
		updateTopologyView();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem i = menu.findItem(R.id.action_console);
		if (i != null)
			i.setVisible(false);
		i = menu.findItem(R.id.action_forget_sshuser);
		if (i != null)
			i.setVisible(false);
		return true;
	}

	@Override
	protected void saveToFile(File f) {
		super.saveToFile(f);
		dirty = false;
	}
}
