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
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.text.TextUtils;
import android.util.Log;

import com.axway.ate.util.UiUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
}
