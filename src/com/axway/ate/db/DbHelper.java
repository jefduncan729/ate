package com.axway.ate.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {

	private static final String TAG = DbHelper.class.getSimpleName();

	public static final String DB_NAME = "ate.db";
	public static final int DB_VERSION = 1;

	public interface Tables {
		public static final String CONN_MGR = "conn_mgr";
	}
	
	public interface CommonColumns extends BaseColumns {
		public static final String STATUS = "status";
		public static final String FLAG = "flag";
		public static final String CREATE_DATE = "create_date";
		public static final String MODIFY_DATE = "modify_date";
		
		public static final int IDX_ID 						= 0;
		public static final int IDX_STATUS 					= 1;
		public static final int IDX_FLAG 					= 2;
		public static final int IDX_CREATE_DATE 			= 3;
		public static final int IDX_MODIFY_DATE 			= 4;
	}

	private static final String BASE_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.axway.ate.";
	private static final String BASE_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.axway.ate.";

	public interface ConnMgrColumns extends CommonColumns {
		public static final Uri CONTENT_URI = DbProvider.BASE_CONTENT_URI.buildUpon().appendPath(Tables.CONN_MGR).build();
		public static final String CONTENT_TYPE = BASE_CONTENT_TYPE + Tables.CONN_MGR;
		public static final String CONTENT_ITEM_TYPE = BASE_CONTENT_ITEM_TYPE + Tables.CONN_MGR;
//		public static final String NAME = "name";
		public static final String HOST = "host";
		public static final String PORT = "port";
		public static final String USE_SSL = "ssl";
		public static final String USER = "user";
		public static final String PASS = "pass";

//		public static final int IDX_NAME 					= IDX_MODIFY_DATE + 1;
		public static final int IDX_HOST					= IDX_MODIFY_DATE + 1;
		public static final int IDX_PORT					= IDX_HOST + 1;
		public static final int IDX_USE_SSL					= IDX_HOST + 2;
		public static final int IDX_USER					= IDX_HOST + 3;
		public static final int IDX_PASS					= IDX_HOST + 4;
		
		public static final String[] LIST_PROJECTION = { _ID, STATUS, FLAG, CREATE_DATE, MODIFY_DATE, HOST, PORT, USE_SSL, USER, PASS };
	}
	
	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTables(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	private void addCommonColumns(StringBuilder sb) {
		sb.append(CommonColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sb.append(CommonColumns.STATUS).append(" INTEGER DEFAULT 0, ");
		sb.append(CommonColumns.FLAG).append(" INTEGER DEFAULT 0, ");
		sb.append(CommonColumns.CREATE_DATE).append(" LONG DEFAULT 0, ");
		sb.append(CommonColumns.MODIFY_DATE).append(" LONG DEFAULT 0, ");
	}
	
	private void createTables(SQLiteDatabase db) {
		if (db == null)
			return;
		createConnMgrTable(db);
	}
	
	private void createConnMgrTable(SQLiteDatabase db) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ").append(Tables.CONN_MGR).append("( ");
		addCommonColumns(sb);
//		sb.append(ConnMgrColumns.NAME).append(" VARCHAR(128) NOT NULL, ");
		sb.append(ConnMgrColumns.HOST).append(" VARCHAR(256) NOT NULL, ");
		sb.append(ConnMgrColumns.PORT).append(" INTEGER DEFAULT 0, ");
		sb.append(ConnMgrColumns.USE_SSL).append(" INTEGER DEFAULT 0, ");
		sb.append(ConnMgrColumns.USER).append(" VARCHAR(128), ");
		sb.append(ConnMgrColumns.PASS).append(" VARCHAR(128)");
//		sb.append("UNIQUE (").append(ConnMgrColumns.NAME).append(") ON CONFLICT REPLACE");
		sb.append(")");
		String sql = sb.toString();
		Log.d(TAG, "Executing SQL: " + sql);
		db.execSQL(sql);
	}
}
