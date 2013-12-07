package com.axway.ate.activity;

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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;

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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.api.ApiException;
import com.axway.ate.api.ServerInfo;
import com.axway.ate.rest.DefaultRestTemplate;
import com.axway.ate.rest.SslRestTemplate;
import com.axway.ate.ssl.TrustedHttpRequestFactory;
import com.axway.ate.util.TopologyCompareResults;
import com.axway.ate.util.TopologyComparer;
import com.axway.ate.util.UiUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Group;
import com.vordel.api.topology.model.Host;
import com.vordel.api.topology.model.Topology;
import com.vordel.api.topology.model.Topology.EntityType;

public class MainService extends Service {

	private static final String TAG = MainService.class.getSimpleName();
	
	private Topology topology;
	private Handler activityHandler;
	private final IBinder binder = new LocalBinder();
	private SharedPreferences prefs;
	private DomainHelper helper;
	private boolean dirty;
	private LoadTopologyTask loadTask;
	private AsyncPostTask postTask;
	private CompareTopologyTask compareTask;
	private TrustCertsTask trustCertsTask;
	private CopySampleTask copySampleTask;
	private CheckCertTask checkCertTask;

	private File curFile;
	private ServerInfo curSrvrInfo;
	
	public static final int SERVICE_READY = 101;
	public static final int NOTIFY_LOADING = SERVICE_READY + 1;
	public static final int NOTIFY_LOADED = SERVICE_READY + 2;
	public static final int NOTIFY_CERT_NOT_TRUSTED = SERVICE_READY + 3;
	public static final int NOTIFY_CERTS_READY = SERVICE_READY + 4;
	public static final int NOTIFY_NO_TRUSTSTORE = SERVICE_READY + 5;
	public static final int NOTIFY_CERTS_ADDED = SERVICE_READY + 6;
	public static final int NOTIFY_EXCEPTION = SERVICE_READY + 7;
	public static final int NOTIFY_LOGIN_COMPLETE = SERVICE_READY + 8;
	public static final int NOTIFY_SAVED = SERVICE_READY + 9;
	public static final int NOTIFY_TOPOLOGY_COMPARED = SERVICE_READY + 10;

	public static final int SOURCE_FILE = 1;
	public static final int SOURCE_NET = 2;
	
	public class LocalBinder extends Binder {
		public MainService getService() {
			return MainService.this;
		}
	}
	
	public MainService() {
		super();
		Log.d(TAG, "constructor");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		topology = null;
		activityHandler = null;
		helper = DomainHelper.getInstance();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		dirty = false;
		loadTask = null;
		postTask = null;
		compareTask = null;
		trustCertsTask = null;
		copySampleTask = null;
		checkCertTask = null;
		curFile = null;
		curSrvrInfo = null;
		ensureSampleFile();
	}

	@Override
	public void onDestroy() {
		cancelAllTasks();
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private void ensureSampleFile() {
		File f = new File(getFilesDir(), Constants.SAMPLE_FILE);
		if (!f.exists()) {
			cancelCopySample();
			copySampleTask = new CopySampleTask(getResources(), f);
			copySampleTask.execute();
		}
	}

	public void deleteFromTopology(EntityType typ, String id) {
		if (topology == null)
			return;
		Group g = null;
		JsonObject json = null;
		boolean done = false;
		String url = null;
		switch (typ) {
			case Host:
				Host h = topology.getHost(id);
				if (h != null) {
					json = helper.toJson(h);					
					url = makeUrl(h);
					done = true;
					topology.removeHost(h);
				}
			break;
			case Group:
				g = topology.getGroup(id);
				if (g != null) {
					json = helper.toJson(g);					
					url = makeUrl(g);
					done = true;
					topology.removeGroup(g);
				}
			break;
			case NodeManager:
				//don't delete nodemanagers
			break;
			case Gateway:
				String[] ids = id.split("/");
				if (ids == null || ids.length != 2)
					return;
				String gid = ids[0];
				String sid = ids[1];
				g = topology.getGroup(gid);
				if (g != null) {
					com.vordel.api.topology.model.Service s = g.getService(sid);
					if (s != null) {
						json = helper.toJson(s);					
						url = makeUrl(g, s);
						g.removeService(sid);
						done = true;
					}
				}
			break;
		}
		if (done) {
			setDirty(true);
//			UiUtils.showToast(this, "Deleted " + id);
			if (url != null && json != null) {
				asyncPost(url, HttpMethod.DELETE, json);
			}
		}
	}

	private void cancelAllTasks() {
		cancelLoadTask();
		cancelPostTask();
		cancelCompareTask();
		cancelCopySample();
		cancelAddCerts();
		cancelCheckCert();
	}

	private void cancelTask(AsyncTask<?, ?, ?> task) {
		if (task == null)
			return;
		task.cancel(true);
	}
	
	private void cancelLoadTask() {
		cancelTask(loadTask);
		loadTask = null;
	}
	
	private void cancelPostTask() {
		cancelTask(postTask);
		postTask = null;
	}
	
	private void cancelCompareTask() {
		cancelTask(compareTask);
		compareTask = null;
	}
	
	private void cancelCopySample() {
		cancelTask(copySampleTask);
		copySampleTask = null;
	}
	
	private void cancelAddCerts() {
		cancelTask(trustCertsTask);
		trustCertsTask = null;
	}
	
	private void cancelCheckCert() {
		cancelTask(checkCertTask);
		checkCertTask = null;
	}
	
	public void loadTopology(ServerInfo info) {
		if (info == null)
			return;
		topology = null;
		if (activityHandler != null)
			activityHandler.sendEmptyMessage(NOTIFY_LOADING);
		cancelLoadTask();
		curFile = null;
		curSrvrInfo = info;
		loadTask = new LoadTopologyTask();
		loadTask.execute(curSrvrInfo);
	}
	
	public void loadTopology(File f) {
		if (f == null || !f.exists())
			return;
		topology = null;
		if (activityHandler != null)
			activityHandler.sendEmptyMessage(NOTIFY_LOADING);
		cancelLoadTask();
		curSrvrInfo = null;
		curFile = f;
		loadTask = new LoadTopologyTask();
		loadTask.execute(curFile);
	}
	
	public Topology getTopology() {
		return topology;
	}

	public Topology loadFromFile(File f) {
		if (f == null || !f.exists())
			return null;
		InputStream is = null;
		topology = null;
		try {
			is = new FileInputStream(f);
			topology = loadFromStream(is);
		} 
		catch (FileNotFoundException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
		return topology;	
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
		JsonElement jo = helper.parse(contents);
		rv = helper.topologyFromJson(jo.getAsJsonObject());
		return rv;
	}

	public void saveToFile(File f, Topology t) {
		if (f == null || t == null)
			return;
		JsonObject json = helper.toJson(t);
		if (json == null)
			return;
		Writer bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(f));
			bw.write(json.toString());
			if (!Constants.SAMPLE_FILE.equals(f.getName()))
				UiUtils.showToast(this, "Saved to file: " + f.getName());
		}
		catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
		finally {
			if (bw != null)
				try {bw.close();} catch (IOException e){}
		}
	}
	
	private void onTopologyLoaded(Topology t, Exception excp) {
		loadTask = null;
		topology = t;
		dirty = false;
		if (activityHandler == null)
			return;
		if (excp == null)
			activityHandler.sendEmptyMessage(NOTIFY_LOADED);
		else {
			if (excp instanceof CertPathValidatorException) {
				activityHandler.sendMessage(activityHandler.obtainMessage(NOTIFY_CERT_NOT_TRUSTED, ((CertPathValidatorException)excp).getCertPath()));
			}
			else
				activityHandler.sendMessage(activityHandler.obtainMessage(NOTIFY_EXCEPTION, excp));
		}
	}
	
	public boolean haveNetwork() {
		boolean rv = false;
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info != null) {
			rv = info.isAvailable() && info.isConnectedOrConnecting();
		}
		return rv;
	}

	public void compareTopology(ServerInfo info, Topology local) {
		if (info == null || local == null)
			return;
		cancelCompareTask();
		compareTask = new CompareTopologyTask(info);
		compareTask.execute(local);
	}
	
	public void addCertsToTrustStore(CertPath cp) {
		if (cp == null)
			return;
		cancelAddCerts();
		trustCertsTask = new TrustCertsTask();
		trustCertsTask.execute(cp);
	}

	public void removeTrustStore() {
		File f = new File(getFilesDir(), TrustedHttpRequestFactory.TRUST_STORE_FNAME);
		if (f.exists()) {
			UiUtils.showToast(this, "Trust store removed");
			f.delete();
		}
		else
			UiUtils.showToast(this, "Trust store not found");
	}
	
	private class TrustCertsTask extends AsyncTask<CertPath, Void, CertPath> {

		@Override
		protected CertPath doInBackground(CertPath... args) {
			CertPath rv = args[0];
			try {
				if (rv != null)
					TrustedHttpRequestFactory.trustCerts(MainService.this, rv);
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
			return rv;
		}

		@Override
		protected void onPostExecute(CertPath result) {
			onCertsTrusted(result);
		}
	}
	
	private void onCertsTrusted(CertPath cp) {
		trustCertsTask = null;
		if (activityHandler != null)
			activityHandler.sendMessage(activityHandler.obtainMessage(NOTIFY_CERTS_ADDED, (cp != null)));
	}

	private Topology loadFromServer(ServerInfo info) {
		Topology rv = null;
		HttpAuthentication authHdr = new HttpBasicAuthentication(info.getUser(), info.getPasswd());
		HttpHeaders reqHdrs = new HttpHeaders();
		reqHdrs.setAuthorization(authHdr);
		RestTemplate restTmpl;
		if (info.isSsl())
			restTmpl = SslRestTemplate.getInstance(this);
		else
			restTmpl = DefaultRestTemplate.getInstance();
		String url = info.buildUrl("topology");
		HttpEntity<?> reqEntity = new HttpEntity<Object>(reqHdrs);
		try {
			ResponseEntity<String> resp = restTmpl.exchange(url,  HttpMethod.GET, reqEntity, String.class);
			int sc = resp.getStatusCode().value();
			if (sc == HttpStatus.SC_OK) {
				JsonElement json = helper.parse(resp.getBody());
				JsonObject jo = json.getAsJsonObject();
				if (jo.has("result")) {
					rv = helper.topologyFromJson(jo.getAsJsonObject("result"));
				}
				else if (jo.has("errors")) {
					ApiException excp = new ApiException(jo.getAsJsonArray("errors"));
					throw excp;
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
					throw new ApiException(e.getStatusCode().value());
				default:
					throw new ApiException(e);
			}
		}
		return rv;
	}

	private String makeUrl(Host h) {
		if (curSrvrInfo == null || h == null)
			return null;
		String endpoint = "topology/hosts/";
		return curSrvrInfo.buildUrl(endpoint);
	}
	
	private String makeUrl(Group g) {
		if (curSrvrInfo == null || g == null)
			return null;
		String endpoint = "topology/groups/";
		return curSrvrInfo.buildUrl(endpoint);
	}
	
	private String makeUrl(Group g, com.vordel.api.topology.model.Service s) {
		if (curSrvrInfo == null || g == null || s == null)
			return null;
		String[] params = null;
		String endpoint = null;
		params = new String[1];
		params[0] = g.getId();
		endpoint = "topology/services/";
		return curSrvrInfo.buildUrl(endpoint, params);
	}

	private void asyncPost(final String url, final HttpMethod method, final JsonObject json) {
		if (curSrvrInfo == null || json == null)
			return;
		if (TextUtils.isEmpty(url))
			return;
		cancelPostTask();
		postTask = new AsyncPostTask(url, method, json);
		postTask.execute((Void)null);
	}
	
	private boolean postToServer(String url, HttpMethod method, JsonObject json) {
		boolean rv = false;
		HttpAuthentication authHdr = new HttpBasicAuthentication(curSrvrInfo.getUser(), curSrvrInfo.getPasswd());
		HttpHeaders reqHdrs = new HttpHeaders();
		reqHdrs.setAuthorization(authHdr);
		reqHdrs.setContentType(MediaType.APPLICATION_JSON);
		RestTemplate restTmpl;
		if (curSrvrInfo.isSsl())
			restTmpl = SslRestTemplate.getInstance(this);
		else
			restTmpl = DefaultRestTemplate.getInstance();
		HttpEntity<?> reqEntity = new HttpEntity<Object>(json.toString(), reqHdrs);
		try {
			ResponseEntity<String> resp = restTmpl.exchange(url,  method, reqEntity, String.class);
			int sc = resp.getStatusCode().value();
			if (sc == HttpStatus.SC_OK) {
				JsonElement jsonResp = helper.parse(resp.getBody());
				JsonObject jo = jsonResp.getAsJsonObject();
				if (jo.has("result")) {
				}
				else if (jo.has("errors")) {
					ApiException excp = new ApiException(jo.getAsJsonArray("errors"));
					throw excp;
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
		return rv;
	}

	private void onPostComplete(Exception excp) {
			if (excp == null) {
				setDirty(false);
				UiUtils.showToast(this, "Posted to server");
			}
			else {
				if (activityHandler != null)
					activityHandler.sendMessage(activityHandler.obtainMessage(NOTIFY_SAVED, excp));
			}
	}
	
	private class AsyncPostTask extends AsyncTask<Void, Void, Void> {
		private Exception excp;
		private String url;
		private HttpMethod method;
		private JsonObject json;

		public AsyncPostTask(String url, HttpMethod method, JsonObject json) {
			super();
			this.url = url;
			this.method = method;
			this.json = json;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				postToServer(url, method, json);
			}
			catch (Exception e) {
				excp = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			onPostComplete(excp);
		}
	}
	
	private class LoadTopologyTask extends AsyncTask<Object, Void, Topology> {

		private Exception excp;
		
		@Override
		protected Topology doInBackground(Object... args) {
			Topology rv = null;
			excp = null;
			File f = null;
			ServerInfo info = null;
			if (args[0] instanceof File)
				f = (File)args[0];
			else if (args[0] instanceof ServerInfo)
				info = (ServerInfo)args[0];
			if (f == null && info == null)
				return rv;
			try {
				if (f == null)
					rv = loadFromServer(info);
				else
					rv = loadFromFile(f);
			}
			catch (ApiException e) {
				excp = e;
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
					excp = cpve;
			}
			return rv;
		}

		@Override
		protected void onPostExecute(Topology result) {
			onTopologyLoaded(result, excp);
		}
		
	}

	private void onSampleCopied() {
		copySampleTask = null;
	}
	
	private class CopySampleTask extends AsyncTask<Void, Void, Void> {

		private Resources res;
		private File sample;

		public CopySampleTask(Resources res, File f) {
			super();
			this.res = res;
			sample = f;
		}
		
		@Override
		protected Void doInBackground(Void... args) {
			InputStream is = null;
			is = res.openRawResource(R.raw.sample_topo);
			Topology t = loadFromStream(is);
			if (t != null)
				saveToFile(sample, t);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			onSampleCopied();
		}
	}
	
	public void setHandler(Handler h) {
		activityHandler = h;
	}

	private SharedPreferences getPrefs() {
		if (prefs == null) {
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		return prefs;
	}
	
	public void saveHost(Host h) {
		if (topology == null || h == null)
			return;
		boolean isNew = false;
		Host th = topology.getHost(h.getId());
		if (th == null) {
			topology.addHost(h);
			isNew = true;
		}
		else
			th.setName(h.getName());
		setDirty(true);
		if (curSrvrInfo != null) {
			HttpMethod method = (isNew ? HttpMethod.POST : HttpMethod.PUT);
			JsonObject json = helper.toJson(h);
			asyncPost(makeUrl(h), method, json);
		}
	}
	
	public void saveGroup(Group g) {
		if (topology == null || g == null)
			return;
		boolean isNew = false;
		Group tg = topology.getGroup(g.getId());
		if (tg == null) {
			topology.addGroup(g);
			isNew = true;
		}
		else {
			tg.setName(g.getName());
			tg.setServices(g.getServices());
			tg.setTags(g.getTags());
		}
		setDirty(true);
		if (curSrvrInfo != null) {
			HttpMethod method = (isNew ? HttpMethod.POST : HttpMethod.PUT);
			JsonObject json = helper.toJson(g);
			asyncPost(makeUrl(g), method, json);
		}
	}
	
	public void saveService(com.vordel.api.topology.model.Service s, Group g) {
		if (topology == null || s == null)
			return;
		boolean isNew = false;
		com.vordel.api.topology.model.Service ts = topology.getService(s.getId());
		if (ts == null) {
			ts = new com.vordel.api.topology.model.Service();
			isNew = true;
		}
		ts.setName(s.getName());
		ts.setTags(s.getTags());
		ts.setHostID(s.getHostID());
		ts.setEnabled(s.getEnabled());
		ts.setManagementPort(s.getManagementPort());
		ts.setScheme(s.getScheme());
		ts.setType(s.getType());
		if (g != null)
			g.addService(ts);
		setDirty(true);
		if (curSrvrInfo != null) {
			HttpMethod method = (isNew ? HttpMethod.POST : HttpMethod.PUT);
			JsonObject json = helper.toJson(s);
			asyncPost(makeUrl(g, s), method, json);
		}
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	private void onTopologyCompared(TopologyCompareResults result) {
		if (activityHandler == null)
			return;
		activityHandler.sendMessage(activityHandler.obtainMessage(NOTIFY_TOPOLOGY_COMPARED, result));
	}

	private TopologyCompareResults performCompare(ServerInfo info, Topology local) {
		TopologyCompareResults rv = null;
		Topology remote = null;
		try {
			remote = loadFromServer(info);
			if (remote == null)
				return rv;
			TopologyComparer comparer = new TopologyComparer(local, remote);
			rv = comparer.compare();
		}
		catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
		return rv;
	}
	
	private class CompareTopologyTask extends AsyncTask<Topology, Void, TopologyCompareResults> {

		private ServerInfo srvrInfo;
		
		public CompareTopologyTask(ServerInfo si) {
			super();
			srvrInfo = si;
		}
		
		@Override
		protected TopologyCompareResults doInBackground(Topology... params) {
			if (srvrInfo == null)
				return null;
			return performCompare(srvrInfo, params[0]);
		}

		@Override
		protected void onPostExecute(TopologyCompareResults result) {
			onTopologyCompared(result);
		}
	}
	
	public void checkCertificateTrust(ServerInfo info) {
		if (info == null)
			return;
		cancelCheckCert();
		checkCertTask = new CheckCertTask();
		checkCertTask.execute(info);
	}

	private void certificateCheckComplete(Boolean result, CertPath certPath) {
		checkCertTask = null;
		if (activityHandler == null)
			return;
		if (result)
			activityHandler.sendMessage(activityHandler.obtainMessage(NOTIFY_CERTS_ADDED, result)); 
		else
			activityHandler.sendMessage(activityHandler.obtainMessage(NOTIFY_CERT_NOT_TRUSTED, certPath));
	}
	
	private class CheckCertTask extends AsyncTask<ServerInfo, Void, Boolean> {

		private CertPath certPath;

		public CheckCertTask() {
			super();
			certPath = null;
		}
		
		@Override
		protected Boolean doInBackground(ServerInfo... params) {
			ServerInfo info = params[0];
			if (info == null)
				return false;
			try {
				HttpAuthentication authHdr = new HttpBasicAuthentication(info.getUser(), info.getPasswd());
				HttpHeaders reqHdrs = new HttpHeaders();
				reqHdrs.setAuthorization(authHdr);
				RestTemplate restTmpl;
				if (info.isSsl())
					restTmpl = SslRestTemplate.getInstance(MainService.this);
				else
					restTmpl = DefaultRestTemplate.getInstance();
				String url = info.buildUrl("topology");
				HttpEntity<?> reqEntity = new HttpEntity<Object>(reqHdrs);
				ResponseEntity<String> resp = restTmpl.exchange(url,  HttpMethod.GET, reqEntity, String.class);
				int sc = resp.getStatusCode().value();
				if (sc == HttpStatus.SC_OK || sc == HttpStatus.SC_NOT_FOUND) {
					return true;
				}
			}
			catch (Exception e) {
				CertPathValidatorException cpve = null;
				Throwable cause = e.getCause();
				while (certPath == null && cause != null) {
					if (cause instanceof CertPathValidatorException) {
						cpve = (CertPathValidatorException)cause;
						certPath = cpve.getCertPath();
					}
					else
						cause = cause.getCause();
				}
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			certificateCheckComplete(result, certPath);
		}
	}
}
