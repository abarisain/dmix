package com.namelessdev.mpdroid.helpers;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import com.namelessdev.mpdroid.MPDApplication;
import org.a0z.mpd.*;

import java.util.Collection;
import java.util.LinkedList;

import static com.namelessdev.mpdroid.tools.StringUtils.isNullOrEmpty;

/**
 * Download Covers Asynchronous with Messages
 *
 * @author Stefan Agner
 * @version $Id: $
 */
public class CoverAsyncHelper extends Handler implements CoverDownloadListener {
    public static final int EVENT_COVERDOWNLOADED = 1;
    public static final int EVENT_COVERNOTFOUND = 2;
    public static final int MAX_SIZE = 0;

    private static final Message COVER_NOT_FOUND_MESSAGE;

    static {
        COVER_NOT_FOUND_MESSAGE = new Message();
        COVER_NOT_FOUND_MESSAGE.what = EVENT_COVERNOTFOUND;
    }

    private MPDApplication app = null;
    private SharedPreferences settings = null;

    private int coverMaxSize = MAX_SIZE;
    private int cachedCoverMaxSize = MAX_SIZE;

    private Collection<CoverDownloadListener> coverDownloadListener;

    public CoverAsyncHelper(MPDApplication app, SharedPreferences settings) {
        this.app = app;
        this.settings = settings;

        coverDownloadListener = new LinkedList<CoverDownloadListener>();
    }

    public void setCoverMaxSizeFromScreen(Activity activity) {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        setCoverMaxSize(Math.min(metrics.widthPixels, metrics.heightPixels));
    }

    public void setCoverMaxSize(int size) {
        if (size < 0)
            size = MAX_SIZE;
        coverMaxSize = size;
    }

    /*
     * If you want cached images to be read as a different size than the downloaded ones.
     * If this equals MAX_SIZE, it will use the coverMaxSize (if not also MAX_SIZE)
     * Example : useful for NowPlayingSmallFragment, where
     * it's useless to read a big image, but since downloading one will fill the cache, download it at a bigger size.
     */
    public void setCachedCoverMaxSize(int size) {
        if (size < 0)
            size = MAX_SIZE;
        cachedCoverMaxSize = size;
    }

    public void addCoverDownloadListener(CoverDownloadListener listener) {
        coverDownloadListener.add(listener);
    }

    public void removeCoverDownloadListener(CoverDownloadListener listener) {
        coverDownloadListener.remove(listener);
    }

    public void downloadCover(Music song) {
	downloadCover(song, false, false);
    }
    public void downloadCover(Music song, boolean priority, boolean cacheOnly) {
        downloadCover(song.getAlbumArtist(), song.getArtist(),
		      song.getAlbum(), song.getPath(), song.getFilename(),
		      priority, cacheOnly);
    }

    public void downloadCover(String albumartist, String artist, String album, String path, String filename) {
        downloadCover(albumartist, artist, album, path, filename, false, false);
    }

    public void downloadCover(String albumartist, String artist, String album, String path, String filename, boolean priority, boolean cacheOnly) {
        final CoverInfo info = new CoverInfo();
	if (!isNullOrEmpty(albumartist)) artist = albumartist;
        info.setArtist(artist);
        info.setAlbum(album);
        info.setPath(path);
        info.setFilename(filename);
        info.setCoverMaxSize(coverMaxSize);
        info.setCachedCoverMaxSize(cachedCoverMaxSize);
        info.setPriority(priority);
        info.setListener(this);
        info.setCacheOnly(cacheOnly);

        if (isNullOrEmpty(album) || isNullOrEmpty(artist)) {
            COVER_NOT_FOUND_MESSAGE.obj = info;
            handleMessage(COVER_NOT_FOUND_MESSAGE);
        } else {
            CoverManager.getInstance(app, settings).addCoverRequest(info);
        }

    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_COVERDOWNLOADED:
                for (CoverDownloadListener listener : coverDownloadListener)
                    listener.onCoverDownloaded((CoverInfo) msg.obj);
                break;

            case EVENT_COVERNOTFOUND:
                for (CoverDownloadListener listener : coverDownloadListener)
                    listener.onCoverNotFound((CoverInfo) msg.obj);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCoverDownloaded(CoverInfo cover) {
        CoverAsyncHelper.this.obtainMessage(EVENT_COVERDOWNLOADED, cover).sendToTarget();
    }

    @Override
    public void onCoverNotFound(CoverInfo cover) {
        CoverAsyncHelper.this.obtainMessage(EVENT_COVERNOTFOUND, cover).sendToTarget();
    }
    public void setCoverRetrieversFromPreferences() {
        CoverManager.getInstance(app, settings).setCoverRetrieversFromPreferences();
    }


}