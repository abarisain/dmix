package com.namelessdev.mpdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.WindowManager.BadTokenException;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.ConnectionListener;
import com.namelessdev.mpdroid.helpers.CachedMPD;
import com.namelessdev.mpdroid.tools.SettingsHelper;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import static android.util.Log.w;

public class MPDApplication extends Application implements ConnectionListener {

	public static final String TAG = "MPDroid";
	
	private static final long DISCONNECT_TIMER = 15000; 
	
	public MPDAsyncHelper oMPDAsyncHelper = null;
	private SettingsHelper settingsHelper = null;
	private ApplicationState state = new ApplicationState();

	private Collection<Object> connectionLocks = new LinkedList<Object>();
	private AlertDialog ad;
	private Activity currentActivity;
	private Timer disconnectSheduler;

	public class ApplicationState {
		public boolean streamingMode = false;
		public boolean settingsShown = false;
		public boolean warningShown = false;
		public MPDStatus currentMpdStatus = null;
	}
	
	class DialogClickListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case AlertDialog.BUTTON_NEUTRAL:
				// Show Settings
				currentActivity.startActivityForResult(new Intent(currentActivity, WifiConnectionSettings.class), SETTINGS);
				break;
			case AlertDialog.BUTTON_NEGATIVE:
				currentActivity.finish();
				break;
			case AlertDialog.BUTTON_POSITIVE:
				connectMPD();
				break;

			}
		}
	}

	public static final int SETTINGS = 5;

	@Override
	public void onCreate() {
		super.onCreate();
		System.err.println("onCreate Application");
		
		MPD.setApplicationContext(getApplicationContext());

		StrictMode.VmPolicy vmpolicy = new StrictMode.VmPolicy.Builder().penaltyLog().build();
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		StrictMode.setVmPolicy(vmpolicy);

		oMPDAsyncHelper = new MPDAsyncHelper();
		oMPDAsyncHelper.addConnectionListener((MPDApplication) getApplicationContext());
		
		settingsHelper = new SettingsHelper(this, oMPDAsyncHelper);
		
		disconnectSheduler = new Timer();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if(!settings.contains("albumTrackSort"))
			settings.edit().putBoolean("albumTrackSort", true).commit();
	}

	public void setActivity(Object activity) {
		if (activity instanceof Activity)
			currentActivity = (Activity) activity;
		
		connectionLocks.add(activity);
		checkConnectionNeeded();
		cancelDisconnectSheduler();
	}

	public void unsetActivity(Object activity) {
		connectionLocks.remove(activity);
		checkConnectionNeeded();
		
		if (currentActivity == activity)
			currentActivity = null;
	}

    private void checkConnectionNeeded() {
        if (connectionLocks.size() > 0) {
            if (!oMPDAsyncHelper.isMonitorAlive()) {
                oMPDAsyncHelper.startMonitor();
            }
            if (!oMPDAsyncHelper.oMPD.isConnected() && (currentActivity == null || !currentActivity.getClass().equals(WifiConnectionSettings.class))) {
                connect();
            }
        } else {
            disconnect();
        }
    }

	public void connect() {
		if(!settingsHelper.updateSettings()) {
			// Absolutely no settings defined! Open Settings!
			if (currentActivity != null && !state.settingsShown) {
				currentActivity.startActivityForResult(new Intent(currentActivity, WifiConnectionSettings.class), SETTINGS);
				state.settingsShown = true;
			}
		}
		
		if (currentActivity != null && !settingsHelper.warningShown() && !state.warningShown) {
			currentActivity.startActivity(new Intent(currentActivity, WarningActivity.class));
			state.warningShown = true;
		}
		connectMPD();
	}
	
	public void terminateApplication() {
		this.currentActivity.finish();
	}
	
	public void disconnect() {
		cancelDisconnectSheduler();
		startDisconnectSheduler();
	}
	
	private void startDisconnectSheduler() {
		disconnectSheduler.schedule(new TimerTask() {
			@Override
			public void run() {
				w(TAG, "Disconnecting (" + DISCONNECT_TIMER + " ms timeout)");
                oMPDAsyncHelper.stopMonitor();
				oMPDAsyncHelper.disconnect();
			}
		}, DISCONNECT_TIMER);
		
	}
	
	private void cancelDisconnectSheduler() {
		disconnectSheduler.cancel();
		disconnectSheduler.purge();
		disconnectSheduler = new Timer();
	}

	private void connectMPD() {
		// dismiss possible dialog
		dismissAlertDialog();
		
		// show connecting to server dialog
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
		
		cancelDisconnectSheduler();
		
		// really connect
		oMPDAsyncHelper.connect();
	}

	public synchronized void connectionFailed(String message) {

        if (ad != null && !(ad instanceof ProgressDialog) && ad.isShowing()) {
            return;
        }

		// dismiss possible dialog
		dismissAlertDialog();
		
        oMPDAsyncHelper.disconnect();

		if (currentActivity == null)
			return;
		
		if (currentActivity != null && connectionLocks.size() > 0) {
			// are we in the settings activity?
			if (currentActivity.getClass() == SettingsActivity.class) {
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
				builder.setCancelable(false);
				builder.setMessage(String.format(getResources().getString(R.string.connectionFailedMessageSetting), message));
				builder.setPositiveButton("OK", new OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
					}
				});
				ad = builder.show();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
				builder.setTitle(getResources().getString(R.string.connectionFailed));
				builder.setMessage(String.format(getResources().getString(R.string.connectionFailedMessage), message));
                builder.setCancelable(false);

				DialogClickListener oDialogClickListener = new DialogClickListener();
				builder.setNegativeButton(getResources().getString(R.string.quit), oDialogClickListener);
				builder.setNeutralButton(getResources().getString(R.string.settings), oDialogClickListener);
				builder.setPositiveButton(getResources().getString(R.string.retry), oDialogClickListener);
				try {
					ad = builder.show();
				} catch (BadTokenException e) {
					// Can't display it. Don't care.
				}
			}
		}

	}

	public synchronized void connectionSucceeded(String message) {
		dismissAlertDialog();
		// checkMonitorNeeded();
	}

	public ApplicationState getApplicationState() {
		return state;
	}
	
	private void dismissAlertDialog() {
		if (ad != null) {
			if (ad.isShowing()) {
				try {
					ad.dismiss();
				} catch (IllegalArgumentException e) {
					// We don't care, it has already been destroyed
				}
			}
		}
	}

	public boolean isTabletUiEnabled() {
		return getResources().getBoolean(R.bool.isTablet)
				&& PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tabletUI", true);
	}

	public boolean isLightThemeSelected() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lightTheme", false);
	}
}
