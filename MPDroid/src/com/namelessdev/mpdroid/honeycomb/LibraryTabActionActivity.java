package com.namelessdev.mpdroid.honeycomb;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.fragments.AlbumsFragment;
import com.namelessdev.mpdroid.fragments.ArtistsFragment;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.FSFragment;
import com.namelessdev.mpdroid.fragments.SongsFragment;

public class LibraryTabActionActivity extends FragmentActivity implements ActionBar.TabListener {

	private static final String TAB_ARTISTS = "tab_artists";
	private static final String TAB_ALBUMS = "tab_albums";
	private static final String TAB_SONGS = "tab_songs";
	private static final String TAB_FILES = "tab_files";

	private Fragment artistsFragment = null;
	private Fragment albumsFragment = null;
	private Fragment songsFragment = null;
	private Fragment filesFragment = null;

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_action);
		try {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDisplayShowTitleEnabled(false);
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
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		if (TAB_ARTISTS.equals(tab.getTag())) {
			((BrowseFragment) artistsFragment).scrollToTop();
		} else if (TAB_ALBUMS.equals(tab.getTag())) {
			((BrowseFragment) albumsFragment).scrollToTop();
		} else if (TAB_SONGS.equals(tab.getTag())) {
			((BrowseFragment) songsFragment).scrollToTop();
		} else if (TAB_FILES.equals(tab.getTag())) {
			((BrowseFragment) filesFragment).scrollToTop();
		}
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Fragment leftFragment = null;
		if (TAB_ARTISTS.equals(tab.getTag())) {
			if (artistsFragment == null) {
				artistsFragment = (Fragment) new ArtistsFragment();
			}
			leftFragment = artistsFragment;
			findViewById(R.id.fragment_middle).setVisibility(View.VISIBLE);
			// findViewById(R.id.fragment_right).setVisibility(View.VISIBLE);
		} else if (TAB_ALBUMS.equals(tab.getTag())) {
			if (albumsFragment == null) {
				albumsFragment = (Fragment) new AlbumsFragment();
			}
			leftFragment = albumsFragment;
			// findViewById(R.id.fragment_middle).setVisibility(View.VISIBLE);
			// findViewById(R.id.fragment_right).setVisibility(View.GONE);
		} else if (TAB_SONGS.equals(tab.getTag())) {
			if (songsFragment == null) {
				songsFragment = (Fragment) new SongsFragment();
			}
			leftFragment = songsFragment;
			// findViewById(R.id.fragment_middle).setVisibility(View.GONE);
			// findViewById(R.id.fragment_right).setVisibility(View.GONE);
		} else if (TAB_FILES.equals(tab.getTag())) {
			if (filesFragment == null) {
				filesFragment = (Fragment) new FSFragment();
			}
			leftFragment = filesFragment;
			// findViewById(R.id.fragment_middle).setVisibility(View.GONE);
			// findViewById(R.id.fragment_right).setVisibility(View.GONE);
		}

		if (leftFragment != null) {
			android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.fragment_left, leftFragment);
			transaction.commit();
		}
	}


	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub

	}

}
