package com.axway.ate.activity;

import com.axway.ate.fragment.EditFragment;
import com.axway.ate.fragment.HostFragment;

public class HostActivity extends SinglePaneActivity {
	
	@Override
	protected EditFragment onCreateFragment() {
		return new HostFragment();
	}
}
