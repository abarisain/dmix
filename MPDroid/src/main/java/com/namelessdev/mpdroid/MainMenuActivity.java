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
        ActionBar.OnNavigationListener,
        ILibraryFragmentActivity, OnBackStackChangedListener,
        PopupMenu.OnMenuItemClickListener {

    private static final boolean DEBUG = false;

    private static final String EXTRA_DISPLAY_MODE = "displaymode";

    private static final String FRAGMENT_TAG_LIBRARY = "library";

    private static final String FRAGMENT_TAG_OUTPUTS = "outputs";

    private static final int SETTINGS = 5;

    private static final String TAG = "MainMenuActivity";

    private int mBackPressExitCount;

    private DisplayMode mCurrentDisplayMode;

    private DrawerLayout mDrawerLayout;

    private ListView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;

    private Handler mExitCounterReset = new Handler();

    private FragmentManager mFragmentManager;

    private LibraryFragment mLibraryFragment;

    private View mLibraryRootFrame;

    private int mOldDrawerPosition = 0;

    private OutputsFragment mOutputsFragment;

    private View mOutputsRootFrame;

    private TextView mTextView;

    static {
        final StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    private ListView initializeDrawerList() {
        final ListView drawerList = (ListView) findViewById(R.id.left_drawer);
        final DrawerItem[] drawerItems = {
                new DrawerItem(getString(R.string.libraryTabActivity),
                        DrawerItem.Action.ACTION_LIBRARY),

                new DrawerItem(getString(R.string.outputs), DrawerItem.Action.ACTION_OUTPUTS),

                new DrawerItem(getString(R.string.settings), DrawerItem.Action.ACTION_SETTINGS)
        };

        // Set the adapter for the list view
        drawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, drawerItems));
        drawerList.setItemChecked(mOldDrawerPosition, true);

        // Set the list's click listener
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        return drawerList;
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

    private OutputsFragment initializeOutputsFragment() {
        OutputsFragment fragment =
                (OutputsFragment) mFragmentManager.findFragmentByTag(FRAGMENT_TAG_OUTPUTS);

        if (fragment == null) {
            fragment = new OutputsFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.outputs_root_frame, fragment, FRAGMENT_TAG_OUTPUTS);
            ft.commit();
        }

        return fragment;
    }

    private TextView initializeTextView() {
        final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final TextView textView = (TextView) inflater.inflate(R.layout.actionbar_title, null);

        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
        textView.setSelected(true);
        textView.requestFocus();

        return textView;
    }

    @Override
    public void onBackPressed() {
        if (mCurrentDisplayMode != DisplayMode.MODE_LIBRARY) {
            switchMode(DisplayMode.MODE_LIBRARY);
        } else if (mFragmentManager.getBackStackEntryCount() > 0) {
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
    public void onBackStackChanged() {
        refreshActionBarTitle();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
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

        mTextView = initializeTextView();

        mLibraryRootFrame = findViewById(R.id.library_root_frame);
        mOutputsRootFrame = findViewById(R.id.outputs_root_frame);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerList = initializeDrawerList();

        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);

        mLibraryFragment = initializeLibraryFragment();
        mOutputsFragment = initializeOutputsFragment();

        if (savedInstanceState == null) {
            switchMode(DisplayMode.MODE_LIBRARY);
        } else {
            switchMode((DisplayMode) savedInstanceState.getSerializable(EXTRA_DISPLAY_MODE));
        }

        /** Reset the persistent override when the application is reset. */
        mApp.setPersistentOverride(false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mpd_searchmenu, menu);
        return true;
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
    public boolean onMenuItemClick(final MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(final int itemPosition, final long itemId) {
        mLibraryFragment.setCurrentItem(itemPosition, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final boolean itemHandled = mDrawerToggle.onOptionsItemSelected(item);

        // Handle item selection
        if (!itemHandled) {
            switch (item.getItemId()) {
                case R.id.menu_search:
                    onSearchRequested();
                    break;
            }
        }

        return super.onOptionsItemSelected(item);
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
    public void onSaveInstanceState(final Bundle outState) {
        outState.putSerializable(EXTRA_DISPLAY_MODE, mCurrentDisplayMode);
        super.onSaveInstanceState(outState);
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

    /**
     * Navigation Drawer helpers
     */

    private void refreshActionBarTitle() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);

        if (mCurrentDisplayMode == DisplayMode.MODE_OUTPUTS) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            if (mCurrentDisplayMode == DisplayMode.MODE_OUTPUTS) {
                mTextView.setText(R.string.outputs);
            }
        } else if (mCurrentDisplayMode == DisplayMode.MODE_LIBRARY) {
            int fmStackCount = 0;

            if (mFragmentManager != null) {
                fmStackCount = mFragmentManager.getBackStackEntryCount();
            }

            if (fmStackCount > 0) {
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                mTextView.setText(mFragmentManager.getBackStackEntryAt(fmStackCount - 1)
                        .getBreadCrumbTitle());
            } else {
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                actionBar.setDisplayShowCustomEnabled(false);
            }
        }
    }

    /** Swaps fragments in the main content view */
    void switchMode(final DisplayMode newMode) {
        mCurrentDisplayMode = newMode;
        switch (mCurrentDisplayMode) {
            case MODE_LIBRARY:
                mLibraryRootFrame.setVisibility(View.VISIBLE);
                mOutputsRootFrame.setVisibility(View.GONE);
                break;
            case MODE_OUTPUTS:
                mLibraryRootFrame.setVisibility(View.GONE);
                mOutputsRootFrame.setVisibility(View.VISIBLE);
                mOutputsFragment.refreshOutputs();
                break;
        }
        refreshActionBarTitle();
    }

    public enum DisplayMode {
        MODE_LIBRARY,
        MODE_OUTPUTS
    }

    private static class DrawerItem {

        private final Action mAction;

        private final String mLabel;

        DrawerItem(final String label, final Action action) {
            super();
            mLabel = label;
            mAction = action;
        }

        @Override
        public String toString() {
            return mLabel;
        }

        private enum Action {
            ACTION_LIBRARY,
            ACTION_OUTPUTS,
            ACTION_SETTINGS
        }
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                final long id) {
            mDrawerLayout.closeDrawer(mDrawerList);

            switch (((DrawerItem) parent.getItemAtPosition(position)).mAction) {
                case ACTION_LIBRARY:
                    // If we are already on the library, pop the whole stack.
                    // Acts like an "up" button
                    if (mCurrentDisplayMode == DisplayMode.MODE_LIBRARY) {
                        final int fmStackCount = mFragmentManager.getBackStackEntryCount();
                        if (fmStackCount > 0) {
                            mFragmentManager.popBackStack(null,
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }
                    }
                    switchMode(DisplayMode.MODE_LIBRARY);
                    break;
                case ACTION_OUTPUTS:
                    switchMode(DisplayMode.MODE_OUTPUTS);
                    break;
                case ACTION_SETTINGS:
                    mDrawerList.setItemChecked(mOldDrawerPosition, true);
                    final Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
                    startActivityForResult(intent, SETTINGS);
                    break;
            }
            mOldDrawerPosition = position;
        }
    }

}
