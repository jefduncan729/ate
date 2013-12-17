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
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.axway.ate.ApiException;
import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.ServerInfo;
import com.axway.ate.rest.DefaultRestTemplate;
import com.axway.ate.rest.SslRestTemplate;
import com.axway.ate.ssl.TrustedHttpRequestFactory;

public class CertificateService extends BaseIntentService {

	private static final String TAG = CertificateService.class.getSimpleName();
	
	public static final String ACTION_CHECK_CERT = ACTION_BASE + "check_cert";
	public static final String ACTION_REMOVE_TRUST_STORE = ACTION_BASE + "remove_trust_store";
	
	public CertificateService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		if (intent == null || intent.getExtras() == null)
			return;
		final String action = intent.getAction();
		Bundle extras = intent.getExtras();
		if (ACTION_REMOVE_TRUST_STORE.equals(action)) {
			removeTrustStore();
			return;
		}
		if (ACTION_CHECK_CERT.equals(action)) {
			final boolean trustCert = extras.getBoolean(Constants.EXTRA_TRUST_CERT, false);
			final ServerInfo srvrInfo = ServerInfo.fromBundle(extras.getBundle(Constants.EXTRA_SERVER_INFO));
			CertPath cp = null;
			boolean trusted = false;
			try {
				attemptGet(srvrInfo);
				trusted = true;
			}
			catch (ApiException excp) {
				cp = isCertValidationErr(excp);
				if (cp != null) {
					Log.d(TAG, "certPath not trusted: " + cp.toString());
					if (trustCert) {
						Log.d(TAG, "adding to trust store");
						try {
							TrustedHttpRequestFactory.trustCerts(this, cp);
							trusted = true;
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
				}
			}
			if (getResultReceiver() == null)
				return;
			if (trusted) {
				getResultReceiver().send(Constants.CERT_TRUSTED, extras);
			}
			else {
				StringBuilder sb = new StringBuilder();
				int i = 1;
			    for (Certificate c: cp.getCertificates()) {
			    	if (c.getType() == "X.509") {
			    		X509Certificate c509 = (X509Certificate)c;
			    		sb.append("[").append(i++).append("]: ").append(c509.getSubjectDN().toString()).append("\n");
			    	}
			    }
				extras.putString(Intent.EXTRA_BUG_REPORT, sb.toString());
				getResultReceiver().send(Constants.CERT_NOT_TRUSTED, extras);
			}
		}
	}

	private void removeTrustStore() {
		File f = new File(getFilesDir(), TrustedHttpRequestFactory.TRUST_STORE_FNAME);
		if (f.exists()) {
			f.delete();
			showToast(getString(R.string.truststore_removed));
			if (getResultReceiver() != null)
				getResultReceiver().send(Constants.TRUST_STORE_REMOVED, null);
		}
		else
			showToast(getString(R.string.truststore_notfound));
	}
	
	private RestTemplate getRestTemplate(ServerInfo info) {
		RestTemplate rv;
		if (info.isSsl())
			rv = SslRestTemplate.getInstance(this);
		else
			rv = DefaultRestTemplate.getInstance();
		return rv;
	}
	
	private void attemptGet(ServerInfo srvrInfo) throws ApiException {
		HttpAuthentication authHdr = new HttpBasicAuthentication(srvrInfo.getUser(), srvrInfo.getPasswd());
		HttpHeaders reqHdrs = new HttpHeaders();
		reqHdrs.setAuthorization(authHdr);
		HttpEntity<?> reqEntity = new HttpEntity<String>("", reqHdrs);
		int sc = 0;
		String url = srvrInfo.buildUrl("topology");
		StringBuilder sb = new StringBuilder();
		sb.append(HttpMethod.GET).append(" ").append(url);
		Log.d(TAG, sb.toString());
		try {
			ResponseEntity<String> resp = getRestTemplate(srvrInfo).exchange(url,  HttpMethod.GET, reqEntity, String.class);
			sc = resp.getStatusCode().value();
			Log.d(TAG, "response status code: " + Integer.toString(sc));
			if (sc == HttpStatus.SC_OK) {
			}
		}
		catch (Exception e) {
			throw new ApiException(e);
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
}
