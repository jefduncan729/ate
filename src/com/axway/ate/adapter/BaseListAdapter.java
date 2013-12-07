package com.axway.ate.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

abstract public class BaseListAdapter<T> extends BaseAdapter {

	private List<T> list;
	private LayoutInflater inflater;
	
	public BaseListAdapter(Context ctx, List<T> list) {
		super();
		this.list = list;
		inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	abstract protected void populateView(T item, View view);
	
	protected int getLayoutId() {
		return android.R.layout.simple_list_item_1;
	}
	
	@Override
	public int getCount() {
		int rv = 0;
		if (list != null)
			rv = list.size();
		return rv;
	}

	@Override
	public T getItem(int pos) {
		T rv = null;
		if (list != null && (pos >= 0 && pos < list.size())) {
			rv = list.get(pos);
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
			rv = inflater.inflate(getLayoutId(), null);
		}
		T item = (T)getItem(pos);
		if (item != null)
			populateView(item, rv);
		return rv;
	}
}
