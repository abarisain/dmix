package com.namelessdev.mpdroid.helpers;

import static com.namelessdev.mpdroid.tools.StringUtils.isNullOrEmpty;
import static com.namelessdev.mpdroid.tools.StringUtils.trim;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.a0z.mpd.Album;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPD;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.DeezerCover;
import com.namelessdev.mpdroid.cover.DiscogsCover;
import com.namelessdev.mpdroid.cover.GracenoteCover;
import com.namelessdev.mpdroid.cover.ICoverRetriever;
import com.namelessdev.mpdroid.cover.LastFMCover;
import com.namelessdev.mpdroid.cover.LocalCover;
import com.namelessdev.mpdroid.cover.MusicBrainzCover;
import com.namelessdev.mpdroid.cover.SpotifyCover;
import com.namelessdev.mpdroid.tools.Tools;

/**
 * Download Covers Asynchronous with Messages
 *
 * @author Stefan Agner
 * @version $Id: $
 */
public class CoverAsyncHelper extends Handler {
    private final String USER_AGENT = "MPDROID/0.0.0 ( MPDROID@MPDROID.com )";
    public static final int EVENT_DOWNLOADCOVER = 0;
    public static final int EVENT_COVERDOWNLOADED = 1;
    public static final int EVENT_COVERNOTFOUND = 2;
    public static final int MAX_SIZE = 0;
    public static final String PREFERENCE_CACHE = "enableLocalCoverCache";
    public static final String PREFERENCE_LASTFM = "enableLastFM";
    public static final String PREFERENCE_LOCALSERVER = "enableLocalCover";
    public static final String PREFERENCE_ONLY_WIFI = "enableCoverOnlyOnWifi";

    private static final Message COVER_NOT_FOUND_MESSAGE;

    static {
        COVER_NOT_FOUND_MESSAGE = new Message();
        COVER_NOT_FOUND_MESSAGE.what = EVENT_COVERNOTFOUND;
    }

    private static final boolean DEBUG = false;

    private MPDApplication app = null;
    private SharedPreferences settings = null;

    private int coverMaxSize = MAX_SIZE;
    private int cachedCoverMaxSize = MAX_SIZE;
    private boolean cacheWritable = true;

    private ICoverRetriever[] coverRetrievers = null;

    public static ExecutorService threadPool;

    static {
		threadPool = Executors.newFixedThreadPool(3);
    }

    private AndroidHttpClient httpClient = null;

    public void setCoverRetrievers(List<CoverRetrievers> whichCoverRetrievers) {
        if (whichCoverRetrievers == null) {
            coverRetrievers = new ICoverRetriever[0];
        }
        coverRetrievers = new ICoverRetriever[whichCoverRetrievers.size()];
        for (int i = 0; i < whichCoverRetrievers.size(); i++) {
            switch (whichCoverRetrievers.get(i)) {
                case CACHE:
                    this.coverRetrievers[i] = new CachedCover(app);
                    break;
                case LASTFM:
                    this.coverRetrievers[i] = new LastFMCover();
                    break;
                case LOCAL:
                    this.coverRetrievers[i] = new LocalCover(this.app, this.settings);
                    break;
                case GRACENOTE:
                    this.coverRetrievers[i] = new GracenoteCover(this.settings);
                    break;
                case DEEZER:
                    this.coverRetrievers[i] = new DeezerCover();
                    break;
                case MUSICBRAINZ:
                    this.coverRetrievers[i] = new MusicBrainzCover();
                    break;
                case DISCOGS:
                    this.coverRetrievers[i] = new DiscogsCover();
                    break;
                case SPOTIFY:
                    this.coverRetrievers[i] = new SpotifyCover();
                    break;
            }
        }
    }

    public interface CoverDownloadListener {
        public void onCoverDownloaded(Bitmap cover);

        public void onCoverNotFound();
    }

    private Collection<CoverDownloadListener> coverDownloadListener;

    public CoverAsyncHelper(MPDApplication app, SharedPreferences settings) {
        this.app = app;
        this.settings = settings;

		coverDownloadListener = new LinkedList<CoverDownloadListener>();
        setCoverRetrieversFromPreferences();
    }

    public void setCoverRetrieversFromPreferences() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
        final List<CoverRetrievers> enabledRetrievers = new ArrayList<CoverRetrievers>();
        // There is a cover provider order, respect it.
        // Cache -> MPD Server -> LastFM
        if (settings.getBoolean(PREFERENCE_CACHE, true)) {
            enabledRetrievers.add(CoverRetrievers.CACHE);
        }
        if (!(settings.getBoolean(PREFERENCE_ONLY_WIFI, false)) | (isWifi())) {
            if (settings.getBoolean(PREFERENCE_LOCALSERVER, false)) {
                enabledRetrievers.add(CoverRetrievers.LOCAL);
            }
            if (settings.getBoolean(PREFERENCE_LASTFM, true)) {
                enabledRetrievers.add(CoverRetrievers.LASTFM);
                enabledRetrievers.add(CoverRetrievers.DEEZER);
                enabledRetrievers.add(CoverRetrievers.SPOTIFY);
                enabledRetrievers.add(CoverRetrievers.DISCOGS);
                enabledRetrievers.add(CoverRetrievers.GRACENOTE);
                enabledRetrievers.add(CoverRetrievers.MUSICBRAINZ);

            }
        }
        setCoverRetrievers(enabledRetrievers);
    }

    /**
     * Checks if device connected or connecting to wifi network
     */
    public boolean isWifi() {
        ConnectivityManager conMan = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get status of wifi connection
        State wifi = conMan.getNetworkInfo(1).getState();
        if (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING) {
            return true;
        } else {
            return false;
        }
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

    public void setCacheWritable(boolean writable) {
        cacheWritable = writable;
    }

    public void addCoverDownloadListener(CoverDownloadListener listener) {
        coverDownloadListener.add(listener);
    }

    public void removeCoverDownloadListener(CoverDownloadListener listener) {
        coverDownloadListener.remove(listener);
    }

    public void downloadCover(String artist, String album, String path, String filename) {
        final CoverInfo info = new CoverInfo();
        info.sArtist = artist;
        info.sAlbum = album;
        info.sPath = path;
        info.sFilename = filename;

        if (isNullOrEmpty(album)) {
            handleMessage(COVER_NOT_FOUND_MESSAGE);
        } else {
            threadPool.execute(new CoverAsyncWorker(info));
        }

    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_COVERDOWNLOADED:
                for (CoverDownloadListener listener : coverDownloadListener)
                    listener.onCoverDownloaded((Bitmap) msg.obj);
                break;

            case EVENT_COVERNOTFOUND:
                for (CoverDownloadListener listener : coverDownloadListener)
                    listener.onCoverNotFound();
                break;
            default:
                break;
        }
    }

    private class CoverAsyncWorker implements Runnable {
        CoverInfo info;

        public CoverAsyncWorker(CoverInfo info) {
            this.info = info;
        }

        public Bitmap[] getBitmapForRetriever(ICoverRetriever retriever) {
            String[] urls = null;
            try {
                // Get URL to the Cover...
                urls = retriever.getCoverUrl(info.sArtist, info.sAlbum, info.sPath, info.sFilename);
            } catch (Exception e1) {
                e1.printStackTrace();
                return null;
            }

            if (urls == null || urls.length == 0) {
                return null;
            }

            Bitmap[] downloadedCovers = null;
            for (String url : urls) {
                if (url == null)
                    continue;
                if (DEBUG)
                    Log.i(MPDApplication.TAG, "Downloading cover art at url : " + url);
                if (retriever.isCoverLocal()) {
                    int maxSize = coverMaxSize;
                    if (cachedCoverMaxSize != MAX_SIZE) {
                        maxSize = cachedCoverMaxSize;
                    }
                    if (maxSize == MAX_SIZE) {
                        downloadedCovers = new Bitmap[]{BitmapFactory.decodeFile(url)};
                    } else {
                        downloadedCovers = new Bitmap[]{Tools.decodeSampledBitmapFromPath(url, maxSize, maxSize, false)};
                    }
                } else {
                    downloadedCovers = download(url);
                }

                if (downloadedCovers != null) {
                    break;
                }
            }
            return downloadedCovers;
        }

        public boolean fillEmptyArtist() {
            if (info.sArtist != null)
                return true;
            try {
                // load songs for this album
                final List<? extends Item> songs = app.oMPDAsyncHelper.oMPD.getSongs(null, new Album(info.sAlbum, info.sArtist));

                if (songs.size() > 0) {
                    Music song = (Music) songs.get(0);
                    info.sFilename = song.getFilename();
                    info.sPath = song.getPath();
                    info.sArtist = MPD.useAlbumArtist() ? song.getAlbumArtist() : song.getArtist();
                    return true;
                }
            } catch (MPDServerException e) {
                // MPD error, bail on loading artwork
            }
            return false;
        }

        public void run() {
            Bitmap[] covers = null;
            if (fillEmptyArtist()) {
                for (ICoverRetriever coverRetriever : coverRetrievers) {
                    covers = getBitmapForRetriever(coverRetriever);
                    if (covers != null && covers[0] != null) {
                        if (DEBUG)
                            Log.i(MPDApplication.TAG, "Found cover art using retriever : " + coverRetriever.getName());
                        // if cover is not read from cache and saving is enabled
                        if (cacheWritable && !(coverRetriever instanceof CachedCover)) {
                            // Save this cover into cache, if it is enabled.
                            for (ICoverRetriever coverRetriever1 : coverRetrievers) {
                                if (coverRetriever1 instanceof CachedCover) {
                                    if (DEBUG)
                                        Log.i(MPDApplication.TAG, "Saving cover art to cache");
                                    // Save the fullsize bitmap
                                    ((CachedCover) coverRetriever1).save(info.sArtist, info.sAlbum, covers[1]);
                                    // Release the cover immediately if not used
                                    if (covers[0] != covers[1]) {
                                        covers[1].recycle();
                                        covers[1] = null;
                                    }
                                }
                            }
                        }
                        CoverAsyncHelper.this.obtainMessage(EVENT_COVERDOWNLOADED, covers[0]).sendToTarget();
                        break;
                    }
                }
            }

            if (covers == null) {
                if (DEBUG)
                    Log.i(MPDApplication.TAG, "No cover art found");
                CoverAsyncHelper.this.obtainMessage(EVENT_COVERNOTFOUND).sendToTarget();
            }
        }
    }

    private void closeHttpClient() {
        if (httpClient != null) {
            httpClient.close();
        }
        httpClient = null;
    }

    private Bitmap[] download(String url) {

        HttpResponse response;
        StatusLine statusLine;
        int statusCode;
        HttpEntity entity;
        InputStream content;
        BufferedInputStream bis;
        ByteArrayOutputStream baos;
        HttpGet httpGet = null;
        byte[] buffer;
        int len;
        InputStream is;

        try {
            url = trim(url);
            if (isNullOrEmpty(url)) {
                return null;
            }

            if (httpClient == null) {
                httpClient = AndroidHttpClient.newInstance(USER_AGENT);
            }

            // Download Cover File...
            url = url.replace(" ", "%20");
            httpGet = new HttpGet(url);
            response = httpClient.execute(httpGet);
            statusLine = response.getStatusLine();
            statusCode = statusLine.getStatusCode();
            entity = response.getEntity();
            content = entity.getContent();
            // Status Code 307 (temporary redirect) is needed for musicbrainz archive cover
            if (statusCode != 200 && statusCode != 307 && statusCode != 302) {
                Log.w(CoverAsyncHelper.class.getName(), "This URL does not exist : Status code : " + statusCode + ", " + url);
                return null;
            }
            bis = new BufferedInputStream(content, 8192);
            baos = new ByteArrayOutputStream();
            buffer = new byte[1024];
            while ((len = bis.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            is = new ByteArrayInputStream(baos.toByteArray());

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);

            int scale = 1;
            if (coverMaxSize != MAX_SIZE || o.outHeight > coverMaxSize || o.outWidth > coverMaxSize) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(coverMaxSize /
                        (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            is.reset();
            o.inSampleSize = 1;
            o.inJustDecodeBounds = false;
            Bitmap fullBmp = BitmapFactory.decodeStream(is, null, o);
            Bitmap bmp = null;
            if (scale == 1) {
                // This can cause some problem (a bitmap being freed will free both references)
                // But the only use is to save it in the cache so it's okay.
                bmp = fullBmp;
            } else {
                o.inSampleSize = scale;
                o.inJustDecodeBounds = false;
                is.reset();
                bmp = BitmapFactory.decodeStream(is, null, o);
            }
            return new Bitmap[]{bmp, fullBmp};

        } catch (Exception e) {
            Log.e(CoverAsyncHelper.class.getSimpleName(), "Failed to download cover :" + e);
            return null;
        } finally {
            if (httpGet != null && !httpGet.isAborted()) {
                httpGet.abort();
            }

        }
    }


    public enum CoverRetrievers {
        CACHE,
        LASTFM,
        LOCAL,
        GRACENOTE,
        DEEZER,
        MUSICBRAINZ,
        DISCOGS,
        SPOTIFY;
    }

    @Override
    protected void finalize() throws Throwable {
        closeHttpClient();
        super.finalize();
    }

}
