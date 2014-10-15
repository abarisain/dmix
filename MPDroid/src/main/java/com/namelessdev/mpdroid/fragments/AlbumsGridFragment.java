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

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.views.AlbumGridDataBinder;

import org.a0z.mpd.item.Artist;
import org.a0z.mpd.item.Genre;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

public class AlbumsGridFragment extends AlbumsFragment {

    private static final int MIN_ITEMS_BEFORE_FAST_SCROLL = 6;

    public AlbumsGridFragment() {
        this(null);
    }

    public AlbumsGridFragment(final Artist artist) {
        this(artist, null);
    }

    public AlbumsGridFragment(final Artist artist, final Genre genre) {
        super(artist, genre);
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (mItems != null) {
            return new ArrayAdapter(getActivity(), new AlbumGridDataBinder(), mItems);
        }
        return super.getCustomListAdapter();
    }

    @Override
    protected int getMinimumItemsCountBeforeFastscroll() {
        return MIN_ITEMS_BEFORE_FAST_SCROLL;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.browsegrid, container, false);
        mList = (AbsListView) view.findViewById(R.id.grid);
        registerForContextMenu(mList);
        mList.setOnItemClickListener(this);
        mLoadingView = view.findViewById(R.id.loadingLayout);
        mLoadingTextView = (TextView) view.findViewById(R.id.loadingText);
        mNoResultView = view.findViewById(R.id.noResultLayout);
        mLoadingTextView.setText(getLoadingText());
        mCoverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.pullToRefresh);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mIsCountDisplayed = false;
    }

    /**
     * This is required because setting the fast scroll prior to KitKat was
     * important because of a bug. This bug has since been corrected, but the
     * opposite order is now required or the fast scroll will not show.
     *
     * @param shouldShowFastScroll If the fast scroll should be shown or not
     */
    @Override
    protected void refreshFastScrollStyle(final boolean shouldShowFastScroll) {
        if (shouldShowFastScroll) {
            refreshFastScrollStyle(View.SCROLLBARS_INSIDE_INSET, true);
        } else {
            refreshFastScrollStyle(View.SCROLLBARS_OUTSIDE_OVERLAY, false);
        }
    }
}
