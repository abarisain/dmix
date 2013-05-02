package com.namelessdev.mpdroid;

import java.util.Collection;
import java.util.HashMap;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDOutput;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.text.format.Formatter;

import com.namelessdev.mpdroid.cover.CachedCover;

public class SettingsActivity extends PreferenceActivity implements
		StatusChangeListener {

	private static final int MAIN = 0;
	private static final int ADD = 1;
	public static final String OPEN_OUTPUT = "open_output";

	private OnPreferenceClickListener onPreferenceClickListener;
	private OnPreferenceClickListener onCheckPreferenceClickListener;
	private HashMap<Integer, CheckBoxPreference> cbPrefs;

	private PreferenceScreen pOutputsScreen;
	private PreferenceScreen pInformationScreen;
	private Handler handler;

	private EditTextPreference pCacheUsage1;
 	private	EditTextPreference pCacheUsage2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final MPDApplication app = (MPDApplication) getApplicationContext();
		handler = new Handler();
		addPreferencesFromResource(R.layout.settings);
		// Log.i("MPDroid", "onCreate");

		onPreferenceClickListener = new OutputPreferenceClickListener();
		onCheckPreferenceClickListener = new CheckPreferenceClickListener();
		cbPrefs = new HashMap<Integer, CheckBoxPreference>();
		pOutputsScreen = (PreferenceScreen) findPreference("outputsScreen");
		pInformationScreen = (PreferenceScreen) findPreference("informationScreen");
		PreferenceScreen pUpdate = (PreferenceScreen) findPreference("updateDB");

		// Use the ConnectionPreferConnectionPreferenceCategoryenceCategory for
		// Wi-Fi based Connection setttings

		/*
		 * PreferenceScreen pConnectionScreen =
		 * (PreferenceScreen)findPreference("connectionScreen");
		 * PreferenceCategory wifiConnection = new
		 * ConnectionPreferenceCategory(this);
		 * wifiConnection.setTitle("Preferred connection");
		 * wifiConnection.setOrder(0);
		 * pConnectionScreen.addPreference(wifiConnection);
		 */

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			final PreferenceCategory interfaceCategory = (PreferenceCategory) findPreference("category_interface");
			interfaceCategory.removePreference(findPreference("lightTheme"));
			interfaceCategory.removePreference(findPreference("lightNowPlayingTheme"));
		}

		if (!getResources().getBoolean(R.bool.isTablet)) {
			final PreferenceCategory interfaceCategory = (PreferenceCategory) findPreference("category_interface");
			interfaceCategory.removePreference(findPreference("tabletUI"));
		}

		final EditTextPreference pVersion = (EditTextPreference) findPreference("version");
		final EditTextPreference pArtists = (EditTextPreference) findPreference("artists");
		final EditTextPreference pAlbums = (EditTextPreference) findPreference("albums");
		final EditTextPreference pSongs = (EditTextPreference) findPreference("songs");

		// set the albumart fields
		CheckBoxPreference c = (CheckBoxPreference) findPreference("enableLocalCover");
		Preference mp = (Preference) findPreference("musicPath");
		Preference cf = (Preference) findPreference("coverFileName");
		if (c.isChecked()) {
			mp.setEnabled(true);
			cf.setEnabled(true);
		} else {
			mp.setEnabled(false);
			cf.setEnabled(false);
		}

		// artwork cache usage
		long size = new CachedCover(app).getCacheUsage();
		String usage = Formatter.formatFileSize(app, size);
		pCacheUsage1 = (EditTextPreference) findPreference("cacheUsage1");
		pCacheUsage1.setSummary(usage);
		pCacheUsage2 = (EditTextPreference) findPreference("cacheUsage2");
		pCacheUsage2.setSummary(usage);

		// album art library listing requires cover art cache
		CheckBoxPreference lcc = (CheckBoxPreference) findPreference("enableLocalCoverCache");
		CheckBoxPreference aal = (CheckBoxPreference) findPreference("enableAlbumArtLibrary");
		aal.setEnabled(lcc.isChecked());

		// Enable/Disable playback resume when call ends only if playback pause
		// is enabled when call starts
		CheckBoxPreference cPause = (CheckBoxPreference) findPreference("pauseOnPhoneStateChange");
		CheckBoxPreference cPlay = (CheckBoxPreference) findPreference("playOnPhoneStateChange");
		cPlay.setEnabled(cPause.isChecked());

		if (!app.oMPDAsyncHelper.oMPD.isConnected()) {
			pOutputsScreen.setEnabled(false);
			pUpdate.setEnabled(false);
			pInformationScreen.setEnabled(false);
			return;
		}
		app.oMPDAsyncHelper.addStatusChangeListener(this);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					final boolean isRandom = app.oMPDAsyncHelper.oMPD
							.getStatus().isRandom();
					final boolean isRepeat = app.oMPDAsyncHelper.oMPD
							.getStatus().isRepeat();
					final String version = app.oMPDAsyncHelper.oMPD
							.getMpdVersion();
					final String artists = ""
							+ app.oMPDAsyncHelper.oMPD.getStatistics()
									.getArtists();
					final String albums = ""
							+ app.oMPDAsyncHelper.oMPD.getStatistics()
									.getAlbums();
					final String songs = ""
							+ app.oMPDAsyncHelper.oMPD.getStatistics()
									.getSongs();
					handler.post(new Runnable() {

						@Override
						public void run() {
							pVersion.setSummary(version);
							pArtists.setSummary(artists);
							pAlbums.setSummary(albums);
							pSongs.setSummary(songs);
						}
					});
				} catch (MPDServerException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		}).start();
		// Server is Connected...

		if (getIntent().getBooleanExtra(OPEN_OUTPUT, false)) {
			populateOutputsScreen();
			setPreferenceScreen(pOutputsScreen);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

	@Override
	protected void onDestroy() {
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.oMPDAsyncHelper.removeStatusChangeListener(this);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		if (getPreferenceScreen().getKey().equals("connectionscreen"))
			menu.add(0, ADD, 1, R.string.clear).setIcon(
					android.R.drawable.ic_menu_add);
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

	/**
	 * Method is beeing called on any click of an preference...
	 */

	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		Log.d("MPDroid", preferenceScreen.getKey());
		MPDApplication app = (MPDApplication) getApplication();

		// Is it the connectionscreen which is called?
		if (preference.getKey() == null)
			return false;

		if (preference.getKey().equals("outputsScreen")) {
			populateOutputsScreen();
			return true;

		} else if (preference.getKey().equals("updateDB")) {
			try {
				MPD oMPD = app.oMPDAsyncHelper.oMPD;
				oMPD.refreshDatabase();
			} catch (MPDServerException e) {
			}
			return true;

		} else if (preference.getKey().equals("clearLocalCoverCache")) {
			new AlertDialog.Builder(this)
					.setTitle(R.string.clearLocalCoverCache)
					.setMessage(R.string.clearLocalCoverCachePrompt)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							MPDApplication app = (MPDApplication) getApplication();
							new CachedCover(app).clear();
							pCacheUsage1.setSummary("0.00B");
							pCacheUsage2.setSummary("0.00B");
						}
					})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// do nothing
						}
					})
					.show();
			return true;

		} else if (preference.getKey().equals("enableLocalCover")) {
			CheckBoxPreference c = (CheckBoxPreference) findPreference("enableLocalCover");
			Preference mp = (Preference) findPreference("musicPath");
			Preference cf = (Preference) findPreference("coverFileName");

			if (c.isChecked()) {
				mp.setEnabled(true);
				cf.setEnabled(true);
			} else {
				mp.setEnabled(false);
				cf.setEnabled(false);
			}
			return true;

		} else if (preference.getKey().equals("enableLocalCoverCache")) {
			// album art library listing requires cover art cache
			CheckBoxPreference lcc = (CheckBoxPreference) findPreference("enableLocalCoverCache");
			CheckBoxPreference aal = (CheckBoxPreference) findPreference("enableAlbumArtLibrary");
			if (lcc.isChecked()) {
				aal.setEnabled(true);
			}else{
				aal.setEnabled(false);
				aal.setChecked(false);
			}
			return true;

		} else if (preference.getKey().equals("pauseOnPhoneStateChange")) {
			// Enable/Disable playback resume when call ends only if playback
			// pause is enabled when call starts
			CheckBoxPreference cPause = (CheckBoxPreference) findPreference("pauseOnPhoneStateChange");
			CheckBoxPreference c = (CheckBoxPreference) findPreference("playOnPhoneStateChange");
			c.setEnabled(cPause.isChecked());
		}
		return false;

	}

	private void populateOutputsScreen() {
		// Populating outputs...
		PreferenceCategory pOutput = (PreferenceCategory) findPreference("outputsCategory");
		final MPDApplication app = (MPDApplication) getApplication();
		try {
			Collection<MPDOutput> list = app.oMPDAsyncHelper.oMPD.getOutputs();

			pOutput.removeAll();
			for (MPDOutput out : list) {
				CheckBoxPreference pref = new CheckBoxPreference(this);
				pref.setPersistent(false);
				pref.setTitle(out.getName());
				pref.setChecked(out.isEnabled());
				pref.setKey("" + out.getId());
				pref.setOnPreferenceClickListener(onPreferenceClickListener);
				cbPrefs.put(out.getId(), pref);
				pOutput.addPreference(pref);

			}
		} catch (MPDServerException e) {
			pOutput.removeAll(); // Connection error occured meanwhile...
		}
	}

	class CheckPreferenceClickListener implements OnPreferenceClickListener {
		MPDApplication app = (MPDApplication) getApplication();

		@Override
		public boolean onPreferenceClick(Preference pref) {
			CheckBoxPreference prefCB = (CheckBoxPreference) pref;
			MPD oMPD = app.oMPDAsyncHelper.oMPD;
			try {
				if (prefCB.getKey().equals("random"))
					oMPD.setRandom(prefCB.isChecked());
				if (prefCB.getKey().equals("repeat"))
					oMPD.setRepeat(prefCB.isChecked());
				return prefCB.isChecked();
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}

	}

	class OutputPreferenceClickListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference pref) {
			CheckBoxPreference prefCB = (CheckBoxPreference) pref;
			MPDApplication app = (MPDApplication) getApplication();
			MPD oMPD = app.oMPDAsyncHelper.oMPD;
			String id = prefCB.getKey();
			try {
				if (prefCB.isChecked()) {
					oMPD.enableOutput(Integer.parseInt(id));
					return false;
				} else {
					oMPD.disableOutput(Integer.parseInt(id));
					return true;
				}
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
	}

	@Override
	public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
		// TODO Auto-generated method stub
	}

	@Override
	public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
		// TODO Auto-generated method stub
	}

	@Override
	public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
		// TODO Auto-generated method stub
	}

	@Override
	public void stateChanged(MPDStatus mpdStatus, String oldState) {
		// TODO Auto-generated method stub
	}

	@Override
	public void repeatChanged(boolean repeating) {
	}

	@Override
	public void randomChanged(boolean random) {
	}

	@Override
	public void connectionStateChanged(boolean connected, boolean connectionLost) {
		// TODO Auto-generated method stub
	}

	@Override
	public void libraryStateChanged(boolean updating) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
}
