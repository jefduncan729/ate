package com.axway.ate.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.axway.ate.Constants;
import com.axway.ate.db.DbHelper.ConnMgrColumns;

public class ServerInfo {
	
	private static final String TAG = ServerInfo.class.getSimpleName();
	
	private long id;
	private int status;
	private String host;
	private int port;
	private boolean ssl;
	private String user;
	private String pwd;
	
	public ServerInfo() {
		super();
		id = 0;
		status = Constants.STATUS_ACTIVE;
		host = null;
		port = 8090;
		ssl = true;
		user = null;
		pwd = null;
	}
	
	public static ServerInfo from(Cursor c) {
		ServerInfo rv = null;
		if (c == null)
			return rv;
		rv = new ServerInfo();
		rv.setStatus(c.getInt(ConnMgrColumns.IDX_STATUS));
		rv.setId(c.getLong(ConnMgrColumns.IDX_ID));
		rv.setHost(c.getString(ConnMgrColumns.IDX_HOST));
		rv.setPort(c.getInt(ConnMgrColumns.IDX_PORT));
		rv.setSsl(c.getInt(ConnMgrColumns.IDX_USE_SSL) == 1);
		rv.setUser(c.getString(ConnMgrColumns.IDX_USER));
		rv.setPasswd(c.getString(ConnMgrColumns.IDX_PASS));
		return rv;
	}
	
	public ServerInfo(String host) {
		this();
		this.host = host;
	}
	
	public ServerInfo(String host, int port) {
		this(host);
		this.port = port;
	}
	
	public ServerInfo(String host, int port, boolean ssl) {
		this(host);
		this.port = port;
		this.ssl = ssl;
	}
	
	public ServerInfo(String host, int port, boolean ssl, String user, String pwd) {
		this(host, port, ssl);
		this.user = user;
		this.pwd = pwd;
	}
	
	public ServerInfo(String host, int port, String user, String pwd) {
		this(host, port, true, user, pwd);
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPasswd() {
		return pwd;
	}

	public void setPasswd(String pwd) {
		this.pwd = pwd;
	}

	@Override
	public boolean equals(Object o) {
		boolean rv = false;
		if (!(o instanceof ServerInfo))
			return false;
		ServerInfo ci = (ServerInfo)o;
		rv = (status == ci.getStatus());
		if (!rv)
			return rv;
		rv = (id == ci.getId());
		if (!rv)
			return rv;
		rv = (port == ci.getPort());
		if (!rv)
			return rv;
		rv = (ssl != ci.isSsl());
		if (!rv)
			return rv;
		if (host == null)
			rv = !TextUtils.isEmpty(ci.getHost());
		else
			rv = (host.equals(ci.getHost()));
		if (!rv)
			return rv;
		if (user == null)
			rv = !TextUtils.isEmpty(ci.getUser());
		else
			rv = (user.equals(ci.getUser()));
		if (!rv)
			return rv;
		if (pwd == null)
			rv = !TextUtils.isEmpty(ci.getPasswd());
		else
			rv = (pwd.equals(ci.getPasswd()));
		if (!rv)
			return rv;
		return true;
	}

	@Override
	public int hashCode() {
		int rv = 0;
		if (host != null)
			rv += host.hashCode();
		if (user != null)
			rv += user.hashCode();
		if (pwd != null)
			rv += pwd.hashCode();
		rv += port;
		if (ssl)
			rv += 1;
		return rv;
	}
	
	public static ServerInfo fromBundle(Bundle args) {
		ServerInfo rv = new ServerInfo();
		if (args != null) {
			rv.setStatus(args.getInt(ConnMgrColumns.STATUS, Constants.STATUS_ACTIVE));
			rv.setId(args.getLong(ConnMgrColumns._ID, 0));
			rv.setHost(args.getString(ConnMgrColumns.HOST, null));
			rv.setUser(args.getString(ConnMgrColumns.USER, null));
			rv.setPasswd(args.getString(ConnMgrColumns.PASS, null));
			rv.setPort(args.getInt(ConnMgrColumns.PORT, 0));
			rv.setSsl(args.getBoolean(ConnMgrColumns.USE_SSL, true));
		}
		return rv;
	}
	
	public Bundle toBundle() {
		Bundle args = new Bundle();
		args.putInt(ConnMgrColumns.STATUS, getStatus());
		args.putLong(ConnMgrColumns._ID, getId());
		args.putString(ConnMgrColumns.HOST, getHost());
		args.putString(ConnMgrColumns.USER, getUser());
		args.putString(ConnMgrColumns.PASS, getPasswd());
		args.putInt(ConnMgrColumns.PORT, getPort());
		args.putBoolean(ConnMgrColumns.USE_SSL, isSsl());
		return args;
	}
	
	public String getBaseUrl() {
		StringBuilder sb = new StringBuilder();
		sb.append("http");
		if (ssl)
			sb.append("s");
		sb.append("://").append(host).append(":").append(port).append("/api/");
		return sb.toString();
	}
	
	public String buildUrl(String endpoint, String... params) {
		String base = getBaseUrl();
		StringBuilder sb = new StringBuilder(base);
		if (endpoint.startsWith("/")) {
			if (base.endsWith("/"))
				sb.append(endpoint.substring(1));
			else
				sb.append(endpoint);
		}
		else
			sb.append(endpoint);
		if (params != null) {
			if (!endpoint.endsWith("/"))
				sb.append("/");
			String val = null;
			for (String k: params) {
				try {
					val = URLEncoder.encode(k, Constants.UTF8);
					sb.append(val).append("/");
				} 
				catch (UnsupportedEncodingException e) {
					Log.e(TAG, e.getLocalizedMessage(), e);
				}
			}
		}
		return sb.toString();
	}

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}
	
	public void copy(ServerInfo other) {
		if (other == null)
			return;
		setStatus(other.getStatus());
		setId(other.getId());
		setHost(other.getHost());
		setPort(other.getPort());
		setSsl(other.isSsl());
		setUser(other.getUser());
		setPasswd(other.getPasswd());
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServerInfo [id=");
		builder.append(id);
		builder.append(", status=");
		builder.append(status);
		builder.append(", host=");
		builder.append(host);
		builder.append(", port=");
		builder.append(port);
		builder.append(", ssl=");
		builder.append(ssl);
		builder.append(", user=");
		builder.append(user);
		builder.append(", pwd=");
		builder.append(pwd);
		builder.append("]");
		return builder.toString();
	}
}
