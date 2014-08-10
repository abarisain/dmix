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
import org.a0z.mpd.exception.MPDServerException;

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
import android.util.Log;
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
        ILibraryFragmentActivity,
        ILibraryTabActivity, OnBackStackChangedListener, PopupMenu.OnMenuItemClickListener {

    public static enum DisplayMode {
        MODE_LIBRARY,
        MODE_OUTPUTS
    }

    public static class DrawerItem {

        public static enum Action {
            ACTION_LIBRARY,
            ACTION_OUTPUTS,
            ACTION_SETTINGS
        }

        public Action action;

        public String label;

        public DrawerItem(String label, Action action) {
            this.label = label;
            this.action = action;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mDrawerLayout.closeDrawer(mDrawerList);

            switch (((DrawerItem) parent.getItemAtPosition(position)).action) {
                default:
                case ACTION_LIBRARY:
                    // If we are already on the library, pop the whole stack.
                    // Acts like an "up" button
                    if (currentDisplayMode == DisplayMode.MODE_LIBRARY) {
                        final int fmStackCount = fragmentManager.getBackStackEntryCount();
                        if (fmStackCount > 0) {
                            fragmentManager.popBackStack(null,
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }
                    }
                    switchMode(DisplayMode.MODE_LIBRARY);
                    break;
                case ACTION_OUTPUTS:
                    switchMode(DisplayMode.MODE_OUTPUTS);
                    break;
                case ACTION_SETTINGS:
                    mDrawerList.setItemChecked(oldDrawerPosition, true);
                    final Intent i = new Intent(MainMenuActivity.this, SettingsActivity.class);
                    startActivityForResult(i, SETTINGS);
                    break;
            }
            oldDrawerPosition = position;
        }
    }

    class MainMenuPagerAdapter extends PagerAdapter {

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }

        @Override
        public int getCount() {
            return 2;
        }

        public Object instantiateItem(View collection, int position) {

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
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
    }

    public static final int PLAYLIST = 1;

    public static final int ARTISTS = 2;

    public static final int SETTINGS = 5;

    public static final int STREAM = 6;

    public static final int LIBRARY = 7;

    public static final int CONNECT = 8;

    private static final String FRAGMENT_TAG_LIBRARY = "library";

    private static final String FRAGMENT_TAG_OUTPUTS = "outputs";

    private static final String EXTRA_DISPLAY_MODE = "displaymode";

    private static final String EXTRA_SLIDING_PANEL_EXPANDED = "slidingpanelexpanded";

    private int backPressExitCount;

    private Handler exitCounterReset;

    private boolean isDualPaneMode;

    private View nowPlayingDualPane;

    private ViewPager nowPlayingPager;

    private View libraryRootFrame;

    private View outputsRootFrame;

    private OutputsFragment outputsFragment;

    private TextView titleView;

    private List<DrawerItem> mDrawerItems;

    private DrawerLayout mDrawerLayout;

    private ListView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;

    private SlidingUpPanelLayout mSlidingLayout;

    private ImageButton mHeaderPlayQueue;

    private ImageButton mHeaderOverflowMenu;

    private View mHeaderDragView;

    private PopupMenu mHeaderOverflowPopupMenu;

    private TextView mHeaderTitle;

    private int oldDrawerPosition;

    private LibraryFragment libraryFragment;

    private QueueFragment mQueueFragment;

    private static final String TAG = "com.namelessdev.mpdroid.MainMenuActivity";

    private FragmentManager fragmentManager;

    private ArrayList<String> mTabList;

    private DisplayMode currentDisplayMode;

    private static final boolean DEBUG = false;

    @Override
    public ArrayList<String> getTabList() {
        return mTabList;
    }

    /**
     * Called when Back button is pressed, displays message to user indicating
     * the if back button is pressed again the application will exit. We keep a
     * count of how many time back button is pressed within 5 seconds. If the
     * count is greater than 1 then call system.exit(0) Starts a post delay
     * handler to reset the back press count to zero after 5 seconds
     */
    @Override
    public void onBackPressed() {
        if (mSlidingLayout.isPanelExpanded()) {
            mSlidingLayout.collapsePanel();
            return;
        }

        if (currentDisplayMode == DisplayMode.MODE_LIBRARY) {
            final int fmStackCount = fragmentManager.getBackStackEntryCount();
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
        if (exitConfirmationRequired && backPressExitCount < 1) {
            Tools.notifyUser(R.string.backpressToQuit);
            backPressExitCount += 1;
            exitCounterReset.postDelayed(new Runnable() {
                @Override
                public void run() {
                    backPressExitCount = 0;
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app.setupServiceBinder();
        setContentView(app.isTabletUiEnabled() ? R.layout.main_activity_nagvigation_tablet
                : R.layout.main_activity_nagvigation);

        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        titleView = (TextView) inflater.inflate(R.layout.actionbar_title, null);
        titleView.setFocusable(true);
        titleView.setFocusableInTouchMode(true);
        titleView.setSelected(true);
        titleView.requestFocus();

        nowPlayingDualPane = findViewById(R.id.nowplaying_dual_pane);
        nowPlayingPager = (ViewPager) findViewById(R.id.pager);
        libraryRootFrame = findViewById(R.id.library_root_frame);
        outputsRootFrame = findViewById(R.id.outputs_root_frame);

        isDualPaneMode = (nowPlayingDualPane != null);
        switchMode(DisplayMode.MODE_LIBRARY);

        exitCounterReset = new Handler();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setCustomView(titleView);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);

        mDrawerItems = new ArrayList<DrawerItem>();
        mDrawerItems.add(new DrawerItem(getString(R.string.libraryTabActivity),
                DrawerItem.Action.ACTION_LIBRARY));

        mDrawerItems.add(new DrawerItem(getString(R.string.outputs),
                DrawerItem.Action.ACTION_OUTPUTS));

        mDrawerItems.add(new DrawerItem(getString(R.string.settings),
                DrawerItem.Action.ACTION_SETTINGS));

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerToggle = new ActionBarDrawerToggle(
                this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                app.isLightThemeSelected() ? R.drawable.ic_drawer_light : R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description */
                R.string.drawer_close /* "close drawer" description */
        ) {

            /**
             * Called when a drawer has settled in a completely closed
             * state.
             */
            public void onDrawerClosed(View view) {
                refreshActionBarTitle();
            }

            /**
             * Called when a drawer has settled in a completely open
             * state.
             */
            public void onDrawerOpened(View drawerView) {
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                actionBar.setDisplayShowCustomEnabled(true);
                titleView.setText(R.string.app_name);
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<DrawerItem>(this,
                R.layout.drawer_list_item, mDrawerItems));
        oldDrawerPosition = 0;
        mDrawerList.setItemChecked(oldDrawerPosition, true);
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        /*
         * Setup the library tab
         */
        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        // Get the list of the currently visible tabs
        mTabList = LibraryTabsUtil.getCurrentLibraryTabs();

        ArrayAdapter<CharSequence> actionBarAdapter = new ArrayAdapter<CharSequence>(
                actionBar.getThemedContext(),
                android.R.layout.simple_spinner_item);
        for (int i = 0; i < mTabList.size(); i++) {
            actionBarAdapter.add(getText(LibraryTabsUtil.getTabTitleResId(mTabList.get(i))));
        }

        actionBarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(actionBarAdapter, this);

        libraryFragment = (LibraryFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_LIBRARY);
        if (libraryFragment == null) {
            libraryFragment = new LibraryFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.library_root_frame, libraryFragment, FRAGMENT_TAG_LIBRARY);
            ft.commit();
        }

        outputsFragment = (OutputsFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_OUTPUTS);
        if (outputsFragment == null) {
            outputsFragment = new OutputsFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.outputs_root_frame, outputsFragment, FRAGMENT_TAG_OUTPUTS);
            ft.commit();
        }

        // Setup the pager
        if (nowPlayingPager != null) {
            nowPlayingPager.setAdapter(new MainMenuPagerAdapter());
            nowPlayingPager.setOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageSelected(int position) {
                            refreshQueueIndicator(position != 0);
                        }
                    });
        }

        if (savedInstanceState != null) {
            switchMode((DisplayMode) savedInstanceState.getSerializable(EXTRA_DISPLAY_MODE));
        }

        final View nowPlayingSmallFragment = findViewById(R.id.now_playing_small_fragment);
        mQueueFragment = (QueueFragment) fragmentManager.findFragmentById(R.id.playlist_fragment);

        mHeaderPlayQueue = (ImageButton) findViewById(R.id.header_show_queue);
        mHeaderOverflowMenu = (ImageButton) findViewById(R.id.header_overflow_menu);
        mHeaderTitle = (TextView) findViewById(R.id.header_title);
        mHeaderDragView = findViewById(R.id.header_dragview);
        if (mHeaderPlayQueue != null) {
            mHeaderPlayQueue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (nowPlayingPager != null && mSlidingLayout != null
                            && mSlidingLayout.isPanelExpanded()) {
                        if (nowPlayingPager.getCurrentItem() == 0) {
                            showQueue();
                        } else {
                            nowPlayingPager.setCurrentItem(0, true);
                            refreshQueueIndicator(false);
                        }
                    }
                }
            });
        }
        if (mHeaderOverflowMenu != null) {
            mHeaderOverflowPopupMenu = new PopupMenu(this, mHeaderOverflowMenu);
            mHeaderOverflowPopupMenu.getMenuInflater().inflate(R.menu.mpd_mainmenu,
                    mHeaderOverflowPopupMenu.getMenu());
            mHeaderOverflowPopupMenu.getMenuInflater().inflate(R.menu.mpd_playlistmenu,
                    mHeaderOverflowPopupMenu.getMenu());
            mHeaderOverflowPopupMenu.getMenu().removeItem(R.id.PLM_EditPL);
            mHeaderOverflowPopupMenu.setOnMenuItemClickListener(this);

            mHeaderOverflowMenu.setOnTouchListener(PopupMenuCompat.getDragToOpenListener(mHeaderOverflowPopupMenu));

            mHeaderOverflowMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
        mSlidingLayout.setPanelHeight((int)getResources().getDimension(R.dimen.nowplaying_small_fragment_height));
        final SlidingUpPanelLayout.PanelSlideListener panelSlideListener =
                new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if (slideOffset > 0.3) {
                    if (getActionBar().isShowing()) {
                        getActionBar().hide();
                    }
                } else {
                    if (!getActionBar().isShowing()) {
                        getActionBar().show();
                    }
                }
                nowPlayingSmallFragment.setVisibility(slideOffset < 1 ? View.VISIBLE : View.GONE);
                nowPlayingSmallFragment.setAlpha(1-slideOffset);
            }

            @Override
            public void onPanelExpanded(View panel) {
                nowPlayingSmallFragment.setVisibility(View.GONE);
                nowPlayingSmallFragment.setAlpha(1);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            @Override
            public void onPanelCollapsed(View panel) {
                nowPlayingSmallFragment.setVisibility(View.VISIBLE);
                nowPlayingSmallFragment.setAlpha(1);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            @Override
            public void onPanelAnchored(View panel) {
            }

            @Override
            public void onPanelHidden(View view) {}
         };
        mSlidingLayout.setPanelSlideListener(panelSlideListener);
        // Ensure that the view state is consistent (otherwise we end up with a view mess)
        // The sliding layout should take care of it itself but does not
        if (savedInstanceState != null) {
            if ((Boolean) savedInstanceState.getSerializable(EXTRA_SLIDING_PANEL_EXPANDED)) {
                mSlidingLayout.expandPanel();
                panelSlideListener.onPanelSlide(mSlidingLayout, 0);
                panelSlideListener.onPanelExpanded(mSlidingLayout);
            } else {
                mSlidingLayout.collapsePanel();
                panelSlideListener.onPanelSlide(mSlidingLayout, 1);
                panelSlideListener.onPanelCollapsed(mSlidingLayout);
            }
        }
        refreshQueueIndicator(false);

        /** Reset the persistent override when the application is reset. */
        app.setPersistentOverride(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            result = !app.isLocalAudible();
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
                if (event.isTracking() && !event.isCanceled() && !app.isLocalAudible()) {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        MPDControl.run(MPDControl.ACTION_VOLUME_STEP_UP);
                    } else  {
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
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        libraryFragment.setCurrentItem(itemPosition, true);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
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
                    this.onSearchRequested();
                    break;
                case CONNECT:
                    app.connect();
                    break;
                case R.id.GMM_Stream:
                    if (app.isStreamActive()) {
                        app.stopStreaming();
                    } else if (app.oMPDAsyncHelper.oMPD.isConnected()) {
                        app.startStreaming();
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
                    if (app.isNotificationActive()) {
                        app.stopNotification();
                    } else {
                        app.startNotification();
                        app.setPersistentOverride(false);
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Reminder : never disable buttons that are shown as actionbar actions
        // here
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    public void prepareNowPlayingMenu(Menu menu) {
        final boolean isStreaming = app.isStreamActive();

        // Reminder : never disable buttons that are shown as actionbar actions
        // here
        final MPD mpd = app.oMPDAsyncHelper.oMPD;
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
        if (!isDualPaneMode && nowPlayingPager != null && nowPlayingPager.getCurrentItem() == 0) {
            saveItem.setVisible(false);
            clearItem.setVisible(false);
        } else {
            saveItem.setVisible(true);
            clearItem.setVisible(true);
        }

        /** If in streamingMode or persistentNotification don't allow a checkbox in the menu. */
        final MenuItem notificationItem = menu.findItem(R.id.GMM_ShowNotification);
        if(notificationItem != null) {
            if (isStreaming || app.isNotificationPersistent()) {
                notificationItem.setVisible(false);
            } else {
                notificationItem.setVisible(true);
            }
            
            setMenuChecked(notificationItem, app.isNotificationActive());
        }

        setMenuChecked(menu.findItem(R.id.GMM_Stream), isStreaming);

        MPDStatus mpdStatus = null;
        try {
            mpdStatus = mpd.getStatus();
        } catch (final MPDServerException e) {
            Log.e(TAG, "Failed to retrieve a status object", e);
        }

        if (mpdStatus != null) {
            setMenuChecked(menu.findItem(R.id.GMM_Single), mpdStatus.isSingle());
            setMenuChecked(menu.findItem(R.id.GMM_Consume), mpdStatus.isConsume());
        }
    }

    @Override
    protected void onPause() {
        if (DEBUG) {
            unregisterReceiver(MPDConnectionHandler.getInstance());
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        backPressExitCount = 0;
        if (DEBUG) {
            registerReceiver(MPDConnectionHandler.getInstance(),
                    new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_DISPLAY_MODE, currentDisplayMode);
        outState.putSerializable(EXTRA_SLIDING_PANEL_EXPANDED, mSlidingLayout.isPanelExpanded());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        app.setActivity(this);

        if (app.isNotificationPersistent()) {
            app.startNotification();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        app.unsetActivity(this);
    }

    @Override
    public void pageChanged(int position) {
        final ActionBar actionBar = getActionBar();
        if (currentDisplayMode == DisplayMode.MODE_LIBRARY
                && actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
            actionBar.setSelectedNavigationItem(position);
        }
    }

    @Override
    public void pushLibraryFragment(Fragment fragment, String label) {
        String title;
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
        switch (currentDisplayMode) {
            case MODE_OUTPUTS:
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                if (currentDisplayMode == DisplayMode.MODE_OUTPUTS) {
                    titleView.setText(R.string.outputs);
                }
                break;
            case MODE_LIBRARY:
                int fmStackCount = 0;
                if (fragmentManager != null) {
                    fmStackCount = fragmentManager.getBackStackEntryCount();
                }
                if (fmStackCount > 0) {
                    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                    titleView.setText(fragmentManager.getBackStackEntryAt(fmStackCount - 1)
                            .getBreadCrumbTitle());
                } else {
                    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    actionBar.setDisplayShowCustomEnabled(false);
                }
                break;
        }
    }

    private void setMenuChecked(MenuItem item, boolean checked) {
        item.setChecked(checked);
    }

    /** Swaps fragments in the main content view */
    public void switchMode(DisplayMode newMode) {
        currentDisplayMode = newMode;
        switch (currentDisplayMode) {
            case MODE_LIBRARY:
                libraryRootFrame.setVisibility(View.VISIBLE);
                outputsRootFrame.setVisibility(View.GONE);
                break;
            case MODE_OUTPUTS:
                libraryRootFrame.setVisibility(View.GONE);
                outputsRootFrame.setVisibility(View.VISIBLE);
                outputsFragment.refreshOutputs();
                break;
        }
        refreshActionBarTitle();
    }

    public void refreshQueueIndicator(boolean queueShown) {
        if (mHeaderPlayQueue != null) {
            mHeaderPlayQueue.setAlpha((float)(queueShown ? 1 : 0.5));
        }
        if (mHeaderTitle != null) {
            mHeaderTitle.setText(queueShown && !isDualPaneMode ? R.string.playQueue : R.string.nowPlaying);
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
        if (nowPlayingPager != null) {
            nowPlayingPager.setCurrentItem(1, true);
        }
        refreshQueueIndicator(true);
    }
}
