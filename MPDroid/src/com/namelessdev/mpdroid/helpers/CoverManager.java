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
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.DeezerCover;
import com.namelessdev.mpdroid.cover.DiscogsCover;
import com.namelessdev.mpdroid.cover.GracenoteCover;
import com.namelessdev.mpdroid.cover.ICoverRetriever;
import com.namelessdev.mpdroid.cover.ItunesCover;
import com.namelessdev.mpdroid.cover.LastFMCover;
import com.namelessdev.mpdroid.cover.LocalCover;
import com.namelessdev.mpdroid.cover.MusicBrainzCover;
import com.namelessdev.mpdroid.cover.SpotifyCover;
import com.namelessdev.mpdroid.tools.MultiMap;
import com.namelessdev.mpdroid.tools.StringUtils;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.AlbumInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.text.TextUtils.isEmpty;
import static android.util.Log.d;
import static android.util.Log.e;
import static android.util.Log.i;
import static android.util.Log.w;
import static com.namelessdev.mpdroid.helpers.CoverInfo.STATE.CACHE_COVER_FETCH;
import static com.namelessdev.mpdroid.helpers.CoverInfo.STATE.CREATE_BITMAP;
import static com.namelessdev.mpdroid.helpers.CoverInfo.STATE.WEB_COVER_FETCH;

/**
 */
public class CoverManager {
    public enum CoverRetrievers {
        CACHE,
        LASTFM,
        LOCAL,
        GRACENOTE,
        DEEZER,
        MUSICBRAINZ,
        DISCOGS,
        SPOTIFY,
        ITUNES
    }

    private class CreateBitmapTask implements Runnable

    {
        private CoverInfo coverInfo;

        private CreateBitmapTask(CoverInfo coverInfo) {
            this.coverInfo = coverInfo;
        }

        @Override
        public void run() {

            Bitmap[] bitmaps;

            if (DEBUG)
                d(CoverManager.class.getSimpleName(),
                        "Making cover bitmap for " + coverInfo.getAlbum());

            if (coverInfo.getCoverRetriever().isCoverLocal()) {
                int maxSize = coverInfo.getCoverMaxSize();
                if (coverInfo.getCachedCoverMaxSize() != CoverInfo.MAX_SIZE) {
                    maxSize = coverInfo.getCachedCoverMaxSize();
                }
                if (maxSize == CoverInfo.MAX_SIZE) {
                    bitmaps = new Bitmap[] {
                            BitmapFactory.decodeByteArray(coverInfo.getCoverBytes(), 0,
                                    coverInfo.getCoverBytes().length)
                    };
                    coverInfo.setBitmap(bitmaps);
                } else {
                    bitmaps = new Bitmap[] {
                            Tools.decodeSampledBitmapFromBytes(coverInfo.getCoverBytes(), maxSize,
                                    maxSize, false)
                    };
                    coverInfo.setBitmap(bitmaps);
                }
            } else {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(coverInfo.getCoverBytes(), 0,
                        coverInfo.getCoverBytes().length, o);

                int scale = 1;
                if (coverInfo.getCoverMaxSize() != CoverInfo.MAX_SIZE
                        || o.outHeight > coverInfo.getCoverMaxSize()
                        || o.outWidth > coverInfo.getCoverMaxSize()) {
                    scale = (int) Math.pow(2,
                            (int) Math.round(Math.log(coverInfo.getCoverMaxSize() /
                                    (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
                }

                o.inSampleSize = 1;
                o.inJustDecodeBounds = false;
                Bitmap fullBmp = BitmapFactory.decodeByteArray(coverInfo.getCoverBytes(), 0,
                        coverInfo.getCoverBytes().length, o);
                Bitmap bmp;
                if (scale == 1) {
                    // This can cause some problem (a bitmap being freed will
                    // free both references)
                    // But the only use is to save it in the cache so it's okay.
                    bmp = fullBmp;
                } else {
                    o.inSampleSize = scale;
                    o.inJustDecodeBounds = false;
                    bmp = BitmapFactory.decodeByteArray(coverInfo.getCoverBytes(), 0,
                            coverInfo.getCoverBytes().length, o);
                }
                bitmaps = new Bitmap[] {
                        bmp, fullBmp
                };
                coverInfo.setBitmap(bitmaps);
                coverInfo.setCoverBytes(null);

                ICoverRetriever cacheRetriever;
                cacheRetriever = getCacheRetriever();
                if (cacheRetriever != null && coverInfo.getCoverRetriever() != cacheRetriever) {
                    if (DEBUG)
                        i(MPDApplication.TAG, "Saving cover art to cache");
                    // Save the fullsize bitmap
                    (getCacheRetriever()).save(coverInfo, fullBmp);

                    // Release the cover immediately if not used
                    if (bitmaps[0] != bitmaps[1]) {
                        bitmaps[1].recycle();
                        bitmaps[1] = null;
                    }
                }
            }

            requests.addLast(coverInfo);
        }

    }

    private class FetchCoverTask implements Runnable

    {
        private CoverInfo coverInfo;

        private FetchCoverTask(CoverInfo coverInfo) {
            this.coverInfo = coverInfo;
        }

        @Override
        public void run() {
            String[] coverUrls;
            boolean remote;
            boolean local;
            boolean canStart = true;
            byte[] coverBytes;

            if (coverInfo.getState() != CoverInfo.STATE.WEB_COVER_FETCH
                    || coverFetchExecutor.getQueue().size() < MAX_REQUESTS) {

                // If the coverretriever is defined in the coverInfo
                // that means that a previous cover fetch failed with this
                // retriever
                // We just start after this retriever to try a cover.
                if (coverInfo.getCoverRetriever() != null) {
                    canStart = false;
                }

                for (ICoverRetriever coverRetriever : coverRetrievers) {
                    try {

                        if (coverRetriever == null) {
                            continue;
                        }

                        if (canStart) {

                            remote = coverInfo.getState() == WEB_COVER_FETCH
                                    && !coverRetriever.isCoverLocal();
                            local = coverInfo.getState() == CACHE_COVER_FETCH
                                    && coverRetriever.isCoverLocal();
                            if (remote || local) {
                                if (DEBUG) {
                                    d(CoverManager.class.getSimpleName(), "Looking for cover "
                                            + coverInfo.getArtist() + ", " + coverInfo.getAlbum()
                                            + " with " + coverRetriever.getName());
                                }
                                coverInfo.setCoverRetriever(coverRetriever);
                                coverUrls = coverRetriever.getCoverUrl(coverInfo);

                                // Normalize (remove special characters ...) the
                                // artist and album names if no result has been
                                // found.
                                if (!(coverUrls != null && coverUrls.length > 0)
                                        && remote
                                        && !(coverRetriever.getName()
                                                .equals(LocalCover.RETRIEVER_NAME))) {
                                    AlbumInfo normalizedAlbumInfo = getNormalizedAlbumInfo(coverInfo);
                                    if (!normalizedAlbumInfo.equals(coverInfo)) {
                                        if (DEBUG)
                                            d(FetchCoverTask.class.getSimpleName(),
                                                    "Retry to fetch cover with normalized names for "
                                                            + normalizedAlbumInfo);
                                        coverUrls = coverRetriever.getCoverUrl(normalizedAlbumInfo);
                                    }
                                }

                                if (coverUrls != null && coverUrls.length > 0) {
                                    List<String> wrongUrlsForCover = wrongCoverUrlMap.get(coverInfo
                                            .getKey());

                                    if (wrongUrlsForCover == null
                                            || !isBlacklistedCoverUrl(coverUrls[0],
                                                    coverInfo.getKey())) {

                                        if (DEBUG)
                                            d(CoverManager.class.getSimpleName(),
                                                    "Cover found for  " + coverInfo.getAlbum()
                                                            + " with " + coverRetriever.getName()
                                                            + " : " + coverUrls[0]);
                                        coverBytes = getCoverBytes(coverUrls, coverInfo);
                                        if (coverBytes != null && coverBytes.length > 0) {
                                            if (!coverRetriever.isCoverLocal()) {
                                                coverUrlMap.put(coverInfo.getKey(), coverUrls[0]);
                                            }
                                            coverInfo.setCoverBytes(coverBytes);
                                            requests.addLast(coverInfo);
                                            return;
                                        } else {
                                            if (DEBUG)
                                                d(CoverManager.class.getSimpleName(),
                                                        "The cover URL for album "
                                                                + coverInfo.getAlbum()
                                                                + " did not work : "
                                                                + coverRetriever.getName());
                                        }

                                    } else {
                                        if (DEBUG) {
                                            d(CoverManager.class.getSimpleName(),
                                                    "Blacklisted cover url found for "
                                                            + coverInfo.getAlbum() + " : "
                                                            + coverUrls[0]);
                                        }
                                    }
                                }

                            }
                        } else {
                            if (DEBUG)
                                d(CoverManager.class.getSimpleName(),
                                        "Bypassing the retriever " + coverRetriever.getName()
                                                + " for album " + coverInfo.getAlbum()
                                                + ", already asked.");
                            canStart = coverRetriever == coverInfo.getCoverRetriever();
                        }

                    } catch (Exception e) {
                        e(CoverManager.class.getSimpleName(), "Fetch cover failure : " + e);
                    }

                }
            } else {
                coverInfo.setRequestGivenUp(true);
                w(CoverManager.class.getSimpleName(), "Too many requests, giving up this one : "
                        + coverInfo.getAlbum());
            }

            requests.addLast(coverInfo);
        }

    }

    private class RequestProcessorTask implements Runnable {

        @Override
        public void run() {

            CoverInfo coverInfo;

            while (active) {

                try {
                    coverInfo = requests.take();

                    if (coverInfo == null || coverInfo.getListener() == null) {
                        return;
                    }

                    switch (coverInfo.getState()) {
                        case NEW:
                            // Do not create a new request if a similar one
                            // already exists
                            // Just register the new cover listener and update
                            // the request priority.
                            helpersByCoverInfo.put(coverInfo, coverInfo.getListener());
                            if (runningRequests.contains(coverInfo)) {
                                CoverInfo existingRequest = getExistingRequest(coverInfo);
                                existingRequest.setPriority(existingRequest.isPriority()
                                        || coverInfo.isPriority());
                                notifyListeners(existingRequest);
                                break;
                            } else {

                                if (!coverInfo.isValid()
                                        || notFoundAlbumKeys.contains(coverInfo.getKey())) {
                                    if (DEBUG) {
                                        d(CoverManager.class.getSimpleName(),
                                                "Incomplete cover request or already not found cover with artist="
                                                        + coverInfo.getArtist() + ", album="
                                                        + coverInfo.getAlbum());
                                    }
                                    coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                                    notifyListeners(coverInfo);
                                } else {
                                    runningRequests.add(coverInfo);
                                    coverInfo.setState(CACHE_COVER_FETCH);
                                    cacheCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                }
                                break;

                            }
                        case CACHE_COVER_FETCH:
                            if (coverInfo.getCoverBytes() == null
                                    || coverInfo.getCoverBytes().length == 0) {
                                coverInfo.setState(WEB_COVER_FETCH);
                                notifyListeners(coverInfo);
                                if (coverInfo.isPriority()) {
                                    priorityCoverFetchExecutor
                                            .submit(new FetchCoverTask(coverInfo));
                                } else {
                                    coverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                }
                                break;
                            } else {
                                coverInfo.setState(CREATE_BITMAP);
                                createBitmapExecutor.submit(new CreateBitmapTask(coverInfo));
                                break;
                            }
                        case WEB_COVER_FETCH:
                            if (coverInfo.getCoverBytes() != null
                                    && coverInfo.getCoverBytes().length > 0) {
                                coverInfo.setState(CREATE_BITMAP);
                                notifyListeners(coverInfo);
                                createBitmapExecutor.submit(new CreateBitmapTask(coverInfo));
                                break;
                            } else {
                                coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                                notifyListeners(coverInfo);
                                break;
                            }
                        case CREATE_BITMAP:
                            if (coverInfo.getBitmap() != null) {
                                coverInfo.setState(CoverInfo.STATE.COVER_FOUND);
                                notifyListeners(coverInfo);
                            } else if (isLastCoverRetriever(coverInfo.getCoverRetriever())) {
                                if (DEBUG)
                                    d(CoverManager.class.getSimpleName(),
                                            "The cover has not been downloaded correctly for album "
                                                    + coverInfo.getAlbum()
                                                    + " with this retriever : "
                                                    + coverInfo.getCoverRetriever()
                                                    + ", trying the next ones ...");
                                coverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                            } else {
                                coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                                notifyListeners(coverInfo);
                            }
                            break;
                        default:
                            e(CoverManager.class.getSimpleName(), "Unknown request : " + coverInfo);
                            coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                            notifyListeners(coverInfo);
                            break;
                    }

                    if (runningRequests.isEmpty()) {
                        saveCovers();
                        saveWrongCovers();
                    }

                } catch (Exception e) {
                    e(CoverManager.class.getSimpleName(), "Cover request processing failure : " + e);
                }
            }

        }
    }

    private static final String[] DISC_REFERENCES = {
            "disc", "cd", "disque"
    };
    public static final String PREFERENCE_CACHE = "enableLocalCoverCache";
    public static final String PREFERENCE_LASTFM = "enableLastFM";
    public static final String PREFERENCE_LOCALSERVER = "enableLocalCover";
    public static final String PREFERENCE_ONLY_WIFI = "enableCoverOnlyOnWifi";
    public static final boolean DEBUG = false;
    public static final int MAX_REQUESTS = 20;
    private static final String FOLDER_SUFFIX = "/covers/";
    public static final String WRONG_COVERS_FILE_NAME = "wrong-covers.bin";
    public static final String COVERS_FILE_NAME = "covers.bin";

    private static ThreadPoolExecutor getCoverFetchExecutor() {
        return new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public static String getCoverFileName(AlbumInfo albumInfo) {
        return albumInfo.getKey() + ".jpg";
    }

    public synchronized static CoverManager getInstance(MPDApplication app,
            SharedPreferences settings) {
        if (instance == null) {
            instance = new CoverManager(app, settings);
        }
        return instance;
    }

    private MPDApplication app = null;
    private SharedPreferences settings = null;
    private static CoverManager instance = null;
    private BlockingDeque<CoverInfo> requests = new LinkedBlockingDeque<CoverInfo>();
    private List<CoverInfo> runningRequests = Collections
            .synchronizedList(new ArrayList<CoverInfo>());
    private ExecutorService requestExecutor = Executors.newFixedThreadPool(1);
    private ThreadPoolExecutor coverFetchExecutor = getCoverFetchExecutor();
    private ExecutorService priorityCoverFetchExecutor = Executors.newFixedThreadPool(1);
    private ExecutorService cacheCoverFetchExecutor = Executors.newFixedThreadPool(1);

    private ExecutorService createBitmapExecutor = cacheCoverFetchExecutor;

    private MultiMap<CoverInfo, CoverDownloadListener> helpersByCoverInfo = new MultiMap<CoverInfo, CoverDownloadListener>();

    private ICoverRetriever[] coverRetrievers = null;

    private boolean active = true;

    private MultiMap<String, String> wrongCoverUrlMap = null;

    private Map<String, String> coverUrlMap = null;

    private Set<String> notFoundAlbumKeys;

    private CoverManager(MPDApplication app, SharedPreferences settings) {
        this.app = app;
        this.settings = settings;

        requestExecutor.submit(new RequestProcessorTask());
        this.setCoverRetrieversFromPreferences();
        initializeCoverData();
    }

    public void addCoverRequest(CoverInfo coverInfo) {
        if (DEBUG) {
            d(CoverManager.class.getSimpleName(),
                    "Looking for cover with artist=" + coverInfo.getArtist() + ", album="
                            + coverInfo.getAlbum());
        }
        this.requests.add(coverInfo);
    }

    protected String cleanGetRequest(String text) {
        String processedtext = null;

        if(text != null) {
            processedtext = text.replaceAll("[^\\w .-]+", " ");
            processedtext = Normalizer.normalize(processedtext, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        }
        return processedtext;
    }

    public void clear() {
        CachedCover cachedCover = getCacheRetriever();
        if (cachedCover != null) {
            cachedCover.clear();
        }
        initializeCoverData();
    }

    public void clear(AlbumInfo albumInfo) {

        CachedCover cachedCover = getCacheRetriever();
        if (cachedCover != null) {
            cachedCover.delete(albumInfo);
        }
        coverUrlMap.remove(albumInfo);
        wrongCoverUrlMap.remove(albumInfo.getKey());
        notFoundAlbumKeys.remove(albumInfo.getKey());
    }

    /**
     * This method cleans and builds a proper URL object from a string.
     *
     * @param _request This is the URL in string form.
     * @return A URL Object
     */
    public static URL buildURLForConnection(final String _request) {
        URL url = null;
        String request = StringUtils.trim(_request);

        if (isEmpty(request)) {
            return null;
        }
        request = request.replace(" ", "%20");

        try {
            url = new URL(request);
        } catch (MalformedURLException e) {
            Log.w(MPDApplication.TAG,
                    "Failed to parse the URL string for URL object generation.", e);
        }

        return url;
    }

    /**
     * This method takes a URL object and returns a HttpURLConnection object.
     *
     * @param url The URL object used to create the connection.
     * @return The connection which is returned; ensure this resource is disconnected after use.
     */
    public static HttpURLConnection getHttpConnection(URL url) {
        HttpURLConnection connection = null;

        if(url == null) {
            Log.d(MPDApplication.TAG, "Cannot create a connection with a null URL");
            return null;
        }

        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            Log.w(MPDApplication.TAG, "Failed to execute cover get request: ", e);
        }

        if(connection != null) {
            connection.setUseCaches(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
        }

        return connection;
    }

    /**
     * This method connects to the HTTP server URL, and gets a HTTP status code. If the
     * status code is OK or similar this method returns true, otherwise false.
     *
     * @param connection An HttpURLConnection object.
     * @return True if the URL exists, false otherwise.
     */
    public static boolean urlExists(HttpURLConnection connection) {
        int statusCode = 0;

        if(connection == null) {
            Log.d(CoverManager.class.getSimpleName(),
                    "Cannot find out if URL exists with a null connection.");
            return false;
        }

        try {
            statusCode = connection.getResponseCode();
        } catch (IOException e) {
            Log.e(MPDApplication.TAG, "Failed to get a valid response code.",e);
        }

        return urlExists(statusCode);
    }

    private byte[] download(String textUrl) {

        URL url = buildURLForConnection(textUrl);
        HttpURLConnection connection = getHttpConnection(url);
        BufferedInputStream bis;
        ByteArrayOutputStream baos;
        byte[] buffer = null;
        int len;

        if(!urlExists(connection)) {
            return null;
        }

        try {
            bis = new BufferedInputStream(connection.getInputStream(), 8192);
            baos = new ByteArrayOutputStream();
            buffer = new byte[1024];
            while ((len = bis.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            buffer = baos.toByteArray();
        } catch (Exception e) {
            e(CoverAsyncHelper.class.getSimpleName(), "Failed to download cover :" + e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return buffer;
    }

    /**
     * This method connects to the HTTP server URL, gets a HTTP status code and if the
     * status code is OK or similar this method returns true, otherwise false.
     *
     * @param statusCode An HttpURLConnection object.
     * @return True if the URL exists, false otherwise.
     */
    public static boolean urlExists(int statusCode) {
        final int TEMPORARY_REDIRECT = 307; /** No constant for 307 exists */

        return ((statusCode == HttpURLConnection.HTTP_OK ||
                statusCode == TEMPORARY_REDIRECT ||
                statusCode == HttpURLConnection.HTTP_MOVED_TEMP));
    }

    @Override
    protected void finalize() throws Throwable {
        stopExecutors();
        instance = null;
        super.finalize();
    }

    private CachedCover getCacheRetriever() {
        for (ICoverRetriever retriever : coverRetrievers) {
            if (retriever.isCoverLocal() && retriever instanceof CachedCover) {
                return (CachedCover) retriever;
            }
        }
        return null;
    }

    private byte[] getCoverBytes(String[] coverUrls, CoverInfo coverInfo) {

        byte[] coverBytes = null;

        for (String url : coverUrls) {

            try {
                if (DEBUG)
                    d(CoverManager.class.getSimpleName(),
                            "Downloading cover (with maxsize " + coverInfo.getCoverMaxSize() + ", "
                                    + coverInfo.getCachedCoverMaxSize() + ") for "
                                    + coverInfo.getAlbum() + " from " + url);
                if (coverInfo.getState() == CACHE_COVER_FETCH) {

                    coverBytes = readBytes(new URL("file://" + url).openStream());

                } else if (coverInfo.getState() == WEB_COVER_FETCH) {
                    coverBytes = download(url);
                }
                if (coverBytes != null) {
                    if (DEBUG)
                        d(CoverManager.class.getSimpleName(),
                                "Cover downloaded for " + coverInfo.getAlbum() + " from " + url
                                        + ", size=" + coverBytes.length);
                }
            } catch (Exception e) {
                w(CoverManager.class.getSimpleName(), "Cover get bytes failure : " + e);
            }
        }
        return coverBytes;
    }

    public String getCoverFolder() {
        final File cacheDir = app.getExternalCacheDir();
        if (cacheDir == null)
            return null;
        return cacheDir.getAbsolutePath() + FOLDER_SUFFIX;
    }

    private CoverInfo getExistingRequest(CoverInfo coverInfo) {
        return runningRequests.get(runningRequests.indexOf(coverInfo));
    }

    public AlbumInfo getNormalizedAlbumInfo(AlbumInfo albumInfo) throws Exception {
        AlbumInfo normalizedAlbumInfo = new AlbumInfo(albumInfo);
        normalizedAlbumInfo.setAlbum(cleanGetRequest(normalizedAlbumInfo.getAlbum()));
        normalizedAlbumInfo.setAlbum(removeDiscReference(normalizedAlbumInfo.getAlbum()));
        normalizedAlbumInfo.setArtist(cleanGetRequest(normalizedAlbumInfo.getArtist()));
        return normalizedAlbumInfo;
    }

    private void initializeCoverData() {
        wrongCoverUrlMap = loadWrongCovers();
        coverUrlMap = loadCovers();
        notFoundAlbumKeys = new HashSet<String>();
    }

    // The gracenote URLs change at every request. We match for this provider on
    // the URL prefix only.
    private boolean isBlacklistedCoverUrl(String url, String albumKey) {
        if (!url.contains(GracenoteCover.URL_PREFIX)) {
            return wrongCoverUrlMap.get(albumKey).contains(url);
        } else {
            for (String wrongUrl : wrongCoverUrlMap.get(albumKey)) {
                if (wrongUrl.contains(GracenoteCover.URL_PREFIX)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isLastCoverRetriever(ICoverRetriever retriever) {

        for (int r = 0; r < coverRetrievers.length; r++) {
            if (coverRetrievers[r] == retriever) {
                if (r < coverRetrievers.length - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if device connected or connecting to wifi network
     */
    public boolean isWifi() {
        ConnectivityManager conMan = (ConnectivityManager) app
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get status of wifi connection
        NetworkInfo.State wifi = conMan.getNetworkInfo(1).getState();

        return (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING);
    }

    private Map<String, String> loadCovers() {
        Map<String, String> wrongCovers = null;
        ObjectInputStream objectInputStream = null;

        try {
            File file = new File(getCoverFolder(), COVERS_FILE_NAME);
            objectInputStream = new ObjectInputStream(new FileInputStream(file));
            wrongCovers = (Map<String, String>) objectInputStream.readObject();
        } catch (Exception e) {
            e(CoverManager.class.getSimpleName(), "Cannot load cover history file: " + e);
            wrongCovers = new HashMap<String, String>();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    Log.e(CoverManager.class.getSimpleName(), "Cannot close cover history file : "
                            + e);

                }
            }
        }

        return wrongCovers;
    }

    private MultiMap<String, String> loadWrongCovers() {
        MultiMap<String, String> wrongCovers = null;
        ObjectInputStream objectInputStream = null;

        try {
            File file = new File(getCoverFolder(), WRONG_COVERS_FILE_NAME);
            objectInputStream = new ObjectInputStream(new FileInputStream(file));
            wrongCovers = (MultiMap<String, String>) objectInputStream.readObject();
        } catch (Exception e) {
            e(CoverManager.class.getSimpleName(), "Cannot load cover blacklist : " + e);
            wrongCovers = new MultiMap<String, String>();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    Log.e(CoverManager.class.getSimpleName(), "Cannot close cover blacklist : " + e);

                }
            }
        }

        return wrongCovers;
    }

    private void logQueues() {
        if (DEBUG) {
            d(CoverManager.class.getSimpleName(), "requests queue size : " + requests.size());
            d(CoverManager.class.getSimpleName(),
                    "running request queue size : " + runningRequests.size());
            for (CoverInfo coverInfo : runningRequests) {
                d(CoverManager.class.getSimpleName(), "Running request : " + coverInfo.toString());
            }
            d(CoverManager.class.getSimpleName(), "helpersByCoverInfo map size : "
                    + helpersByCoverInfo.size());
        }
    }

    public void markWrongCover(AlbumInfo albumInfo) {

        CachedCover cacheCoverRetriever;
        String wrongUrl;
        if (DEBUG)
            d(CoverManager.class.getSimpleName(), "Blacklisting cover for " + albumInfo);

        if (!albumInfo.isValid()) {
            Log.w(CoverManager.class.getSimpleName(),
                    "Cannot blacklist cover, missing artist or album : " + albumInfo);
            return;
        }

        wrongUrl = coverUrlMap.get(albumInfo.getKey());
        // Do not blacklist cover if from local storage (url starts with /...)
        if (wrongUrl != null && !wrongUrl.startsWith("/")) {
            if (DEBUG)
                d(CoverManager.class.getSimpleName(), "Cover URL to be blacklisted  " + wrongUrl);

            wrongCoverUrlMap.put(albumInfo.getKey(), wrongUrl);

            cacheCoverRetriever = getCacheRetriever();
            if (cacheCoverRetriever != null) {
                if (DEBUG)
                    d(CoverManager.class.getSimpleName(),
                            "Removing blacklisted cover from cache : ");
                coverUrlMap.remove(albumInfo.getKey());
                cacheCoverRetriever.delete(albumInfo);
            }
        } else {
            Log.w(CoverManager.class.getSimpleName(), "Cannot blacklist the cover for album : "
                    + albumInfo + " because no cover URL has been recorded for it");

        }
    }

    private void notifyListeners(CoverInfo coverInfo) {

        if (helpersByCoverInfo.containsKey(coverInfo)) {
            Iterator<CoverDownloadListener> listenerIterator = helpersByCoverInfo.get(coverInfo)
                    .iterator();
            while (listenerIterator.hasNext()) {
                CoverDownloadListener listener = listenerIterator.next();

                switch (coverInfo.getState()) {
                    case COVER_FOUND:
                        removeRequest(coverInfo);
                        if (DEBUG)
                            d(CoverManager.class.getSimpleName(),
                                    "Cover found for " + coverInfo.getAlbum());
                        listener.onCoverDownloaded(coverInfo);
                        // Do a copy for the other listeners (not to share
                        // bitmaps between views because of the recycling)
                        if (listenerIterator.hasNext()) {
                            coverInfo = new CoverInfo(coverInfo);
                            Bitmap copyBitmap = coverInfo.getBitmap()[0].copy(
                                    coverInfo.getBitmap()[0].getConfig(),
                                    coverInfo.getBitmap()[0].isMutable());
                            coverInfo.setBitmap(new Bitmap[] {
                                    copyBitmap
                            });
                        }
                        break;
                    case COVER_NOT_FOUND:
                        // Re-try the cover art download
                        // if the request has been given up or if the path is
                        // missing (like in artist view)
                        if (!coverInfo.isRequestGivenUp() && !isEmpty(coverInfo.getPath())) {
                            notFoundAlbumKeys.add(coverInfo.getKey());
                        }
                        removeRequest(coverInfo);
                        if (DEBUG)
                            d(CoverManager.class.getSimpleName(), "Cover not found for "
                                    + coverInfo.getAlbum());
                        listener.onCoverNotFound(coverInfo);
                        break;
                    case WEB_COVER_FETCH:
                        listener.onCoverDownloadStarted(coverInfo);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public byte[] readBytes(InputStream inputStream) throws IOException {
        try {
            // this dynamically extends to take the bytes you read
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            // this is storage overwritten on each iteration with bytes
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            // we need to know how may bytes were read to write them to the
            // byteBuffer
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            // and then we can return your byte array.
            return byteBuffer.toByteArray();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    // Remove disc references from albums (like CD1, disc02 ...)
    protected String removeDiscReference(String album) {
        String cleanedAlbum = album.toLowerCase();
        for (String discReference : DISC_REFERENCES) {
            cleanedAlbum = cleanedAlbum.replaceAll(discReference + "\\s*\\d+", " ");
        }
        return cleanedAlbum;
    }

    private void removeRequest(CoverInfo coverInfo) {
        runningRequests.remove(coverInfo);
        helpersByCoverInfo.remove(coverInfo);
        logQueues();
    }

    private void saveCovers() {
        saveCovers(COVERS_FILE_NAME, coverUrlMap);
    }

    private void saveCovers(String fileName, Object object) {
        ObjectOutputStream outputStream = null;
        try {
            File file = new File(getCoverFolder(), fileName);
            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(object);
        } catch (Exception e) {
            e(CoverManager.class.getSimpleName(), "Cannot save covers : " + e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(CoverManager.class.getSimpleName(), "Cannot close cover file : " + e);

                }
            }
        }
    }

    private void saveWrongCovers() {
        saveCovers(WRONG_COVERS_FILE_NAME, wrongCoverUrlMap);
    }

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
                    if (GracenoteCover.isClientIdAvailable(settings)) {
                        this.coverRetrievers[i] = new GracenoteCover(this.settings);
                    }
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
                case ITUNES:
                    this.coverRetrievers[i] = new ItunesCover();
                    break;
            }
        }
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
                enabledRetrievers.add(CoverRetrievers.ITUNES);
                enabledRetrievers.add(CoverRetrievers.DEEZER);
                enabledRetrievers.add(CoverRetrievers.SPOTIFY);
                enabledRetrievers.add(CoverRetrievers.DISCOGS);
                enabledRetrievers.add(CoverRetrievers.GRACENOTE);
                enabledRetrievers.add(CoverRetrievers.MUSICBRAINZ);
            }
        }
        setCoverRetrievers(enabledRetrievers);
    }

    private void stopExecutors() {
        try {
            i(CoverManager.class.getSimpleName(), "Shutting down cover executors");
            active = false;
            this.priorityCoverFetchExecutor.shutdown();
            this.requestExecutor.shutdown();
            this.createBitmapExecutor.shutdown();
            this.coverFetchExecutor.shutdown();
            this.cacheCoverFetchExecutor.shutdown();
        } catch (Exception ex) {
            e(CoverAsyncHelper.class.getSimpleName(), "Failed to shutdown cover executors :" + ex);
        }

    }
}
