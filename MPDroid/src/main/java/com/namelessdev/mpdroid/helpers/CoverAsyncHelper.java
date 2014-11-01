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

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.tools.Tools;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Download Covers Asynchronous with Messages
 *
 * @author Stefan Agner
 */
public class CoverAsyncHelper extends Handler implements CoverDownloadListener {

    public static final int EVENT_COVER_NOT_FOUND = 2;

    private static final Message COVER_NOT_FOUND_MESSAGE;

    private static final int EVENT_COVER_DOWNLOADED = 1;

    private static final int EVENT_COVER_DOWNLOAD_STARTED = 3;

    private static final int MAX_SIZE = 0;

    private int mCoverMaxSize = MAX_SIZE;

    private int mCachedCoverMaxSize = MAX_SIZE;

    private final MPDApplication mApp = MPDApplication.getInstance();

    static {
        COVER_NOT_FOUND_MESSAGE = new Message();
        COVER_NOT_FOUND_MESSAGE.what = EVENT_COVER_NOT_FOUND;
    }

    private final Collection<CoverDownloadListener> mCoverDownloadListeners;

    public CoverAsyncHelper() {
        super();

        mCoverDownloadListeners = new LinkedList<>();
    }

    private static void displayCoverRetrieverName(final CoverInfo coverInfo) {

        try {
            if (!coverInfo.getCoverRetriever().isCoverLocal()) {
                final String message = '\u0022' + coverInfo.getAlbum() + '\u0022' +
                        " cover found with " + coverInfo.getCoverRetriever().getName();
                Tools.notifyUser(message);
            }
        } catch (final RuntimeException ignored) {
            // Nothing to do
        }
    }

    public static void setCoverRetrieversFromPreferences() {
        CoverManager.getInstance().setCoverRetrieversFromPreferences();
    }

    public void addCoverDownloadListener(final CoverDownloadListener listener) {
        mCoverDownloadListeners.add(listener);
    }

    public void downloadCover(final AlbumInfo albumInfo) {
        downloadCover(albumInfo, false);
    }

    public void downloadCover(final AlbumInfo albumInfo, final boolean priority) {
        final CoverInfo info = new CoverInfo(albumInfo);
        info.setCoverMaxSize(mCoverMaxSize);
        info.setCachedCoverMaxSize(mCachedCoverMaxSize);
        info.setPriority(priority);
        info.setListener(this);
        tagListenerCovers(albumInfo);

        if (albumInfo.isValid()) {
            CoverManager.getInstance().addCoverRequest(info);
        } else {
            COVER_NOT_FOUND_MESSAGE.obj = info;
            handleMessage(COVER_NOT_FOUND_MESSAGE);
        }

    }

    @Override
    public void handleMessage(final Message msg) {
        super.handleMessage(msg);

        switch (msg.what) {
            case EVENT_COVER_DOWNLOADED:
                final CoverInfo coverInfo = (CoverInfo) msg.obj;
                if (coverInfo.getCachedCoverMaxSize() < mCachedCoverMaxSize ||
                        coverInfo.getCoverMaxSize() < mCoverMaxSize) {
                    // We've got the wrong size, get it again from the cache
                    downloadCover(coverInfo);
                    break;
                }

                for (final CoverDownloadListener listener : mCoverDownloadListeners) {
                    listener.onCoverDownloaded(coverInfo);
                }

                if (CoverManager.DEBUG) {
                    displayCoverRetrieverName(coverInfo);
                }
                break;
            case EVENT_COVER_NOT_FOUND:
                for (final CoverDownloadListener listener : mCoverDownloadListeners) {
                    listener.onCoverNotFound((CoverInfo) msg.obj);
                }
                break;
            case EVENT_COVER_DOWNLOAD_STARTED:
                for (final CoverDownloadListener listener : mCoverDownloadListeners) {
                    listener.onCoverDownloadStarted((CoverInfo) msg.obj);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onCoverDownloadStarted(final CoverInfo cover) {
        obtainMessage(EVENT_COVER_DOWNLOAD_STARTED, cover).sendToTarget();
    }

    @Override
    public void onCoverDownloaded(final CoverInfo cover) {
        obtainMessage(EVENT_COVER_DOWNLOADED, cover).sendToTarget();
    }

    @Override
    public void onCoverNotFound(final CoverInfo coverInfo) {
        obtainMessage(EVENT_COVER_NOT_FOUND, coverInfo).sendToTarget();
    }

    public void removeCoverDownloadListener(final CoverDownloadListener listener) {
        mCoverDownloadListeners.remove(listener);
    }

    /*
     * If you want cached images to be read as a different size than the
     * downloaded ones. If this equals MAX_SIZE, it will use the coverMaxSize
     * (if not also MAX_SIZE) Example : useful for NowPlayingSmallFragment,
     * where it's useless to read a big image, but since downloading one will
     * fill the cache, download it at a bigger size.
     */
    public void setCachedCoverMaxSize(final int size) {
        if (size < 0) {
            mCachedCoverMaxSize = MAX_SIZE;
        } else {
            mCachedCoverMaxSize = size;
        }
    }

    public void setCoverMaxSize(final int size) {
        if (size < 0) {
            mCoverMaxSize = MAX_SIZE;
        } else {
            mCoverMaxSize = size;
        }
    }

    public void setCoverMaxSizeFromScreen(final Activity activity) {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        setCoverMaxSize(Math.min(metrics.widthPixels, metrics.heightPixels));
    }

    @Override
    public void tagAlbumCover(final AlbumInfo albumInfo) {
        // Nothing to do
    }

    private void tagListenerCovers(final AlbumInfo albumInfo) {
        for (final CoverDownloadListener listener : mCoverDownloadListeners) {
            listener.tagAlbumCover(albumInfo);
        }
    }
}
