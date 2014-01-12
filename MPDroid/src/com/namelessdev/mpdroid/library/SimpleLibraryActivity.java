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

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MPDroidActivities.MPDroidFragmentActivity;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.fragments.AlbumsFragment;
import com.namelessdev.mpdroid.fragments.AlbumsGridFragment;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.FSFragment;
import com.namelessdev.mpdroid.fragments.LibraryFragment;
import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.namelessdev.mpdroid.fragments.SongsFragment;
import com.namelessdev.mpdroid.fragments.StreamsFragment;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.exception.MPDServerException;

public class SimpleLibraryActivity extends MPDroidFragmentActivity implements
        ILibraryFragmentActivity, OnBackStackChangedListener {

    public final String EXTRA_ALBUM = "album";
    public final String EXTRA_ARTIST = "artist";
    public final String EXTRA_STREAM = "streams";
    public final String EXTRA_FOLDER = "folder";

    private TextView titleView;

    @Override
    public void onBackStackChanged() {
        refreshTitleFromCurrentFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tabs);

        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        titleView = (TextView) inflater.inflate(R.layout.actionbar_title, null);
        titleView.setFocusable(true);
        titleView.setFocusableInTouchMode(true);
        titleView.setSelected(true);
        titleView.requestFocus();

        final ActionBar actionBar = getActionBar();
        actionBar.setCustomView(titleView);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);

        Object targetElement = null;
        if (savedInstanceState == null) {
            Fragment rootFragment = null;
            if (getIntent().getBooleanExtra("streams", false)) {
                rootFragment = new StreamsFragment();
            } else {
                targetElement = getIntent().getParcelableExtra(EXTRA_ALBUM);
                if (targetElement == null)
                    targetElement = getIntent().getParcelableExtra(EXTRA_ARTIST);
                if (targetElement == null)
                    targetElement = getIntent().getStringExtra(EXTRA_FOLDER);
                if (targetElement == null) {
                    throw new RuntimeException(
                            "Error : cannot start SimpleLibraryActivity without an extra");
                } else {
                    if (targetElement instanceof Artist) {
                        AlbumsFragment af;
                        final SharedPreferences settings = PreferenceManager
                                .getDefaultSharedPreferences(getApplication());
                        if (settings.getBoolean(LibraryFragment.PREFERENCE_ALBUM_LIBRARY, true)) {
                            af = new AlbumsGridFragment((Artist) targetElement);
                        } else {
                            af = new AlbumsFragment((Artist) targetElement);
                        }
                        rootFragment = af;
                    } else if (targetElement instanceof Album) {
                        rootFragment = new SongsFragment().init((Album) targetElement);
                    } else if (targetElement instanceof String) {
                        rootFragment = new FSFragment().init((String) targetElement);
                    }
                }
            }
            if (rootFragment != null) {
                if (rootFragment instanceof BrowseFragment)
                    setTitle(((BrowseFragment) rootFragment).getTitle());
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // For onKeyLongPress to work
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        final MPDApplication app = (MPDApplication) getApplicationContext();
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            app.oMPDAsyncHelper.oMPD.next();
                        } catch (MPDServerException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            app.oMPDAsyncHelper.oMPD.previous();
                        } catch (MPDServerException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, final KeyEvent event) {
        final MPDApplication app = (MPDApplication) getApplicationContext();
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (event.isTracking() && !event.isCanceled()
                        && !app.getApplicationState().streamingMode) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                app.oMPDAsyncHelper.oMPD.adjustVolume(
                                        event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ?
                                                NowPlayingFragment.VOLUME_STEP
                                                : -NowPlayingFragment.VOLUME_STEP);
                            } catch (MPDServerException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void pushLibraryFragment(Fragment fragment, String label) {
        String title = "";
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
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        titleView.setText(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        titleView.setText(getString(titleId));
    }
}
