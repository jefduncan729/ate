package com.axway.ate.fragment;

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;

import com.axway.ate.DomainHelper;
import com.axway.ate.adapter.TopologyAdapter;
import com.axway.ate.async.JsonLoader;
import com.axway.ate.async.TopologyLoader;
import com.google.gson.JsonObject;
import com.vordel.api.topology.model.Topology;

public class TopologyLoaderFragment extends TopologyListFragment implements OnItemClickListener, LoaderManager.LoaderCallbacks<JsonObject> {
	
	private static final String TAG = TopologyLoaderFragment.class.getSimpleName();

	public TopologyLoaderFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getLoaderManager().initLoader(0, getArguments(), this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = inflater.inflate(android.R.layout.list_content, null);
		return rv;
	}

	@Override
	public Loader<JsonObject> onCreateLoader(int id, Bundle args) {
		return new JsonLoader(getActivity(), args);
	}

	@Override
	public void onLoadFinished(Loader<JsonObject> loader, JsonObject newT) {
		t = DomainHelper.getInstance().topologyFromJson(newT);
		if (listener != null)
			listener.onTopologyLoaded(t);
		if (t == null)
			setListAdapter(null);
		else {
			getListView().setOnItemClickListener(this);
			setListAdapter(new TopologyAdapter(getActivity(), t, src));
		}
	}

	@Override
	public void onLoaderReset(Loader<JsonObject> loader) {
		setListAdapter(null);
	}
}
