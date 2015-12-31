/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.views.AlbumGridDataBinder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AlbumsGridFragment extends AlbumsFragment {

    private static final int MIN_ITEMS_BEFORE_FAST_SCROLL = 6;

    @Override
    protected ListAdapter getCustomListAdapter() {
        return new ArrayAdapter<>(getActivity(), new AlbumGridDataBinder(mArtist), mItems);
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

        setupStandardToolbar(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mIsCountDisplayed = false;
    }
}
