package com.axway.ate.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.axway.ate.R;

public class HomeAdapter extends BaseAdapter {
	
	private static final String TAG = HomeAdapter.class.getSimpleName();
	private static final int[] DRAWABLE_IDS = { R.drawable.server, R.drawable.internet, R.drawable.folder};	// , R.drawable.preferences };
	private static final int[] STRING_IDS = { R.string.action_conn_mgr, R.string.connect_anm, R.string.local_files};	//, R.string.action_settings };
	
	public static final int IDX_CONN_MGR = 0;
	public static final int IDX_SERVER = 1;
	public static final int IDX_LOCAL = 2;
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
		View rv = null;
		if (position < DRAWABLE_IDS.length) {
			rv = inflater.inflate(R.layout.launcher_item, null);
			ImageView img = (ImageView)rv.findViewById(android.R.id.icon);
			TextView txt = (TextView)rv.findViewById(android.R.id.text1);
			img.setImageResource(DRAWABLE_IDS[position]);
			txt.setText(labels[position]);
		}
//		else {
//			rv = inflater.inflate(R.layout.chart_image, null);
//			ImageView img = (ImageView)rv.findViewById(android.R.id.icon);
////			StringBuilder url = new StringBuilder("https://chart.googleapis.com/chart?cht=p3&chtt=System+Overview&chs=300x200&chd=t:");
////			url.append("80,11,9");
////			url.append("&chdl=success|failure|exception&chds=0,100&chco=ff0000,00ff00,0000ff&chma=5,5,10,10&chm=d,FF0000,0,-1,5");
//			Ion.with(img).load("https://chart.googleapis.com/chart?cht=p3&chtt=System+Overview&chs=300x200&chd=t:80,11,9&chdl=success%7cfailure%7cexception&chds=0,100&chco=ff0000,00ff00,0000ff&chma=5,5,10,10&chm=d,FF0000,0,-1,5");
//		}
		return rv;
	}
}
