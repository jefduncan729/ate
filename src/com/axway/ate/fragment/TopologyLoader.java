package com.axway.ate.fragment;

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

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.axway.ate.ApiException;
import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.ServerInfo;
import com.axway.ate.rest.DefaultRestTemplate;
import com.axway.ate.rest.SslRestTemplate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Topology;

public class TopologyLoader extends AsyncTaskLoader<Topology>{

	private static final String TAG = TopologyLoader.class.getSimpleName();

	private Topology topology;
	private Bundle args;
	
	public TopologyLoader(Context context, Bundle args) {
		super(context);
		topology = null;
		this.args = args;
	}

	@Override
	public Topology loadInBackground() {
		Topology rv = null;
		if (args == null)
			return rv;
		try {
			rv = performLoad();
		}
		catch (ApiException excp) {
			Log.e(TAG, excp.getLocalizedMessage(), excp);
		}
		return rv;
	}
	
	private Topology performLoad() {
		Topology rv = null;
		ServerInfo info = ServerInfo.fromBundle(args.getBundle(Constants.EXTRA_SERVER_INFO));
		if (info == null)
			return rv;
		String url = info.buildUrl("topology");
		RestTemplate restTmpl;
		if (info.isSsl())
			restTmpl = SslRestTemplate.getInstance(getContext());
		else
			restTmpl = DefaultRestTemplate.getInstance();
		HttpAuthentication authHdr = new HttpBasicAuthentication(info.getUser(), info.getPasswd());
		HttpHeaders reqHdrs = new HttpHeaders();
		reqHdrs.setAuthorization(authHdr);
		HttpEntity<?> reqEntity = new HttpEntity<String>("", reqHdrs);
		JsonObject jsonResult = null;
		int sc = 0;
		StringBuilder sb = new StringBuilder();
		sb.append(HttpMethod.GET).append(" ").append(url).append(" ").append(url);
		Log.d(TAG, sb.toString());
		try {
			ResponseEntity<String> resp = restTmpl.exchange(url,  HttpMethod.GET, reqEntity, String.class);
			sc = resp.getStatusCode().value();
			Log.d(TAG, "response status code: " + Integer.toString(sc));
			if (sc == HttpStatus.SC_OK) {
				JsonElement jsonResp = DomainHelper.getInstance().parse(resp.getBody());
				if (jsonResp != null) {
					JsonObject jo = jsonResp.getAsJsonObject();
					if (jo.has("result")) {
						jsonResult = jo.getAsJsonObject("result");
						rv = DomainHelper.getInstance().topologyFromJson(jsonResult);
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

	@Override
	protected void onStartLoading() {
		if (topology == null)
			forceLoad();
		else
			deliverResult(topology);
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(Topology data) {
		topology = data;
		if (isStarted())
			super.deliverResult(data);
	}
}
