package org.musicpd.android;

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

import org.musicpd.android.helpers.MPDAsyncHelper;
import org.musicpd.android.helpers.MPDAsyncHelper.MPDConnectionInfo;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.NetworkHelper;
import org.musicpd.android.tools.SettingsHelper;

public class PhoneStateReceiver extends BroadcastReceiver {
	// Used to trace when the app pauses / resumes playback
	private static final String PAUSED_MARKER = "wasPausedInCall";

	@Override
	public void onReceive(Context context, Intent intent) {

		if (!NetworkHelper.isLocalNetworkConnected(context)) {
			Log.d("No local network available.");
			return;
		}

		Log.d("Phonestate received");
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		// Get config vars
		boolean pauseOnCall = settings.getBoolean("pauseOnPhoneStateChange",
				false);
		boolean playOnCallStop = pauseOnCall
				&& settings.getBoolean("playOnPhoneStateChange", false);

		Log.d("Pause on call " + pauseOnCall);
		if (pauseOnCall) {
			Bundle bundle = intent.getExtras();
			if (null == bundle) {
				Log.e("Bundle was null");
				return;
			}
			String state = bundle.getString(TelephonyManager.EXTRA_STATE);

			final boolean shouldPause = pauseOnCall
					&& (state
							.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING) || state
							.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK));
			Log.d("Should pause " + shouldPause);

			final boolean shouldPlay = (playOnCallStop
					&& settings.getBoolean(PAUSED_MARKER, false) && state
					.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE));
			Log.d("Should play " + shouldPlay);

			if (shouldPause || shouldPlay) {
				// get congigured MPD connection
				final MPDAsyncHelper oMPDAsyncHelper = new MPDAsyncHelper();
				SettingsHelper settingsHelper = new SettingsHelper(
						(ContextWrapper) context.getApplicationContext(),
						oMPDAsyncHelper);
				settingsHelper.updateConnectionSettings();

				// schedule real work
				oMPDAsyncHelper.execAsync(new Runnable() {
					@Override
					public void run() {
						Log.d("Runnable started");

						try {
							MPD mpd = oMPDAsyncHelper.oMPD;
							if (!mpd.isConnected()) {
								Log.d("Trying to connect");
								// MPD connection has to be done synchronously
								MPDConnectionInfo conInfo = (MPDConnectionInfo) oMPDAsyncHelper
										.getConnectionSettings();
								mpd.connect(conInfo.sServer, conInfo.iPort);

								if (conInfo.sPassword != null)
									mpd.password(conInfo.sPassword);

								if (mpd.isConnected()) {
									Log.d("Connected");
								} else {
									Log.e("Not connected");
								}
							}
							if (shouldPause) {
								Log.d("Trying to pause");
								if (mpd.getStatus().getState()
										.equals(MPDStatus.MPD_STATE_PLAYING)) {
									mpd.pause();
									settings.edit()
											.putBoolean(PAUSED_MARKER, true)
											.commit();
									Log.d("Playback paused");
								}
							} else if (shouldPlay) {
								Log.d("Trying to play");
								mpd.play();
								settings.edit()
										.putBoolean(PAUSED_MARKER, false)
										.commit();
								Log.d("Playback resumed");
							}
							mpd.disconnect();
						} catch (MPDServerException e) {
							Log.w(e);
						} catch (UnknownHostException e) {
							Log.w(e);
						}
					}
				});
			}
		}
	}
}
