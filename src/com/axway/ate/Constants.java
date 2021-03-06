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

    public static final int FLAG_CERT_NOT_TRUSTED = 0;
    public static final int FLAG_CERT_TRUSTED = 1;

	public static final String HTTP_SCHEME = "http";
	public static final String HTTPS_SCHEME = HTTP_SCHEME + "s";

	public static final String EXTRA_BASE = "com.axway.ate.";
	public static final String EXTRA_ACTION = EXTRA_BASE + "action";
	public static final String EXTRA_TRUST_CERT = EXTRA_BASE + "trust_cert";
	
	public static final String EXTRA_JSON_ITEM = EXTRA_BASE + "json_item";
	public static final String EXTRA_ITEM_ID = EXTRA_BASE + "item_id";
	public static final String EXTRA_ITEM_NAME = EXTRA_BASE + "item_name";
	public static final String EXTRA_ITEM_TYPE = EXTRA_BASE + "item_type";
	public static final String EXTRA_JSON_TOPOLOGY = EXTRA_BASE + "json_topology";
	public static final String EXTRA_DELETE_FROM_DISK = EXTRA_BASE + "del_from_disk";
	public static final String EXTRA_SERVICES_PORT = EXTRA_BASE + "svcs_port";
	public static final String EXTRA_MGMT_PORT = EXTRA_BASE + "mgmt_port";
	public static final String EXTRA_REFERRING_ITEM_TYPE = EXTRA_BASE + "ref_item_type";
	public static final String EXTRA_REFERRING_ITEM_ID = EXTRA_BASE + "ref_id";
	public static final String EXTRA_URL = EXTRA_BASE + "url";
	public static final String EXTRA_SERVER_INFO = EXTRA_BASE + "srvr_info";
	public static final String EXTRA_USE_SSL = EXTRA_BASE + "use_ssl";
	public static final String EXTRA_NODE_MGR_GROUP = EXTRA_BASE + "nm_grp";
	public static final String EXTRA_CONSOLE_HANDLE = EXTRA_BASE + "console_handle";
	public static final String EXTRA_FROM_GROUP = EXTRA_BASE + "from_grp";
	public static final String EXTRA_TO_GROUP = EXTRA_BASE + "to_grp";
	public static final String EXTRA_LAYOUT = EXTRA_BASE + "layout";
	public static final String EXTRA_COMPARE_RESULT = EXTRA_BASE + "compare_result";
	public static final String EXTRA_TOPO_SOURCE = EXTRA_BASE + "topo_src";
	public static final String EXTRA_HAVE_CONSOLE = EXTRA_BASE + "have_console";
	public static final String EXTRA_HOST_ID = EXTRA_BASE + "host_id";
	public static final String EXTRA_FILENAME = EXTRA_BASE + "filename";
	public static final String EXTRA_REFRESH_INTERVAL = EXTRA_BASE + "refreshSecs";
	
	public static final String KEY_SHOW_NODE_MGRS = "showNodeMgrs";
	public static final String KEY_RELOAD_AFTER_UPD = "reloadAfterUpdate";
	public static final String KEY_REMEMBER_USER = "rememberUser";
	public static final String KEY_SSHUSER = "sshUser";
	
	public static final boolean DEF_SHOW_NODE_MGRS = false;
	
	public static final int FLAG_SHOW_NODE_MGRS   = 0x0001;
	public static final int FLAG_RELOAD_AFTER_UPD = 0x0010;
			
	public static final int CERT_NOT_TRUSTED = 1001;
	public static final int CERT_TRUSTED = 1002;
	public static final int TRUST_STORE_REMOVED = 1003;
	
	public static final String JACKPAL_TERMINAL_PACKAGE = "jackpal.androidterm";
	public static final String JACKPAL_EXTRA_WINDOW_HANDLE = "jackpal.androidterm.window_handle";
	public static final String JACKPAL_EXTRA_INITIAL_CMD = "jackpal.androidterm.iInitialCommand";
	public static final String JACKPAL_ACTION_RUN_SCRIPT = "jackpal.androidterm.RUN_SCRIPT";
	public static final String JACKPAL_ACTION_NEW_WINDOW = "jackpal.androidterm.OPEN_NEW_WINDOW";
	
	public static final int DEF_REFRESH_SECS = 60;
}
