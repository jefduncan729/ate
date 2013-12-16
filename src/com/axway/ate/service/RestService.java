package com.axway.ate.service;

import java.security.cert.CertPath;

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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.axway.ate.ApiException;
import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.ServerInfo;
import com.axway.ate.rest.DefaultRestTemplate;
import com.axway.ate.rest.SslRestTemplate;
import com.axway.ate.util.TopologyCompareResults;
import com.axway.ate.util.TopologyComparer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

public class RestService extends BaseIntentService {
	
	private static final String TAG = RestService.class.getSimpleName();

	public static final String ACTION_MOVE_GATEWAY = ACTION_BASE + "move_gateway";
	public static final String ACTION_COMPARE = ACTION_BASE + "compare";

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
//			int sc = 0;
//			CertPath cp = helper.isCertValidationErr(excp);
//			if (cp == null) {
			int sc = excp.getStatusCode();
			if (sc == 0)
				sc = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			extras.putString(Intent.EXTRA_BUG_REPORT, excp.getLocalizedMessage());
//			else {
//				Log.d(TAG, "certPath not trusted: " + cp.toString());
//				sc = Constants.CERT_NOT_TRUSTED;
//				if (trustCert) {
//					Log.d(TAG, "adding to trust store");
//					try {
//						TrustedHttpRequestFactory.trustCerts(this, cp);
//						showToast(getString(R.string.cert_trusted));
//						sc = HttpStatus.SC_OK;
//					} 
//					catch (KeyStoreException e) {
//						Log.e(TAG, e.getLocalizedMessage(), e);
//					} 
//					catch (NoSuchAlgorithmException e) {
//						Log.e(TAG, e.getLocalizedMessage(), e);
//					} 
//					catch (CertificateException e) {
//						Log.e(TAG, e.getLocalizedMessage(), e);
//					} 
//					catch (IOException e) {
//						Log.e(TAG, e.getLocalizedMessage(), e);
//					}
//				}
//				if (sc == Constants.CERT_NOT_TRUSTED) {
//					StringBuilder sb = new StringBuilder();
//					int i = 1;
//				    for (Certificate c: cp.getCertificates()) {
//				    	if (c.getType() == "X.509") {
//				    		X509Certificate c509 = (X509Certificate)c;
//				    		sb.append("[").append(i++).append("]: ").append(c509.getSubjectDN().toString()).append("\n");
//				    	}
//				    }
//					extras.putString(Intent.EXTRA_BUG_REPORT, sb.toString());
//				}
//				if (getResultReceiver() == null) {
//					showToast(getString(sc == Constants.CERT_NOT_TRUSTED ? R.string.cert_not_trusted : R.string.cert_trusted));
//					return;
//				}
//			}
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
			endpoint = "topology/hosts";
		}
		else if (eType == EntityType.Group) {
			if (method == HttpMethod.DELETE) {
				params = new String[1];
				params[0] = id;
				if (delFromDisk) {
					qStr.append("deleteDiskGroup=true");
				}
			}
			endpoint = "topology/groups";
		}
		else if (eType == EntityType.Gateway || eType == EntityType.NodeManager) {
			String grpId = extras.getString(Constants.EXTRA_REFERRING_ITEM_ID);
			if (TextUtils.isEmpty(grpId))
				throw new IllegalStateException("expecting to find group for service: " + id);
			endpoint = "topology/services";
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
//	
//	private JsonObject doGet(String url) throws ApiException {
//		HttpAuthentication authHdr = new HttpBasicAuthentication(srvrInfo.getUser(), srvrInfo.getPasswd());
//		HttpHeaders reqHdrs = new HttpHeaders();
//		reqHdrs.setAuthorization(authHdr);
//		HttpEntity<?> reqEntity = new HttpEntity<String>("", reqHdrs);
//		JsonObject rv = null;
//		int sc = 0;
//		StringBuilder sb = new StringBuilder();
//		sb.append(HttpMethod.GET).append(" ").append(url).append(" ").append(url);
//		Log.d(TAG, sb.toString());
//		try {
//			ResponseEntity<String> resp = getRestTemplate(srvrInfo).exchange(url,  HttpMethod.GET, reqEntity, String.class);
//			sc = resp.getStatusCode().value();
//			Log.d(TAG, "response status code: " + Integer.toString(sc));
//			if (sc == HttpStatus.SC_OK) {
//				JsonElement jsonResp = helper.parse(resp.getBody());
//				if (jsonResp != null) {
//					JsonObject jo = jsonResp.getAsJsonObject();
//					if (jo.has("result")) {
//						rv = jo.getAsJsonObject("result");
//					}
//					else if (jo.has("errors")) {
//						ApiException excp = new ApiException(jo.getAsJsonArray("errors"));
//						throw excp;
//					}
//				}
//			}
//		}
//		catch (ResourceAccessException e) {
//			throw new ApiException(e);
//		}
//		catch (HttpClientErrorException e) {
//			switch (e.getStatusCode().value()) {
//				case HttpStatus.SC_UNAUTHORIZED:
//				case HttpStatus.SC_NOT_FOUND:
//				case HttpStatus.SC_FORBIDDEN:
//					throw new ApiException(e.getStatusCode().value());
//				default:
//					throw new ApiException(e);
//			}
//		}
//		catch (Exception e) {
//			throw new ApiException(e);
//		}
//		return rv;
//	}
	
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
			json = null;	//doGet(srvrInfo.buildUrl("topology"));
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
}
