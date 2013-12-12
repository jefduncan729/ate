package com.axway.ate;

import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;

public interface TopologyClient {

	public void addHost(Host h, boolean useSsl) throws ApiException;
	public void addGroup(Group g) throws ApiException;
	public void addService(Service s, int svcsPort) throws ApiException;
	
	public void updateHost(Host h) throws ApiException;
	public void updateGroup(Group g) throws ApiException;
	public void updateService(Service s) throws ApiException;
	
	public void removeHost(Host h) throws ApiException;
	public void removeGroup(Group g) throws ApiException;
	public void removeService(Service s) throws ApiException;

	public void moveService(Service s, Group fromGrp, Group toGrp) throws ApiException;
	
	public void loadTopology() throws ApiException;
	
	public Topology getTopology();
}
