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

package com.namelessdev.mpdroid.library;

import com.namelessdev.mpdroid.MPDroidActivities;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.fragments.AlbumsFragment;
import com.namelessdev.mpdroid.fragments.AlbumsGridFragment;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.FSFragment;
import com.namelessdev.mpdroid.fragments.LibraryFragment;
import com.namelessdev.mpdroid.fragments.SongsFragment;
import com.namelessdev.mpdroid.fragments.StreamsFragment;
import com.namelessdev.mpdroid.helpers.MPDControl;

import org.a0z.mpd.item.Album;
import org.a0z.mpd.item.Artist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class SimpleLibraryActivity extends MPDroidActivities.MPDroidActivity implements
        ILibraryFragmentActivity, OnBackStackChangedListener {

    private static final String EXTRA_ALBUM = "album";

    private static final String EXTRA_ARTIST = "artist";

    private static final String EXTRA_FOLDER = "folder";

    private static final String EXTRA_STREAM = "streams";

    private TextView mTitleView;

    private Fragment getRootFragment() {
        final Intent intent = getIntent();
        Fragment rootFragment = null;

        if (intent.getBooleanExtra(EXTRA_STREAM, false)) {
            rootFragment = new StreamsFragment();
        } else {
            Object targetElement = intent.getParcelableExtra(EXTRA_ALBUM);
            if (targetElement == null) {
                targetElement = intent.getParcelableExtra(EXTRA_ARTIST);
            }
            if (targetElement == null) {
                targetElement = intent.getStringExtra(EXTRA_FOLDER);
            }
            if (targetElement == null) {
                throw new RuntimeException(
                        "Error : cannot start SimpleLibraryActivity without an extra");
            } else {
                if (targetElement instanceof Artist) {
                    final SharedPreferences settings = PreferenceManager
                            .getDefaultSharedPreferences(mApp);

                    if (settings.getBoolean(LibraryFragment.PREFERENCE_ALBUM_LIBRARY, true)) {
                        rootFragment = new AlbumsGridFragment((Artist) targetElement);
                    } else {
                        rootFragment = new AlbumsFragment((Artist) targetElement);
                    }
                } else if (targetElement instanceof Album) {
                    rootFragment = new SongsFragment().init((Album) targetElement);
                } else if (targetElement instanceof String) {
                    rootFragment = new FSFragment().init((String) targetElement);
                }
            }
        }

        return rootFragment;
    }

    @Override
    public void onBackStackChanged() {
        refreshTitleFromCurrentFragment();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tabs);

        final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mTitleView = (TextView) inflater.inflate(R.layout.actionbar_title, null);

        mTitleView.setFocusable(true);
        mTitleView.setFocusableInTouchMode(true);
        mTitleView.setSelected(true);
        mTitleView.requestFocus();

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(mTitleView);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        if (savedInstanceState == null) {
            final Fragment rootFragment = getRootFragment();

            if (rootFragment != null) {
                if (rootFragment instanceof BrowseFragment) {
                    setTitle(((BrowseFragment) rootFragment).getTitle());
                }
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.replace(R.id.root_frame, rootFragment);
                ft.commit();
            } else {
                throw new RuntimeException("Error : SimpleLibraryActivity root fragment is null");
            }
        } else {
            refreshTitleFromCurrentFragment();
        }
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final boolean result;

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // For onKeyLongPress to work
            event.startTracking();
            result = !mApp.isLocalAudible();
        } else {
            result = super.onKeyDown(keyCode, event);
        }

        return result;
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        boolean result = true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                MPDControl.run(MPDControl.ACTION_NEXT);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                MPDControl.run(MPDControl.ACTION_PREVIOUS);
                break;
            default:
                result = super.onKeyLongPress(keyCode, event);
                break;
        }

        return result;
    }

    @Override
    public boolean onKeyUp(final int keyCode, @NonNull final KeyEvent event) {
        boolean result = true;

        if (event.isTracking() && !event.isCanceled() && !mApp.isLocalAudible()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    MPDControl.run(MPDControl.ACTION_VOLUME_STEP_UP);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    MPDControl.run(MPDControl.ACTION_VOLUME_STEP_DOWN);
                    break;
                default:
                    result = super.onKeyUp(keyCode, event);
                    break;
            }
        } else {
            result = super.onKeyUp(keyCode, event);
        }

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean result = false;

        if (item.getItemId() == android.R.id.home) {
            finish();
            result = true;
        }

        return result;
    }

    @Override
    public void pushLibraryFragment(final Fragment fragment, final String label) {
        final String title;
        if (fragment instanceof BrowseFragment) {
            title = ((BrowseFragment) fragment).getTitle();
        } else {
            title = fragment.toString();
        }
        setTitle(title);
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.root_frame, fragment);
        ft.addToBackStack(label);
        ft.setBreadCrumbTitle(title);
        ft.commit();
    }

    private void refreshTitleFromCurrentFragment() {
        final FragmentManager supportFM = getSupportFragmentManager();
        final int fmStackCount = supportFM.getBackStackEntryCount();

        if (fmStackCount > 0) {
            setTitle(supportFM.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
        } else {
            final Fragment displayedFragment = getSupportFragmentManager().findFragmentById(
                    R.id.root_frame);
            if (displayedFragment instanceof BrowseFragment) {
                setTitle(((BrowseFragment) displayedFragment).getTitle());
            } else {
                setTitle(displayedFragment.toString());
            }
        }
    }

    @Override
    public void setTitle(final CharSequence title) {
        super.setTitle(title);
        mTitleView.setText(title);
    }

    @Override
    public void setTitle(final int titleId) {
        super.setTitle(titleId);
        mTitleView.setText(getString(titleId));
    }
}
