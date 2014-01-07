package com.axway.ate.fragment;

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;

import com.axway.ate.ApiException;
import com.axway.ate.DomainHelper;
import com.axway.ate.adapter.TopologyAdapter;
import com.axway.ate.async.JsonLoader;
import com.axway.ate.async.JsonLoader.ExceptionHandler;
import com.google.gson.JsonElement;

public class TopologyLoaderFragment extends TopologyListFragment implements OnItemClickListener, LoaderManager.LoaderCallbacks<JsonElement>, ExceptionHandler {
	
	private static final String TAG = TopologyLoaderFragment.class.getSimpleName();

	public TopologyLoaderFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Bundle args = getArguments();
//		String url = args.getString(Constants.EXTRA_URL);		
//		Ion.with(getActivity(), url).setLogging(TAG, Log.DEBUG).asString().setCallback(new FutureCallback<String>() {
//
//			@Override
//			public void onCompleted(Exception excp, String json) {
//				t = DomainHelper.getInstance().topologyFromJson(json);	//newT.getAsJsonObject());
//				if (listener != null)
//					listener.onTopologyLoaded(t);
//				if (t == null)
//					setListAdapter(null);
//				else {
//					getListView().setOnItemClickListener(TopologyLoaderFragment.this);
//					setListAdapter(new TopologyAdapter(getActivity(), t, src));
//				}
//			}
//		});
		getLoaderManager().initLoader(0, getArguments(), this);
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rv = inflater.inflate(android.R.layout.list_content, null);
		return rv;
	}

	@Override
	public Loader<JsonElement> onCreateLoader(int id, Bundle args) {
		JsonLoader rv = new JsonLoader(getActivity(), args);
		rv.setExceptionHandler(this);
		return rv;
	}

	@Override
	public void onLoadFinished(Loader<JsonElement> loader, JsonElement newT) {
		if (newT != null && newT.isJsonObject())
			t = DomainHelper.getInstance().topologyFromJson(newT.getAsJsonObject());
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
	public void onLoaderReset(Loader<JsonElement> loader) {
		setListAdapter(null);
	}

	@Override
	public void onLoaderException(ApiException e) {
		if (listener != null)
			listener.onLoadError(e);
	}
}
