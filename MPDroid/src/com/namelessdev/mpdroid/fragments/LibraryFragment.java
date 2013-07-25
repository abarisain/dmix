package com.namelessdev.mpdroid.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryTabActivity;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;

public class LibraryFragment extends Fragment {
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every loaded fragment in memory. If this becomes too
	 * memory intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter sectionsPagerAdapter = null;

	public static final String PREFERENCE_ALBUM_LIBRARY = "enableAlbumArtLibrary";

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager viewPager = null;
	ILibraryTabActivity activity = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.library_tabs_fragment, container, false);
		viewPager = (ViewPager) view;
		if (sectionsPagerAdapter != null)
			viewPager.setAdapter(sectionsPagerAdapter);
		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (activity != null)
					activity.pageChanged(position);
			}
		});
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ILibraryTabActivity)) {
			throw new RuntimeException("Error : LibraryFragment can only be attached to an activity implementing ILibraryTabActivity");
		}
		this.activity = (ILibraryTabActivity) activity;
		sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
		if (viewPager != null)
			viewPager.setAdapter(sectionsPagerAdapter);
	}

	public void setCurrentItem(int item, boolean smoothScroll) {
		if (viewPager != null)
			viewPager.setCurrentItem(item, smoothScroll);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			Fragment fragment = null;
			String tab = activity.getTabList().get(i);
			if (tab.equals(LibraryTabsUtil.TAB_ARTISTS)) {
				fragment = new ArtistsFragment().init(null);
			} else if (tab.equals(LibraryTabsUtil.TAB_ALBUMS)) {
				// display either normal album listing, or album artwork grid
				final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(((Activity) activity).getApplication());
				if (settings.getBoolean(PREFERENCE_ALBUM_LIBRARY, false)) {
					fragment = new AlbumsGridFragment().init(null);
				}else{
					fragment = new AlbumsFragment().init(null);
				}
			} else if (tab.equals(LibraryTabsUtil.TAB_PLAYLISTS)) {
				fragment = new PlaylistsFragment();
			} else if (tab.equals(LibraryTabsUtil.TAB_STREAMS)) {
				fragment = new StreamsFragment();
			} else if (tab.equals(LibraryTabsUtil.TAB_FILES)) {
				fragment = new FSFragment();
			} else if (tab.equals(LibraryTabsUtil.TAB_GENRES)) {
				fragment = new GenresFragment();
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return activity.getTabList().size();
		}

	}
}
