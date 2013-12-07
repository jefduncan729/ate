package com.axway.ate.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.db.DbHelper.ConnMgrColumns;

public class ConnMgrFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener{
	private static final String TAG = ConnMgrFragment.class.getSimpleName();

	public interface Listener {
		public void onDelete(Uri uri, String name);
		public void onAdd();
		public void onEdit(Uri uri);
		public void onStatusChange(Uri uri, int newStatus);
		public void onCheckCert(Uri uri);
	}
	
	private CursorAdapter adapter;
	private Listener listener;
	private Uri ctxUri;
	private String ctxName;

	public ConnMgrFragment() {
		super();
		adapter = null;
		ctxUri = null;
		ctxName = null;
		listener = null;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		adapter = new ConnMgrAdapter(getActivity(), null, 0);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof Listener)
			listener = (Listener)activity;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		getListView().setOnCreateContextMenuListener(this);
		setEmptyText("Add a connection");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.conn_mgr, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean rv = true;
		switch (item.getItemId()) {
			case R.id.action_add:
				if (listener != null)
					listener.onAdd();
			break;
			default:
				rv = super.onOptionsItemSelected(item); 
		}
		return rv;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View view, int pos, long id) {
		Cursor c = (Cursor)listView.getItemAtPosition(pos);
		if (listener == null || c == null)
			return;
		Uri uri = ContentUris.withAppendedId(ConnMgrColumns.CONTENT_URI, c.getLong(ConnMgrColumns.IDX_ID)); 
		listener.onEdit(uri);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (listener == null || ctxUri == null)
			return false;
		boolean rv = true;
		switch (item.getItemId()) {
			case R.id.action_delete:
				listener.onDelete(ctxUri, ctxName);
			break;
			case R.id.action_enable:
				listener.onStatusChange(ctxUri, Constants.STATUS_ACTIVE);
			break;
			case R.id.action_disable:
				listener.onStatusChange(ctxUri, Constants.STATUS_INACTIVE);
			break;
			case R.id.action_check_cert:
				listener.onCheckCert(ctxUri);
			break;
			default:
				rv = super.onContextItemSelected(item);
		}
		return rv;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		ctxUri = null;
		ctxName = null;
		AdapterContextMenuInfo cmi = (AdapterContextMenuInfo)menuInfo;
		Cursor c = (Cursor)getListView().getItemAtPosition(cmi.position);
		if (c == null)
			return;
		ctxUri = ContentUris.withAppendedId(ConnMgrColumns.CONTENT_URI, c.getLong(ConnMgrColumns.IDX_ID));
		ctxName = c.getString(ConnMgrColumns.IDX_HOST) + ":" + Integer.toString(c.getInt(ConnMgrColumns.IDX_PORT));
		menu.setHeaderTitle(c.getString(ConnMgrColumns.IDX_HOST));
		menu.add(0, R.id.action_delete, 1, R.string.action_delete);
		int status = c.getInt(ConnMgrColumns.IDX_STATUS);
		if (status == Constants.STATUS_ACTIVE)
			menu.add(0, R.id.action_disable, 2, R.string.action_disable);
		else
			menu.add(0, R.id.action_enable, 3, R.string.action_enable);
		boolean ssl = (c.getInt(ConnMgrColumns.IDX_USE_SSL) == 1);
		if (ssl)
			menu.add(0, R.id.action_check_cert, 4, R.string.action_check_cert);
	}

	public void refresh() {
		getLoaderManager().restartLoader(0, null, this);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), ConnMgrColumns.CONTENT_URI, null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		adapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
	
	private class ConnMgrAdapter extends CursorAdapter {

		private class ViewHolder {
			TextView txt01;
			TextView txt02;
			
			public ViewHolder() {
				super();
				txt01 = null;
				txt02 = null;
			}
		}
		
		public ConnMgrAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder)view.getTag();
			if (holder == null)
				return;
			if (holder.txt01 != null) {
				String h = cursor.getString(ConnMgrColumns.IDX_HOST);
				int p = cursor.getInt(ConnMgrColumns.IDX_PORT);
				holder.txt01.setText(h + ":" + Integer.toString(p));
				int status = cursor.getInt(ConnMgrColumns.IDX_STATUS);
				if (status == Constants.STATUS_ACTIVE)
					holder.txt01.setPaintFlags(holder.txt01.getPaintFlags()  & ~Paint.STRIKE_THRU_TEXT_FLAG);
				else
					holder.txt01.setPaintFlags(holder.txt01.getPaintFlags()  | Paint.STRIKE_THRU_TEXT_FLAG);
			}
			if (holder.txt02 != null) {
				holder.txt02.setText(buildDetails(cursor));
			}
		}

		private String buildDetails(Cursor cursor) {
			StringBuilder sb = new StringBuilder();
			sb.append("SSL: ");
			if (cursor.getInt(ConnMgrColumns.IDX_USE_SSL) == 1)
				sb.append("yes");
			else
				sb.append("no");
			String usr = cursor.getString(ConnMgrColumns.IDX_USER);
			if (!TextUtils.isEmpty(usr))
				sb.append(", user: ").append(usr);
			return sb.toString();
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View rv = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
			ViewHolder holder = new ViewHolder();
			holder.txt01 = (TextView)rv.findViewById(android.R.id.text1);
			holder.txt02 = (TextView)rv.findViewById(android.R.id.text2);
			rv.setTag(holder);
			return rv;
		}
		
	}
}
