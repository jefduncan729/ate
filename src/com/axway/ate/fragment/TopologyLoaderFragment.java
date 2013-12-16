package com.axway.ate.fragment;

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;

import com.axway.ate.adapter.TopologyAdapter;
import com.vordel.api.topology.model.Topology;

public class TopologyLoaderFragment extends TopologyListFragment implements OnItemClickListener, LoaderManager.LoaderCallbacks<Topology> {
	
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
//
//	@Override
//	public void onViewCreated(View view, Bundle savedInstanceState) {
//		super.onViewCreated(view, savedInstanceState);
//		setEmptyText("No topology loaded\n(visit Connection Manager)");
//	}

	@Override
	public Loader<Topology> onCreateLoader(int id, Bundle args) {
		return new TopologyLoader(getActivity(), args);
	}

	@Override
	public void onLoadFinished(Loader<Topology> loader, Topology newT) {
		t = newT;
		if (listener != null)
			listener.onTopologyLoaded(t);
		if (t == null)
			setListAdapter(null);
		else {
			getListView().setOnItemClickListener(this);
			setListAdapter(new TopologyAdapter(getActivity(), t, src));
		}
		updateOptionsMenu();
	}

	@Override
	public void onLoaderReset(Loader<Topology> loader) {
		setListAdapter(null);
	}
}
