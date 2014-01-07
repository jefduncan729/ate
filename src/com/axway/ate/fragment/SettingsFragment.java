package com.axway.ate.fragment;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.axway.ate.Constants;
import com.axway.ate.R;
import com.axway.ate.util.IntegerPreference;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private IntegerPreference prefRefresh;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		initUi();
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (Constants.EXTRA_REFRESH_INTERVAL.equals(key)) {
			setRefreshSummary(prefs);
		}
	}

	private void initUi() {
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		prefRefresh = (IntegerPreference)getPreferenceScreen().findPreference(Constants.EXTRA_REFRESH_INTERVAL);
		setRefreshSummary(prefs);
	}
	
	private void setRefreshSummary(SharedPreferences prefs) {
		int r = prefs.getInt(Constants.EXTRA_REFRESH_INTERVAL, Constants.DEF_REFRESH_SECS);
		String s = "The Service Monitor will ";
		if (r == 0)
			s = s + "not refresh automatically";
		else
			s = s + " refresh every " + Integer.toString(r) + " seconds";
		prefRefresh.setSummary(s);
	}
}
