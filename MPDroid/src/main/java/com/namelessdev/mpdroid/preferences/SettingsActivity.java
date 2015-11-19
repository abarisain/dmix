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

package com.namelessdev.mpdroid.preferences;

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.status.StatusChangeListener;
import com.namelessdev.mpdroid.MPDActivity;
import com.namelessdev.mpdroid.R;

import android.os.Bundle;

public class SettingsActivity extends MPDActivity implements StatusChangeListener {

    private SettingsFragment mSettingsFragment;

    /**
     * Called upon connection.
     *
     * @param commandErrorCode If this number is non-zero, the number will correspond to a
     *                         {@link MPDException} error code. If this number is zero, the
     *                         connection MPD protocol commands were successful.
     */
    @Override
    public void connectionConnected(final int commandErrorCode) {
        super.connectionConnected(commandErrorCode);

        mSettingsFragment.onConnectionStateChanged();
    }

    /**
     * Called upon disconnection.
     *
     * @param reason The reason given for disconnection.
     */
    @Override
    public void connectionDisconnected(final String reason) {
        super.connectionDisconnected(reason);

        mSettingsFragment.onConnectionStateChanged();
    }

    @Override
    protected int getThemeResId() {
        final int themeResId;

        if (isLightThemeSelected()) {
            themeResId = R.style.AppTheme_Light;
        } else {
            themeResId = R.style.AppTheme;
        }

        return themeResId;
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {

    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        mSettingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mSettingsFragment).commit();
    }

    @Override
    public void onPause() {
        mApp.getMPD().getConnectionStatus().removeListener(this);
        mApp.removeStatusChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mApp.addStatusChangeListener(this);
        mApp.getMPD().getConnectionStatus().addListener(this);
    }

    /**
     * Called upon a change in the Output idle subsystem.
     */
    @Override
    public void outputsChanged() {
    }

    @Override
    public void playlistChanged(final int oldPlaylistVersion) {

    }

    @Override
    public void randomChanged() {

    }

    @Override
    public void repeatChanged() {

    }

    @Override
    public void stateChanged(final int oldState) {

    }

    @Override
    public void stickerChanged() {

    }

    /**
     * Called when a stored playlist has been modified, renamed, created or deleted.
     */
    @Override
    public void storedPlaylistChanged() {
    }

    @Override
    public void trackChanged(final int oldTrack) {

    }

    @Override
    public void volumeChanged(final int oldVolume) {

    }
}
