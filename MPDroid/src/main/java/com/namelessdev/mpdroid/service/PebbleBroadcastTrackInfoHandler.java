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

package com.namelessdev.mpdroid.service;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.UpdateTrackInfo;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.item.Music;

import android.content.Intent;
import android.util.Log;

/**
 * A class to broadcast the com.getpebble.action.NOW_PLAYING Intent on Track change
 */
public class PebbleBroadcastTrackInfoHandler implements StatusChangeListener,
        UpdateTrackInfo.FullTrackInfoUpdate {
    private final MPDApplication mApp = MPDApplication.getInstance();
    private static final String TAG = "PebbleBroadcastTrackInfoHandler";

    private UpdateTrackInfo mUpdateTrackInfo = null;

    public PebbleBroadcastTrackInfoHandler(){
        Log.d(TAG, "initialized");
        mUpdateTrackInfo = new UpdateTrackInfo();
        mUpdateTrackInfo.addCallback(this);
        mApp.oMPDAsyncHelper.addStatusChangeListener(this);
    }

    @Override
    protected void finalize() throws Throwable {
        mApp.oMPDAsyncHelper.removeStatusChangeListener(this);
        mUpdateTrackInfo.removeCallback(this);
    }

    /*
     * UpdateTrackInfo.FullTrackInfoUpdate
     */

    @Override
    public void onCoverUpdate(final AlbumInfo albumInfo) {

    }

    @Override
    public void onTrackInfoUpdate(final Music updatedSong, final float trackRating,
            final CharSequence album,
            final CharSequence artist, final CharSequence date, final CharSequence title) {
        final Intent i = new Intent("com.getpebble.action.NOW_PLAYING");
        i.putExtra("artist", artist);
        i.putExtra("album", album);
        i.putExtra("track", title);
        Log.d(TAG, "broadcasting intent to pebble");
        MPDApplication.getInstance().sendBroadcast(i);
    }


    /*
     * StatusChangeListener
     */

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {

    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {

    }


    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {

    }

    @Override
    public void randomChanged(final boolean random) {

    }

    @Override
    public void repeatChanged(final boolean repeating) {

    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {
    }

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {

    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        Log.d(TAG, "trackChanged");
        mUpdateTrackInfo.refresh(mpdStatus);
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {

    }


}
