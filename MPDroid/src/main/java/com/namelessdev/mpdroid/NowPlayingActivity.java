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

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.namelessdev.mpdroid.helpers.MPDControl;
import com.namelessdev.mpdroid.helpers.QueueControl;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StyleRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


public class NowPlayingActivity extends MPDActivity {

    private static final String TAG = "NowPlayingActivity";

    private boolean mIsDualPaneMode;

    private ViewPager mNowPlayingPager;

    /**
     * This method returns the current theme resource ID.
     *
     * @return The current theme resource ID.
     */
    @StyleRes
    @Override
    protected int getThemeResId() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final int themeID;

        if (settings.getBoolean("smallSeekbars", true)) {
            if (isLightThemeSelected()) {
                themeID = R.style.AppTheme_Light_SmallSeekBars;
            } else {
                themeID = R.style.AppTheme_SmallSeekBars;
            }
        } else {
            themeID = super.getThemeResId();
        }

        return themeID;
    }

    private ViewPager initializeNowPlayingPager() {
        final ViewPager nowPlayingPager = (ViewPager) findViewById(R.id.pager);
        if (nowPlayingPager != null) {
            nowPlayingPager.setAdapter(new NowPlayingPagerAdapter(this));
            nowPlayingPager.addOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageSelected(final int position) {
                            refreshQueueIndicator(position != 0);
                        }

                        private void refreshQueueIndicator(final boolean queueShown) {
                            if (queueShown && !mIsDualPaneMode) {
                                setTitle(R.string.playQueue);
                            } else {
                                setTitle(R.string.nowPlaying);
                            }
                        }
                    });
        }

        return nowPlayingPager;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        /*final Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }*/
        super.onCreate(savedInstanceState);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Transition ts = new Fade();  //Slide(); //Explode();
            window.setEnterTransition(ts);
            window.setExitTransition(ts);
        }*/

        if (mApp.isTabletUiEnabled()) {
            setContentView(R.layout.activity_now_playing_tablet);
        } else {
            setContentView(R.layout.activity_now_playing);
        }

        mIsDualPaneMode = findViewById(R.id.nowplaying_dual_pane) != null;
        mNowPlayingPager = initializeNowPlayingPager();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();

        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.mpd_now_playing_menu, menu);
        inflater.inflate(R.menu.mpd_queuemenu, menu);
        menu.removeItem(R.id.PLM_EditPL);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean itemHandled = true;

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_search:
                onSearchRequested();
                break;
            case R.id.GMM_Stream:
                if (mApp.isStreamActive()) {
                    mApp.stopStreaming();
                } else if (mApp.getMPD().isConnected()) {
                    mApp.startStreaming();
                }
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
            case R.id.PLM_Clear:
                QueueControl.run(QueueControl.CLEAR);
                Tools.notifyUser(R.string.playlistCleared);
                break;
            /**
             * TODO: Better reimplementation of PLM_Save
             * (QueueFragment:onOptionsItemSelected(final MenuItem item))
             */
            case R.id.PLM_Save:
                final Fragment fragment = getSupportFragmentManager()
                        .findFragmentById(R.id.queue_fragment);

                if (fragment == null) {
                    Log.e(TAG, "Failed to get a fragment to save the playlist.");
                } else {
                    fragment.onOptionsItemSelected(item);
                }
                break;
            default:
                itemHandled = super.onOptionsItemSelected(item);
                break;
        }

        return itemHandled;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final boolean isStreaming = mApp.isStreamActive();
        final MPD mpd = mApp.getMPD();
        final MPDStatus mpdStatus = mpd.getStatus();

        final MenuItem saveItem = menu.findItem(R.id.PLM_Save);
        final MenuItem clearItem = menu.findItem(R.id.PLM_Clear);
        final boolean isQueueVisible = !mIsDualPaneMode && mNowPlayingPager != null
                && mNowPlayingPager.getCurrentItem() == 1;

        saveItem.setVisible(isQueueVisible);
        clearItem.setVisible(isQueueVisible);

        /** If in streamingMode or persistentNotification don't allow a checkbox in the menu. */
        final MenuItem notificationItem = menu.findItem(R.id.GMM_ShowNotification);
        if (notificationItem != null) {
            if (isStreaming || mApp.isNotificationPersistent()) {
                notificationItem.setVisible(false);
            } else {
                notificationItem.setVisible(true);
            }

            notificationItem.setChecked(mApp.isNotificationActive());
        }

        menu.findItem(R.id.GMM_Stream).setChecked(isStreaming);
        menu.findItem(R.id.GMM_Single).setChecked(mpdStatus.isSingle());
        menu.findItem(R.id.GMM_Consume).setChecked(mpdStatus.isConsume());

        return true;
    }

    public void showQueue() {
    }

    private static final class NowPlayingPagerAdapter extends PagerAdapter {

        private final Activity mActivity;

        private NowPlayingPagerAdapter(final Activity activity) {
            super();

            mActivity = activity;
        }

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
            final int resId;

            switch (position) {
                case 0:
                    resId = R.id.nowplaying_fragment;
                    break;
                case 1:
                    resId = R.id.queue_fragment;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown fragment requested.");
            }

            return mActivity.findViewById(resId);
        }

        @Override
        public boolean isViewFromObject(final View view, final Object object) {
            return view.equals(object);
        }
    }
}
