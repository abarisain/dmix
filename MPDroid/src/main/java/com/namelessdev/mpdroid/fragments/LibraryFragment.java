/*
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.fragments;

import com.anpmech.mpd.item.Music;
import com.astuetz.PagerSlidingTabStrip;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.SettingsActivity;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;
import com.namelessdev.mpdroid.ui.ToolbarHelper;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class LibraryFragment extends Fragment {

    public static final String PREFERENCE_ALBUM_CACHE = "useLocalAlbumCache";

    public static final String PREFERENCE_ALBUM_LIBRARY = "enableAlbumArtLibrary";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE = "artistTagToUse";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_ALBUMARTIST = Music.TAG_ALBUM_ARTIST;

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_ARTIST = Music.TAG_ARTIST;

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_BOTH = "both";

    private final MPDApplication mApp = MPDApplication.getInstance();

    Activity mActivity = null;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory intensive, it may be
     * best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter = null;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager = null;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        mActivity = activity;
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.library_tabs_fragment, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        if (mSectionsPagerAdapter != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }
        /*mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(final int position) {
                if (mActivity != null) {
                    mActivity.pageChanged(position);
                }
            }
        });*/

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);
        tabs.setViewPager(mViewPager);

        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.inflateMenu(R.menu.mpd_main_menu);
        ToolbarHelper.addStandardMenuItemClickListener(this, toolbar, null);
        ToolbarHelper.addSearchView(getActivity(), toolbar);

        return view;
    }

    public void setCurrentItem(final int item, final boolean smoothScroll) {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(item, smoothScroll);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private List<String> currentTabs;

        public SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);
            currentTabs = LibraryTabsUtil.getCurrentLibraryTabs();
        }

        @Override
        public int getCount() {
            return currentTabs.size();
        }

        @Override
        public Fragment getItem(final int i) {
            final BrowseFragment fragment;
            final String tab = currentTabs.get(i);

            switch (tab) {
                case LibraryTabsUtil.TAB_ALBUMS:
                    final SharedPreferences settings = PreferenceManager
                            .getDefaultSharedPreferences(mApp);
                    if (settings.getBoolean(PREFERENCE_ALBUM_LIBRARY, true)) {
                        fragment = new AlbumsGridFragment(null);
                    } else {
                        fragment = new AlbumsFragment(null);
                    }
                    break;
                case LibraryTabsUtil.TAB_ARTISTS:
                    fragment = new ArtistsFragment().init(null);
                    break;
                case LibraryTabsUtil.TAB_FILES:
                    fragment = new FSFragment();
                    break;
                case LibraryTabsUtil.TAB_GENRES:
                    fragment = new GenresFragment();
                    break;
                case LibraryTabsUtil.TAB_PLAYLISTS:
                    fragment = new PlaylistsFragment();
                    break;
                case LibraryTabsUtil.TAB_STREAMS:
                    fragment = new StreamsFragment();
                    break;
                default:
                    fragment = null;
                    break;
            }

            if (fragment != null) {
                fragment.setEmbedded(true);
            }

            return fragment;
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            final String tab = currentTabs.get(position);

            return mActivity.getResources().getString(LibraryTabsUtil.getTabTitleResId(tab));
        }
    }
}
