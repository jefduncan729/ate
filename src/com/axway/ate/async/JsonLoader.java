package com.axway.ate.async;

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

public class JsonLoader extends AsyncTaskLoader<JsonElement>{

	private static final String TAG = JsonLoader.class.getSimpleName();

	private JsonElement jsonObj;
	private Bundle args;
	private ExceptionHandler excpHandler;
	
	public interface ExceptionHandler {
		public void onLoaderException(ApiException e);
	}
	
	public JsonLoader(Context context, Bundle args) {
		super(context);
		jsonObj = null;
		excpHandler = null;
		this.args = args;
	}

	public void setExceptionHandler(ExceptionHandler newVal) {
		excpHandler = newVal;
	}
	
	@Override
	public JsonElement loadInBackground() {
		JsonElement rv = null;
		if (args == null)
			return rv;
		try {
			ServerInfo info = ServerInfo.fromBundle(args.getBundle(Constants.EXTRA_SERVER_INFO));
			if (info == null)
				Log.e(TAG, "no serverinfo");
			else
				rv = loadFromServer(info);
		}
		catch (ApiException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
			if (excpHandler != null)
				excpHandler.onLoaderException(e);
		}
		return rv;
	}
	
	private JsonElement loadFromServer(ServerInfo info) {
		String url = args.getString(Constants.EXTRA_URL);	//info.buildUrl("topology");
		RestTemplate restTmpl;
		if (info.isSsl())
			restTmpl = SslRestTemplate.getInstance(getContext());
		else
			restTmpl = DefaultRestTemplate.getInstance();
//		return DomainHelper.getInstance().getJsonFromUrl(restTmpl, url, info.getUser(), info.getPasswd());
		JsonElement rv = null;
		HttpHeaders reqHdrs = new HttpHeaders();
		if (info.getUser() != null) {
			HttpAuthentication authHdr = new HttpBasicAuthentication(info.getUser(), info.getPasswd());
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
				JsonElement jsonResp = DomainHelper.getInstance().parse(resp.getBody());
				if (jsonResp != null) {
					JsonObject jo = jsonResp.getAsJsonObject();
					if (jo.has("result")) {
						rv = jo.get("result");
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

	@Override
	protected void onStartLoading() {
		if (jsonObj == null)
			forceLoad();
		else
			deliverResult(jsonObj);
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(JsonElement data) {
		jsonObj = data;
		if (isStarted())
			super.deliverResult(data);
	}
}
