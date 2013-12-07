package com.axway.ate.adapter;

import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.axway.ate.api.ServerInfo;

public class ServerInfoListAdapter extends BaseListAdapter<ServerInfo> {
	
	public ServerInfoListAdapter(Context ctx, List<ServerInfo> list) {
		super(ctx, list);
	}

	@Override
	protected void populateView(ServerInfo item, View view) {
		if (item == null || view == null)
			return;
		TextView txt = (TextView)view.findViewById(android.R.id.text1);
		StringBuilder sb = new StringBuilder(item.getHost());
		sb.append(":").append(item.getPort());
		txt.setText(sb.toString());
	}
}
