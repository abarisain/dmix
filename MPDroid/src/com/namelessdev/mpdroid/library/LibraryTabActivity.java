package com.namelessdev.mpdroid.library;

import java.util.ArrayList;

import org.a0z.mpd.exception.MPDServerException;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MPDroidActivities.MPDroidFragmentActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.LibraryFragment;
import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;

public class LibraryTabActivity extends MPDroidFragmentActivity implements OnNavigationListener,
		ILibraryFragmentActivity,
		ILibraryTabActivity, OnBackStackChangedListener {

	private static final String FRAGMENT_TAG_LIBRARY = "library";

	LibraryFragment libraryFragment;
	FragmentManager fragmentManager;

	ActionBar actionBar;
	ArrayList<String> mTabList;
	private MPDApplication app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_tabs);

		app = (MPDApplication) getApplicationContext();

		fragmentManager = getSupportFragmentManager();

		// Get the list of the currently visible tabs
		mTabList = LibraryTabsUtil.getCurrentLibraryTabs(this.getApplicationContext());

		// Set up the action bar.
		actionBar = getActionBar();
		// Will set the action bar to it's List style.
		fragmentManager.addOnBackStackChangedListener(this);
		final int fmStackCount = fragmentManager.getBackStackEntryCount();
		if (fmStackCount > 0) {
			refreshActionBarNavigation(false, fragmentManager.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
		} else {
			refreshActionBarNavigation(true, null);
		}
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		ArrayAdapter<CharSequence> actionBarAdapter = new ArrayAdapter<CharSequence>(actionBar.getThemedContext(),
				android.R.layout.simple_spinner_item);
		for (int i = 0; i < mTabList.size(); i++) {
			actionBarAdapter.add(getText(LibraryTabsUtil.getTabTitleResId(mTabList.get(i))));
		}

		actionBarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(actionBarAdapter, this);

		libraryFragment = (LibraryFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_LIBRARY);
		if (libraryFragment == null) {
			libraryFragment = new LibraryFragment();
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.replace(R.id.root_frame, libraryFragment, FRAGMENT_TAG_LIBRARY);
			ft.commit();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.mpd_browsermenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_search:
				this.onSearchRequested();
				return true;
			case android.R.id.home:
				final int fmStackCount = fragmentManager.getBackStackEntryCount();
				if (fmStackCount > 0) {
					fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
				} else {
					finish();
				}
				return true;
		}
		return false;
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		libraryFragment.setCurrentItem(itemPosition, true);
		return true;
	}

	public void refreshActionBarNavigation(boolean enableTabs, CharSequence title) {
		if (enableTabs) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setDisplayShowTitleEnabled(false);
		} else {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(title);
		}
	}

	@Override
	public void pushLibraryFragment(Fragment fragment, String label) {
		String title = "";
		if (fragment instanceof BrowseFragment) {
			title = ((BrowseFragment) fragment).getTitle();
		} else {
			title = fragment.toString();
		}
		refreshActionBarNavigation(false, title);
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.replace(R.id.root_frame, fragment);
		ft.addToBackStack(label);
		ft.setBreadCrumbTitle(title);
		ft.commit();
	}

	@Override
	public ArrayList<String> getTabList() {
		return mTabList;
	}

	@Override
	public void pageChanged(int position) {
		if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST)
			actionBar.setSelectedNavigationItem(position);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		final MPDApplication app = (MPDApplication) getApplicationContext();
		switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							app.oMPDAsyncHelper.oMPD.next();
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}
				}).start();
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							app.oMPDAsyncHelper.oMPD.previous();
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}
				}).start();
				return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			// For onKeyLongPress to work
			event.startTracking();
			return !app.getApplicationState().streamingMode;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (event.isTracking() && !event.isCanceled() && !app.getApplicationState().streamingMode) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								app.oMPDAsyncHelper.oMPD
										.adjustVolume(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ? NowPlayingFragment.VOLUME_STEP
												: -NowPlayingFragment.VOLUME_STEP);
							} catch (MPDServerException e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
				return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onBackStackChanged() {
		final int fmStackCount = fragmentManager.getBackStackEntryCount();
		if (fmStackCount > 0) {
			refreshActionBarNavigation(false, fragmentManager.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
		} else {
			refreshActionBarNavigation(true, null);
		}
	}
}
