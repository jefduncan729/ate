package com.axway.ate;

public class Constants {

	public static final long NULL_ID = -1;
	
	public static final String UTF8 = "UTF-8";
	
	public static final String DEF_API_USERNAME = "admin";
	public static final String DEF_API_PASSWD = "changeme";
	
    public static final String TOPOLOGY_FILE_EXT = ".topo";
    public static final String SAMPLE_FILE = "sample" + TOPOLOGY_FILE_EXT;
    
    public static final int STATUS_INACTIVE = 0;
    public static final int STATUS_ACTIVE = 1;

	public static final String HTTP_SCHEME = "http";
	public static final String HTTPS_SCHEME = HTTP_SCHEME + "s";

	public static final String EXTRA_BASE = "com.axway.ate.";
	public static final String EXTRA_ACTION = EXTRA_BASE + "action";
	public static final String EXTRA_TRUST_CERT = EXTRA_BASE + "trust_cert";
		
	public static final int CERT_NOT_TRUSTED = 1001;
}
