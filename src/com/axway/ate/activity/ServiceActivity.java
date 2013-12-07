package com.axway.ate.activity;

import com.axway.ate.fragment.EditFragment;
import com.axway.ate.fragment.ServiceFragment;

public class ServiceActivity extends SinglePaneActivity {

	@Override
	protected EditFragment onCreateFragment() {
		return new ServiceFragment();
	}
}
