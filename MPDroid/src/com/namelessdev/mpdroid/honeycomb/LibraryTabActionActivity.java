package com.namelessdev.mpdroid.honeycomb;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.namelessdev.mpdroid.R;

public class LibraryTabActionActivity extends Activity implements ActionBar.TabListener {

	private static final String TAB_ARTISTS = "tab_artists";
	private static final String TAB_ALBUMS = "tab_albums";
	private static final String TAB_SONGS = "tab_songs";
	private static final String TAB_FILES = "tab_files";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.artists);
		try {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			actionBar.addTab(actionBar.newTab().setTabListener(this).setTag(TAB_ARTISTS).setIcon(R.drawable.ic_tab_artists)
					.setText(R.string.artists));
			actionBar.addTab(actionBar.newTab().setTabListener(this).setTag(TAB_ALBUMS).setIcon(R.drawable.ic_tab_albums)
					.setText(R.string.albums));
			actionBar.addTab(actionBar.newTab().setTabListener(this).setTag(TAB_SONGS).setIcon(R.drawable.ic_tab_songs)
					.setText(R.string.songs));
			actionBar.addTab(actionBar.newTab().setTabListener(this).setTag(TAB_FILES).setIcon(R.drawable.ic_tab_playlists)
					.setText(R.string.files));
		} catch (NoClassDefFoundError e) {
			// Older android
		} catch (NullPointerException e) {

		} catch (NoSuchMethodError e) {

		}
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}

}
