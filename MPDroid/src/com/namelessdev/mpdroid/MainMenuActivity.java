package com.namelessdev.mpdroid;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.namelessdev.mpdroid.fragments.PlaylistFragment;
import com.namelessdev.mpdroid.tools.Tools;

public class MainMenuActivity extends SherlockFragmentActivity implements OnNavigationListener {

	public static final int PLAYLIST = 1;

	public static final int ARTISTS = 2;

	public static final int SETTINGS = 5;

	public static final int STREAM = 6;

	public static final int LIBRARY = 7;

	public static final int CONNECT = 8;
	
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
     * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
     * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;	
    private int backPressExitCount;
    private Handler exitCounterReset;

	@SuppressLint("NewApi")
	@TargetApi(11)
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        
        exitCounterReset = new Handler();
        
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        
        ArrayAdapter<CharSequence> actionBarAdapter = new ArrayAdapter<CharSequence>(this, R.layout.sherlock_spinner_item);
        actionBarAdapter.add(getString(R.string.nowPlaying));
        actionBarAdapter.add(getString(R.string.playQueue));
        
        if(Build.VERSION.SDK_INT >= 14) {
        	//Bug on ICS with sherlock's layout
        	actionBarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        } else {
        	actionBarAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        }
        actionBar.setListNavigationCallbacks(actionBarAdapter, this);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // When swiping between different sections, select the corresponding tab.
        // We can also use ActionBar.Tab#select() to do this if we have a reference to the
        // Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
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

	/**
	 * Called when Back button is pressed, displays message to user indicating the if back button is pressed again the application will exit. We keep a count of how many time back
	 * button is pressed within 5 seconds. If the count is greater than 1 then call system.exit(0)
	 * 
	 * Starts a post delay handler to reset the back press count to zero after 5 seconds
	 * 
	 * @return None
	 */
	@Override
	public void onBackPressed() {
		if (backPressExitCount < 1) {
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
			 * Nasty force quit, should shutdown everything nicely but there just too many async tasks maybe I'll correctly implement app.terminateApplication();
			 */
			System.exit(0);
		}
		return;
	}
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		mViewPager.setCurrentItem(itemPosition);
		return true;
	}
	
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = null;
            switch (i) {
            	case 0: fragment = new NowPlayingFragment(); break;
            	case 1: fragment = new PlaylistFragment(); break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.nowPlaying);
                case 1: return getString(R.string.playQueue);
            }
            return null;
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.mpd_mainmenu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		//Reminder : never disable buttons that are shown as actionbar actions here
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
			if (((MPDApplication) this.getApplication()).getApplicationState().streamingMode) { // yeah, yeah getApplication for that may be ugly but
																						// ...
				i = new Intent(this, StreamingService.class);
				i.setAction("com.namelessdev.mpdroid.DIE");
				this.startService(i);
				((MPDApplication) this.getApplication()).getApplicationState().streamingMode = false;
				// Toast.makeText(this, "MPD Streaming Stopped", Toast.LENGTH_SHORT).show();
			} else {
				i = new Intent(this, StreamingService.class);
				i.setAction("com.namelessdev.mpdroid.START_STREAMING");
				this.startService(i);
				((MPDApplication) this.getApplication()).getApplicationState().streamingMode = true;
				// Toast.makeText(this, "MPD Streaming Started", Toast.LENGTH_SHORT).show();
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
	
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		final MPDApplication app = (MPDApplication) getApplicationContext();
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if(!app.getApplicationState().streamingMode) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							app.oMPDAsyncHelper.oMPD.adjustVolume(keyCode == KeyEvent.KEYCODE_VOLUME_UP ? NowPlayingFragment.VOLUME_STEP : -NowPlayingFragment.VOLUME_STEP);
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}
				}).start();
				return true;
			}
			break;
		default:
			return super.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}
    
}
