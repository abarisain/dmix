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

import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.helpers.CoverManager;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatistics;
import org.a0z.mpd.exception.MPDException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;

public class SettingsFragment extends PreferenceFragment {

    private static final String TAG = "SettingsFragment";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private CheckBoxPreference mAlbumArtLibrary;

    private EditTextPreference mAlbums;

    private EditTextPreference mArtists;

    private EditTextPreference mCacheUsage1;

    private EditTextPreference mCacheUsage2;

    private CheckBoxPreference mCheckBoxPreference;

    private Preference mCoverFilename;

    private Handler mHandler;

    private PreferenceScreen mInformationScreen;

    private CheckBoxPreference mLocalCoverCheckbox;

    private Preference mMusicPath;

    private boolean mPreferencesBound;

    private EditTextPreference mSongs;

    private EditTextPreference mVersion;

    public SettingsFragment() {
        super();
        mPreferencesBound = false;
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        refreshDynamicFields();
    }

    public void onConnectionStateChanged() {
        final MPD mpd = mApp.oMPDAsyncHelper.oMPD;
        final boolean isConnected = mpd.isConnected();

        mInformationScreen.setEnabled(isConnected);

        if (isConnected) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String versionText = mpd.getMpdVersion();
                    final MPDStatistics mpdStatistics = mpd.getStatistics();

                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            mVersion.setSummary(versionText);
                            mArtists.setSummary(String.valueOf(mpdStatistics.getArtists()));
                            mAlbums.setSummary(String.valueOf(mpdStatistics.getAlbums()));
                            mSongs.setSummary(String.valueOf(mpdStatistics.getSongs()));
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mHandler = new Handler();

        mInformationScreen = (PreferenceScreen) findPreference("informationScreen");

        if (!getResources().getBoolean(R.bool.isTablet)) {
            final PreferenceScreen interfaceCategory = (PreferenceScreen) findPreference(
                    "nowPlayingScreen");
            interfaceCategory.removePreference(findPreference("tabletUI"));
        }

        mVersion = (EditTextPreference) findPreference("version");
        mArtists = (EditTextPreference) findPreference("artists");
        mAlbums = (EditTextPreference) findPreference("albums");
        mSongs = (EditTextPreference) findPreference("songs");

        mLocalCoverCheckbox = (CheckBoxPreference) findPreference(
                "enableLocalCover");
        mMusicPath = findPreference("musicPath");
        mCoverFilename = findPreference("coverFileName");
        if (mLocalCoverCheckbox.isChecked()) {
            mMusicPath.setEnabled(true);
            mCoverFilename.setEnabled(true);
        } else {
            mMusicPath.setEnabled(false);
            mCoverFilename.setEnabled(false);
        }

        mCacheUsage1 = (EditTextPreference) findPreference("cacheUsage1");
        mCacheUsage2 = (EditTextPreference) findPreference("cacheUsage2");

        // Album art library listing requires cover art cache
        mCheckBoxPreference = (CheckBoxPreference) findPreference(
                "enableLocalCoverCache");
        mAlbumArtLibrary = (CheckBoxPreference) findPreference(
                "enableAlbumArtLibrary");
        mAlbumArtLibrary.setEnabled(mCheckBoxPreference.isChecked());

        /** Allow these to be changed individually, pauseOnPhoneStateChange might be overridden. */
        final CheckBoxPreference phonePause = (CheckBoxPreference) findPreference(
                "pauseOnPhoneStateChange");
        final CheckBoxPreference phoneStateChange = (CheckBoxPreference) findPreference(
                "playOnPhoneStateChange");

        mPreferencesBound = true;
        refreshDynamicFields();
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            @NonNull final Preference preference) {
        // Is it the connectionscreen which is called?
        if (preference.getKey() == null) {
            return false;
        }

        if ("refreshMPDDatabase".equals(preference.getKey())) {
            try {
                mApp.oMPDAsyncHelper.oMPD.refreshDatabase();
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to refresh the database.", e);
            }
            return true;
        } else if ("clearLocalCoverCache".equals(preference.getKey())) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.clearLocalCoverCache)
                    .setMessage(R.string.clearLocalCoverCachePrompt)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            // Todo : The covermanager must already have been
                            // initialized, get rid of the getInstance arguments
                            CoverManager.getInstance().clear();
                            mCacheUsage1.setSummary("0.00B");
                            mCacheUsage2.setSummary("0.00B");
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            // do nothing
                        }
                    })
                    .show();
            return true;

        } else if ("enableLocalCover".equals(preference.getKey())) {
            if (mLocalCoverCheckbox.isChecked()) {
                mMusicPath.setEnabled(true);
                mCoverFilename.setEnabled(true);
            } else {
                mMusicPath.setEnabled(false);
                mCoverFilename.setEnabled(false);
            }
            return true;
        } else if ("enableLocalCoverCache".equals(preference.getKey())) {
            // album art library listing requires cover art cache
            if (mCheckBoxPreference.isChecked()) {
                mAlbumArtLibrary.setEnabled(true);
            } else {
                mAlbumArtLibrary.setEnabled(false);
                mAlbumArtLibrary.setChecked(false);
            }
            return true;

        } else if ("pauseOnPhoneStateChange".equals(preference.getKey())) {
            /**
             * Allow these to be changed individually,
             * pauseOnPhoneStateChange might be overridden.
             */
            final CheckBoxPreference phonePause = (CheckBoxPreference) findPreference(
                    "pauseOnPhoneStateChange");
            final CheckBoxPreference phoneStateChange = (CheckBoxPreference) findPreference(
                    "playOnPhoneStateChange");
        } else if ("clearSearchHistory".equals(preference.getKey())) {
            final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                    SearchRecentProvider.AUTHORITY, SearchRecentProvider.MODE);
            suggestions.clearHistory();
            preference.setEnabled(false);
        }

        return false;

    }

    public void refreshDynamicFields() {
        if (getActivity() == null || !mPreferencesBound) {
            return;
        }
        final long size = new CachedCover().getCacheUsage();
        final String usage = Formatter.formatFileSize(mApp, size);
        mCacheUsage1.setSummary(usage);
        mCacheUsage2.setSummary(usage);
        onConnectionStateChanged();
    }

}
