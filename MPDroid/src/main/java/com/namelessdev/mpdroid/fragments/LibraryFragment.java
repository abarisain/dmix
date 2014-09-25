/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.fragments;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryTabActivity;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;

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

public class LibraryFragment extends Fragment {

    public static final String PREFERENCE_ALBUM_CACHE = "useLocalAlbumCache";

    public static final String PREFERENCE_ALBUM_LIBRARY = "enableAlbumArtLibrary";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE = "artistTagToUse";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_ALBUMARTIST = "albumartist";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_ARTIST = "artist";

    public static final String PREFERENCE_ARTIST_TAG_TO_USE_BOTH = "both";

    private final MPDApplication mApp = MPDApplication.getInstance();

    ILibraryTabActivity mActivity = null;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter = null;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager = null;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof ILibraryTabActivity)) {
            throw new RuntimeException(
                    "Error : LibraryFragment can only be attached to an activity implementing ILibraryTabActivity");
        }
        mActivity = (ILibraryTabActivity) activity;
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.library_tabs_fragment, container, false);
        mViewPager = (ViewPager) view;
        if (mSectionsPagerAdapter != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(final int position) {
                if (mActivity != null) {
                    mActivity.pageChanged(position);
                }
            }
        });
        return view;
    }

    public void setCurrentItem(final int item, final boolean smoothScroll) {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(item, smoothScroll);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the primary sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mActivity.getTabList().size();
        }

        @Override
        public Fragment getItem(final int i) {
            final Fragment fragment;
            final String tab = mActivity.getTabList().get(i);

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

            return fragment;
        }
    }
}
