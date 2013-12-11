package com.axway.ate.activity;

import android.app.Fragment;

import com.axway.ate.fragment.SettingsFragment;

public class SettingsActivity extends SinglePaneActivity {

	@Override
	protected Fragment onCreateFragment() {
		return new SettingsFragment();
	}
}
