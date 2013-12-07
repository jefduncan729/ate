package com.axway.ate.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import android.content.Context;
import android.util.Log;

public class TrustedHttpRequestFactory extends SimpleClientHttpRequestFactory {
	private static final String TAG = TrustedHttpRequestFactory.class.getSimpleName();

	public static final String TRUST_STORE_FNAME = "trust_store.bks";
	public static final String TRUST_STORE_PASS = "Secret1";
	
	private Context context;
		
	protected TrustedHttpRequestFactory() {
		super();
		context = null;
	}
	
	public TrustedHttpRequestFactory(Context context) {
		this();
		this.context = context;
	}

	public static void trustCerts(Context ctx, CertPath cp) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		File f = new File(ctx.getFilesDir(), TRUST_STORE_FNAME);
		final InputStream trustStoreLocation;
		if (f.exists())
			trustStoreLocation = new FileInputStream(f);
		else
			trustStoreLocation = null;

	    final KeyStore trustStore = KeyStore.getInstance("BKS");
	    trustStore.load(trustStoreLocation, TRUST_STORE_PASS.toCharArray());

	    final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	    tmf.init(trustStore);
	    for (Certificate c: cp.getCertificates()) {
	    	if (c.getType() == "X.509") {
	    		X509Certificate c509 = (X509Certificate)c;
	    		trustStore.setCertificateEntry(c509.getSubjectDN().toString(), c509);
	    	}
	    }
	    final OutputStream out = new FileOutputStream(f);
	    trustStore.store(out, TRUST_STORE_PASS.toCharArray());
	}
	
	private SSLSocketFactory getSSLSocketFactory() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
//	    final InputStream trustStoreLocation = context.getResources().openRawResource(R.raw.apiserver);
		File f = new File(context.getFilesDir(), TRUST_STORE_FNAME);
		final InputStream trustStoreLocation = (f.exists() ? new FileInputStream(f) : null);

	    final KeyStore trustStore = KeyStore.getInstance("BKS");
	    trustStore.load(trustStoreLocation, TRUST_STORE_PASS.toCharArray());

	    final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	    tmf.init(trustStore);

	    final SSLContext sslCtx = SSLContext.getInstance("TLS");
	    sslCtx.init(null, tmf.getTrustManagers(), new SecureRandom());

	    return sslCtx.getSocketFactory();
	}

	@Override
	protected HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
	    final HttpURLConnection httpUrlConnection = super.openConnection(url, proxy);
	    if (url.getProtocol().toLowerCase().equals("https")) {
	        try {
	            ((HttpsURLConnection)httpUrlConnection).setSSLSocketFactory(getSSLSocketFactory());
	            ((HttpsURLConnection)httpUrlConnection).setHostnameVerifier(new NullHostnameVerifier());
	        } 
	        catch (Exception e) {
	        	Log.e(TAG, e.getLocalizedMessage(), e);
	        }

	    } 
	    return httpUrlConnection;
	}

	private static class NullHostnameVerifier implements HostnameVerifier {
	    public boolean verify(String hostname, SSLSession session) {
	        return true;
	    }
	}
}
