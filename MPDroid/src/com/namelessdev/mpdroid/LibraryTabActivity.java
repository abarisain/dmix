package com.namelessdev.mpdroid;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.namelessdev.mpdroid.fragments.AlbumsFragment;
import com.namelessdev.mpdroid.fragments.ArtistsFragment;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.FSFragment;
import com.namelessdev.mpdroid.fragments.GenresFragment;
import com.namelessdev.mpdroid.fragments.PlaylistsFragment;
import com.namelessdev.mpdroid.fragments.StreamsFragment;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;


public class LibraryTabActivity extends SherlockFragmentActivity implements OnNavigationListener, ILibraryFragmentActivity {

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
	ActionBar actionBar;
    
    ArrayList<String> mTabList;


    @TargetApi(11)
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tabs);
        
        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Get the list of the currently visible tabs
        mTabList=LibraryTabsUtil.getCurrentLibraryTabs(this.getApplicationContext());

        // Set up the action bar.
		actionBar = getSupportActionBar();
		// Will set the action bar to it's List style.
		refreshActionBarNavigation(true, null);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        ArrayAdapter<CharSequence> actionBarAdapter = new ArrayAdapter<CharSequence>(this, R.layout.sherlock_spinner_item);
        for (int i=0;i<mTabList.size();i++){
            actionBarAdapter.add(getText(LibraryTabsUtil.getTabTitleResId(mTabList.get(i))));
        }

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.mpd_browsermenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_search:
			this.onSearchRequested();
			return true;
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		mViewPager.setCurrentItem(itemPosition, true);
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
			String tab = mTabList.get(i); 
			if (tab.equals(LibraryTabsUtil.TAB_ARTISTS))
				fragment = new ArtistsFragment().init(null);
			else if (tab.equals(LibraryTabsUtil.TAB_ALBUMS))
				fragment = new AlbumsFragment().init(null);
			else if (tab.equals(LibraryTabsUtil.TAB_PLAYLISTS))
				fragment = new PlaylistsFragment();
			else if (tab.equals(LibraryTabsUtil.TAB_STREAMS))
				fragment = new StreamsFragment();
			else if (tab.equals(LibraryTabsUtil.TAB_FILES))
				fragment = new FSFragment();
			else if (tab.equals(LibraryTabsUtil.TAB_GENRES))
				fragment = new GenresFragment();
			return fragment;
		}

		@Override
		public int getCount() {
			return mTabList.size();
		}

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
		mViewPager.setVisibility(View.GONE);
		String title = "";
		if (fragment instanceof BrowseFragment) {
			title = ((BrowseFragment) fragment).getTitle();
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
	public void onBackPressed() {
		super.onBackPressed();
		final FragmentManager supportFM = getSupportFragmentManager();
		final int fmStackCount = supportFM.getBackStackEntryCount();
		if(fmStackCount > 0) {
			refreshActionBarNavigation(false, supportFM.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
			mViewPager.setVisibility(View.GONE);
		} else {
			refreshActionBarNavigation(true, null);
			mViewPager.setVisibility(View.VISIBLE);
		}
	}
}
