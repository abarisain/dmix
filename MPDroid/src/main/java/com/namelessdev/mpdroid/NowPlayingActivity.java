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

package com.namelessdev.mpdroid;

import android.support.v4.view.ViewPager;

import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.anpmech.mpd.MPD;
import com.anpmech.mpd.subsystem.status.MPDStatus;
import com.namelessdev.mpdroid.helpers.MPDControl;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.transition.Fade;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


public class NowPlayingActivity extends MPDroidActivities.MPDroidActivity {

    private boolean mIsDualPaneMode;

    private ViewPager mNowPlayingPager;

    public void showQueue() {
        // TODO : Implement stub
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS);
        super.onCreate(savedInstanceState);

        Transition ts = new Fade();  //Slide(); //Explode();
        getWindow().setEnterTransition(ts);
        getWindow().setExitTransition(ts);

        setContentView(R.layout.activity_now_playing);

        mIsDualPaneMode = findViewById(R.id.nowplaying_dual_pane) != null;
        mNowPlayingPager = initializeNowPlayingPager();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mpd_mainmenu, menu);
        getMenuInflater().inflate(R.menu.mpd_queuemenu, menu);
        menu.removeItem(R.id.PLM_EditPL);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean result = true;
        //final boolean itemHandled = mQueueFragment != null && mQueueFragment.onOptionsItemSelected(item);

        final boolean itemHandled = false;

        // Handle item selection
        if (!itemHandled) {
            switch (item.getItemId()) {
                case R.id.menu_search:
                    onSearchRequested();
                    break;
                case R.id.GMM_Stream:
                    if (mApp.isStreamActive()) {
                        mApp.stopStreaming();
                    } else if (mApp.oMPDAsyncHelper.oMPD.isConnected()) {
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
                default:
                    result = super.onOptionsItemSelected(item);
                    break;
            }
        }
        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final boolean isStreaming = mApp.isStreamActive();
        final MPD mpd = mApp.oMPDAsyncHelper.oMPD;
        final MPDStatus mpdStatus = mpd.getStatus();


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

            notificationItem.setChecked(mApp.isNotificationActive());
        }

        menu.findItem(R.id.GMM_Stream).setChecked(isStreaming);
        menu.findItem(R.id.GMM_Single).setChecked(mpdStatus.isSingle());
        menu.findItem(R.id.GMM_Consume).setChecked(mpdStatus.isConsume());

        return true;
    }

    private ViewPager initializeNowPlayingPager() {
        final ViewPager nowPlayingPager = (ViewPager) findViewById(R.id.pager);
        if (nowPlayingPager != null) {
            nowPlayingPager.setAdapter(new MainMenuPagerAdapter());
            nowPlayingPager.setOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
                        @Override
                        public void onPageSelected(final int position) {
                            refreshQueueIndicator(position != 0);
                        }
                    });
        }

        return nowPlayingPager;
    }


    private void refreshQueueIndicator(final boolean queueShown) {
        /*if (mHeaderPlayQueue != null) {
            if (queueShown) {
                mHeaderPlayQueue.setAlpha(1.0f);
            } else {
                mHeaderPlayQueue.setAlpha(0.5f);
            }
        }*/

        if (queueShown && !mIsDualPaneMode) {
            setTitle(R.string.playQueue);
        } else {
            setTitle(R.string.nowPlaying);
        }
    }

    private class MainMenuPagerAdapter extends PagerAdapter {

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
                    resId = R.id.queue_fragment;
                    break;
                default:
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
