package com.axway.ate.adapter;

import com.axway.ate.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class HomeAdapter extends BaseAdapter {
	
	private static final String TAG = HomeAdapter.class.getSimpleName();
	private static final int[] DRAWABLE_IDS = { R.drawable.internet, R.drawable.folder, R.drawable.server, R.drawable.preferences };
	private static final int[] STRING_IDS = { R.string.connect_anm, R.string.local_files, R.string.action_conn_mgr, R.string.action_settings };
	
	public static final int IDX_SERVER = 0;
	public static final int IDX_LOCAL = 1;
	public static final int IDX_CONN_MGR = 2;
	public static final int IDX_SETTINGS = 3;

	private LayoutInflater inflater;
	private String[] labels;
	
	public HomeAdapter(Context ctx) {
		super();
		inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		labels = new String[STRING_IDS.length];
		for (int i = 0; i < STRING_IDS.length; i++)
			labels[i] = ctx.getString(STRING_IDS[i]);
	}
	
	@Override
	public int getCount() {
		return DRAWABLE_IDS.length;
	}

	@Override
	public Object getItem(int position) {
		return DRAWABLE_IDS[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rv = inflater.inflate(R.layout.launcher_item, null);
		ImageView img = (ImageView)rv.findViewById(android.R.id.icon);
		TextView txt = (TextView)rv.findViewById(android.R.id.text1);
		img.setImageResource(DRAWABLE_IDS[position]);
		txt.setText(labels[position]);
		return rv;
	}
}
