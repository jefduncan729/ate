package com.axway.ate.service;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.axway.ate.ApiException;
import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.R;
import com.axway.ate.ServerInfo;
import com.axway.ate.activity.TopologyActivity;
import com.axway.ate.rest.DefaultRestTemplate;
import com.axway.ate.rest.SslRestTemplate;
import com.axway.ate.ssl.TrustedHttpRequestFactory;
import com.axway.ate.util.UiUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Topology.EntityType;

public class RestService extends BaseIntentService {
	
	private static final String TAG = RestService.class.getSimpleName();

	public static final String ACTION_BASE = "com.axway.ate.";
	public static final String ACTION_CHECK_CERT = ACTION_BASE + "check_cert";
	public static final String ACTION_REMOVE_TRUST_STORE = ACTION_BASE + "remove_trust_store";
	
	private ServerInfo srvrInfo;
	private DomainHelper helper;
	private boolean trustCert;
	private String action;

	public RestService() {
		super(TAG);
		srvrInfo = null;
		trustCert = false;
		helper = DomainHelper.getInstance();
		action = null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		if (intent == null || intent.getExtras() == null)
			return;
		action = intent.getAction();
		Bundle extras = intent.getExtras();
		if (ACTION_REMOVE_TRUST_STORE.equals(action)) {
			removeTrustStore();
			return;
		}
		srvrInfo = ServerInfo.fromBundle(extras.getBundle(Constants.EXTRA_SERVER_INFO));
		if (srvrInfo == null) {
			Log.e(TAG, "no server info passed");
			return;
		}
		HttpMethod method = HttpMethod.GET;
		if (ACTION_CHECK_CERT.equals(action)) {
			trustCert = extras.getBoolean(Constants.EXTRA_TRUST_CERT, false);
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
				resp = doUpdate(makeUrl(eType, json, method, extras), json, method);
			}
			else if (HttpMethod.GET == method) {
				String url = extras.getString(Constants.EXTRA_URL);
				if (TextUtils.isEmpty(url)) {
					Log.e(TAG, "no url passed");
					return;
				}
				resp = doGet(url);
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
			if (ACTION_CHECK_CERT.equals(action))
				showToast(getString(R.string.cert_trusted));
			if (resp != null)
				extras.putString(Constants.EXTRA_JSON_ITEM, resp.toString());
			if (getResultReceiver() != null)
				getResultReceiver().send(HttpStatus.SC_OK, extras);
		}
		else {
			int sc = 0;
			CertPath cp = isCertValidationErr(excp);
			if (cp == null) {
				sc = excp.getStatusCode();
				if (sc == 0)
					sc = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				extras.putString(Intent.EXTRA_BUG_REPORT, excp.getLocalizedMessage());
			}
			else {
				Log.d(TAG, "certPath not trusted: " + cp.toString());
				sc = Constants.CERT_NOT_TRUSTED;
				if (trustCert) {
					Log.d(TAG, "adding to trust store");
					try {
						TrustedHttpRequestFactory.trustCerts(this, cp);
						showToast(getString(R.string.cert_trusted));
						sc = HttpStatus.SC_OK;
					} 
					catch (KeyStoreException e) {
						Log.e(TAG, e.getLocalizedMessage(), e);
					} 
					catch (NoSuchAlgorithmException e) {
						Log.e(TAG, e.getLocalizedMessage(), e);
					} 
					catch (CertificateException e) {
						Log.e(TAG, e.getLocalizedMessage(), e);
					} 
					catch (IOException e) {
						Log.e(TAG, e.getLocalizedMessage(), e);
					}
				}
				if (sc == Constants.CERT_NOT_TRUSTED) {
					StringBuilder sb = new StringBuilder();
					int i = 1;
				    for (Certificate c: cp.getCertificates()) {
				    	if (c.getType() == "X.509") {
				    		X509Certificate c509 = (X509Certificate)c;
				    		sb.append("[").append(i++).append("]: ").append(c509.getSubjectDN().toString()).append("\n");
				    	}
				    }
					extras.putString(Intent.EXTRA_BUG_REPORT, sb.toString());
				}
				if (getResultReceiver() == null) {
					showToast(getString(sc == Constants.CERT_NOT_TRUSTED ? R.string.cert_not_trusted : R.string.cert_trusted));
					return;
				}
			}
			if (getResultReceiver() != null)
				getResultReceiver().send(sc, extras);
		}
	}

	private CertPath isCertValidationErr(Exception e) {
		CertPath rv = null;
		CertPathValidatorException cpve = null;
		Throwable cause = e.getCause();
		while (cpve == null && cause != null) {
			if (cause instanceof CertPathValidatorException) {
				cpve = (CertPathValidatorException)cause;
			}
			else
				cause = cause.getCause();
		}
		if (cpve != null)
			rv = cpve.getCertPath();
		return rv;
	}

	private String makeUrl(EntityType eType, JsonObject json, HttpMethod method, Bundle extras) {
		String[] params = null;
		String endpoint = null;
		String id = json.get("id").getAsString();
		StringBuilder qStr = new StringBuilder();
		boolean delFromDisk = extras.getBoolean(Constants.EXTRA_DELETE_FROM_DISK, false); 
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
		else if (eType == EntityType.Gateway) {
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
			if (method == HttpMethod.POST) {
				int sp = extras.getInt(Constants.EXTRA_SERVICES_PORT, 8080);
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
	
	private JsonObject doUpdate(String url, JsonObject json, HttpMethod method) throws ApiException {
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
		sb.append(HttpMethod.GET).append(" ").append(url).append(" ").append(url);
		Log.d(TAG, sb.toString());
		try {
			ResponseEntity<String> resp = getRestTemplate(srvrInfo).exchange(url,  HttpMethod.GET, reqEntity, String.class);
			sc = resp.getStatusCode().value();
			Log.d(TAG, "response status code: " + Integer.toString(sc));
			if (sc == HttpStatus.SC_OK || sc == HttpStatus.SC_CREATED || sc == HttpStatus.SC_NO_CONTENT) {
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

	private void removeTrustStore() {
		File f = new File(getFilesDir(), TrustedHttpRequestFactory.TRUST_STORE_FNAME);
		if (f.exists()) {
			f.delete();
			showToast(getString(R.string.truststore_removed));
		}
		else
			showToast(getString(R.string.truststore_notfound));
	}
}
