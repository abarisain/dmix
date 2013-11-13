package com.namelessdev.mpdroid;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.namelessdev.mpdroid.MPDroidActivities.MPDroidFragmentActivity;
import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.namelessdev.mpdroid.library.LibraryTabActivity;
import com.namelessdev.mpdroid.tools.Tools;

public class MainMenuActivity extends MPDroidFragmentActivity {

	public static enum DisplayMode {
		MODE_NOWPLAYING,
		MODE_QUEUE,
		MODE_LIBRARY
	}
	
	public static final int PLAYLIST = 1;

	public static final int ARTISTS = 2;

	public static final int SETTINGS = 5;

	public static final int STREAM = 6;

	public static final int LIBRARY = 7;

	public static final int CONNECT = 8;

	private int backPressExitCount;
	private Handler exitCounterReset;
	private boolean isDualPaneMode;
	private MPDApplication app;
	private View nowPlayingFragment;
	private View nowPlayingDualPane;
	private View libraryRootFrame;
	private View playlistFragment;

	private String[] mDrawerItems;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	
	private DisplayMode currentDisplayMode;

	@SuppressLint("NewApi")
	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (MPDApplication) getApplication();

		setContentView(app.isTabletUiEnabled() ? R.layout.main_activity_nagvigation_tablet : R.layout.main_activity_nagvigation);

		nowPlayingFragment = findViewById(R.id.nowplaying_fragment);
		nowPlayingDualPane = findViewById(R.id.nowplaying_dual_pane);
		libraryRootFrame = findViewById(R.id.root_frame);
		playlistFragment = findViewById(R.id.playlist_fragment);

		isDualPaneMode = (nowPlayingDualPane != null);
		switchMode(DisplayMode.MODE_NOWPLAYING);

		exitCounterReset = new Handler();

		if (android.os.Build.VERSION.SDK_INT >= 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		String[] mDrawerItems = null;
		if (isDualPaneMode) {
			mDrawerItems = new String[] { getString(R.string.libraryTabActivity), getString(R.string.nowPlaying) };
		} else {
			mDrawerItems = new String[] { getString(R.string.libraryTabActivity), getString(R.string.nowPlaying),
					getString(R.string.playQueue) };
		}
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerToggle = new ActionBarDrawerToggle(
				this, /* host Activity */
				mDrawerLayout, /* DrawerLayout object */
				app.isLightThemeSelected() ? R.drawable.ic_drawer_light : R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
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
						actionBar.setTitle(R.string.app_name);
					}
				};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		// Set the adapter for the list view
		mDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, mDrawerItems));
		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
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

	private void openLibrary() {
		final Intent i = new Intent(this, LibraryTabActivity.class);
		startActivity(i);
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
			case R.id.GMM_LibTab:
				openLibrary();
				return true;
			case R.id.GMM_Settings:
				i = new Intent(this, SettingsActivity.class);
				startActivityForResult(i, SETTINGS);
				return true;
			case R.id.GMM_Outputs:
				i = new Intent(this, SettingsActivity.class);
				i.putExtra(SettingsActivity.OPEN_OUTPUT, true);
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

	private void refreshActionBarTitle()
	{
		final ActionBar actionBar = getActionBar();
		switch (currentDisplayMode)
		{
			case MODE_NOWPLAYING:
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
				setTitle(R.string.nowPlaying);
				break;
			case MODE_QUEUE:
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
				setTitle(R.string.playQueue);
				break;
			case MODE_LIBRARY:
				break;
		}
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			mDrawerList.setItemChecked(position, true);
			final DisplayMode newMode;
			switch (position) {
				default:
				case 0:
					newMode = DisplayMode.MODE_LIBRARY;
					break;
				case 1:
					newMode = DisplayMode.MODE_NOWPLAYING;
					break;
				case 2:
					newMode = DisplayMode.MODE_QUEUE;
					break;
			}
			switchMode(newMode);
		}
	}

	/** Swaps fragments in the main content view */
	private void switchMode(DisplayMode newMode) {
		currentDisplayMode = newMode;
		switch (currentDisplayMode)
		{
			case MODE_NOWPLAYING:
				if (isDualPaneMode) {
					nowPlayingDualPane.setVisibility(View.VISIBLE);
				} else {
					nowPlayingFragment.setVisibility(View.VISIBLE);
					playlistFragment.setVisibility(View.GONE);
				}
				libraryRootFrame.setVisibility(View.GONE);
				break;
			case MODE_QUEUE:
				// No need to check for dual panel mode since the menu item won't even appear
				nowPlayingFragment.setVisibility(View.GONE);
				playlistFragment.setVisibility(View.VISIBLE);
				libraryRootFrame.setVisibility(View.GONE);
				break;
			case MODE_LIBRARY:
				if (isDualPaneMode) {
					nowPlayingDualPane.setVisibility(View.GONE);
				} else {
					nowPlayingFragment.setVisibility(View.GONE);
					playlistFragment.setVisibility(View.GONE);
				}
				libraryRootFrame.setVisibility(View.VISIBLE);
				break;
		}
		refreshActionBarTitle();
	}
}
