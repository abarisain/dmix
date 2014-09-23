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

package com.namelessdev.mpdroid;

import com.namelessdev.mpdroid.MPDroidActivities.MPDroidFragmentActivity;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.LibraryFragment;
import com.namelessdev.mpdroid.fragments.OutputsFragment;
import com.namelessdev.mpdroid.fragments.QueueFragment;
import com.namelessdev.mpdroid.helpers.MPDConnectionHandler;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.library.ILibraryTabActivity;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;
import com.namelessdev.mpdroid.tools.Tools;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.PopupMenuCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainMenuActivity extends MPDroidFragmentActivity implements OnNavigationListener,
        ILibraryFragmentActivity, ILibraryTabActivity, OnBackStackChangedListener,
        PopupMenu.OnMenuItemClickListener {

    public static final int ARTISTS = 2;

    public static final int CONNECT = 8;

    public static final int LIBRARY = 7;

    public static final int PLAYLIST = 1;

    public static final int SETTINGS = 5;

    public static final int STREAM = 6;

    private static final boolean DEBUG = false;

    private static final String EXTRA_DISPLAY_MODE = "displaymode";

    private static final String EXTRA_SLIDING_PANEL_EXPANDED = "slidingpanelexpanded";

    private static final String FRAGMENT_TAG_LIBRARY = "library";

    private static final String FRAGMENT_TAG_OUTPUTS = "outputs";

    private static final String TAG = "MainMenuActivity";

    private int mBackPressExitCount;

    private DisplayMode mCurrentDisplayMode;

    private DrawerLayout mDrawerLayout;

    private ListView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;

    private Handler mExitCounterReset;

    private FragmentManager mFragmentManager;

    private View mHeaderDragView;

    private PopupMenu mHeaderOverflowPopupMenu;

    private ImageButton mHeaderPlayQueue;

    private TextView mHeaderTitle;

    private boolean mIsDualPaneMode;

    private LibraryFragment mLibraryFragment;

    private View mLibraryRootFrame;

    private ViewPager mNowPlayingPager;

    private int mOldDrawerPosition;

    private OutputsFragment mOutputsFragment;

    private View mOutputsRootFrame;

    private QueueFragment mQueueFragment;

    private SlidingUpPanelLayout mSlidingLayout;

    private ArrayList<String> mTabList;

    private TextView mTextView;

    private static void setMenuChecked(final MenuItem item, final boolean checked) {
        item.setChecked(checked);
    }

    @Override
    public ArrayList<String> getTabList() {
        return mTabList;
    }

    @Override
    public void onBackPressed() {
        if (mSlidingLayout.isPanelExpanded()) {
            mSlidingLayout.collapsePanel();
            return;
        }

        if (mCurrentDisplayMode == DisplayMode.MODE_LIBRARY) {
            final int fmStackCount = mFragmentManager.getBackStackEntryCount();
            if (fmStackCount > 0) {
                super.onBackPressed();
                return;
            }
        } else {
            switchMode(DisplayMode.MODE_LIBRARY);
            return;
        }

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean exitConfirmationRequired = settings.getBoolean("enableExitConfirmation",
                false);
        if (exitConfirmationRequired && mBackPressExitCount < 1) {
            Tools.notifyUser(R.string.backpressToQuit);
            mBackPressExitCount += 1;
            mExitCounterReset.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBackPressExitCount = 0;
                }
            }, 5000);
        } else {
            finish();
        }
    }

    /**
     * Library methods
     */

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

        mApp.setupServiceBinder();
        setContentView(mApp.isTabletUiEnabled() ? R.layout.main_activity_nagvigation_tablet
                : R.layout.main_activity_nagvigation);

        final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mTextView = (TextView) inflater.inflate(R.layout.actionbar_title, null);
        mTextView.setFocusable(true);
        mTextView.setFocusableInTouchMode(true);
        mTextView.setSelected(true);
        mTextView.requestFocus();

        final View nowPlayingDualPane = findViewById(R.id.nowplaying_dual_pane);
        mNowPlayingPager = (ViewPager) findViewById(R.id.pager);
        mLibraryRootFrame = findViewById(R.id.library_root_frame);
        mOutputsRootFrame = findViewById(R.id.outputs_root_frame);

        mIsDualPaneMode = nowPlayingDualPane != null;
        switchMode(DisplayMode.MODE_LIBRARY);

        mExitCounterReset = new Handler();

        final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll()
                .build();
        StrictMode.setThreadPolicy(policy);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setCustomView(mTextView);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);

        final List<DrawerItem> drawerItems = new ArrayList<>();
        drawerItems.add(new DrawerItem(getString(R.string.libraryTabActivity),
                DrawerItem.Action.ACTION_LIBRARY));

        drawerItems.add(new DrawerItem(getString(R.string.outputs),
                DrawerItem.Action.ACTION_OUTPUTS));

        drawerItems.add(new DrawerItem(getString(R.string.settings),
                DrawerItem.Action.ACTION_SETTINGS));

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        final int drawerRes;

        if (mApp.isLightThemeSelected()) {
            drawerRes = R.drawable.ic_drawer_light;
        } else {
            drawerRes = R.drawable.ic_drawer;
        }

        mDrawerToggle = new ActionBarDrawerToggle(
                this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                drawerRes, /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description */
                R.string.drawer_close /* "close drawer" description */
        ) {

            /**
             * Called when a drawer has settled in a completely closed
             * state.
             */
            @Override
            public void onDrawerClosed(final View drawerView) {
                refreshActionBarTitle();
            }

            /**
             * Called when a drawer has settled in a completely open
             * state.
             */
            @Override
            public void onDrawerOpened(final View drawerView) {
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                actionBar.setDisplayShowCustomEnabled(true);
                mTextView.setText(R.string.app_name);
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, drawerItems));
        mOldDrawerPosition = 0;
        mDrawerList.setItemChecked(mOldDrawerPosition, true);
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        /*
         * Setup the library tab
         */
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);

        // Get the list of the currently visible tabs
        mTabList = LibraryTabsUtil.getCurrentLibraryTabs();

        final ArrayAdapter<CharSequence> actionBarAdapter = new ArrayAdapter<>(
                actionBar.getThemedContext(),
                android.R.layout.simple_spinner_item);
        for (int i = 0; i < mTabList.size(); i++) {
            actionBarAdapter.add(getText(LibraryTabsUtil.getTabTitleResId(mTabList.get(i))));
        }

        actionBarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(actionBarAdapter, this);

        mLibraryFragment = (LibraryFragment) mFragmentManager
                .findFragmentByTag(FRAGMENT_TAG_LIBRARY);
        if (mLibraryFragment == null) {
            mLibraryFragment = new LibraryFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.library_root_frame, mLibraryFragment, FRAGMENT_TAG_LIBRARY);
            ft.commit();
        }

        mOutputsFragment = (OutputsFragment) mFragmentManager
                .findFragmentByTag(FRAGMENT_TAG_OUTPUTS);
        if (mOutputsFragment == null) {
            mOutputsFragment = new OutputsFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.outputs_root_frame, mOutputsFragment, FRAGMENT_TAG_OUTPUTS);
            ft.commit();
        }

        // Setup the pager
        if (mNowPlayingPager != null) {
            mNowPlayingPager.setAdapter(new MainMenuPagerAdapter());
            mNowPlayingPager.setOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageSelected(final int position) {
                            refreshQueueIndicator(position != 0);
                        }
                    });
        }

        if (savedInstanceState != null) {
            switchMode((DisplayMode) savedInstanceState.getSerializable(EXTRA_DISPLAY_MODE));
        }

        final View nowPlayingSmallFragment = findViewById(R.id.now_playing_small_fragment);
        mQueueFragment = (QueueFragment) mFragmentManager.findFragmentById(R.id.playlist_fragment);

        mHeaderPlayQueue = (ImageButton) findViewById(R.id.header_show_queue);
        final ImageButton headerOverflowMenu = (ImageButton) findViewById(
                R.id.header_overflow_menu);
        mHeaderTitle = (TextView) findViewById(R.id.header_title);
        mHeaderDragView = findViewById(R.id.header_dragview);
        if (mHeaderPlayQueue != null) {
            mHeaderPlayQueue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (mNowPlayingPager != null && mSlidingLayout != null
                            && mSlidingLayout.isPanelExpanded()) {
                        if (mNowPlayingPager.getCurrentItem() == 0) {
                            showQueue();
                        } else {
                            mNowPlayingPager.setCurrentItem(0, true);
                            refreshQueueIndicator(false);
                        }
                    }
                }
            });
        }
        if (headerOverflowMenu != null) {
            mHeaderOverflowPopupMenu = new PopupMenu(this, headerOverflowMenu);
            mHeaderOverflowPopupMenu.getMenuInflater().inflate(R.menu.mpd_mainmenu,
                    mHeaderOverflowPopupMenu.getMenu());
            mHeaderOverflowPopupMenu.getMenuInflater().inflate(R.menu.mpd_playlistmenu,
                    mHeaderOverflowPopupMenu.getMenu());
            mHeaderOverflowPopupMenu.getMenu().removeItem(R.id.PLM_EditPL);
            mHeaderOverflowPopupMenu.setOnMenuItemClickListener(this);

            headerOverflowMenu.setOnTouchListener(
                    PopupMenuCompat.getDragToOpenListener(mHeaderOverflowPopupMenu));

            headerOverflowMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (mSlidingLayout != null && mSlidingLayout.isPanelExpanded()) {
                        prepareNowPlayingMenu(mHeaderOverflowPopupMenu.getMenu());
                        mHeaderOverflowPopupMenu.show();
                    }
                }
            });
        }
        // Sliding panel
        mSlidingLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingLayout.setEnableDragViewTouchEvents(true);
        mSlidingLayout.setPanelHeight(
                (int) getResources().getDimension(R.dimen.nowplaying_small_fragment_height));
        final SlidingUpPanelLayout.PanelSlideListener panelSlideListener =
                new SlidingUpPanelLayout.PanelSlideListener() {
                    @Override
                    public void onPanelAnchored(final View panel) {
                    }

                    @Override
                    public void onPanelCollapsed(final View panel) {
                        nowPlayingSmallFragment.setVisibility(View.VISIBLE);
                        nowPlayingSmallFragment.setAlpha(1);
                        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    }

                    @Override
                    public void onPanelExpanded(final View panel) {
                        nowPlayingSmallFragment.setVisibility(View.GONE);
                        nowPlayingSmallFragment.setAlpha(1);
                        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    }

                    @Override
                    public void onPanelHidden(final View view) {
                    }

                    @Override
                    public void onPanelSlide(final View panel, final float slideOffset) {
                        if (slideOffset > 0.3) {
                            if (getActionBar().isShowing()) {
                                getActionBar().hide();
                            }
                        } else {
                            if (!getActionBar().isShowing()) {
                                getActionBar().show();
                            }
                        }
                        nowPlayingSmallFragment
                                .setVisibility(slideOffset < 1 ? View.VISIBLE : View.GONE);
                        nowPlayingSmallFragment.setAlpha(1 - slideOffset);
                    }
                };
        mSlidingLayout.setPanelSlideListener(panelSlideListener);
        // Ensure that the view state is consistent (otherwise we end up with a view mess)
        // The sliding layout should take care of it itself but does not
        if (savedInstanceState != null) {
            if ((Boolean) savedInstanceState.getSerializable(EXTRA_SLIDING_PANEL_EXPANDED)) {
                mSlidingLayout.expandPanel();
                panelSlideListener.onPanelSlide(mSlidingLayout, 1);
                panelSlideListener.onPanelExpanded(mSlidingLayout);
            } else {
                mSlidingLayout.collapsePanel();
                panelSlideListener.onPanelSlide(mSlidingLayout, 0);
                panelSlideListener.onPanelCollapsed(mSlidingLayout);
            }
        }
        refreshQueueIndicator(false);

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
    public final boolean onKeyUp(final int keyCode, final KeyEvent event) {
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
        boolean result = true;
        final boolean itemHandled = mDrawerToggle.onOptionsItemSelected(item) ||
                (mQueueFragment != null && mQueueFragment.onOptionsItemSelected(item));

        // Handle item selection
        if (!itemHandled) {
            switch (item.getItemId()) {
                case R.id.menu_search:
                    onSearchRequested();
                    break;
                case CONNECT:
                    mApp.connect();
                    break;
                case R.id.GMM_Stream:
                    if (mApp.isStreamActive()) {
                        mApp.stopStreaming();
                    } else if (mApp.oMPDAsyncHelper.oMPD.isConnected()) {
                        mApp.startStreaming();
                    }
                    break;
                case R.id.GMM_bonjour:
                    startActivity(new Intent(this, ServerListActivity.class));
                    break;
                case R.id.GMM_Consume:
                    MPDControl.run(MPDControl.ACTION_CONSUME);
                    break;
                case R.id.GMM_Single:
                    MPDControl.run(MPDControl.ACTION_SINGLE);
                    break;
                case R.id.GMM_ShowNotification:
                    if (mApp.isNotificationActive()) {
                        mApp.stopNotification();
                    } else {
                        mApp.startNotification();
                        mApp.setPersistentOverride(false);
                    }
                    break;
                default:
                    result = super.onOptionsItemSelected(item);
                    break;
            }
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
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        // Reminder : never disable buttons that are shown as actionbar actions
        // here
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
        outState.putSerializable(EXTRA_SLIDING_PANEL_EXPANDED, mSlidingLayout.isPanelExpanded());
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
    public void pageChanged(final int position) {
        final ActionBar actionBar = getActionBar();
        if (mCurrentDisplayMode == DisplayMode.MODE_LIBRARY
                && actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
            actionBar.setSelectedNavigationItem(position);
        }
    }

    public void prepareNowPlayingMenu(final Menu menu) {
        final boolean isStreaming = mApp.isStreamActive();
        final MPD mpd = mApp.oMPDAsyncHelper.oMPD;
        final MPDStatus mpdStatus = mpd.getStatus();

        // Reminder : never disable buttons that are shown as actionbar actions
        // here
        if (!mpd.isConnected()) {
            if (menu.findItem(CONNECT) == null) {
                menu.add(0, CONNECT, 0, R.string.connect);
            }
        } else {
            if (menu.findItem(CONNECT) != null) {
                menu.removeItem(CONNECT);
            }
        }

        final MenuItem saveItem = menu.findItem(R.id.PLM_Save);
        final MenuItem clearItem = menu.findItem(R.id.PLM_Clear);
        if (!mIsDualPaneMode && mNowPlayingPager != null
                && mNowPlayingPager.getCurrentItem() == 0) {
            saveItem.setVisible(false);
            clearItem.setVisible(false);
        } else {
            saveItem.setVisible(true);
            clearItem.setVisible(true);
        }

        /** If in streamingMode or persistentNotification don't allow a checkbox in the menu. */
        final MenuItem notificationItem = menu.findItem(R.id.GMM_ShowNotification);
        if (notificationItem != null) {
            if (isStreaming || mApp.isNotificationPersistent()) {
                notificationItem.setVisible(false);
            } else {
                notificationItem.setVisible(true);
            }

            setMenuChecked(notificationItem, mApp.isNotificationActive());
        }

        setMenuChecked(menu.findItem(R.id.GMM_Stream), isStreaming);
        setMenuChecked(menu.findItem(R.id.GMM_Single), mpdStatus.isSingle());
        setMenuChecked(menu.findItem(R.id.GMM_Consume), mpdStatus.isConsume());
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
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        switch (mCurrentDisplayMode) {
            case MODE_OUTPUTS:
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                if (mCurrentDisplayMode == DisplayMode.MODE_OUTPUTS) {
                    mTextView.setText(R.string.outputs);
                }
                break;
            case MODE_LIBRARY:
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
                break;
        }
    }

    public void refreshQueueIndicator(final boolean queueShown) {
        if (mHeaderPlayQueue != null) {
            mHeaderPlayQueue.setAlpha((float) (queueShown ? 1 : 0.5));
        }
        if (mHeaderTitle != null) {
            mHeaderTitle.setText(
                    queueShown && !mIsDualPaneMode ? R.string.playQueue : R.string.nowPlaying);
        }

        // Restrain the sliding panel sliding zone
        if (mSlidingLayout != null) {
            if (queueShown) {
                mSlidingLayout.setDragView(mHeaderDragView);
            } else {
                mSlidingLayout.setDragView(null);
                // Sliding layout made mHeaderDragView clickable, revert it
                mHeaderDragView.setClickable(false);
            }
        }
    }

    public void showQueue() {
        if (mSlidingLayout != null) {
            mSlidingLayout.expandPanel();
        }
        if (mNowPlayingPager != null) {
            mNowPlayingPager.setCurrentItem(1, true);
        }
        refreshQueueIndicator(true);
    }

    /** Swaps fragments in the main content view */
    public void switchMode(final DisplayMode newMode) {
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

    public static class DrawerItem {

        public final Action mAction;

        public final String mLabel;

        public DrawerItem(final String label, final Action action) {
            super();
            mLabel = label;
            mAction = action;
        }

        @Override
        public String toString() {
            return mLabel;
        }

        public enum Action {
            ACTION_LIBRARY,
            ACTION_OUTPUTS,
            ACTION_SETTINGS
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

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

    class MainMenuPagerAdapter extends PagerAdapter {

        @Override
        public void destroyItem(final ViewGroup container, final int position,
                final Object object) {
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {

            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.nowplaying_fragment;
                    break;
                case 1:
                    resId = R.id.playlist_fragment;
                    break;
            }
            return findViewById(resId);
        }

        @Override
        public boolean isViewFromObject(final View arg0, final Object arg1) {
            return arg0.equals(arg1);
        }
    }
}
