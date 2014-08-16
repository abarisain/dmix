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

import org.a0z.mpd.AlbumInfo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Download Covers Asynchronous with Messages
 *
 * @author Stefan Agner
 */
public class CoverAsyncHelper extends Handler implements CoverDownloadListener {

    private static final int EVENT_COVER_DOWNLOADED = 1;

    public static final int EVENT_COVER_NOT_FOUND = 2;

    private static final int EVENT_COVER_DOWNLOAD_STARTED = 3;

    private static final int MAX_SIZE = 0;

    private int coverMaxSize = MAX_SIZE;

    private int cachedCoverMaxSize = MAX_SIZE;

    private static final Message COVER_NOT_FOUND_MESSAGE;

    private final MPDApplication app = MPDApplication.getInstance();

    static {
        COVER_NOT_FOUND_MESSAGE = new Message();
        COVER_NOT_FOUND_MESSAGE.what = EVENT_COVER_NOT_FOUND;
    }

    private final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

    private final Collection<CoverDownloadListener> coverDownloadListener;

    public CoverAsyncHelper() {
        super();

        coverDownloadListener = new LinkedList<>();
    }

    public void addCoverDownloadListener(final CoverDownloadListener listener) {
        coverDownloadListener.add(listener);
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

    public void downloadCover(final AlbumInfo albumInfo) {
        downloadCover(albumInfo, false);
    }

    public void downloadCover(final AlbumInfo albumInfo, final boolean priority) {
        final CoverInfo info = new CoverInfo(albumInfo);
        info.setCoverMaxSize(coverMaxSize);
        info.setCachedCoverMaxSize(cachedCoverMaxSize);
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
                if (coverInfo.getCachedCoverMaxSize() < cachedCoverMaxSize ||
                        coverInfo.getCoverMaxSize() < coverMaxSize) {
                    // We've got the wrong size, get it again from the cache
                    downloadCover(coverInfo);
                    break;
                }

                for (final CoverDownloadListener listener : coverDownloadListener) {
                    listener.onCoverDownloaded(coverInfo);
                }

                if (CoverManager.DEBUG) {
                    displayCoverRetrieverName(coverInfo);
                }
                break;
            case EVENT_COVER_NOT_FOUND:
                for (final CoverDownloadListener listener : coverDownloadListener) {
                    listener.onCoverNotFound((CoverInfo) msg.obj);
                }
                break;
            case EVENT_COVER_DOWNLOAD_STARTED:
                for (final CoverDownloadListener listener : coverDownloadListener) {
                    listener.onCoverDownloadStarted((CoverInfo) msg.obj);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onCoverDownloaded(final CoverInfo cover) {
        CoverAsyncHelper.this.obtainMessage(EVENT_COVER_DOWNLOADED, cover).sendToTarget();
    }

    @Override
    public void onCoverDownloadStarted(final CoverInfo cover) {
        CoverAsyncHelper.this.obtainMessage(EVENT_COVER_DOWNLOAD_STARTED, cover).sendToTarget();
    }

    @Override
    public void onCoverNotFound(final CoverInfo coverInfo) {
        CoverAsyncHelper.this.obtainMessage(EVENT_COVER_NOT_FOUND, coverInfo).sendToTarget();
    }

    public void removeCoverDownloadListener(final CoverDownloadListener listener) {
        coverDownloadListener.remove(listener);
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
            cachedCoverMaxSize = MAX_SIZE;
        } else {
            cachedCoverMaxSize = size;
        }
    }

    public void setCoverMaxSize(final int size) {
        if (size < 0) {
            coverMaxSize = MAX_SIZE;
        } else {
            coverMaxSize = size;
        }
    }

    public void setCoverMaxSizeFromScreen(final Activity activity) {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        setCoverMaxSize(Math.min(metrics.widthPixels, metrics.heightPixels));
    }

    public void setCoverRetrieversFromPreferences() {
        CoverManager.getInstance().setCoverRetrieversFromPreferences();
    }

    @Override
    public void tagAlbumCover(final AlbumInfo albumInfo) {
        // Nothing to do
    }

    private void tagListenerCovers(final AlbumInfo albumInfo) {
        for (final CoverDownloadListener listener : coverDownloadListener) {
            listener.tagAlbumCover(albumInfo);
        }
    }
}
