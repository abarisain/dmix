package com.namelessdev.mpdroid;

import java.util.Collection;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.WindowManager.BadTokenException;

import com.namelessdev.mpdroid.MPDAsyncHelper.ConnectionListener;

public class MPDApplication extends Application implements ConnectionListener, OnSharedPreferenceChangeListener {

	private Collection<Activity> connectionLocks = new LinkedList<Activity>();
	private AlertDialog ad;
	private DialogClickListener oDialogClickListener;

	private boolean streamingMode = false;
	private boolean settingsShown = false;
	private boolean warningShown = false;

	private static final int DEFAULT_MPD_PORT = 6600;
	private static final int DEFAULT_STREAMING_PORT = 8000;

	private Activity currentActivity;

	public static final int SETTINGS = 5;

	public void setActivity(Activity activity) {
		currentActivity = activity;
		connectionLocks.add(activity);
		checkMonitorNeeded();
		checkConnectionNeeded();
	}

	public void unsetActivity(Activity activity) {
		connectionLocks.remove(activity);
		checkMonitorNeeded();
		checkConnectionNeeded();
		if (currentActivity == activity)
			currentActivity = null;
	}

	private void checkMonitorNeeded() {
		if (connectionLocks.size() > 0) {
			if (!oMPDAsyncHelper.isMonitorAlive())
				oMPDAsyncHelper.startMonitor();
		} else
			oMPDAsyncHelper.stopMonitor();

	}

	private void checkConnectionNeeded() {
		if (connectionLocks.size() > 0) {
			if (!oMPDAsyncHelper.oMPD.isConnected() && !currentActivity.getClass().equals(WifiConnectionSettings.class)) {
				connect();
			}

		} else {
			disconnect();
		}
	}

	public void disconnect() {
		oMPDAsyncHelper.disconnect();
	}

	public void connect() {
		// Get Settings...
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);// getSharedPreferences("org.pmix", MODE_PRIVATE);
		settings.registerOnSharedPreferenceChangeListener(this);

		String wifiSSID = getCurrentSSID();

		if (!settings.getString(wifiSSID + "hostname", "").equals("")) {
			readSettings(settings, wifiSSID);
		} else if (!settings.getString("hostname", "").equals("")) {
			readSettings(settings, null);
		} else {
			// Absolutely no settings defined! Open Settings!
			if (currentActivity != null && !settingsShown) {
				currentActivity.startActivityForResult(new Intent(currentActivity, WifiConnectionSettings.class), SETTINGS);
				settingsShown = true;
			}
		}
		if (currentActivity != null && !settings.getBoolean("warningShown", false) && !warningShown) {
			currentActivity.startActivity(new Intent(currentActivity, WarningActivity.class));
			warningShown = true;
		}
		connectMPD();

	}

	private void connectMPD() {
		if (ad != null) {
			if (ad.isShowing()) {
				try {
					ad.dismiss();
				} catch (IllegalArgumentException e) {
					// We don't care, it has already been destroyed
				}
			}
		}
		if (!isNetworkConnected()) {
			connectionFailed("No network.");
			return;
		}
		if (currentActivity != null) {
			ad = new ProgressDialog(currentActivity);
			ad.setTitle(getResources().getString(R.string.connecting));
			ad.setMessage(getResources().getString(R.string.connectingToServer));
			ad.setCancelable(false);
			ad.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					// Handle all keys!
					return true;
				}
			});
			try {
				ad.show();
			} catch (BadTokenException e) {
				// Can't display it. Don't care.
			}
		}
		oMPDAsyncHelper.doConnect();
	}

	public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
		String wifiSSID = getCurrentSSID();

		if (key.equals("albumartist")) {
			// clear current cached artist list on change of tag settings
			// ArtistsActivity.items = null;

		} else if (!settings.getString(wifiSSID + "hostname", "").equals("")) {
			readSettings(settings, wifiSSID);
		} else if (!settings.getString("hostname", "").equals("")) {
			readSettings(settings, null);
		} else {
			return;
		}
	}

	public void connectionFailed(String message) {
		System.out.println("Connection Failed: " + message);
		if (ad != null) {
			if (ad.isShowing()) {
				try {
					ad.dismiss();
				} catch (IllegalArgumentException e) {
					// We don't care, it has already been destroyed
				}
			}
		}
		if (currentActivity == null) {
			return;
		}
		if (currentActivity != null && connectionLocks.size() > 0 && currentActivity.getClass() != null) {
			if (currentActivity.getClass().equals(SettingsActivity.class)) {

				AlertDialog.Builder test = new AlertDialog.Builder(currentActivity);
				test.setMessage("Connection failed, check your connection settings. (" + message + ")");
				test.setPositiveButton("OK", new OnClickListener() {

					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub

					}
				});
				ad = test.show();
			} else {
				System.out.println(this.getClass());
				oDialogClickListener = new DialogClickListener();
				AlertDialog.Builder test = new AlertDialog.Builder(currentActivity);
				test.setTitle(getResources().getString(R.string.connectionFailed));
				test.setMessage(String.format(getResources().getString(R.string.connectionFailedMessage), message));
				test.setNegativeButton(getResources().getString(R.string.quit), oDialogClickListener);
				test.setNeutralButton(getResources().getString(R.string.settings), oDialogClickListener);
				test.setPositiveButton(getResources().getString(R.string.retry), oDialogClickListener);
				try {
					ad = test.show();
				} catch (BadTokenException e) {
					// Can't display it. Don't care.
				}
			}
		}

	}

	private void readSettings(SharedPreferences settings, String wifiSSID) {
		String sServer = "";
		if (wifiSSID == null)
			sServer = settings.getString("hostname", "");
		else
			sServer = settings.getString(wifiSSID + "hostname", "");

		int iPort, iPortStreaming;
		try {
			iPort = Integer.parseInt(settings.getString(wifiSSID + "port", Integer.toString(DEFAULT_MPD_PORT)).trim());
		} catch (NumberFormatException e) {
			iPort = DEFAULT_MPD_PORT;
		}
		try {
			iPortStreaming = Integer.parseInt(settings.getString(wifiSSID + "portStreaming", Integer.toString(DEFAULT_STREAMING_PORT))
					.trim());
		} catch (NumberFormatException e) {
			iPortStreaming = DEFAULT_STREAMING_PORT;
		}

		String sServerStreaming = settings.getString(wifiSSID + "hostnameStreaming", "");
		if (sServerStreaming.trim().equals("")) {
			sServerStreaming = null;
		}
		String sPassword = settings.getString(wifiSSID + "password", "");
		oMPDAsyncHelper.setConnectionInfo(sServer, iPort, sPassword, sServerStreaming, iPortStreaming);
	}

	public void connectionSucceeded(String message) {
		try {
			ad.dismiss();
		} catch (IllegalArgumentException e) {
			// Do nothing, maybe it has already been dismissed because of a rotation
		}
		// checkMonitorNeeded();
	}

	public class DialogClickListener implements OnClickListener {

		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case AlertDialog.BUTTON3:
				// Show Settings
				currentActivity.startActivityForResult(new Intent(currentActivity, WifiConnectionSettings.class), SETTINGS);
				break;
			case AlertDialog.BUTTON2:
				currentActivity.finish();
				break;
			case AlertDialog.BUTTON1:
				connectMPD();
				break;

			}
		}
	}

	private WifiManager mWifiManager;

	// Change this... (sag)
	public MPDAsyncHelper oMPDAsyncHelper = null;

	@Override
	public void onCreate() {
		super.onCreate();
		System.err.println("onCreate Application");

		oMPDAsyncHelper = new MPDAsyncHelper();
		oMPDAsyncHelper.addConnectionListener((MPDApplication) getApplicationContext());

		mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

	}

	public String getCurrentSSID() {
		WifiInfo info = mWifiManager.getConnectionInfo();
		return info.getSSID();
	}

	public void setWifiConnected(boolean bWifiConnected) {
		if (bWifiConnected) {
			connect();
			// checkMonitorNeeded();
		} else {
			// disconnect();
		}
	}

	public boolean isWifiConnected() {
		return true;
		// return bWifiConnected;
		// TODO : DIRTY WIFI HACK :(
	}

	public boolean isNetworkConnected() {
		ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (conMgr.getActiveNetworkInfo() == null)
			return false;
		return (conMgr.getActiveNetworkInfo().isAvailable() && conMgr.getActiveNetworkInfo().isConnected());
	}

	public void setStreamingMode(boolean streamingMode) {
		this.streamingMode = streamingMode;
	}

	public boolean isStreamingMode() {
		return streamingMode;
	}

}
