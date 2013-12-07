package com.axway.ate.fragment;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.axway.ate.adapter.ServerInfoListAdapter;
import com.axway.ate.api.ServerInfo;

public class SelectServerDialog extends DialogFragment implements OnItemClickListener {

	public interface Listener {
		public void onServerSelected(ServerInfo info, int action);
	}
	
	private static final String TAG = SelectServerDialog.class.getSimpleName();
	private List<ServerInfo> list;
	private ListView listView;
	private Listener listener;
	private int action;
	
	public SelectServerDialog() {
		super();
		list = null;
		listener = null;
		action = 0;
	}

	public void setListener(Listener l) {
		listener = l;
	}
	
	@Override
	public void onItemClick(AdapterView<?> lv, View view, int pos, long id) {
		ServerInfo si = (ServerInfo)lv.getItemAtPosition(pos);
		if (listener != null && si != null) {
			listener.onServerSelected(si, action);
			dismiss();
		}
	}
	
	public void setServerInfoList(List<ServerInfo> newVal) {
		list = newVal;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
    	Activity a = getActivity();
    	LayoutInflater inflater = LayoutInflater.from(a);
    	View dlgView = inflater.inflate(android.R.layout.list_content, null);
    	listView = (ListView)dlgView.findViewById(android.R.id.list);
		listView.setOnItemClickListener(this);
		listView.setAdapter(new ServerInfoListAdapter(getActivity(), list));
		AlertDialog.Builder bldr = new AlertDialog.Builder(a);
		bldr.setTitle("Select Connection")
			.setView(dlgView);
		bldr.setNegativeButton(android.R.string.cancel, AlertDialogFragment.NOOP_LISTENER);
		return bldr.create();
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}
}
