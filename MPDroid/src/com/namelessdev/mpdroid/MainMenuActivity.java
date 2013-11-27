package com.namelessdev.mpdroid;

import java.util.ArrayList;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.namelessdev.mpdroid.MPDroidActivities.MPDroidFragmentActivity;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.LibraryFragment;
import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.library.ILibraryTabActivity;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;
import com.namelessdev.mpdroid.tools.Tools;

public class MainMenuActivity extends MPDroidFragmentActivity implements OnNavigationListener, ILibraryFragmentActivity,
		ILibraryTabActivity, OnBackStackChangedListener {

	public static enum DisplayMode {
		MODE_NOWPLAYING,
        MODE_QUEUE,
		MODE_LIBRARY
	}

	public static class DrawerItem {
		public static enum Action {
			ACTION_NOWPLAYING,
			ACTION_LIBRARY,
			ACTION_OUTPUTS
		}

		public Action action;
		public String label;

		public DrawerItem(String label, Action action) {
			this.label = label;
			this.action = action;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	public static final int PLAYLIST = 1;

	public static final int ARTISTS = 2;

	public static final int SETTINGS = 5;

	public static final int STREAM = 6;

	public static final int LIBRARY = 7;

	public static final int CONNECT = 8;

	private static final String FRAGMENT_TAG_LIBRARY = "library";

	private int backPressExitCount;
	private Handler exitCounterReset;
	private boolean isDualPaneMode;
	private MPDApplication app;
	private View nowPlayingDualPane;
    private ViewPager nowPlayingPager;
    private View libraryRootFrame;

	private List<DrawerItem> mDrawerItems;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private int oldDrawerPosition;

	private LibraryFragment libraryFragment;
	private FragmentManager fragmentManager;
	private ArrayList<String> mTabList;

	private DisplayMode currentDisplayMode;

	@SuppressLint("NewApi")
	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (MPDApplication) getApplication();

		setContentView(app.isTabletUiEnabled() ? R.layout.main_activity_nagvigation_tablet : R.layout.main_activity_nagvigation);

		nowPlayingDualPane = findViewById(R.id.nowplaying_dual_pane);
        nowPlayingPager = (ViewPager) findViewById(R.id.pager);
        libraryRootFrame = findViewById(R.id.library_root_frame);

		isDualPaneMode = (nowPlayingDualPane != null);
		switchMode(DisplayMode.MODE_NOWPLAYING);

		exitCounterReset = new Handler();

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		mDrawerItems = new ArrayList<DrawerItem>();
		mDrawerItems.add(new DrawerItem(getString(R.string.nowPlaying), DrawerItem.Action.ACTION_NOWPLAYING));
		mDrawerItems.add(new DrawerItem(getString(R.string.libraryTabActivity), DrawerItem.Action.ACTION_LIBRARY));
		mDrawerItems.add(new DrawerItem(getString(R.string.outputs), DrawerItem.Action.ACTION_OUTPUTS));

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerToggle = new ActionBarDrawerToggle(
				this, /* host Activity */
				mDrawerLayout, /* DrawerLayout object */
				R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
				R.string.drawer_open, /* "open drawer" description */
				R.string.drawer_close /* "close drawer" description */
				) {

					/** Called when a drawer has settled in a completely closed state. */
					public void onDrawerClosed(View view) {
						refreshActionBarTitle();
					}

					/** Called when a drawer has settled in a completely open state. */
					public void onDrawerOpened(View drawerView) {
						actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
						actionBar.setDisplayShowTitleEnabled(true);
						actionBar.setTitle(R.string.app_name);
					}
				};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		// Set the adapter for the list view
		mDrawerList.setAdapter(new ArrayAdapter<DrawerItem>(this,
				R.layout.drawer_list_item, mDrawerItems));
		oldDrawerPosition = 0;
		mDrawerList.setItemChecked(oldDrawerPosition, true);
		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		/*
		 * Setup the library tab
		 */
		fragmentManager = getSupportFragmentManager();
		fragmentManager.addOnBackStackChangedListener(this);

		// Get the list of the currently visible tabs
		mTabList = LibraryTabsUtil.getCurrentLibraryTabs(this.getApplicationContext());

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
			ft.replace(R.id.library_root_frame, libraryFragment, FRAGMENT_TAG_LIBRARY);
			ft.commit();
		}

        // Setup the pager
        if (nowPlayingPager != null) {
            nowPlayingPager.setAdapter(new MainMenuPagerAdapter());
            nowPlayingPager.setOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageSelected(int position) {
                            refreshActionBarTitle();
                        }
                    });
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
	protected void onResume() {
		super.onResume();
		backPressExitCount = 0;
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * Called when Back button is pressed, displays message to user indicating the if back button is pressed again the application will
	 * exit. We keep a count of how many time back
	 * button is pressed within 5 seconds. If the count is greater than 1 then call system.exit(0)
	 * 
	 * Starts a post delay handler to reset the back press count to zero after 5 seconds
	 * 
	 * @return None
	 */
	@Override
	public void onBackPressed() {
		if (currentDisplayMode == DisplayMode.MODE_LIBRARY) {
			final int fmStackCount = fragmentManager.getBackStackEntryCount();
			if (fmStackCount > 0) {
				super.onBackPressed();
				return;
			}
		}

		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean exitConfirmationRequired = settings.getBoolean("enableExitConfirmation", false);
		if (exitConfirmationRequired && backPressExitCount < 1) {
			Tools.notifyUser(String.format(getResources().getString(R.string.backpressToQuit)), this);
			backPressExitCount += 1;
			exitCounterReset.postDelayed(new Runnable() {
				@Override
				public void run() {
					backPressExitCount = 0;
				}
			}, 5000);
		} else {
			/*
			 * Nasty force quit, should shutdown everything nicely but there just too many async tasks maybe I'll correctly implement
			 * app.terminateApplication();
			 */
			System.exit(0);
		}
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.mpd_mainmenu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Reminder : never disable buttons that are shown as actionbar actions here
		super.onPrepareOptionsMenu(menu);
		MPDApplication app = (MPDApplication) this.getApplication();
		MPD mpd = app.oMPDAsyncHelper.oMPD;
		if (!mpd.isConnected()) {
			if (menu.findItem(CONNECT) == null) {
				menu.add(0, CONNECT, 0, R.string.connect);
			}
		} else {
			if (menu.findItem(CONNECT) != null) {
				menu.removeItem(CONNECT);
			}
		}
		setMenuChecked(menu.findItem(R.id.GMM_Stream), app.getApplicationState().streamingMode);
		final MPDStatus mpdStatus = app.getApplicationState().currentMpdStatus;
		if (mpdStatus != null) {
			setMenuChecked(menu.findItem(R.id.GMM_Single), mpdStatus.isSingle());
			setMenuChecked(menu.findItem(R.id.GMM_Consume), mpdStatus.isConsume());
		}
		return true;
	}

	private void setMenuChecked(MenuItem item, boolean checked) {
		// Set the icon to a checkbox so 2.x users also get one
		item.setChecked(checked);
		item.setIcon(checked ? R.drawable.btn_check_buttonless_on : R.drawable.btn_check_buttonless_off);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		Intent i = null;
		final MPDApplication app = (MPDApplication) this.getApplication();
		final MPD mpd = app.oMPDAsyncHelper.oMPD;

		// Handle item selection
		switch (item.getItemId()) {
			case R.id.menu_search:
				this.onSearchRequested();
				return true;
			case R.id.GMM_Settings:
				i = new Intent(this, SettingsActivity.class);
				startActivityForResult(i, SETTINGS);
				return true;
			case CONNECT:
				((MPDApplication) this.getApplication()).connect();
				return true;
			case R.id.GMM_Stream:
				if (app.getApplicationState().streamingMode) {
					i = new Intent(this, StreamingService.class);
					i.setAction("com.namelessdev.mpdroid.DIE");
					this.startService(i);
					((MPDApplication) this.getApplication()).getApplicationState().streamingMode = false;
					// Toast.makeText(this, "MPD Streaming Stopped", Toast.LENGTH_SHORT).show();
				} else {
					if (app.oMPDAsyncHelper.oMPD.isConnected()) {
						i = new Intent(this, StreamingService.class);
						i.setAction("com.namelessdev.mpdroid.START_STREAMING");
						this.startService(i);
						((MPDApplication) this.getApplication()).getApplicationState().streamingMode = true;
						// Toast.makeText(this, "MPD Streaming Started", Toast.LENGTH_SHORT).show();
					}
				}
				return true;
			case R.id.GMM_bonjour:
				startActivity(new Intent(this, ServerListActivity.class));
				return true;
			case R.id.GMM_Consume:
				try {
					mpd.setConsume(!mpd.getStatus().isConsume());
				} catch (MPDServerException e) {
				}
				return true;
			case R.id.GMM_Single:
				try {
					mpd.setSingle(!mpd.getStatus().isSingle());
				} catch (MPDServerException e) {
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}

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

	/**
	 * Library methods
	 */

	@Override
	public void onBackStackChanged() {
		refreshActionBarTitle();
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		libraryFragment.setCurrentItem(itemPosition, true);
		return true;
	}

	@Override
	public void pushLibraryFragment(Fragment fragment, String label) {
		String title = "";
		if (fragment instanceof BrowseFragment) {
			title = ((BrowseFragment) fragment).getTitle();
		} else {
			title = fragment.toString();
		}
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.replace(R.id.library_root_frame, fragment);
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
		final ActionBar actionBar = getActionBar();
		if (currentDisplayMode == DisplayMode.MODE_LIBRARY && actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST)
			actionBar.setSelectedNavigationItem(position);
	}

	/**
	 * Navigation Drawer helpers
	 */

	private void refreshActionBarTitle()
	{
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		switch (currentDisplayMode)
		{
            case MODE_QUEUE:
			case MODE_NOWPLAYING:
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                if (nowPlayingPager != null && nowPlayingPager.getCurrentItem() > 0) {
                    actionBar.setTitle(R.string.playQueue);
                } else {
                    actionBar.setTitle(R.string.nowPlaying);
                }
				break;
			case MODE_LIBRARY:
				final int fmStackCount = fragmentManager.getBackStackEntryCount();
				if (fmStackCount > 0) {
					actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
					actionBar.setTitle(fragmentManager.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
				} else {
					actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
					actionBar.setDisplayShowTitleEnabled(false);
				}
				break;
		}
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mDrawerLayout.closeDrawer(mDrawerList);

			switch (((DrawerItem) parent.getItemAtPosition(position)).action) {
				default:
				case ACTION_LIBRARY:
					// If we are already on the library, pop the whole stack. Acts like an "up" button
					if (currentDisplayMode == DisplayMode.MODE_LIBRARY) {
						final int fmStackCount = fragmentManager.getBackStackEntryCount();
						if (fmStackCount > 0) {
							fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
						}
					}
                    switchMode(DisplayMode.MODE_LIBRARY);
					break;
				case ACTION_NOWPLAYING:
					switchMode(DisplayMode.MODE_NOWPLAYING);
					break;
				case ACTION_OUTPUTS:
					mDrawerList.setItemChecked(oldDrawerPosition, true);
					final Intent i = new Intent(MainMenuActivity.this, SettingsActivity.class);
					i.putExtra(SettingsActivity.OPEN_OUTPUT, true);
					startActivityForResult(i, SETTINGS);
					break;
			}
			oldDrawerPosition = position;
		}
	}

	/** Swaps fragments in the main content view */
	public void switchMode(DisplayMode newMode) {
		currentDisplayMode = newMode;
		switch (currentDisplayMode)
		{
            case MODE_QUEUE:
			case MODE_NOWPLAYING:
				if (isDualPaneMode) {
					nowPlayingDualPane.setVisibility(View.VISIBLE);
				} else {
                    nowPlayingPager.setVisibility(View.VISIBLE);
                    if (currentDisplayMode == DisplayMode.MODE_NOWPLAYING) {
                        nowPlayingPager.setCurrentItem(0, true);
                    } else {
                        nowPlayingPager.setCurrentItem(1, true);
                    }
				}
				libraryRootFrame.setVisibility(View.GONE);
                // Force MODE_NOWPLAYING even if MODE_QUEUE was asked
                currentDisplayMode = DisplayMode.MODE_NOWPLAYING;
				break;
			case MODE_LIBRARY:
				if (isDualPaneMode) {
					nowPlayingDualPane.setVisibility(View.GONE);
				} else {
                    nowPlayingPager.setVisibility(View.GONE);
				}
				libraryRootFrame.setVisibility(View.VISIBLE);
				break;
		}
		refreshActionBarTitle();
	}

    class MainMenuPagerAdapter extends PagerAdapter {

        public Object instantiateItem(View collection, int position) {

            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.nowplaying_fragment;
                    break;
                case 1:
                    resId = R.id.playlist_fragment;
                    break;
            }
            return findViewById(resId);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == ((View) arg1);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            return;
        }
    }
}
