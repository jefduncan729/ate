package com.axway.ate.fragment;

import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;

import com.axway.ate.Constants;
import com.axway.ate.DomainHelper;
import com.axway.ate.adapter.TopologyAdapter;
import com.vordel.api.topology.model.Topology;

public class TopologyFileFragment extends TopologyListFragment implements OnItemClickListener {
	private static final String TAG = TopologyFileFragment.class.getSimpleName();

	public TopologyFileFragment() {
		super();
		t = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		Bundle args = getArguments();
		if (args != null) {
			t = DomainHelper.getInstance().topologyFromJson(args.getString(Constants.EXTRA_JSON_TOPOLOGY));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshAdapter();
	}
	
	public void update(Topology in, String src, boolean haveConsole) {
		t = in;
		this.src = src;
		this.haveConsole = haveConsole;
		refreshAdapter();
	}
	
	private void refreshAdapter() {
		if (t == null)
			setListAdapter(null);
		else {
			getListView().setOnItemClickListener(this);
			setListAdapter(new TopologyAdapter(getActivity(), t, src));
		}
	}
}
