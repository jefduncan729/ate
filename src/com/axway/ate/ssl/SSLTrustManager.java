package com.axway.ate.ssl;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLTrustManager {
	
	private X509TrustManager origTrustMgr;
	
	public SSLTrustManager() {
		try {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore)null);
			TrustManager[] trustMgrs = tmf.getTrustManagers();
			origTrustMgr = (X509TrustManager)trustMgrs[0];
		}
		catch (Exception e) {
			
		}
	}
	
	public SSLSocketFactory getSocketFactory() {
		SSLSocketFactory rv = null;
		try {
			TrustManager[] wrappedTrustMgrs = new TrustManager[] {
					new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() {
							return origTrustMgr.getAcceptedIssuers();
						}
						
						public void checkClientTrusted(X509Certificate[] certs, String authType) {
							try {
								origTrustMgr.checkClientTrusted(certs, authType);
							}
							catch (CertificateException e) {
								
							}
						}
						
						public void checkServerTrusted(X509Certificate[] certs, String authType) {
							try {
								origTrustMgr.checkServerTrusted(certs, authType);
							}
							catch (CertificateException e) {
								
							}
						}
					}
				};
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null,  wrappedTrustMgrs, new SecureRandom());
			return sslContext.getSocketFactory();
		}
		catch (Exception e) {
			return null;
		}
	}
}
