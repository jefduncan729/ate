package com.axway.ate.activity;

import com.axway.ate.R;
import com.axway.ate.fragment.SettingsFragment;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

public class SettingsActivity extends ServiceAwareActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.app_name) + " - " + getString(R.string.action_settings));
		FragmentManager fm = getFragmentManager();
		if (fm.findFragmentById(android.R.id.content) == null) {
			SettingsFragment frag = new SettingsFragment();
			fm.beginTransaction().add(android.R.id.content, frag).commit();
		}
	}
}
