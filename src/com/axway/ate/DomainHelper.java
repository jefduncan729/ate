package com.axway.ate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.springframework.http.HttpAuthentication;
import org.springframework.http.HttpBasicAuthentication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import android.text.TextUtils;
import android.util.Log;

import com.axway.ate.kps.KpsStore;
import com.axway.ate.kps.KpsType;
import com.axway.ate.metrics.BaseMetrics;
import com.axway.ate.metrics.GroupMetrics;
import com.axway.ate.metrics.SysOverview;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vordel.api.monitoring.model.MetricGroup;
import com.vordel.api.monitoring.model.Summary;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;
import com.vordel.api.topology.model.Topology.ServiceType;

public class DomainHelper {
	private static final String TAG = DomainHelper.class.getSimpleName();
	
	public static DateFormat DATE_TIME_FORMAT = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
	public static DateFormat DATE_ONLY_FORMAT = new java.text.SimpleDateFormat("MM/dd/yyyy", Locale.US);
	public static DateFormat TIME_ONLY_FORMAT = new java.text.SimpleDateFormat("HH:mm:ss", Locale.US);
	public static DateFormat TIME_SHORT_FORMAT = new java.text.SimpleDateFormat("HH:mm", Locale.US);
	
	private static DomainHelper instance = null;
	private JsonParser parser;
	
	protected DomainHelper() {
		super();
		parser = null;
	}
	
	public static DomainHelper getInstance() {
		if (instance == null)
			instance = new DomainHelper();
		return instance;
	}

	public String formatDatetime(long time) {
		Date d = new Date(time);
		String rv = DATE_TIME_FORMAT.format(d);
		return rv;
	}

	public String formatDate(long time) {
		Date d = new Date(time);
		String rv = DATE_ONLY_FORMAT.format(d);
		return rv;
	}

	public String formatTime(long time) {
		Date d = new Date(time);
		String rv = TIME_ONLY_FORMAT.format(d);
		return rv;
	}
	
	public JsonParser getJsonParser() {
		if (parser == null)
			parser = new JsonParser();
		return parser;
	}

	public JsonElement parse(String json) {
		JsonElement rv = null;
		if (!TextUtils.isEmpty(json))
			rv = getJsonParser().parse(json);
		return rv;
	}
	
	public Topology loadFromStream(InputStream is) {
		if (is == null)
			return null;
		Topology rv = null;
		String contents = null;
		Reader r = new BufferedReader(new InputStreamReader(is));
		Writer w = new StringWriter();
		char buf[] = new char[4096];
		int n = 0;
		try {
			while ((n = r.read(buf)) != -1) {
				w.write(buf, 0, n);
			}
			contents = w.toString();
		}
		catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
		finally {
			try { is.close(); } catch (IOException e) {}
		}
		if (TextUtils.isEmpty(contents))
			return rv;
		JsonElement jo = parse(contents);
		rv = topologyFromJson(jo.getAsJsonObject());
		return rv;
	}

	public Topology loadFromFile(File f) {
		if (f == null || !f.exists())
			return null;
		InputStream is = null;
		Topology rv = null;
		try {
			is = new FileInputStream(f);
			rv = loadFromStream(is);
		} 
		catch (FileNotFoundException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
		return rv;	
	}	

	public void saveToFile(File f, Topology topology) {
		if (f == null || topology == null)
			return;
		JsonObject json = toJson(topology);
		if (json == null)
			return;
		Writer bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(f));
			bw.write(json.toString());
		}
		catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
		finally {
			if (bw != null)
				try {bw.close();} catch (IOException e){}
		}		
	}

	public Topology topologyFromJson(String jsonStr) {
		if (TextUtils.isEmpty(jsonStr))
			return null;
		JsonElement e = parse(jsonStr);
		if (e == null)
			return null;
		return topologyFromJson(e.getAsJsonObject());
	}
	
	public Topology topologyFromJson(JsonObject json) {
		Topology rv = null;
		if (json == null)
			return rv;
		rv = new Topology();
		if (json.has("id"))
			rv.setId(json.get("id").getAsString());
		if (json.has("productVersion"))
			rv.setProductVersion(json.get("productVersion").getAsString());
		if (json.has("timestamp"))
			rv.setTimestamp(json.get("timestamp").getAsLong());
		if (json.has("version"))
			rv.setVersion(json.get("version").getAsInt());
		if (json.has("groups")) {		
			JsonArray grps = json.getAsJsonArray("groups");
			if (grps != null && grps.size() > 0) {
				for (int i = 0; i < grps.size(); i++) {
					Group g = groupFromJson(grps.get(i).getAsJsonObject());
					if (g != null)
						rv.addGroup(g);
				}
			}
		}
		if (json.has("hosts")) {
			JsonArray hosts = json.getAsJsonArray("hosts");
			if (hosts != null && hosts.size() > 0) {
				for (int i = 0; i < hosts.size(); i++) {
					Host h = hostFromJson(hosts.get(i).getAsJsonObject());
					if (h != null)
						rv.addHost(h);
				}
			}
		}
		if (json.has("uniqueIdCounters")) {
			Map<EntityType, Integer> ctrs = jsonToCounters(json.getAsJsonObject("uniqueIdCounters"));
			if (ctrs != null)
				rv.setUniqueIdCounters(ctrs);
		}
		return rv;
	}
	
	private JsonObject countersToJson(Map<EntityType, Integer> ctrs) {
		JsonObject rv = null;
		if (ctrs == null)
			return rv;
		if (ctrs != null) {
			rv = new JsonObject();
			Set<EntityType> keys = ctrs.keySet();
			for (EntityType k: keys)
				rv.addProperty(k.name(), ctrs.get(k));
		}
		return rv;
	}
	
	private Map<EntityType, Integer> jsonToCounters(JsonObject json) {
		Map<EntityType, Integer> rv = new HashMap<EntityType, Integer>();
		if (json == null)
			return rv;
		Set<Entry<String, JsonElement>> ctrs = json.entrySet();
		if (ctrs == null || ctrs.size() == 0)
			return rv;
		for (Entry<String, JsonElement> e: ctrs) {
			rv.put(EntityType.valueOf(e.getKey()), e.getValue().getAsInt());
		}
		return rv;
	}
	
	public JsonObject toJson(Topology t) {
		JsonObject rv = null;
		if (t == null)
			return rv;
		rv = new JsonObject();
		rv.addProperty("id", t.getId());
		rv.addProperty("productVersion", t.getProductVersion());
		rv.addProperty("timestamp", t.getTimestamp());
		rv.addProperty("version", t.getVersion());
		if (t.getGroups() != null) {
			JsonArray grps = new JsonArray();
			for (Group g: t.getGroups()) {
				grps.add(toJson(g));
			}
			rv.add("groups", grps);
		}
		if (t.getGroups() != null) {
			JsonArray hosts = new JsonArray();
			for (Host h: t.getHosts()) {
				hosts.add(toJson(h));
			}
			rv.add("hosts", hosts);
		}
		if (t.getUniqueIdCounters() != null)
			rv.add("uniqueIdCounters", countersToJson(t.getUniqueIdCounters()));
		return rv;
	}

	public Group groupFromJson(String jsonStr) {
		if (TextUtils.isEmpty(jsonStr))
			return null;
		JsonElement e = parse(jsonStr);
		if (e == null)
			return null;
		return groupFromJson(e.getAsJsonObject());
	}
	
	public Group groupFromJson(JsonObject json) {
		Group rv = null;
		if (json == null)
			return rv;
		rv = new Group();
		if (json.has("id")) {
			String s = json.get("id").getAsString();
			if (!TextUtils.isEmpty(s))
				rv.setId(s);
		}
		if (json.has("name"))
			rv.setName(json.get("name").getAsString());
		if (json.has("services")) {
			JsonArray svcs = json.getAsJsonArray("services");
			if (svcs != null && svcs.size() > 0) {
				for (int i = 0; i < svcs.size(); i++) {
					Service s = serviceFromJson(svcs.get(i).getAsJsonObject());
					if (s != null)
						rv.addService(s);
				}
			}
		}
		if (json.has("tags"))
			rv.setTags(jsonToTags(json.getAsJsonObject("tags")));
		return rv;
	}

	public JsonObject toJson(Group g) {
		JsonObject rv = null;
		if (g == null)
			return rv;
		rv = new JsonObject();
		rv.addProperty("id", g.getId() == null ? "" : g.getId());
		rv.addProperty("name", g.getName() == null ? "" : g.getName());
		if (g.getServices() != null) {
			JsonArray svcs = new JsonArray();
			for (Service s: g.getServices()) {
				svcs.add(toJson(s));
			}
			rv.add("services", svcs);
		}
		rv.add("tags", tagsToJson(g.getTags()));
		return rv;
	}

	public Service serviceFromJson(String jsonStr) {
		if (TextUtils.isEmpty(jsonStr))
			return null;
		JsonElement e = parse(jsonStr);
		if (e == null)
			return null;
		return serviceFromJson(e.getAsJsonObject());
	}

	public Service serviceFromJson(JsonObject json) {
		Service rv = null;
		if (json == null)
			return rv;
		rv = new Service();
		if (json.has("id")) {
			String s = json.get("id").getAsString();
			if (!TextUtils.isEmpty(s))
				rv.setId(s);
		}
		if (json.has("name"))
			rv.setName(json.get("name").getAsString());
		if (json.has("enabled"))
			rv.setEnabled(json.get("enabled").getAsBoolean());
		if (json.has("hostID"))
			rv.setHostID(json.get("hostID").getAsString());
		if (json.has("managementPort"))
			rv.setManagementPort(json.get("managementPort").getAsInt());
		if (json.has("scheme"))
			rv.setScheme(json.get("scheme").getAsString());
		if (json.has("type"))
			rv.setType(ServiceType.valueOf(json.get("type").getAsString()));
		if (json.has("tags"))
			rv.setTags(jsonToTags(json.getAsJsonObject("tags")));
		return rv;
	}

	public JsonObject toJson(Service s) {
		JsonObject rv = null;
		if (s == null)
			return rv;
		rv = new JsonObject();
		rv.addProperty("id", s.getId() == null ? "" : s.getId());
		rv.addProperty("name", s.getName() == null ? "" : s.getName());
		rv.addProperty("enabled", s.getEnabled());
		rv.addProperty("hostID", s.getHostID() == null ? "" : s.getHostID());
		rv.addProperty("managementPort", s.getManagementPort());
		rv.addProperty("scheme", s.getScheme() == null ? "http" : s.getScheme());
		rv.addProperty("type", s.getType().toString());
		rv.add("tags", tagsToJson(s.getTags()));
		return rv;
	}
	
	private JsonObject tagsToJson(Map<String, String> tags) {
		JsonObject rv = null;
		if (tags == null)
			return rv;
		if (tags != null) {
			rv = new JsonObject();
			Set<String> keys = tags.keySet();
			for (String k: keys)
				rv.addProperty(k, tags.get(k));
		}
		return rv;
	}
	
	private Map<String, String> jsonToTags(JsonObject json) {
		Map<String, String> rv = new HashMap<String, String>();
		if (json == null)
			return rv;
		Set<Entry<String, JsonElement>> tags = json.entrySet();
		if (tags == null || tags.size() == 0)
			return rv;
		for (Entry<String, JsonElement> e: tags) {
			rv.put(e.getKey(), e.getValue().getAsString());
		}
		return rv;
	}

	public Host hostFromJson(String jsonStr) {
		if (TextUtils.isEmpty(jsonStr))
			return null;
		JsonElement e = parse(jsonStr);
		if (e == null)
			return null;
		return hostFromJson(e.getAsJsonObject());
	}
	
	public Host hostFromJson(JsonObject json) {
		Host rv = null;
		if (json == null)
			return rv;
		rv = new Host();
		if (json.has("id")) {
			String s = json.get("id").getAsString();
			if (!TextUtils.isEmpty(s))
				rv.setId(s);
		}
		if (json.has("name"))
			rv.setName(json.get("name").getAsString());
		return rv;
	}
	
	public JsonObject toJson(Host host) {
		JsonObject rv = null;
		if (host == null)
			return rv;
		rv = new JsonObject();
		rv.addProperty("id", host.getId() == null ? "" : host.getId());
		rv.addProperty("name", host.getName() == null ? "" : host.getName());
		return rv;
	}
	
	public String endpointFor(Object o) {
		String rv = null;
		if (o instanceof Topology) {
			rv = "topology/";
		}
		else if (o instanceof Group) {
			rv = "topology/groups/";
			rv += ((Group)o).getId(); 
		}
		else if (o instanceof Host) {
			rv = "topology/hosts/";
			rv += ((Host)o).getId(); 
		}
		else if (o instanceof Service) {
			rv = "topology/services/";
		}
		return rv;
	}
	
	public String prettyPrint(Topology topology) {
		StringBuilder sb = new StringBuilder();
		if (topology == null)
			return sb.toString();
		sb.append("\nID: ").append(topology.getId());
		sb.append("\nproductVersion: ").append(topology.getProductVersion());
		sb.append("\ntimestamp: ").append(topology.getTimestamp());
		sb.append("\nversion: ").append(topology.getVersion());
		Collection<Host> hosts = topology.getHosts();
		if (hosts != null && hosts.size() > 0) {
			sb.append("\n\nHosts: ");
			for (Host h: hosts) {
				sb.append("\n    ID: ").append(h.getId());
				sb.append("\n    name: ").append(h.getName());
			}
		}
		Collection<Group> grps = topology.getGroups();
		if (grps != null && grps.size() > 0) {
			sb.append("\n\nGroups: ");
			for (Group g: grps) {
				sb.append("\n    ID: ").append(g.getId());
				sb.append("\n    name: ").append(g.getName());
				Collection<Service> svcs = g.getServices();
				if (svcs != null && svcs.size() > 0) {
					sb.append("\n    Services: ");
					for (Service s: svcs) {
						sb.append("\n        ID: ").append(s.getId());
						sb.append("\n        name: ").append(s.getName());
						sb.append("\n        hostID: ").append(s.getHostID());
						sb.append("\n        mgmtPort: ").append(s.getManagementPort());
						sb.append("\n        scheme: ").append(s.getScheme());
						sb.append("\n        enabled: ").append(s.getEnabled());
						sb.append("\n        type: ").append(s.getType());
					}
				}
			}
		}
		return sb.toString();
	}
	
	public Service createNodeMgr(Host h, boolean ssl, int mgmtPort) {
		Service nmSvc = new Service();
		nmSvc.setHostID(h.getId());
		nmSvc.setType(ServiceType.nodemanager);
		nmSvc.setEnabled(true);
		nmSvc.setScheme(ssl ? Constants.HTTPS_SCHEME : Constants.HTTP_SCHEME);
		nmSvc.setManagementPort(mgmtPort);
		nmSvc.setName(EntityType.NodeManager.name() + "-" + Integer.toString(nmSvc.getManagementPort()));
		return nmSvc;
	}
	
	
	public Summary summaryFromJson(JsonObject json) {
		Summary rv = null;
		if (json == null)
			return rv;
		rv = new Summary();
		if (json.has("id")) {
			String s = json.get("id").getAsString();
			if (!TextUtils.isEmpty(s))
				rv.setId(s);
		}
		if (json.has("name"))
			rv.setName(json.get("name").getAsString());
		if (json.has("groupId"))
			rv.setGroupId(json.get("groupId").getAsString());
		if (json.has("groupName"))
			rv.setGroupName(json.get("groupName").getAsString());
		if (json.has("hostName"))
			rv.setHostName(json.get("hostName").getAsString());
		if (json.has("summaryMetrics"))
			rv.setSummaryMetrics(jsonToMetrics(json.getAsJsonObject("summaryMetrics")));
		return rv;
	}
	
	private HashMap<String, Object> jsonToMetrics(JsonObject json) {
		HashMap<String, Object> rv = new HashMap<String, Object>();
		if (json == null)
			return rv;
		Set<Entry<String, JsonElement>> mets = json.entrySet();
		if (mets == null || mets.size() == 0)
			return rv;
		for (Entry<String, JsonElement> e: mets) {
			rv.put(e.getKey(), e.getValue());
		}
		return rv;
	}
	
	public MetricGroup metricGroupFromJson(JsonObject json) {
		if (json == null)
			return null;
		MetricGroup rv = new MetricGroup();
		if (json.has("id"))
			rv.setId(json.get("id").getAsInt());
		if (json.has("name"))
			rv.setName(json.get("name").getAsString());
		if (json.has("type"))
			rv.setType(json.get("type").getAsString());
		if (json.has("parentId"))
			rv.setParentId(json.get("parentId").getAsInt());
		return rv;
	}

	private void loadBaseMetrics(BaseMetrics bm, JsonObject json) {
		if (json.has("gatewayGroupName"))
			bm.setGatewayGroupName(json.get("gatewayGroupName").getAsString());
		if (json.has("groupName"))
			bm.setGroupName(json.get("groupName").getAsString());
		if (json.has("groupType"))
			bm.setGroupType(json.get("groupType").getAsString());
		if (json.has("gatewayId"))
			bm.setGatewayId(json.get("gatewayId").getAsString());
		if (json.has("name"))
			bm.setName(json.get("name").getAsString());		
	}
	
	public SysOverview sysOverviewFromJson(JsonObject json) {
		if (json == null)
			return null;
		SysOverview rv = new SysOverview();
		loadBaseMetrics(rv, json);
		if (json.has("cpuUsed"))
			rv.setCpuUsed(json.get("cpuUsed").getAsLong());
		if (json.has("sysMemUsed"))
			rv.setSysMemUsed(json.get("sysMemUsed").getAsLong());
		if (json.has("sysMemTotal"))
			rv.setSysMemTotal(json.get("sysMemTotal").getAsLong());
		if (json.has("diskUsedPercent"))
			rv.setDiskUsedPercent(json.get("diskUsedPercent").getAsInt());
		if (json.has("exceptions"))
			rv.setExceptions(json.get("exceptions").getAsLong());
		if (json.has("failures"))
			rv.setFailures(json.get("failures").getAsLong());
		if (json.has("successes"))
			rv.setSuccesses(json.get("successes").getAsLong());
		if (json.has("uptime"))
			rv.setUptime(json.get("uptime").getAsLong());
		if (json.has("numSLABreaches"))
			rv.setNumSlaBreaches(json.get("numSLABreaches").getAsLong());
		if (json.has("numAlerts"))
			rv.setNumAlerts(json.get("numAlerts").getAsLong());
		if (json.has("numClients"))
			rv.setNumClients(json.get("numClients").getAsLong());
		if (json.has("cpuUsedMin") && json.has("cpuUsedMax") && json.has("cpuUsedAvg"))
			rv.setCpuUsedMma(json.get("cpuUsedMin").getAsLong(), json.get("cpuUsedMax").getAsLong(), json.get("cpuUsedAvg").getAsLong());
		if (json.has("memoryUsedMin") && json.has("memoryUsedMax") && json.has("memoryUsedAvg"))
			rv.setMemUsedMma(json.get("memoryUsedMin").getAsLong(), json.get("memoryUsedMax").getAsLong(), json.get("memoryUsedAvg").getAsLong());
		if (json.has("systemCpuMin") && json.has("systemCpuMax") && json.has("systemCpuAvg"))
			rv.setSysCpuMma(json.get("systemCpuMin").getAsLong(), json.get("systemCpuMax").getAsLong(), json.get("systemCpuAvg").getAsLong());
		return rv;
	}

	public String prettyPrint(SysOverview so) {
		StringBuilder sb = new StringBuilder();
		if (so == null)
			return sb.toString();
		basePrettyPrint(so, sb);
		sb.append("\nSuccesses: ").append(so.getSuccesses());
		sb.append("\nFailures: ").append(so.getFailures());
		sb.append("\nExceptions: ").append(so.getExceptions());
		
		sb.append("\n\nUptime: ").append(so.getUptime());
		sb.append("\nCPU Usage: ").append(so.getCpuUsed());
		sb.append("\nMemory: ").append(so.getSysMemUsed()).append("/").append(so.getSysMemTotal());
		sb.append("\nDisk Usage: ").append(so.getDiskUsedPercent());
		
		sb.append("\n\nSLA Breaches: ").append(so.getNumSlaBreaches());
		sb.append("\nAlerts: ").append(so.getNumAlerts());
		sb.append("\nClients: ").append(so.getNumClients());
		
		sb.append("\n\nCPU:\nMin\tMax\tAvg\n");		
		sb.append(so.getCpuUsedMma().getMin()).append("\t").append(so.getCpuUsedMma().getMax()).append("\t").append(so.getCpuUsedMma().getAvg());
		sb.append("\n\nMemory:\nMin\tMax\tAvg\n");		
		sb.append(so.getMemUsedMma().getMin()).append("\t").append(so.getMemUsedMma().getMax()).append("\t").append(so.getMemUsedMma().getAvg());
		sb.append("\n\nSystem CPU:\nMin\tMax\tAvg\n");		
		sb.append(so.getSysCpuMma().getMin()).append("\t").append(so.getSysCpuMma().getMax()).append("\t").append(so.getSysCpuMma().getAvg());
		return sb.toString();
	}
	
	public GroupMetrics groupMetricsFromJson(JsonObject json) {
		if (json == null)
			return null;
		GroupMetrics rv = new GroupMetrics();
		loadBaseMetrics(rv, json);
/*		
		private long[] respTimeRanges;
		private long[] respStatRanges;
*/		
		if (json.has("volumeBytesIn"))
			rv.setVolumeBytesIn(json.get("volumeBytesIn").getAsLong());
		if (json.has("volumeBytesOut"))
			rv.setVolumeBytesOut(json.get("volumeBytesOut").getAsLong());
		if (json.has("numReportedUps"))
			rv.setNumReportedUps(json.get("numReportedUps").getAsLong());
		if (json.has("numReportedDowns"))
			rv.setNumReportedDowns(json.get("numReportedDowns").getAsInt());
		if (json.has("numInConnections"))
			rv.setNumInConnections(json.get("numInConnections").getAsLong());
		if (json.has("numOutConnections"))
			rv.setNumOutConnections(json.get("numOutConnections").getAsLong());
		if (json.has("numTransactions"))
			rv.setNumTransactions(json.get("numTransactions").getAsLong());
		if (json.has("uptime"))
			rv.setUptime(json.get("uptime").getAsLong());
		if (json.has("respTimeMin") && json.has("respTimeMax") && json.has("respTimeAvg"))
			rv.setRespTimeMma(json.get("respTimeMin").getAsLong(), json.get("respTimeMax").getAsLong(), json.get("respTimeAvg").getAsLong());
		return rv;
	}

	private String basePrettyPrint(BaseMetrics bm, StringBuilder sb) {
		sb.append("Group Type: ").append(bm.getGroupType());
		sb.append("\nName: ").append(bm.getName());
		sb.append("\nGroup Name: ").append(bm.getGroupName());
		sb.append("\nGateway Group Name: ").append(bm.getGatewayGroupName());
		sb.append("\nGateway Id: ").append(bm.getGatewayId());
		return sb.toString();
	}
	
	public String prettyPrint(GroupMetrics gm) {
		StringBuilder sb = new StringBuilder();
		if (gm == null)
			return sb.toString();
		basePrettyPrint(gm, sb);
		sb.append("\nConnections: ");
		sb.append("\nIn: ").append(gm.getNumInConnections());
		sb.append(", Out: ").append(gm.getNumOutConnections());
		
		sb.append("\n\nReported Ups: ").append(gm.getNumReportedUps());
		sb.append("\nReported Downs: ").append(gm.getNumReportedDowns());
		sb.append("\nTransactions: ").append(gm.getNumTransactions());
		sb.append("\nUptime: ").append(gm.getUptime());
		
		sb.append("\n\nVolume: ");
		sb.append("\nBytes In: ").append(gm.getVolumeBytesIn());
		sb.append(", Bytes Out: ").append(gm.getVolumeBytesOut());
		return sb.toString();
	}
	
	public String prettyPrint(Summary s) {
		StringBuilder sb = new StringBuilder();
		if (s == null)
			return sb.toString();
		sb.append("Id: ").append(s.getId());
		sb.append("\nName: ").append(s.getName());
		sb.append("\nGroup Id: ").append(s.getGroupId());
		sb.append("\nGroup Name: ").append(s.getGroupName());
		sb.append("\nHost Name: ").append(s.getHostName());
		if (s.getSummaryMetrics() != null) {
			sb.append("\nMetrics:");
			for (Map.Entry<String, Object> e: s.getSummaryMetrics().entrySet()) {
				Object o = e.getValue();
				sb.append("\n    ").append(e.getKey()).append(": ").append(o == null ? "null": o.toString());
			}
		}
		return sb.toString();
	}
	/**
	 * Get a JSON object from a url; MUST be called in a separate thread from the UI (AsyncTask, etc)
	 * @param restTmpl
	 * @param url
	 * @return
	 */
	public JsonElement getJsonFromUrl(RestTemplate restTmpl, String url) throws ApiException {
		return getJsonFromUrl(restTmpl, url, null, null);
	}
	
	public JsonElement getJsonFromUrl(RestTemplate restTmpl, String url, String user, String passwd) throws ApiException {
		JsonElement rv = null;
		HttpHeaders reqHdrs = new HttpHeaders();
		if (user != null) {
			HttpAuthentication authHdr = new HttpBasicAuthentication(user, passwd);
			reqHdrs.setAuthorization(authHdr);
		}
		HttpEntity<?> reqEntity = new HttpEntity<String>("", reqHdrs);
		int sc = 0;
		StringBuilder sb = new StringBuilder();
		sb.append(HttpMethod.GET).append(" ").append(url);
		Log.d(TAG, sb.toString());
		try {
			ResponseEntity<String> resp = restTmpl.exchange(url,  HttpMethod.GET, reqEntity, String.class);
			sc = resp.getStatusCode().value();
			Log.d(TAG, "response status code: " + Integer.toString(sc));
			if (sc == HttpStatus.SC_OK) {
				JsonElement jsonResp = parse(resp.getBody());
				if (jsonResp != null) {
					JsonObject jo = jsonResp.getAsJsonObject();
					if (jo.has("result")) {
						rv = jo.get("result");
//						if (je.isJsonArray())
//							rv = je.getAsJsonArray("result");
//						else
//							rv = je.getAsJsonObject("result");
					}
					else if (jo.has("errors")) {
						ApiException excp = new ApiException(jo.getAsJsonArray("errors"));
						throw excp;
					}
					else
						rv = jo;
				}
			}
		}
		catch (ResourceAccessException e) {
			throw new ApiException(e);
		}
		catch (HttpClientErrorException e) {
			switch (e.getStatusCode().value()) {
				case HttpStatus.SC_UNAUTHORIZED:
				case HttpStatus.SC_NOT_FOUND:
				case HttpStatus.SC_FORBIDDEN:
					throw new ApiException(e.getStatusCode().value());
				default:
					throw new ApiException(e);
			}
		}
		catch (Exception e) {
			throw new ApiException(e);
		}
		if (rv != null)
			Log.d(TAG, "jsonElement retrieved: " + rv.toString());
		return rv;
	}
/*	
	public Model modelFromJson(JsonObject json) {
		Model rv = null;
		if (json == null)
			return rv;
		rv = new Model();
		if (json.has("types")) {
			JsonArray types = json.get("types").getAsJsonArray();
			for (int i = 0; i < types.size(); i++) {
				rv.addType(typeFromJson(types.get(i).getAsJsonObject()));
			}
		}
		return rv;
	}
*/	
	
	public KpsType typeFromJson(JsonObject json) {
		if (json == null)
			return null;
		KpsType rv = new KpsType();
		if (json.has("identity"))
			rv.setIdentity(json.get("identity").getAsString());
		if (json.has("description")) {
			JsonElement je = json.get("description");
			if (!je.isJsonNull())
				rv.setDescription(json.get("description").getAsString());
		}
		if (json.has("properties")) {
			JsonObject jo = json.get("properties").getAsJsonObject();
			Set<Map.Entry<String, JsonElement>> props = jo.entrySet();
			for (Map.Entry<String, JsonElement> e: props) {
				rv.addProperty(e.getKey(), e.getValue().getAsString());
			}
		}
		return rv;
	}
	
	public KpsStore storeFromJson(JsonObject json) {
		if (json == null)
			return null;
		KpsStore rv = new KpsStore();
		if (json.has("identity"))
			rv.setIdentity(json.get("identity").getAsString());
		if (json.has("description") && !json.isJsonNull()) {
			rv.setDescription(json.get("description").getAsString());
		}
		if (json.has("typeId"))
			rv.setTypeId(json.get("typeId").getAsString());
		if (json.has("implId"))
			rv.setImplId(json.get("implId").getAsString());
		if (json.has("config")) {
			JsonObject jo = json.get("config").getAsJsonObject();
			Set<Map.Entry<String, JsonElement>> props = jo.entrySet();
			for (Map.Entry<String, JsonElement> e: props) {
				if (e.getKey().equals("internal"))
					rv.addProperty("internal", e.getValue().getAsBoolean());
			}
		}
		return rv;
	}
	
	public String prettyPrint(KpsType kpsType) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n    Identity: ").append(kpsType.getIdentity());
		sb.append("\n    Description: ").append(kpsType.getDescription());
		sb.append("\n    Properties: ");
		return sb.toString();
	}
}
