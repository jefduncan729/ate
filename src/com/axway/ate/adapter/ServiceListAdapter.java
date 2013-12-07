package com.axway.ate.adapter;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.vordel.api.topology.model.Service;

public class ServiceListAdapter extends BaseListAdapter<Service> {


	public ServiceListAdapter(Context ctx, List<Service> list) {
		super(ctx, list);
	}
	
	@Override
	protected void populateView(Service s, View view) {
		TextView txt01 = (TextView)view.findViewById(android.R.id.text1);
		txt01.setText(s.getName());
	}
}
