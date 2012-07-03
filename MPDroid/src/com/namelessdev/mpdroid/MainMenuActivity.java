package com.namelessdev.mpdroid;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.namelessdev.mpdroid.tools.Tools;

public class MainMenuActivity extends SherlockFragmentActivity implements OnNavigationListener {

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
        actionBarAdapter.add(getString(R.string.playlist));
        
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
	 * @param None
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
                case 1: return getString(R.string.playlist);
            }
            return null;
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply displays dummy text.
     */
    public static class DummySectionFragment extends Fragment {
        public DummySectionFragment() {
        }

        public static final String ARG_SECTION_NUMBER = "section_number";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            TextView textView = new TextView(getActivity());
            textView.setGravity(Gravity.CENTER);
            Bundle args = getArguments();
            textView.setText(Integer.toString(args.getInt(ARG_SECTION_NUMBER)));
            return textView;
        }
    }
}
