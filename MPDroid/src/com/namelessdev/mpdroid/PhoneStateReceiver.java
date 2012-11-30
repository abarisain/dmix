package com.namelessdev.mpdroid;

import java.net.UnknownHostException;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.MPDConnectionInfo;
import com.namelessdev.mpdroid.tools.SettingsHelper;

public class PhoneStateReceiver extends BroadcastReceiver {
	// Used to trace when the app pauses / resumes playback
	private static final String PAUSED_MARKER = "wasPausedInCall";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(MPDApplication.TAG, "Phonestate received");
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		// Get config vars
		boolean pauseOnCall = settings.getBoolean("pauseOnPhoneStateChange",
				false);
		boolean playOnCallStop = pauseOnCall
				&& settings.getBoolean("playOnPhoneStateChange", false);

		Log.d(MPDApplication.TAG, "Pause on call " + pauseOnCall);
		if (pauseOnCall) {
			Bundle bundle = intent.getExtras();
			if (null == bundle) {
				Log.d(MPDApplication.TAG, "Bundle was null");
				return;
			}
			String state = bundle.getString(TelephonyManager.EXTRA_STATE);

			final boolean shouldPause = pauseOnCall
					&& (state
							.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING) || state
							.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK));
			Log.d(MPDApplication.TAG, "Should pause " + shouldPause);

			final boolean shouldPlay = (playOnCallStop
					&& settings.getBoolean(PAUSED_MARKER, false) && state
					.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE));
			Log.d(MPDApplication.TAG, "Should play " + shouldPlay);

			if (shouldPause || shouldPlay) {

				// get congigured MPD connection
				final MPDAsyncHelper oMPDAsyncHelper = new MPDAsyncHelper();
				SettingsHelper settingsHelper = new SettingsHelper(
						(ContextWrapper) context.getApplicationContext(),
						oMPDAsyncHelper);
				settingsHelper.updateSettings();

				// schedule real work
				oMPDAsyncHelper.execAsync(new Runnable() {
					@Override
					public void run() {
						Log.d(MPDApplication.TAG, "Runnable started");

						try {
							// prepare values for runnable
							MPD mpd = oMPDAsyncHelper.oMPD;
							if (!mpd.isConnected()) {
								Log.d(MPDApplication.TAG, "Trying to connect");
								// When using oMPDAsyncHelper.connect();
								// while the app has been killed
								// mpd.play() and mpd.pause () always throws
								// "MPD Connection is not established"
								// oMPDAsyncHelper.connect();
								// Had to use a synchronous call to establish
								// the connection :/
								MPDConnectionInfo conInfo = (MPDConnectionInfo) oMPDAsyncHelper
										.getConnectionSettings();
								mpd.connect(conInfo.sServer, conInfo.iPort);
								if (conInfo.sPassword != null)
									mpd.password(conInfo.sPassword);

								if (mpd.isConnected()) {
									Log.e(MPDApplication.TAG, "Not connected");
								} else {
									Log.d(MPDApplication.TAG, "Connected");
								}
							}
							if (shouldPause) {
								Log.d(MPDApplication.TAG, "Trying to pause");
								if (mpd.getStatus().getState()
										.equals(MPDStatus.MPD_STATE_PLAYING)) {
									mpd.pause();
									settings.edit()
											.putBoolean(PAUSED_MARKER, true)
											.commit();
									Log.d(MPDApplication.TAG, "Playback paused");
								}
							} else if (shouldPlay) {
								Log.d(MPDApplication.TAG, "Trying to play");
								mpd.play();
								settings.edit()
										.putBoolean(PAUSED_MARKER, false)
										.commit();
								Log.d(MPDApplication.TAG, "Playback resumed");
							}
							// Always throws MpdConnectionLost !
							// mpd.disconnect();
						} catch (MPDServerException e) {
							e.printStackTrace();
							Log.d(MPDApplication.TAG, "MPD Error", e);
						} catch (UnknownHostException e) {
							e.printStackTrace();
							Log.d(MPDApplication.TAG, "MPD Error", e);
						}
					}
				});
			}
		}
	}
}
