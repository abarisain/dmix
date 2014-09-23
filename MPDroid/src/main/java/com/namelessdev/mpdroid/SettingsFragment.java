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
import org.a0z.mpd.exception.MPDServerException;

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
import android.text.format.Formatter;
import android.util.Log;

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

    private CheckBoxPreference mPhonePause;

    private CheckBoxPreference mPhoneStateChange;

    private boolean mPreferencesBound;

    private EditTextPreference mSongs;

    private EditTextPreference mVersion;

    public SettingsFragment() {
        mPreferencesBound = false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        refreshDynamicFields();
    }

    public void onConnectionStateChanged() {
        final MPD mpd = mApp.oMPDAsyncHelper.oMPD;
        mInformationScreen.setEnabled(mpd.isConnected());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
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
                } catch (final MPDServerException e) {
                    Log.e(TAG, "Failed to get MPD statistics.", e);
                }
            }
        }).start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        mPhonePause = (CheckBoxPreference) findPreference("pauseOnPhoneStateChange");
        mPhoneStateChange = (CheckBoxPreference) findPreference("playOnPhoneStateChange");

        mPreferencesBound = true;
        refreshDynamicFields();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        // Is it the connectionscreen which is called?
        if (preference.getKey() == null) {
            return false;
        }

        if (preference.getKey().equals("refreshMPDDatabase")) {
            try {
                mApp.oMPDAsyncHelper.oMPD.refreshDatabase();
            } catch (final MPDServerException e) {
                Log.e(TAG, "Failed to refresh the database.", e);
            }
            return true;
        } else if (preference.getKey().equals("clearLocalCoverCache")) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.clearLocalCoverCache)
                    .setMessage(R.string.clearLocalCoverCachePrompt)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Todo : The covermanager must already have been
                            // initialized, get rid of the getInstance arguments
                            CoverManager.getInstance().clear();
                            mCacheUsage1.setSummary("0.00B");
                            mCacheUsage2.setSummary("0.00B");
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .show();
            return true;

        } else if (preference.getKey().equals("enableLocalCover")) {
            if (mLocalCoverCheckbox.isChecked()) {
                mMusicPath.setEnabled(true);
                mCoverFilename.setEnabled(true);
            } else {
                mMusicPath.setEnabled(false);
                mCoverFilename.setEnabled(false);
            }
            return true;
        } else if (preference.getKey().equals("enableLocalCoverCache")) {
            // album art library listing requires cover art cache
            if (mCheckBoxPreference.isChecked()) {
                mAlbumArtLibrary.setEnabled(true);
            } else {
                mAlbumArtLibrary.setEnabled(false);
                mAlbumArtLibrary.setChecked(false);
            }
            return true;

        } else if (preference.getKey().equals("pauseOnPhoneStateChange")) {
            /**
             * Allow these to be changed individually,
             * pauseOnPhoneStateChange might be overridden.
             */
            CheckBoxPreference phonePause = (CheckBoxPreference) findPreference(
                    "pauseOnPhoneStateChange");
            CheckBoxPreference phoneStateChange = (CheckBoxPreference) findPreference(
                    "playOnPhoneStateChange");
        } else if (preference.getKey().equals("clearSearchHistory")) {
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
        long size = new CachedCover().getCacheUsage();
        final String usage = Formatter.formatFileSize(mApp, size);
        mCacheUsage1.setSummary(usage);
        mCacheUsage2.setSummary(usage);
        onConnectionStateChanged();
    }

}
