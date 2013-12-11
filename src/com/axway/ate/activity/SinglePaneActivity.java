package com.axway.ate.activity;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.axway.ate.R;
import com.axway.ate.fragment.EditFragment;

abstract public class SinglePaneActivity extends BaseActivity {
	private static final String TAG = SinglePaneActivity.class.getSimpleName();
	
	protected Fragment frag;
	
	protected SinglePaneActivity() {
		super();
		frag = null;
	}
	
	abstract protected Fragment onCreateFragment();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        if (getIntent().hasExtra(Intent.EXTRA_TITLE)) {
            setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));
        }

        final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        setTitle(customTitle != null ? customTitle : getTitle());

        if (savedInstanceState == null) {
            frag = onCreateFragment();
            frag.setArguments(intentToFragArgs(getIntent()));
            getFragmentManager().beginTransaction()
                    .add(R.id.container01, frag, "single_pane")
                    .commit();
        } 
        else {
            frag = (EditFragment)getFragmentManager().findFragmentByTag("single_pane");
        }
	}

	protected int getLayoutId() {
		return R.layout.single_pane;
	}
	
	protected Bundle intentToFragArgs(Intent i) {
		Bundle rv = new Bundle();
		if (i.getExtras() != null)
			rv.putAll(i.getExtras());
		return rv;
	}
//
//	@Override
//	protected void afterServiceConnected(boolean isConnected) {
//		super.afterServiceConnected(isConnected);
//		Log.d(TAG, "afterServiceConnected");
//		if (frag != null)
//			frag.onServiceAvailable(service);
//	}
}
