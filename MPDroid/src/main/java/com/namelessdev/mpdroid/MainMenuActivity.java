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
import com.namelessdev.mpdroid.fragments.OutputsFragment;
import com.namelessdev.mpdroid.helpers.MPDConnectionHandler;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

public class MainMenuActivity extends MPDroidActivities.MPDroidActivity implements
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

    private LibraryFragment mLibraryFragment;

    static {
        final StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    private LibraryFragment initializeLibraryFragment() {
        LibraryFragment fragment =
                (LibraryFragment) mFragmentManager.findFragmentByTag(FRAGMENT_TAG_LIBRARY);

        if (fragment == null) {
            fragment = new LibraryFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.library_root_frame, fragment, FRAGMENT_TAG_LIBRARY);
            ft.commit();
        }

        return fragment;
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

        getSupportActionBar().hide();

        if (mApp.hasGooglePlayDeathWarningBeenDisplayed()
                && !mApp.hasGooglePlayThankYouBeenDisplayed()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.gpThanksTitle))
                    .setMessage(getString(R.string.gpThanksMessage))
                    .setNegativeButton(getString(R.string.gpThanksOkButton),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface,
                                        final int i) {
                                    mApp.markGooglePlayThankYouAsRead();
                                }
                            })
                    .setCancelable(false)
                    .show();
        }

        mApp.setupServiceBinder();

        setContentView(R.layout.main_activity_nagvigation);

        mFragmentManager = getSupportFragmentManager();

        mLibraryFragment = initializeLibraryFragment();

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
    public final boolean onKeyUp(final int keyCode, @NonNull final KeyEvent event) {
        boolean result = true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (event.isTracking() && !event.isCanceled() && !mApp.isLocalAudible()) {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        MPDControl.run(MPDControl.ACTION_VOLUME_STEP_UP);
                    } else {
                        MPDControl.run(MPDControl.ACTION_VOLUME_STEP_DOWN);
                    }
                }
                break;
            default:
                result = super.onKeyUp(keyCode, event);
                break;
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
        mApp.setActivity(this);

        if (mApp.isNotificationPersistent()) {
            mApp.startNotification();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mApp.unsetActivity(this);
    }

    @Override
    public void pushLibraryFragment(final Fragment fragment, final String label) {
        final String title;
        if (fragment instanceof BrowseFragment) {
            title = ((BrowseFragment) fragment).getTitle();
        } else {
            title = fragment.toString();
        }
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.library_root_frame, fragment);
        ft.addToBackStack(label);
        ft.setBreadCrumbTitle(title);
        ft.commit();
    }

}
