/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.views.AlbumGridDataBinder;

import org.a0z.mpd.Artist;
import org.a0z.mpd.Genre;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

public class AlbumsGridFragment extends AlbumsFragment {

    private static final int MIN_ITEMS_BEFORE_FASTSCROLL = 6;

    public AlbumsGridFragment() {
        this(null);
    }

    public AlbumsGridFragment(Artist artist) {
        this(artist, null);
    }

    public AlbumsGridFragment(Artist artist, Genre genre) {
        super(artist, genre);
        isCountPossiblyDisplayed = false;
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (items != null) {
            return new ArrayAdapter(getActivity(), new AlbumGridDataBinder(app,
                    app.isLightThemeSelected()), items);
        }
        return super.getCustomListAdapter();
    }

    @Override
    protected int getMinimumItemsCountBeforeFastscroll() {
        return MIN_ITEMS_BEFORE_FASTSCROLL;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browsegrid, container, false);
        list = (GridView) view.findViewById(R.id.grid);
        registerForContextMenu(list);
        list.setOnItemClickListener(this);
        loadingView = view.findViewById(R.id.loadingLayout);
        loadingTextView = (TextView) view.findViewById(R.id.loadingText);
        noResultView = view.findViewById(R.id.noResultLayout);
        loadingTextView.setText(getLoadingText());
        coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
        pullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.pullToRefresh);

        return view;
    }

    @Override
    protected void refreshFastScrollStyle(boolean shouldShowFastScroll) {
        // Note : setting the scrollbar style before setting the fastscroll
        // state is very important pre-KitKat, because of a bug.
        // It is also very important post-KitKat because it needs the opposite
        // order or it won't show the FastScroll
        if (shouldShowFastScroll) {
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                // No need to enable FastScroll, this setter enables it.
                list.setFastScrollAlwaysVisible(true);
                list.setScrollBarStyle(AbsListView.SCROLLBARS_INSIDE_INSET);
            } else {
                list.setScrollBarStyle(AbsListView.SCROLLBARS_INSIDE_INSET);
                list.setFastScrollAlwaysVisible(true);
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                list.setFastScrollAlwaysVisible(false);
                // Matches the XML style
                list.setScrollBarStyle(AbsListView.SCROLLBARS_OUTSIDE_OVERLAY);
            } else {
                list.setScrollBarStyle(AbsListView.SCROLLBARS_OUTSIDE_OVERLAY);
                list.setFastScrollAlwaysVisible(false);
            }
        }
    }
}
