package org.pmix.ui;

import java.util.Collection;
import java.util.HashMap;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDOutput;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.event.MPDConnectionStateChangedEvent;
import org.a0z.mpd.event.MPDPlaylistChangedEvent;
import org.a0z.mpd.event.MPDRandomChangedEvent;
import org.a0z.mpd.event.MPDRepeatChangedEvent;
import org.a0z.mpd.event.MPDStateChangedEvent;
import org.a0z.mpd.event.MPDTrackChangedEvent;
import org.a0z.mpd.event.MPDUpdateStateChangedEvent;
import org.a0z.mpd.event.MPDVolumeChangedEvent;
import org.a0z.mpd.event.StatusChangeListener;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements StatusChangeListener {
	
	private static final int MAIN = 0;
	private static final int ADD = 1;
	
	private OnPreferenceClickListener onPreferenceClickListener;
	private OnPreferenceClickListener onCheckPreferenceClickListener;
	private HashMap<Integer, CheckBoxPreference> cbPrefs;
	private CheckBoxPreference pRandom;
	private CheckBoxPreference pRepeat;
	
	private PreferenceScreen pOutputsScreen;
	private PreferenceScreen pInformationScreen;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MPDApplication app = (MPDApplication)getApplicationContext();
		addPreferencesFromResource(R.layout.settings);
		Log.i("PMix", "onCreate");
		
		
		onPreferenceClickListener = new OutputPreferenceClickListener();
		onCheckPreferenceClickListener = new CheckPreferenceClickListener();
		cbPrefs = new HashMap<Integer, CheckBoxPreference>();
		pOutputsScreen = (PreferenceScreen)findPreference("outputsScreen");
		pRandom = (CheckBoxPreference)findPreference("random");
		pRepeat = (CheckBoxPreference)findPreference("repeat");
		pInformationScreen = (PreferenceScreen)findPreference("informationScreen");

		// Use the ConnectionPreferConnectionPreferenceCategoryenceCategory for Wi-Fi based Connection setttings
		
		/*
		PreferenceScreen pConnectionScreen = (PreferenceScreen)findPreference("connectionScreen");
		PreferenceCategory wifiConnection = new ConnectionPreferenceCategory(this);
		wifiConnection.setTitle("Preferred connection");
		wifiConnection.setOrder(0);
		pConnectionScreen.addPreference(wifiConnection);
		*/

		EditTextPreference pVersion = (EditTextPreference)findPreference("version");
		EditTextPreference pArtists = (EditTextPreference)findPreference("artists");
		EditTextPreference pAlbums = (EditTextPreference)findPreference("albums");
		EditTextPreference pSongs = (EditTextPreference)findPreference("songs");
		
		if(!app.oMPDAsyncHelper.oMPD.isConnected())
		{
			pOutputsScreen.setEnabled(false);
			pRandom.setEnabled(false);
			pRepeat.setEnabled(false);
			pInformationScreen.setEnabled(false);
			return;
		}
		app.oMPDAsyncHelper.addStatusChangeListener(this);

		try {
			
			
			// Server is Connected...
			pRandom.setChecked(app.oMPDAsyncHelper.oMPD.getStatus().isRandom());
			pRandom.setOnPreferenceClickListener(onCheckPreferenceClickListener);
			pRepeat.setChecked(app.oMPDAsyncHelper.oMPD.getStatus().isRepeat());
			pRepeat.setOnPreferenceClickListener(onCheckPreferenceClickListener);
			pVersion.setSummary(app.oMPDAsyncHelper.oMPD.getMpdVersion());
			pArtists.setSummary(""+app.oMPDAsyncHelper.oMPD.getStatistics().getArtists());
			pAlbums.setSummary(""+app.oMPDAsyncHelper.oMPD.getStatistics().getAlbums());
			pSongs.setSummary(""+app.oMPDAsyncHelper.oMPD.getStatistics().getSongs());
			
				
		} catch (MPDServerException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication)getApplicationContext();
		app.unsetActivity(this);
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0,MAIN, 0, R.string.mainMenu).setIcon(android.R.drawable.ic_menu_revert);
		if(getPreferenceScreen().getKey().equals("connectionscreen"))
			menu.add(0,ADD, 1, R.string.clear).setIcon(android.R.drawable.ic_menu_add);
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
	 * Method is beeing called  on any click of an preference...
	 */
	
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		Log.d("PMix", preferenceScreen.getKey());
		MPDApplication app = (MPDApplication)getApplication();
		
		// Is it the connectionscreen which is called?
		if(preference.getKey() == null)
			return false;
		
		if(preference.getKey().equals("outputsScreen"))
		{
			// Populating outputs...
			PreferenceCategory pOutput = (PreferenceCategory)findPreference("outputsCategory");
			try {
				Collection<MPDOutput> list = app.oMPDAsyncHelper.oMPD.getOutputs();
				
				pOutput.removeAll();
				for(MPDOutput out : list)
				{
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
			return true;
		}
			
		
		return false;
		
	}
	
	class CheckPreferenceClickListener implements OnPreferenceClickListener {
		MPDApplication app = (MPDApplication)getApplication();

		@Override
		public boolean onPreferenceClick(Preference pref) {
			CheckBoxPreference prefCB = (CheckBoxPreference)pref;
			MPD oMPD = app.oMPDAsyncHelper.oMPD;
			try {
				if(prefCB.getKey().equals("random"))
					oMPD.setRandom(prefCB.isChecked());
				if(prefCB.getKey().equals("repeat"))
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
			CheckBoxPreference prefCB = (CheckBoxPreference)pref;
			MPDApplication app = (MPDApplication)getApplication();
			MPD oMPD = app.oMPDAsyncHelper.oMPD;
			String id = prefCB.getKey();
			try {
				if(prefCB.isChecked())
				{
					oMPD.enableOutput(Integer.parseInt(id));
					return false;
				}
				else
				{
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
	public void connectionStateChanged(MPDConnectionStateChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void playlistChanged(MPDPlaylistChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void randomChanged(MPDRandomChangedEvent event) {
		pRandom.setChecked(event.isRandom());
	}

	@Override
	public void repeatChanged(MPDRepeatChangedEvent event) {
		pRepeat.setChecked(event.isRepeat());
	}

	@Override
	public void stateChanged(MPDStateChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trackChanged(MPDTrackChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateStateChanged(MPDUpdateStateChangedEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void volumeChanged(MPDVolumeChangedEvent event) {
		// TODO Auto-generated method stub
		
	}
	
}
