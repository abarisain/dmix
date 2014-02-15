package org.musicpd.android.tools;

import org.a0z.mpd.MPD;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import org.musicpd.android.helpers.MPDAsyncHelper;

public class SettingsHelper implements OnSharedPreferenceChangeListener {
	private static final int DEFAULT_MPD_PORT = 6600;
	private static final int DEFAULT_STREAMING_PORT = 8000;
	
	private WifiManager mWifiManager;
	private SharedPreferences settings;
	private MPDAsyncHelper oMPDAsyncHelper;
	
	public SettingsHelper(ContextWrapper parent, MPDAsyncHelper MPDAsyncHelper) {
		// Get Settings and register ourself for updates
		settings = PreferenceManager.getDefaultSharedPreferences(parent);// getSharedPreferences("org.pmix", MODE_PRIVATE);
		settings.registerOnSharedPreferenceChangeListener(this);
		
		// get reference on WiFi service
		mWifiManager = (WifiManager) parent.getSystemService(Context.WIFI_SERVICE);
		
		oMPDAsyncHelper = MPDAsyncHelper;
	}
	
	public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
		updateSettings();
	}
	
	public boolean warningShown() {
		return getBooleanSetting("newWarningShown");
	}
	
	public void setHostname(String hostname) {
		String wifiSSID = getCurrentSSID();
		settings
			.edit()
			.putString(getStringWithSSID("hostname", wifiSSID), hostname)
			.commit();
	}
	
	public boolean updateSettings() {
		
		
		MPD.setSortByTrackNumber(settings.getBoolean("albumTrackSort", MPD.sortByTrackNumber()));
		MPD.setSortAlbumsByYear(settings.getBoolean("sortAlbumsByYear", MPD.sortAlbumsByYear()));
		MPD.setUseAlbumArtist(settings.getBoolean("albumartist", MPD.useAlbumArtist()));
		MPD.setShowAlbumTrackCount(settings.getBoolean("showAlbumTrackCount", MPD.showAlbumTrackCount()));
		// MPD.setShowArtistAlbumCount(settings.getBoolean("showArtistAlbumCount", MPD.showArtistAlbumCount()));

		return updateConnectionSettings();
	}
	
	public boolean updateConnectionSettings(){
		return updateConnectionSettings(getCurrentConfiguredSSID());
	}
	
	private boolean updateConnectionSettings(String wifiSSID) {
		if (getStringSetting(getStringWithSSID("hostname", wifiSSID)) == null)
			return false;
		
		oMPDAsyncHelper.getConnectionSettings().sServer				= getStringSetting(getStringWithSSID("hostname", wifiSSID));
		oMPDAsyncHelper.getConnectionSettings().iPort				= getIntegerSetting(getStringWithSSID("port", wifiSSID), DEFAULT_MPD_PORT);
		oMPDAsyncHelper.getConnectionSettings().sPassword			= getStringSetting(getStringWithSSID("password", wifiSSID));
		oMPDAsyncHelper.getConnectionSettings().sServerStreaming	= getStringSetting(getStringWithSSID("hostnameStreaming", wifiSSID));
		oMPDAsyncHelper.getConnectionSettings().iPortStreaming		= getIntegerSetting(getStringWithSSID("portStreaming", wifiSSID), DEFAULT_STREAMING_PORT);
		oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming = getStringSetting(getStringWithSSID("suffixStreaming", wifiSSID));
		if (oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming == null)
			oMPDAsyncHelper.getConnectionSettings().sSuffixStreaming = "";
		
		return true;
	}

	private int getIntegerSetting(String name, int defaultValue) {
		try {
			return Integer.parseInt(settings.getString(name, Integer.toString(defaultValue)).trim());
		} catch (NumberFormatException e) {
			return DEFAULT_MPD_PORT;
		}
	}
	
	private String getStringSetting(String name) {
		String value = settings.getString(name, "").trim();
		
		if (value.equals(""))
			return null;
		else
			return value;
	}
	
	private boolean getBooleanSetting(String name) {
		return settings.getBoolean(name, false);
	}
	
	private String getCurrentConfiguredSSID() {
		String wifiSSID = getCurrentSSID();
		return
			getStringSetting(getStringWithSSID("hostname",  wifiSSID)) == null ? null :
			wifiSSID == null || wifiSSID.trim().equals("") ? null :
			wifiSSID;
	}

	private String getCurrentSSID() {
		WifiInfo info = mWifiManager.getConnectionInfo();
		final String ssid = info.getSSID();
		return ssid == null ? null : ssid.replace("\"", "");
	}
	
	private String getStringWithSSID(String param, String wifiSSID) {
		if (wifiSSID == null)
			return param;
		else
			return wifiSSID + param;
	}
}
