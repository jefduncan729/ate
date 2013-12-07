package com.axway.ate.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;

public class TopologyComparer {
	private static final String TAG = TopologyComparer.class.getSimpleName();

	private Topology clientTopo;
	private Topology serverTopo;
	
	public TopologyComparer() {
		super();
		clientTopo = null;
		serverTopo = null;
	}
	
	public TopologyComparer(Topology local, Topology remote) {
		this();
		clientTopo = local;
		serverTopo = remote;
	}

	protected boolean isChanged(Map<String, String> c, Map<String, String> s) {
		if (c == null || s == null)
			return false;
		if (c.size() == 0 && s.size() == 0)
			return false;
		if (c.size() != s.size())
			return true;
		for (String k: c.keySet()) {
			String vc = c.get(k);
			String vs = s.get(k);
			if (!vc.equals(vs))
				return true;
		}
		return false;
	}
	
	protected boolean isChanged(Host c, Host s) {
		return (!c.getName().equals(s.getName()));
	}

	protected boolean isChanged(Group c, Group s) {
		if (!c.getName().equals(s.getName()))
			return true;
		if (isChanged(c.getTags(), s.getTags()))
			return true;
		Set<String> cHostIds = c.getHostIds();
		Set<String> sHostIds = s.getHostIds();
		if (cHostIds.size() != sHostIds.size())
			return true;
		for (String ch: cHostIds) {
			if (!sHostIds.contains(ch))
				return true;
		}
		return false;
	}

	protected boolean isChanged(Service c, Service s) {
		if (!c.getName().equals(s.getName()))
			return true;
		if (c.getEnabled() != s.getEnabled())
			return true;
		if (!c.getHostID().equals(s.getHostID()))
			return true;
		if (c.getManagementPort() != s.getManagementPort())
			return true;
		if (!c.getScheme().equals(s.getScheme()))
			return true;
		if (c.getType() != s.getType())
			return true;
		if (isChanged(c.getTags(), s.getTags()))
			return true;
		return false;
	}
	
	protected boolean isChanged(Object client, Object server) {
		boolean rv = false;
		if (client == null || server == null)
			return rv;
		if (client instanceof Host) {
			Host c = (Host)client;
			Host s = (Host)server;
			rv = isChanged(c, s);
		}
		if (client instanceof Group) {
			Group c = (Group)client;
			Group s = (Group)server;
			rv = isChanged(c, s);
		}
		if (client instanceof Service) {
			Service c = (Service)client;
			Service s = (Service)server;
			rv = isChanged(c, s);
		}
		return rv;
	}

	private void compareHosts(TopologyCompareResults results) {
		Collection<Host> clientHosts = clientTopo.getHosts();
		Collection<Host> serverHosts = serverTopo.getHosts();
		if (clientHosts.size() == 0 && serverHosts.size() == 0)
			return;
		if (clientHosts.size() >= serverHosts.size()) {
			for (Host ch: clientHosts) {
				Host sh = serverTopo.getHost(ch.getId());
				if (sh == null)
					results.addAddedEntry(results.createEntry(ch));
				else if (isChanged(ch, sh))
					results.addChangedEntry(results.createEntry(ch));
			}
		}
		else {
			for (Host sh: serverHosts) {
				Host ch = clientTopo.getHost(sh.getId());
				if (ch == null)
					results.addRemovedEntry(results.createEntry(sh));
				else if (isChanged(ch, sh))
					results.addChangedEntry(results.createEntry(sh));
			}
		}
	}

	private void compareGroups(TopologyCompareResults results) {
		Collection<Group> clientGroups = clientTopo.getGroups();
		Collection<Group> serverGroups = serverTopo.getGroups();
		if (clientGroups.size() == 0 && serverGroups.size() == 0)
			return;
		if (clientGroups.size() >= serverGroups.size()) {
			for (Group cg: clientGroups) {
				Group sg = serverTopo.getGroup(cg.getId());
				if (sg == null)
					results.addAddedEntry(results.createEntry(cg));
				else {
					if (isChanged(cg, sg))
						results.addChangedEntry(results.createEntry(cg));
					compareServices(cg, sg, results);
				}
			}
		}
		else {
			for (Group sg: serverGroups) {
				Group cg = clientTopo.getGroup(sg.getId());
				if (cg == null)
					results.addRemovedEntry(results.createEntry(sg));
				else {
					if (isChanged(cg, sg))
						results.addChangedEntry(results.createEntry(sg));
					compareServices(cg, sg, results);
				}
			}
		}
	}

	private void compareServices(Group cGrp, Group sGrp, TopologyCompareResults results) {
		Collection<Service> clientSvcs = cGrp.getServices();
		Collection<Service> serverSvcs = sGrp.getServices();
		if (clientSvcs.size() == 0 && serverSvcs.size() == 0)
			return;
		if (clientSvcs.size() >= serverSvcs.size()) {
			for (Service cs: clientSvcs) {
				Service ss = serverTopo.getService(cs.getId());
				if (ss == null)
					results.addAddedEntry(results.createEntry(cs));
				else if (isChanged(cs, ss))
					results.addChangedEntry(results.createEntry(cs));
			}
		}
		else {
			for (Service ss: serverSvcs) {
				Service cs = clientTopo.getService(ss.getId());
				if (cs == null)
					results.addRemovedEntry(results.createEntry(ss));
				else if (isChanged(cs, ss))
					results.addChangedEntry(results.createEntry(ss));
			}
		}
	}
	
	public TopologyCompareResults compare() {
		TopologyCompareResults rv = null;
		if (clientTopo == null || serverTopo == null)
			return rv;
		rv = new TopologyCompareResults();
		if (clientTopo.equals(serverTopo))
			return rv;
		compareHosts(rv);
		compareGroups(rv);
		return rv;
	}
}
