package com.axway.ate.activity;

import com.axway.ate.fragment.EditFragment;
import com.axway.ate.fragment.GroupFragment;

public class GroupActivity extends SinglePaneActivity {

	@Override
	protected EditFragment onCreateFragment() {
		return new GroupFragment();
	}
	
}
