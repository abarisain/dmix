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

package com.namelessdev.mpdroid.library;

import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Artist;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.Stream;
import com.anpmech.mpd.subsystem.AudioOutput;
import com.namelessdev.mpdroid.MPDActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.fragments.AlbumsFragment;
import com.namelessdev.mpdroid.fragments.AlbumsGridFragment;
import com.namelessdev.mpdroid.fragments.ArtistsFragment;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.FSFragment;
import com.namelessdev.mpdroid.fragments.OutputsFragment;
import com.namelessdev.mpdroid.fragments.SongsFragment;
import com.namelessdev.mpdroid.fragments.StreamsFragment;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.tools.Tools;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.transition.Transition;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class SimpleLibraryActivity extends MPDActivity implements ILibraryFragmentActivity,
        OnBackStackChangedListener {

    private TextView mTitleView;

    /**
     * This method instantiates a fragment with Intent extras as a argument.
     *
     * @param tClass The class to instantiate.
     * @param intent The intent to get the Extra bundles from.
     * @param <T>    The {@code tClass} must be extended from the {@link Fragment} type.
     * @return The instantiated fragment with the Intent extras as a argument.
     */
    private <T extends Fragment> Fragment getFragment(final Class<T> tClass, final Intent intent) {
        Bundle bundle = null;

        if (intent != null) {
            bundle = intent.getExtras();
        }
        return Fragment.instantiate(this, tClass.getName(), bundle);
    }

    /**
     * This method instantiates a fragment.
     *
     * @param tClass The class to instantiate.
     * @param <T>    The {@code tClass} must be extended from the {@link Fragment} type.
     * @return The instantiated Fragment.
     */
    private <T extends Fragment> Fragment getFragment(final Class<T> tClass) {
        return getFragment(tClass, null);
    }

    private Fragment getRootFragment() {
        final Intent intent = getIntent();
        final Fragment rootFragment;

        if (intent.hasExtra(Album.EXTRA)) {
            rootFragment = getFragment(SongsFragment.class, intent);
        } else if (intent.hasExtra(Artist.EXTRA)) {
            final SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(mApp);

            if (settings.getBoolean(ArtistsFragment.PREFERENCE_ALBUM_LIBRARY, true)) {
                rootFragment = getFragment(AlbumsGridFragment.class, intent);
            } else {
                rootFragment = getFragment(AlbumsFragment.class, intent);
            }
        } else if (intent.hasExtra(Directory.EXTRA)) {
            rootFragment = getFragment(FSFragment.class, intent);
        } else if (intent.hasExtra(Stream.EXTRA)) {
            rootFragment = getFragment(StreamsFragment.class);
        } else if (intent.hasExtra(AudioOutput.EXTRA)) {
            rootFragment = getFragment(OutputsFragment.class);
            setTitle(R.string.outputs);
        } else {
            throw new IllegalStateException("SimpleLibraryActivity started with invalid extra: " +
                    Tools.debugIntent(intent, getCallingActivity()));
        }

        return rootFragment;
    }

    @Override
    public void onBackStackChanged() {
        refreshTitleFromCurrentFragment(getSupportFragmentManager());
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_library);

        final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final FragmentManager fragmentManager = getSupportFragmentManager();

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
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            final Fragment rootFragment = getRootFragment();

            if (rootFragment == null) {
                throw new UnsupportedOperationException(
                        "Error : SimpleLibraryActivity root fragment is null");
            }

            if (rootFragment instanceof BrowseFragment) {
                getSupportActionBar().hide();
            }

            final FragmentTransaction ft = fragmentManager.beginTransaction();

            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.root_frame, rootFragment);
            ft.commit();
        } else {
            refreshTitleFromCurrentFragment(fragmentManager);
        }
        fragmentManager.addOnBackStackChangedListener(this);
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        final boolean result;

        if (item.getItemId() == android.R.id.home) {
            finish();
            result = true;
        } else {
            result = super.onOptionsItemSelected(item);
        }

        return result;
    }

    @Override
    public void pushLibraryFragment(final Fragment fragment, final String label) {
        pushLibraryFragment(fragment, label, null, null, null);
    }

    @Override
    public void pushLibraryFragment(final Fragment fragment, final String label,
            final View transitionView, final String transitionName, final Transition transition) {
        final String title;
        if (fragment instanceof BrowseFragment) {
            title = ((BrowseFragment<?>) fragment).getTitle();
        } else {
            title = fragment.toString();
        }
        setTitle(title);
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (transitionView != null) {
            ft.addSharedElement(transitionView, transitionName);
            fragment.setSharedElementEnterTransition(transition);
        } else {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        ft.replace(R.id.root_frame, fragment);
        ft.addToBackStack(label);
        ft.setBreadCrumbTitle(title);
        ft.commit();
    }

    private void refreshTitleFromCurrentFragment(final FragmentManager fragmentManager) {
        final int fmStackCount = fragmentManager.getBackStackEntryCount();

        if (fmStackCount > 0) {
            setTitle(fragmentManager.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
        } else {
            final Fragment displayedFragment = getSupportFragmentManager().findFragmentById(
                    R.id.root_frame);

            if (displayedFragment instanceof BrowseFragment) {
                setTitle(((BrowseFragment<?>) displayedFragment).getTitle());
            } else if (displayedFragment != null) {
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
