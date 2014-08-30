/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.event.StatusChangeListener;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity implements StatusChangeListener {

    private final MPDApplication app = MPDApplication.getInstance();

    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsFragment = new SettingsFragment();
        app.oMPDAsyncHelper.addStatusChangeListener(this);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        app.setActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        app.setActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
    }

    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) {
        settingsFragment.onConnectionStateChanged();
    }

    @Override
    public void libraryStateChanged(boolean updating, boolean dbChanged) {

    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {

    }

    @Override
    public void randomChanged(boolean random) {

    }

    @Override
    public void repeatChanged(boolean repeating) {

    }

    @Override
    public void stateChanged(MPDStatus mpdStatus, int oldState) {

    }

    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) {

    }

    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {

    }
}
