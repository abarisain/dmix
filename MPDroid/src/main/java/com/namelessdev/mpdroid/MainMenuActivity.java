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

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.LibraryFragment;
import com.namelessdev.mpdroid.helpers.MPDConnectionHandler;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.preferences.ConnectionModifier;
import com.namelessdev.mpdroid.preferences.ConnectionSettings;
import com.namelessdev.mpdroid.tools.Tools;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.transition.Transition;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;

public class MainMenuActivity extends MPDActivity implements
        ILibraryFragmentActivity {

    private static final boolean DEBUG = false;

    private static final String EXTRA_DISPLAY_MODE = "displaymode";

    private static final String FRAGMENT_TAG_LIBRARY = "library";

    private static final String FRAGMENT_TAG_OUTPUTS = "outputs";

    private static final int SETTINGS = 5;

    private static final String TAG = "MainMenuActivity";

    private int mBackPressExitCount;

    private Handler mExitCounterReset = new Handler();

    private FragmentManager mFragmentManager;

    /**
     * This method determines if a default server has been setup yet.
     *
     * @param context The context to retrieve the settings.
     * @return True if a default server has been setup, false otherwise.
     */
    private static boolean hasDefaultServer(final Context context) {
        final SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(context);

        return settings.contains(ConnectionModifier.KEY_HOSTNAME);
    }

    /**
     * This method returns the current theme resource ID.
     *
     * @return The current theme resource ID.
     */
    @Override
    protected int getThemeResId() {
        final int themeID;

        if (isLightThemeSelected()) {
            themeID = R.style.AppTheme_MainMenu_Light;
        } else {
            themeID = R.style.AppTheme_MainMenu;
        }

        return themeID;
    }

    private void initializeLibraryFragment() {
        if (mFragmentManager.findFragmentByTag(FRAGMENT_TAG_LIBRARY) == null) {
            final Fragment fragment = Fragment.instantiate(this, LibraryFragment.class.getName());
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.library_root_frame, fragment, FRAGMENT_TAG_LIBRARY);
            ft.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (mFragmentManager.getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

            if (settings.getBoolean("enableExitConfirmation", false) && mBackPressExitCount < 1) {
                Tools.notifyUser(R.string.backpressToQuit);
                mBackPressExitCount += 1;
                mExitCounterReset.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBackPressExitCount = 0;
                    }
                }, 5000L);
            } else {
                finish();
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!hasDefaultServer(this)) {
            final Intent intent = new Intent(this, ConnectionSettings.class);

            // Absolutely no settings defined! Open Settings!
            startActivityForResult(intent, SETTINGS);
        }

        getSupportActionBar().hide();

        mApp.setupServiceBinder();

        setContentView(R.layout.main_activity_nagvigation);

        mFragmentManager = getSupportFragmentManager();

        initializeLibraryFragment();

        /** Reset the persistent override when the application is reset. */
        mApp.setPersistentOverride(false);
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
    protected void onPause() {
        if (DEBUG) {
            unregisterReceiver(MPDConnectionHandler.getInstance());
        }
        super.onPause();
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        // Reminder: Never disable buttons that are shown as actionbar actions here.
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBackPressExitCount = 0;

        if (DEBUG) {
            registerReceiver(MPDConnectionHandler.getInstance(),
                    new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mApp.isNotificationPersistent()) {
            mApp.startNotification();
        }
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
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (transitionView != null) {
            ft.addSharedElement(transitionView, transitionName);
            fragment.setSharedElementEnterTransition(transition);
            fragment.setSharedElementReturnTransition(null);
            // TODO : Fix the exit transitions. They're hard to get, but it would be nice to have a shared exit
            /*Fade fade = new Fade();
            fade.setStartDelay(400);
            fragment.setEnterTransition(fade);
            fragment.setExitTransition(null);*/
        } else {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        ft.replace(R.id.library_root_frame, fragment);
        ft.addToBackStack(label);
        ft.setBreadCrumbTitle(title);
        ft.commit();
    }

}
