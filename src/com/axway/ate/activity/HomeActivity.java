package com.axway.ate.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.ServerInfo;
import com.axway.ate.adapter.HomeAdapter;
import com.axway.ate.db.DbHelper.ConnMgrColumns;
import com.axway.ate.fragment.SelectFileDialog;
import com.axway.ate.fragment.SelectServerDialog;
import com.axway.ate.util.UiUtils;

public class HomeActivity extends BaseActivity implements SelectServerDialog.Listener, SelectFileDialog.Listener, OnItemClickListener {

	private static final String TAG = HomeActivity.class.getSimpleName();
	
	protected static final String TAG_SEL_SRVR_DLG = "selSrvrDlg";
	protected static final String TAG_SEL_FILE_DLG = "selFileDlg";
		
	
	private GridView grid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		grid = (GridView)findViewById(R.id.container01);
		grid.setOnItemClickListener(this);
	}

	private void workWithServer() {
		ServerInfo info = getOnlyServerInfo();
		if (info == null)
			selectServer();
		else
			onServerSelected(info, R.id.action_load_from_anm);
	}

	private void workWithFiles() {
		File f = getOnlyTopologyFile();
		if (f == null)
			showSelectFileDialog();
		else
			onFileSelected(f.getName());
	}
	
	private void showConnMgr() {
		Intent i = new Intent(this, ConnMgrActivity.class);
		startActivityForResult(i, R.id.action_conn_mgr);
	}
	
	private ServerInfo getOnlyServerInfo() {
		ServerInfo rv = null;
		String where = ConnMgrColumns.STATUS + " = ?";
		String[] whereArgs = new String[] { Integer.toString(Constants.STATUS_ACTIVE) };
		Cursor c = getContentResolver().query(ConnMgrColumns.CONTENT_URI, null, where, whereArgs, null);
		if (c.getCount() == 1) {
			if (c.moveToFirst()) {
				rv = ServerInfo.from(c);
			}
		}
		c.close();
		if (c == null || c.getCount() == 0)
			alertDialog(getString(R.string.add_conn));
		return rv;
	}
	
	private void selectServer() {
		String where = ConnMgrColumns.STATUS + " = ?";
		String[] whereArgs = new String[] { Integer.toString(Constants.STATUS_ACTIVE) };
		Cursor c = getContentResolver().query(ConnMgrColumns.CONTENT_URI, null, where, whereArgs, null);
		if (c == null)
			return;
		List<ServerInfo> list = new ArrayList<ServerInfo>();
		while (c.moveToNext()) {
			list.add(ServerInfo.from(c));
		}
		c.close();
		if (list.size() == 0)
			return;
		SelectServerDialog dlg = new SelectServerDialog();
		dlg.setAction(R.id.action_load_from_anm);
		dlg.setListener(this);
		dlg.setServerInfoList(list);
		dlg.show(getFragmentManager(), TAG_SEL_SRVR_DLG);
	}

	@Override
	public void onServerSelected(ServerInfo info, int action) {
		if (info == null)
			return;
		Intent i = new Intent(this, TopologyRestActivity.class);
		i.putExtra(Constants.EXTRA_SERVER_INFO, info.toBundle());
		startActivity(i);
	}

	private File getOnlyTopologyFile() {
		ArrayList<String> files = getSavedFiles();
		if (files == null || files.size() != 1)
			return null;
		return new File(getFilesDir(), files.iterator().next());
	}
	
	private ArrayList<String> getSavedFiles() {
		File dir = getFilesDir();
		File[] files = null;
		ArrayList<String> jsonFiles = new ArrayList<String>();
		if (dir != null) {
			files = dir.listFiles();
			for (File f: files) {
				if (f.getName().endsWith(Constants.TOPOLOGY_FILE_EXT))
					jsonFiles.add(f.getName());
			}
		}
		return jsonFiles;
	}
	
	protected void showSelectFileDialog() {
		ArrayList<String> jsonFiles = getSavedFiles();
		if (jsonFiles.size() == 0) {
			UiUtils.showToast(this, getString(R.string.no_saved_files));
			return;
		}
		SelectFileDialog dlg = new SelectFileDialog();
		Bundle args = new Bundle();
		args.putStringArrayList(Constants.EXTRA_JSON_ITEM, jsonFiles);
		args.putInt(Constants.EXTRA_ACTION, R.id.action_load_from_disk);
		dlg.setArguments(args);
		dlg.setCancelable(false);
		dlg.setListener(this);
		dlg.show(getFragmentManager(), TAG_SEL_FILE_DLG);
	}

	@Override
	public void onFileSelected(String fname) {
		File f = new File(getFilesDir(), fname);
		if (f.exists()) {
			Intent i = new Intent(this, TopologyFileActivity.class);
			i.putExtra(Constants.EXTRA_FILENAME, f.getAbsolutePath());
			startActivity(i);
		}
		else
			UiUtils.showToast(this, getString(R.string.file_not_found));
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
		switch (position) {
			case HomeAdapter.IDX_SERVER:
				workWithServer();
			break;
			case HomeAdapter.IDX_LOCAL:
				workWithFiles();
			break;
			case HomeAdapter.IDX_CONN_MGR:
				showConnMgr();
			break;
			case HomeAdapter.IDX_SETTINGS:
				showSettings();
			break;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		grid.setAdapter(new HomeAdapter(this));
	}
}
