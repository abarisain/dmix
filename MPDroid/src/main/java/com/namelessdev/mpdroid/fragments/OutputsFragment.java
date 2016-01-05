/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

package com.namelessdev.mpdroid.fragments;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.connection.MPDConnectionListener;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.subsystem.AudioOutput;
import com.anpmech.mpd.subsystem.status.StatusChangeListener;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.MainMenuActivity;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment is used to display {@link AudioOutput} information and status.
 */
public class OutputsFragment extends ListFragment implements AdapterView.OnItemClickListener,
        MPDAsyncHelper.AsyncExecListener, MPDConnectionListener, StatusChangeListener {

    /**
     * The class log identifier.
     */
    private static final String TAG = "OutputsFragment";

    /**
     * The Application instance.
     */
    private final MPDApplication mApp = MPDApplication.getInstance();

    /**
     * The Outputs cache.
     */
    private final List<AudioOutput> mOutputs = new ArrayList<>();

    /**
     * This method is run after a AsyncExec Runnable.
     *
     * @param token The token used to run the Runnable.
     */
    @Override
    public void asyncComplete(final CharSequence token) {
        if (AudioOutput.EXTRA.equals(token)) {
            try {
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
                final ListView list = getListView();
                for (int i = 0; i < mOutputs.size(); i++) {
                    list.setItemChecked(i, mOutputs.get(i).isEnabled());
                    list.setEnabled(checkOutputCommands());
                }
            } catch (final IllegalStateException e) {
                Log.e(TAG, "Illegal Activity state while trying to refresh output list", e);
            }
        }
    }

    /**
     * This method checks the AudioOutput commands or availability.
     *
     * @return True if the output commands are available, false otherwise.
     */
    private boolean checkOutputCommands() {
        return mApp.getMPD().isCommandAvailable(MPDCommand.MPD_CMD_OUTPUTENABLE) &&
                mApp.getMPD().isCommandAvailable(MPDCommand.MPD_CMD_OUTPUTDISABLE);
    }

    /**
     * Called upon connection.
     *
     * @param commandErrorCode If this number is non-zero, the number will correspond to a
     *                         {@link MPDException} error code. If this number is zero, the
     *                         connection MPD protocol commands were successful.
     */
    @Override
    public void connectionConnected(final int commandErrorCode) {
        refreshOutputs();
    }

    /**
     * Called when connecting.
     *
     * <p>This implies that we've disconnected. This callback is intended to be transient. Status
     * change from connected to connecting may happen, but if a connection is not established, with
     * a connected callback, the disconnection status callback should be called.</p>
     */
    @Override
    public void connectionConnecting() {
    }

    /**
     * Called upon disconnection.
     *
     * @param reason The reason given for disconnection.
     */
    @Override
    public void connectionDisconnected(final String reason) {
    }

    /**
     * Called when the MPD server update database starts and stops.
     *
     * @param updating  true when updating, false when not updating.
     * @param dbChanged After update, if the database has changed, this will be true else false.
     */
    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        final ListView listView = getListView();
        final ListAdapter arrayAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_list_item_multiple_choice, mOutputs);
        setListAdapter(arrayAdapter);

        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(this);

        // Not needed since MainMenuActivity will take care of telling us to refresh
        if (!(activity instanceof MainMenuActivity) && mApp.getMPD().isConnected()) {
            refreshOutputs();
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        if (mOutputs.isEmpty() || position >= mOutputs.size()) {
            Log.e(TAG, "Failed to modify out of sync outputs.");
        } else {
            final MPD mpd = mApp.getMPD();
            final AudioOutput output = mOutputs.get(position);

            try {
                if (getListView().isItemChecked(position)) {
                    mpd.enableOutput(output.getId());
                } else {
                    mpd.disableOutput(output.getId());
                }
            } catch (final IOException | IllegalStateException | MPDException e) {
                Log.e(TAG, "Failed to modify output.", e);
            }
        }
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        mApp.removeStatusChangeListener(this);

        super.onPause();
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();

        mApp.addStatusChangeListener(this);
    }

    /**
     * Called upon a change in the Output idle subsystem.
     */
    @Override
    public void outputsChanged() {
        refreshOutputs();
    }

    /**
     * Called when playlist changes on MPD server.
     *
     * @param oldPlaylistVersion old playlist version.
     */
    @Override
    public void playlistChanged(final int oldPlaylistVersion) {
    }

    /**
     * Called when MPD server random feature changes state.
     */
    @Override
    public void randomChanged() {
    }

    /**
     * This method refreshes the Outputs UI and cache.
     */
    public void refreshOutputs() {
        final Runnable updateAudioOutputs = new UpdateAudioOutputs(mApp.getMPD(), mOutputs);

        mApp.getAsyncHelper().execAsync(this, AudioOutput.EXTRA, updateAudioOutputs);

    }

    /**
     * Called when MPD server repeat feature changes state.
     */
    @Override
    public void repeatChanged() {
    }

    /**
     * Called when MPD state changes on server.
     *
     * @param oldState previous state.
     */
    @Override
    public void stateChanged(final int oldState) {
    }

    /**
     * Called when any sticker of any track has been changed on server.
     */
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
    public String toString() {
        return AudioOutput.EXTRA;
    }

    /**
     * Called when playing track is changed on server.
     *
     * @param oldTrack track number before event.
     */
    @Override
    public void trackChanged(final int oldTrack) {
    }

    /**
     * Called when volume changes on MPD server.
     *
     * @param oldVolume volume before event
     */
    @Override
    public void volumeChanged(final int oldVolume) {
    }

    /**
     * This class is used to update AudioOutputs.
     */
    private static final class UpdateAudioOutputs implements Runnable {

        /**
         * The MPD instance used to update the AudioOutputs.
         */
        private final MPD mMPD;

        /**
         * The list of AudioOutputs to modify.
         */
        private final List<AudioOutput> mOutputs;

        /**
         * Sole constructor.
         *
         * @param mpd     The MPD instance to use to update the AudioOutputs.
         * @param outputs The list of AudioOutputs to modify.
         */
        private UpdateAudioOutputs(final MPD mpd, final List<AudioOutput> outputs) {
            super();

            mMPD = mpd;
            mOutputs = outputs;
        }

        @Override
        public void run() {
            try {
                mOutputs.clear();
                mOutputs.addAll(mMPD.getOutputs());
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to list outputs.", e);
            }
        }
    }
}
