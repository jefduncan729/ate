package com.axway.ate.adapter;

import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TagListAdapter extends BaseAdapter {
	private static final String TAG = TagListAdapter.class.getSimpleName();

	private Map<String, String> tags;
	private LayoutInflater inflater;
	
	public TagListAdapter(Context ctx, Map<String, String> tags) {
		super();
		this.tags = tags;
		inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount() {
		if (tags == null)
			return 0;
		return tags.size();
	}

	@Override
	public Object getItem(int pos) {
		if (tags == null || pos < 0 || pos >= tags.size())
			return null;
		Map.Entry<String, String> rv = null;
		Set<Map.Entry<String, String>> entries = tags.entrySet();
		int i = 0;
		for (Map.Entry<String, String> entry: entries) {
			if (i++ == pos) {
				rv = entry;
				break;
			}
		}
		return rv;
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}

	@Override
	public View getView(int pos, View view, ViewGroup parent) {
		View rv = view;
		if (rv == null) {
			rv = inflater.inflate(android.R.layout.simple_list_item_1, null);
		}
		TextView txt = (TextView)rv.findViewById(android.R.id.text1);
		Map.Entry<String, String> item = (Map.Entry<String, String>)getItem(pos);
		if (item != null) {
			txt.setText(item.getKey() + ": " + item.getValue());
		}
		return rv;
	}
}
