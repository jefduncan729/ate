package com.axway.ate.service;

import java.util.Random;

import org.apache.http.HttpStatus;
import org.springframework.http.HttpAuthentication;
import org.springframework.http.HttpBasicAuthentication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.axway.ate.ApiException;
import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.ServerInfo;
import com.axway.ate.rest.DefaultRestTemplate;
import com.axway.ate.rest.SslRestTemplate;
import com.axway.ate.util.ChartUrlBuilder;
import com.axway.ate.util.TopologyCompareResults;
import com.axway.ate.util.TopologyComparer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Service;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

public class RestService extends BaseIntentService {
	
	private static final String TAG = RestService.class.getSimpleName();

	public static final String ACTION_MOVE_GATEWAY = ACTION_BASE + "move_gateway";
	public static final String ACTION_COMPARE = ACTION_BASE + "compare";
	public static final String ACTION_GATEWAY_STATUS = ACTION_BASE + "gateway_status";
	public static final String ACTION_CREATE_CHART = ACTION_BASE + "chart";

	public static final String TOPOLOGY_ENDPOINT = "topology";
	public static final String HOSTS_ENDPOINT = TOPOLOGY_ENDPOINT + "/hosts";
	public static final String GROUPS_ENDPOINT = TOPOLOGY_ENDPOINT + "/groups";
	public static final String SERVICES_ENDPOINT = TOPOLOGY_ENDPOINT + "/services";
	
	private ServerInfo srvrInfo;
	private DomainHelper helper;
	private String action;
	private boolean movingSvc;
	private JsonObject svcToMove;
	private String fromGrpId;
	private String toGrpId;
	
	public RestService() {
		super(TAG);
		srvrInfo = null;
		helper = DomainHelper.getInstance();
		action = null;
		svcToMove = null;
		fromGrpId = null;
		toGrpId = null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		if (intent == null || intent.getExtras() == null)
			return;
		action = intent.getAction();
		Bundle extras = intent.getExtras();
		srvrInfo = ServerInfo.fromBundle(extras.getBundle(Constants.EXTRA_SERVER_INFO));
		if (srvrInfo == null) {
			Log.e(TAG, "no server info passed");
			return;
		}
		if (ACTION_COMPARE.equals(action)) {
			doCompare(extras);
			return;
		}
		if (ACTION_GATEWAY_STATUS.equals(action)) {
			doGatewayStatus(extras);
			return;
		}
		if (ACTION_CREATE_CHART.equals(action)) {
			doCreateChart(extras);
			return;
		}
		HttpMethod method = null;
		if (ACTION_MOVE_GATEWAY.equals(action)) {
			movingSvc = true;
			fromGrpId = extras.getString(Constants.EXTRA_FROM_GROUP);
			toGrpId = extras.getString(Constants.EXTRA_TO_GROUP);
			if (TextUtils.isEmpty(fromGrpId) || TextUtils.isEmpty(toGrpId)) {
				Log.e(TAG, "expecting fromGrp and toGrp");
				return;
			}
			extras.putString(Constants.EXTRA_REFERRING_ITEM_ID, fromGrpId);
			extras.putBoolean(Constants.EXTRA_DELETE_FROM_DISK, false);
			method = HttpMethod.DELETE;
		}
		else
			method = HttpMethod.valueOf(action);
		ApiException excp = null;
		JsonObject resp = null;
		try {
			if (HttpMethod.POST == method || HttpMethod.PUT == method || HttpMethod.DELETE == method) {
				String s = extras.getString(Constants.EXTRA_ITEM_TYPE);
				if (TextUtils.isEmpty(s)) {
					Log.e(TAG, "no entity type passed");
					return;
				}
				EntityType eType = EntityType.valueOf(s);
				s = extras.getString(Constants.EXTRA_JSON_ITEM);
				if (TextUtils.isEmpty(s)) {
					Log.e(TAG, "no json data passed");
					return;
				}
				JsonObject json = null;
				JsonElement je = helper.parse(s);
				if (je == null) {
					Log.e(TAG, "json parsing error");
					return;
				}
				json = je.getAsJsonObject();
				if (movingSvc)
					svcToMove = json;
				resp = doUpdate(eType, json, method, extras);
			}
		}
		catch (ApiException e) {
			excp = e;
			String msg = e.getLocalizedMessage();
			if (TextUtils.isEmpty(msg))
				msg = "unknown exception";
			Log.e(TAG, msg, e);
		}
		extras.putString(Constants.EXTRA_ACTION, action);
		if (excp == null) {
			if (resp != null)
				extras.putString(Constants.EXTRA_JSON_ITEM, resp.toString());
			if (getResultReceiver() != null)
				getResultReceiver().send(HttpStatus.SC_OK, extras);
		}
		else {
			int sc = excp.getStatusCode();
			if (sc == 0)
				sc = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			extras.putString(Intent.EXTRA_BUG_REPORT, excp.getLocalizedMessage());
			if (getResultReceiver() != null)
				getResultReceiver().send(sc, extras);
		}
	}

	private String makeUrl(EntityType eType, JsonObject json, HttpMethod method, Bundle extras) {
		String[] params = null;
		String endpoint = null;
		String id = json.get("id").getAsString();
		StringBuilder qStr = new StringBuilder();
		boolean delFromDisk = false;
		if (extras != null)
			delFromDisk = extras.getBoolean(Constants.EXTRA_DELETE_FROM_DISK, false); 
		if (eType == EntityType.Host) {
			if (method == HttpMethod.DELETE) {
				params = new String[1];
				params[0] = id;
			}
			endpoint = HOSTS_ENDPOINT;
		}
		else if (eType == EntityType.Group) {
			if (method == HttpMethod.DELETE) {
				params = new String[1];
				params[0] = id;
				if (delFromDisk) {
					qStr.append("deleteDiskGroup=true");
				}
			}
			endpoint = GROUPS_ENDPOINT;
		}
		else if (eType == EntityType.Gateway || eType == EntityType.NodeManager) {
			String grpId = extras.getString(Constants.EXTRA_REFERRING_ITEM_ID);
			if (TextUtils.isEmpty(grpId))
				throw new IllegalStateException("expecting to find group for service: " + id);
			endpoint = SERVICES_ENDPOINT;
			if (method == HttpMethod.DELETE) {
				params = new String[2];
				params[0] = grpId;
				params[1] = id;
				if (delFromDisk) {
					qStr.append("deleteDiskInstance=true");
				}
			}
			else {
				params = new String[1];
				params[0] = grpId;
			}
			if (method == HttpMethod.POST && eType == EntityType.Gateway) {
				int sp = 0;
				if (extras != null)
					sp = extras.getInt(Constants.EXTRA_SERVICES_PORT, 8080);
				qStr.append("servicesPort=" + Integer.toString(sp));
			}
		}
		String rv = srvrInfo.buildUrl(endpoint, params);
		if (qStr.length() > 0)
			rv = rv + "?" + qStr.toString();
		return rv;
	}
	
	private RestTemplate getRestTemplate(ServerInfo info) {
		RestTemplate rv;
		if (info.isSsl())
			rv = SslRestTemplate.getInstance(this);
		else
			rv = DefaultRestTemplate.getInstance();
		return rv;
	}
	
	private JsonObject doUpdate(EntityType eType, JsonObject json, HttpMethod method, Bundle extras) throws ApiException {
		String url = makeUrl(eType, json, method, extras);
		HttpAuthentication authHdr = new HttpBasicAuthentication(srvrInfo.getUser(), srvrInfo.getPasswd());
		HttpHeaders reqHdrs = new HttpHeaders();
		reqHdrs.setAuthorization(authHdr);
		HttpEntity<?> reqEntity = null;
		JsonObject rv = null;
		int sc = 0;
		if (method == HttpMethod.DELETE) {
			reqEntity = new HttpEntity<String>("", reqHdrs);
		}
		else {
			reqHdrs.setContentType(MediaType.APPLICATION_JSON);
			reqEntity = new HttpEntity<String>(json.toString(), reqHdrs);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(method).append(" ").append(url).append(" ").append(json.toString());
		Log.d(TAG, sb.toString());
		try {
			ResponseEntity<String> resp = getRestTemplate(srvrInfo).exchange(url,  method, reqEntity, String.class);
			sc = resp.getStatusCode().value();
			Log.d(TAG, "response status code: " + Integer.toString(sc));
			if (sc == HttpStatus.SC_OK || sc == HttpStatus.SC_CREATED || sc == HttpStatus.SC_NO_CONTENT) {
				if (sc == HttpStatus.SC_NO_CONTENT && movingSvc && svcToMove != null && toGrpId != null) {
					Bundle ex = new Bundle();
					ex.putString(Constants.EXTRA_REFERRING_ITEM_ID, toGrpId);
					movingSvc = false;
					doUpdate(EntityType.Gateway, svcToMove, HttpMethod.POST, ex);
				}
				else {
					JsonElement jsonResp = helper.parse(resp.getBody());
					if (jsonResp != null) {
						JsonObject jo = jsonResp.getAsJsonObject();
						if (jo.has("result")) {
							rv = jo.getAsJsonObject("result");
						}
						else if (jo.has("errors")) {
							ApiException excp = new ApiException(jo.getAsJsonArray("errors"));
							throw excp;
						}
					}
				}
			}
		}
		catch (ResourceAccessException e) {
			throw new ApiException(e);
		}
		catch (HttpClientErrorException e) {
			switch (e.getStatusCode().value()) {
				case HttpStatus.SC_BAD_REQUEST:
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
		return rv;
	}

	private JsonObject doGet(String url) throws ApiException {
		HttpAuthentication authHdr = new HttpBasicAuthentication(srvrInfo.getUser(), srvrInfo.getPasswd());
		HttpHeaders reqHdrs = new HttpHeaders();
		reqHdrs.setAuthorization(authHdr);
		HttpEntity<?> reqEntity = new HttpEntity<String>("", reqHdrs);
		JsonObject rv = null;
		int sc = 0;
		StringBuilder sb = new StringBuilder();
		sb.append(HttpMethod.GET).append(" ").append(url);
		Log.d(TAG, sb.toString());
		try {
			ResponseEntity<String> resp = getRestTemplate(srvrInfo).exchange(url,  HttpMethod.GET, reqEntity, String.class);
			sc = resp.getStatusCode().value();
			Log.d(TAG, "response status code: " + Integer.toString(sc));
			if (sc == HttpStatus.SC_OK) {
				JsonElement jsonResp = helper.parse(resp.getBody());
				if (jsonResp != null) {
					JsonObject jo = jsonResp.getAsJsonObject();
					if (jo.has("result")) {
						rv = jo.getAsJsonObject("result");
					}
					else if (jo.has("errors")) {
						ApiException excp = new ApiException(jo.getAsJsonArray("errors"));
						throw excp;
					}
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
		return rv;
	}
	
	private void doCompare(Bundle data) {
		if (data == null)
			return;		
		String jstr = data.getString(Constants.EXTRA_JSON_TOPOLOGY);
		if (TextUtils.isEmpty(jstr))
			return;
		Topology cliTopo = helper.topologyFromJson(jstr);
		Topology srvrTopo = null;
		if (cliTopo == null)
			return;
		JsonObject json = null;
		try {
			json = doGet(srvrInfo.buildUrl(TOPOLOGY_ENDPOINT));
		}
		catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
			return;
		}
		srvrTopo = helper.topologyFromJson(json);
		if (srvrTopo == null)
			return;
		TopologyCompareResults rv = null;
		try {
			TopologyComparer comparer = new TopologyComparer(srvrTopo, cliTopo);
			rv = comparer.compare();
		}
		catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
		if (rv == null)
			return;
		if (getResultReceiver() != null) {
			data.putString(Constants.EXTRA_ACTION, ACTION_COMPARE);
			data.putString(Constants.EXTRA_COMPARE_RESULT, rv.prettyPrint());
			getResultReceiver().send(HttpStatus.SC_OK, data);
		}
	}
	
	private void doGatewayStatus(Bundle data) {
		String jstr = data.getString(Constants.EXTRA_JSON_TOPOLOGY);
		if (TextUtils.isEmpty(jstr))
			return;
		Topology t = helper.topologyFromJson(jstr);
		if (t == null)
			return;
		Service s = t.getService(data.getString(Constants.EXTRA_ITEM_ID));
		if (s == null)
			return;
		Host h = t.getHost(s.getHostID());
		if (h == null)
			return;
		HttpAuthentication authHdr = new HttpBasicAuthentication(srvrInfo.getUser(), srvrInfo.getPasswd());
		HttpHeaders reqHdrs = new HttpHeaders();
		reqHdrs.setAuthorization(authHdr);
		HttpEntity<?> reqEntity = new HttpEntity<String>("", reqHdrs);
		int sc = 0;
		StringBuilder url = new StringBuilder();
		url.append(s.getScheme()).append("://").append(h.getName()).append(":").append(s.getManagementPort());
		
		StringBuilder sb = new StringBuilder();
		sb.append(HttpMethod.GET).append(" ").append(url.toString());
		Log.d(TAG, sb.toString());
		boolean running = false;
		try {
			ResponseEntity<String> resp = getRestTemplate(srvrInfo).exchange(url.toString(),  HttpMethod.GET, reqEntity, String.class);
			sc = resp.getStatusCode().value();
			Log.d(TAG, "response status code: " + Integer.toString(sc));
			if (sc == HttpStatus.SC_OK || sc == HttpStatus.SC_NOT_FOUND) 
				running = true;
		}
		catch (Exception e) {
			running = false;
		}
		String msg = s.getName() + " is" + (running ? " " : " not ") + "running";
		showToast(msg);
	}
	
	private String doCreateChart(Bundle extras) {
		ChartUrlBuilder bldr = new ChartUrlBuilder();
		bldr.setChartTitle("System Overview")
			.setChartType(ChartUrlBuilder.LINE_CHART)
			.setHeight(300)
			.setWidth(600)
			.setMargins(5,5,10,10);
		Random rand = new Random(System.currentTimeMillis());
		ChartUrlBuilder.Axis yAxis = bldr.addAxis(ChartUrlBuilder.AXIS_LEFT);
		yAxis.addLabel("count");
		yAxis.setStart(0);
		yAxis.setEnd(200);
		ChartUrlBuilder.DataSet dss = null;
		dss = bldr.addDataSet();
		dss.setLegend("successes");
		dss.setColor(Color.GREEN);
		for (int i = 0; i < 120; i++)
			dss.addValue(Math.abs(rand.nextInt(200)));
		ChartUrlBuilder.DataSet dsf = null;
		dsf = bldr.addDataSet();
		dsf.setLegend("failures");
		dsf.setColor(Color.RED);
		for (int i = 0; i < 120; i++)
			dsf.addValue(Math.abs(rand.nextInt(200)));
		String rv = bldr.build();
		Log.d(TAG, "chart url: " + rv);
		return rv;
	}
}
