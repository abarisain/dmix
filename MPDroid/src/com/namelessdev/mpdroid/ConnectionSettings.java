package com.namelessdev.mpdroid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

@SuppressWarnings("deprecation")
public class ConnectionSettings extends PreferenceActivity {
	public static final int MAIN = 0;

	private static final String KEY_CONNECTION_CATEGORY = "connectionCategory";

	private String mSSID;
	private PreferenceCategory mMasterCategory;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.connectionsettings);

		final PreferenceScreen preferenceScreen = getPreferenceScreen();

		mMasterCategory = (PreferenceCategory) preferenceScreen.findPreference(KEY_CONNECTION_CATEGORY);

		if (getIntent().getStringExtra("SSID") != null) {
			// WiFi-Based Settings
			mSSID = getIntent().getStringExtra("SSID");
			createDynamicSettings(mSSID, mMasterCategory);
		} else {
			// Default settings
			createDynamicSettings("", mMasterCategory);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MAIN, 0, R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert);

		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent i = null;

		switch (item.getItemId()) {

		case MAIN:
			i = new Intent(this, MainMenuActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		}
		return false;
	}

	private void createDynamicSettings(String keyPrefix, PreferenceCategory toCategory) {

		EditTextPreference prefHost = new EditTextPreference(this);
		prefHost.setDialogTitle(R.string.host);
		prefHost.setTitle(R.string.host);
		prefHost.setSummary(R.string.hostDescription);
		prefHost.setDefaultValue("");
		prefHost.setKey(keyPrefix + "hostname");
		toCategory.addPreference(prefHost);

		EditTextPreference prefPort = new EditTextPreference(this);
		prefPort.setDialogTitle(R.string.port);
		prefPort.setTitle(R.string.port);
		prefPort.setSummary(R.string.portDescription);
		prefPort.setDefaultValue("6600");
		prefPort.setKey(keyPrefix + "port");
		toCategory.addPreference(prefPort);

		EditTextPreference prefPassword = new EditTextPreference(this);
		prefPassword.setDialogTitle(R.string.password);
		prefPassword.setTitle(R.string.password);
		prefPassword.setSummary(R.string.passwordDescription);
		prefPassword.setDefaultValue("");
		prefPassword.setKey(keyPrefix + "password");
		toCategory.addPreference(prefPassword);

		EditTextPreference prefHostStreaming = new EditTextPreference(this);
		prefHostStreaming.setDialogTitle(R.string.hostStreaming);
		prefHostStreaming.setTitle(R.string.hostStreaming);
		prefHostStreaming.setSummary(R.string.hostStreamingDescription);
		prefHostStreaming.setDefaultValue("");
		prefHostStreaming.setKey(keyPrefix + "hostnameStreaming");
		toCategory.addPreference(prefHostStreaming);

		// Meh.
		EditTextPreference prefStreamingPort = new EditTextPreference(this);
		prefStreamingPort.setDialogTitle(R.string.portStreaming);
		prefStreamingPort.setTitle(R.string.portStreaming);
		prefStreamingPort.setSummary(R.string.portStreamingDescription);
		prefStreamingPort.setDefaultValue("8000");
		prefStreamingPort.setKey(keyPrefix + "portStreaming");
		toCategory.addPreference(prefStreamingPort);

		EditTextPreference suffixStreamingPort = new EditTextPreference(this);
		suffixStreamingPort.setDialogTitle(R.string.suffixStreaming);
		suffixStreamingPort.setTitle(R.string.suffixStreaming);
		suffixStreamingPort.setSummary(R.string.suffixStreamingDescription);
		suffixStreamingPort.setDefaultValue("");
		suffixStreamingPort.setKey(keyPrefix + "suffixStreaming");
		toCategory.addPreference(suffixStreamingPort);

		onContentChanged();

	}
}
