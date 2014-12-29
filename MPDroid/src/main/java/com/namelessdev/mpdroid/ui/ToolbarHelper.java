/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.ui;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.SettingsActivity;
import com.namelessdev.mpdroid.fragments.OutputsFragment;
import com.namelessdev.mpdroid.library.SimpleLibraryActivity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

/**
 * Class that is meant to help standard Toolbars (and ActionBars)
 */
public class ToolbarHelper {
    private ToolbarHelper() {}

    /**
     * Make the toolbar show a "back" button. Use this when you have the toolbar inside an activity.
     */
    @SuppressWarnings("unused")
    public static void showBackButton(final Activity activity, Toolbar toolbar) {
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    activity.onBackPressed();
                }
            }
        });
    }

    /**
     * Make the toolbar show a "back" button. Use this when you have the toolbar inside a fragment.
     */
    public static void showBackButton(final Fragment fragment, Toolbar toolbar) {
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity a = fragment.getActivity();
                if (a != null) {
                    a.onBackPressed();
                }
            }
        });
    }

    public static void hideBackButton(Toolbar toolbar) {
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);
    }

    public static void addSearchView(Activity activity, Toolbar toolbar) {
        toolbar.inflateMenu(R.menu.mpd_searchmenu);
        // Don't catch everything, we'd rather have a crash than an unusuable search field
        SearchView searchView = (SearchView) toolbar.getMenu().findItem(R.id.menu_search).getActionView();
        manuallySetupSearchView(activity, searchView);
    }

    public static void manuallySetupSearchView(Activity activity, SearchView searchView) {
        SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
    }

    public static void addStandardMenuItemClickListener(final Fragment fragment, Toolbar toolbar, final Toolbar.OnMenuItemClickListener chainedListener) {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem menuItem) {
                if (chainedListener != null) {
                    boolean chainedListenerResult = chainedListener.onMenuItemClick(menuItem);
                    if (chainedListenerResult) {
                        return true;
                    }
                }

                final Activity activity = fragment.getActivity();
                if (activity != null) {
                    standardOnMenuItemClick(activity, menuItem);
                }

                return false;
            }
        });
    }

    @SuppressWarnings("unused")
    public static void addStandardMenuItemClickListener(final Activity activity, Toolbar toolbar, final Toolbar.OnMenuItemClickListener chainedListener) {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem menuItem) {
                if (chainedListener != null) {
                    boolean chainedListenerResult = chainedListener.onMenuItemClick(menuItem);
                    if (chainedListenerResult) {
                        return true;
                    }
                }

                if (activity != null) {
                    standardOnMenuItemClick(activity, menuItem);
                }

                return false;
            }
        });
    }

    private static boolean standardOnMenuItemClick(Context context, final MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_outputs:
                final Intent outputIntent = new Intent(context,
                        SimpleLibraryActivity.class);
                outputIntent.putExtra(OutputsFragment.EXTRA, "1");
                context.startActivity(outputIntent);
                return true;
            case R.id.menu_settings:
                final Intent settingsIntent = new Intent(context,
                        SettingsActivity.class);
                context.startActivity(settingsIntent);
                return true;
        }
        return false;
    }
}
