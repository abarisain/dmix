package com.namelessdev.mpdroid;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;

public class PhoneStateReceiver extends BroadcastReceiver {
	private static final String PAUSED_MARKER = "wasPausedInCall";

	@Override
	public void onReceive(Context context, Intent intent) {

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);

		boolean pauseOnCall = settings.getBoolean("pauseOnPhoneStateChange",
				false);
		boolean playOnCallStop = settings.getBoolean("playOnPhoneStateChange",
				false);

		if (pauseOnCall || playOnCallStop) {

			Bundle bundle = intent.getExtras();
			if (null == bundle)
				return;
			String state = bundle.getString(TelephonyManager.EXTRA_STATE);

			final boolean shouldPause = pauseOnCall
					&& (state
							.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING) || state
							.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK));

			final boolean shouldPlay = (playOnCallStop
					&& settings.getBoolean(PAUSED_MARKER, false) && state
					.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE));

			if (shouldPause || shouldPlay) {
				MPDApplication app = (MPDApplication) context
						.getApplicationContext();
				if (app == null)
					return;
				// get MPD connection
				MPDAsyncHelper oMPDAsyncHelper = app.oMPDAsyncHelper;
				oMPDAsyncHelper.connect();
				// prepare values for runnable
				final MPD mpd = oMPDAsyncHelper.oMPD;
				// schedule real work
				oMPDAsyncHelper.execAsync(new Runnable() {
					@Override
					public void run() {
						try {
							Editor ed = settings.edit();
							if (shouldPause) {
								if (mpd.getStatus().getState()
										.equals(MPDStatus.MPD_STATE_PLAYING)) {
									mpd.pause();
									ed.putBoolean(PAUSED_MARKER, true);
									ed.commit();
								}
							} else if (shouldPlay) {
								mpd.play();
								ed.putBoolean(PAUSED_MARKER, false);
								ed.commit();
							}
						} catch (MPDServerException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});

			}
		}
	}
}
