package com.axway.ate.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.axway.ate.db.DbHelper.CommonColumns;
import com.axway.ate.db.DbHelper.ConnMgrColumns;
import com.axway.ate.db.DbHelper.Tables;

public class DbProvider extends ContentProvider {

	private static final String TAG = DbProvider.class.getSimpleName();
	
	public static final String CONTENT_AUTHORITY = "com.axway.ate";
	public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	public static final int CONN_MGR = 1;
	public static final int CONN_MGR_ID = 2;

	private DbHelper dbHelper;
	private static UriMatcher uriMatcher = buildUriMatcher();
	private static UriMatcher buildUriMatcher() {
		final UriMatcher rv = new UriMatcher(UriMatcher.NO_MATCH);
		final String auth = CONTENT_AUTHORITY;
		
		rv.addURI(auth, Tables.CONN_MGR, CONN_MGR);
		rv.addURI(auth, Tables.CONN_MGR + "/*", CONN_MGR_ID);

		return rv;
	}
	
	public DbProvider() {
		super();
		dbHelper = null;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int rv = 0;
		if (uri == null) {
			Log.e(TAG, "delete called with null value(s)");
			return rv;
		}			
		long id = idFromUri(uri);
		String tblName = null;
		int n = uriMatcher.match(uri);
		switch (n) {
			case CONN_MGR:
				tblName = Tables.CONN_MGR;
			break;
			case CONN_MGR_ID:
				tblName = Tables.CONN_MGR;
			break;
		}
		if (tblName == null)
			return rv;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		Log.d(TAG, "delete: " + tblName + ", " + Long.toString(id));
		if (id == 0) {
			if (TextUtils.isEmpty(selection))
				Log.w(TAG, "unbounded delete called (and honored): " + tblName);
			rv = db.delete(tblName, selection, selectionArgs);
		}
		else
			rv = db.delete(tblName, CommonColumns._ID + "=" + Long.toString(id) + (TextUtils.isEmpty(selection) ? "" : " AND (" + selection + ")"), selectionArgs);
		Log.d(TAG, "rows deleted: " + Integer.toString(rv));
		if (rv > 0)
			getContext().getContentResolver().notifyChange(uri, null);			
		return rv;
	}

	@Override
	public String getType(Uri uri) {
		String rv = null;
		int n = uriMatcher.match(uri);
		switch (n) {
			case CONN_MGR:
				rv = ConnMgrColumns.CONTENT_TYPE;
			break;
			case CONN_MGR_ID:
				rv = ConnMgrColumns.CONTENT_ITEM_TYPE;
			break;
		}
		return rv;
	}
	
	private void cleanCommonValues(ContentValues values) {
		long now = System.currentTimeMillis();
		if (!values.containsKey(CommonColumns.MODIFY_DATE))
			values.put(CommonColumns.MODIFY_DATE, now);
		if (!values.containsKey(CommonColumns.CREATE_DATE))
			values.put(CommonColumns.CREATE_DATE, now);
		if (!values.containsKey(CommonColumns.STATUS))
			values.put(CommonColumns.STATUS, 0);
		if (!values.containsKey(CommonColumns.FLAG))
			values.put(CommonColumns.FLAG, 0);
	}

	private void cleanConnMgrValues(ContentValues values) {
		cleanCommonValues(values);
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues valuesIn) {
		Uri rv = null;
		String tblName = null;
		Uri baseUri = null;
		ContentValues values = (valuesIn == null ? new ContentValues() : new ContentValues(valuesIn));
		int n = uriMatcher.match(uri);
		switch (n) {
			case CONN_MGR:
				cleanConnMgrValues(values);
				tblName = Tables.CONN_MGR;
				baseUri = ConnMgrColumns.CONTENT_URI;
			break;
			case CONN_MGR_ID:
				cleanConnMgrValues(values);
				tblName = Tables.CONN_MGR;
				baseUri = ConnMgrColumns.CONTENT_URI;
			break;
		}
		if (tblName == null || baseUri == null)
			return rv;
		if (values.containsKey(CommonColumns._ID))
			values.remove(CommonColumns._ID);
		long rowId = 0;
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		Log.d(TAG, "insert: " + tblName + ", " + values.toString());
		rowId = db.insert(tblName, null, values);
		if (rowId > 0) {
			rv = ContentUris.withAppendedId(baseUri, rowId);
			getContext().getContentResolver().notifyChange(rv, null);
		}
		return rv;
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DbHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor rv = null;
		String tblName = null;
		String s = uri.getLastPathSegment();
		long id = idFromUri(uri);
		int n = uriMatcher.match(uri);
		if (projection == null)
			projection = ConnMgrColumns.LIST_PROJECTION;
		switch (n) {
			case CONN_MGR:
				tblName = Tables.CONN_MGR;
				if (sortOrder == null)
					sortOrder = ConnMgrColumns.HOST + " ASC, " + ConnMgrColumns.PORT + " DESC";
			break;
			case CONN_MGR_ID:
				tblName = Tables.CONN_MGR;
				if (selection == null) {
					selection = ConnMgrColumns._ID + " = ?";
					selectionArgs = new String[] { Long.toString(id) };
				}
				else {
					selection = selection + " AND (" + ConnMgrColumns._ID + " = " + Long.toString(id) + ")";
				}
			break;
		}
		if (tblName == null)
			return rv;
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		rv = db.query(tblName, projection, selection, selectionArgs, null, null, sortOrder);
		return rv;
	}

	@Override
	public int update(Uri uri, ContentValues valuesIn, String selection, String[] selectionArgs) {
		int rv = 0;
		String tblName = null;
		ContentValues values = (valuesIn == null ? new ContentValues() : new ContentValues(valuesIn));
		int n = uriMatcher.match(uri);
		long id = idFromUri(uri);
		switch (n) {
			case CONN_MGR:
				tblName = Tables.CONN_MGR;
				cleanConnMgrValues(values);
			break;
			case CONN_MGR_ID:
				tblName = Tables.CONN_MGR;
				if (selection == null) {
					selection = ConnMgrColumns._ID + " = ?";
					selectionArgs = new String[] { Long.toString(id) };
				}
			break;
		}
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		rv = db.update(tblName, valuesIn, selection, selectionArgs);
		if (rv > 0)
			getContext().getContentResolver().notifyChange(uri, null);
		return rv;
	}
	
	private long idFromUri(Uri uri) {
		long rv = 0;
		if (uri == null)
			return rv;
		String s = uri.getLastPathSegment();
		try {
			rv = Long.parseLong(s); 
		}
		catch (NumberFormatException e) {
			rv = 0;
		}
		return rv;
	}
}
