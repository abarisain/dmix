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
import com.namelessdev.mpdroid.tools.Tools;

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
import java.util.regex.Pattern;

import static android.text.TextUtils.isEmpty;
import static com.namelessdev.mpdroid.helpers.CoverInfo.STATE.CACHE_COVER_FETCH;
import static com.namelessdev.mpdroid.helpers.CoverInfo.STATE.CREATE_BITMAP;
import static com.namelessdev.mpdroid.helpers.CoverInfo.STATE.WEB_COVER_FETCH;

/**
 */
public final class CoverManager {

    public static final boolean DEBUG = false;

    public static final String PREFERENCE_CACHE = "enableLocalCoverCache";

    public static final String PREFERENCE_LASTFM = "enableLastFM";

    public static final String PREFERENCE_LOCALSERVER = "enableLocalCover";

    public static final String PREFERENCE_ONLY_WIFI = "enableCoverOnlyOnWifi";

    private static final Pattern BLOCK_IN_COMBINING_DIACRITICAL_MARKS =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final String COVERS_FILE_NAME = "covers.bin";

    private static final String[] DISC_REFERENCES = {
            "disc", "cd", "disque"
    };

    private static final String FOLDER_SUFFIX = "/covers/";

    private static final int MAX_REQUESTS = 20;

    private static final String TAG = "CoverManager";

    private static final Pattern TEXT_PATTERN = Pattern.compile("[^\\w .-]+");

    private static final String WRONG_COVERS_FILE_NAME = "wrong-covers.bin";

    private static final MPDApplication sApp = MPDApplication.getInstance();

    private final SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(sApp);

    private static CoverManager sInstance = null;

    private final ExecutorService mCacheCoverFetchExecutor = Executors.newFixedThreadPool(1);

    private final ExecutorService mCreateBitmapExecutor = mCacheCoverFetchExecutor;

    private final ThreadPoolExecutor mCoverFetchExecutor = getCoverFetchExecutor();

    private final MultiMap<CoverInfo, CoverDownloadListener> mHelpersByCoverInfo
            = new MultiMap<>();

    private final ExecutorService mPriorityCoverFetchExecutor = Executors.newFixedThreadPool(1);

    private final ExecutorService mRequestExecutor = Executors.newFixedThreadPool(1);

    private final BlockingDeque<CoverInfo> mRequests = new LinkedBlockingDeque<>();

    private final List<CoverInfo> mRunningRequests = Collections
            .synchronizedList(new ArrayList<CoverInfo>());

    private boolean mActive = true;

    private ICoverRetriever[] mCoverRetrievers = null;

    private Map<String, String> mCoverUrlMap = null;

    private Set<String> mNotFoundAlbumKeys;

    private MultiMap<String, String> mWrongCoverUrlMap = null;

    private CoverManager() {
        super();
        mRequestExecutor.submit(new RequestProcessorTask());
        setCoverRetrieversFromPreferences();
        initializeCoverData();
    }

    /**
     * This method cleans and builds a proper URL object from a string.
     *
     * @param incomingRequest This is the URL in string form.
     * @return A URL Object
     */
    public static URL buildURLForConnection(final String incomingRequest) {
        URL url = null;
        String request = null;

        if (incomingRequest != null) {
            request = incomingRequest.trim();
        }

        if (isEmpty(request)) {
            return null;
        }
        request = request.replace(" ", "%20");

        try {
            url = new URL(request);
        } catch (final MalformedURLException e) {
            Log.w(TAG, "Failed to parse the URL string for URL object generation.", e);
        }

        return url;
    }

    static String cleanGetRequest(final CharSequence text) {
        String processedText = null;

        if (text != null) {
            processedText = TEXT_PATTERN.matcher(text).replaceAll(" ");

            processedText = Normalizer.normalize(processedText, Normalizer.Form.NFD);

            processedText =
                    BLOCK_IN_COMBINING_DIACRITICAL_MARKS.matcher(processedText).replaceAll("");
        }

        return processedText;
    }

    /**
     * This method connects to the HTTP server URL, and gets a HTTP status code. If the
     * status code is OK or similar this method returns true, otherwise false.
     *
     * @param connection An HttpURLConnection object.
     * @return True if the URL exists, false otherwise.
     */
    public static boolean doesUrlExist(final HttpURLConnection connection) {
        int statusCode = 0;

        if (connection == null) {
            Log.d(TAG, "Cannot find out if URL exists with a null connection.");
            return false;
        }

        try {
            statusCode = connection.getResponseCode();
        } catch (final IOException e) {
            if (DEBUG) {
                Log.e(TAG, "Failed to get a valid response code.", e);
            }
        }

        return doesUrlExist(statusCode);
    }

    /**
     * This method connects to the HTTP server URL, gets a HTTP status code and if the
     * status code is OK or similar this method returns true, otherwise false.
     *
     * @param statusCode An HttpURLConnection object.
     * @return True if the URL exists, false otherwise.
     */
    public static boolean doesUrlExist(final int statusCode) {
        final int temporaryRedirect = 307; /** No constant for 307 exists */

        return statusCode == HttpURLConnection.HTTP_OK ||
                statusCode == temporaryRedirect ||
                statusCode == HttpURLConnection.HTTP_MOVED_TEMP;
    }

    private static byte[] download(final String textUrl) {

        final URL url = buildURLForConnection(textUrl);
        final HttpURLConnection connection = getHttpConnection(url);
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = null;
        byte[] buffer = null;
        int len;

        if (!doesUrlExist(connection)) {
            return null;
        }

        /** TODO: After minSdkVersion="19" use try-with-resources here. */
        try {
            bis = new BufferedInputStream(connection.getInputStream(), 8192);
            baos = new ByteArrayOutputStream();
            buffer = new byte[1024];
            while ((len = bis.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            buffer = baos.toByteArray();
        } catch (final Exception e) {
            if (DEBUG) {
                Log.e(TAG, "Failed to download cover.", e);
            }
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close the BufferedInputStream.", e);
                }
            }

            if (baos != null) {
                try {
                    baos.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close the BufferedArrayOutputStream.", e);
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
        return buffer;
    }

    private static byte[] getCoverBytes(final String[] coverUrls, final CoverInfo coverInfo) {

        byte[] coverBytes = null;

        for (final String url : coverUrls) {

            try {
                if (DEBUG) {
                    Log.d(TAG, "Downloading cover (with maxsize " + coverInfo.getCoverMaxSize()
                            + ", " + coverInfo.getCachedCoverMaxSize() + ") for "
                            + coverInfo.getAlbum() + " from " + url);
                }
                if (coverInfo.getState() == CACHE_COVER_FETCH) {

                    coverBytes = readBytes(new URL("file://" + url).openStream());

                } else if (coverInfo.getState() == WEB_COVER_FETCH) {
                    coverBytes = download(url);
                }
                if (coverBytes != null) {
                    if (DEBUG) {
                        Log.d(TAG, "Cover downloaded for " + coverInfo.getAlbum() + " from " + url
                                + ", size=" + coverBytes.length);
                    }
                    break;
                }
            } catch (final Exception e) {
                Log.w(TAG, "Cover get bytes failure.", e);
            }
        }
        return coverBytes;
    }

    private static ThreadPoolExecutor getCoverFetchExecutor() {
        return new ThreadPoolExecutor(2, 2, 0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    public static String getCoverFileName(final AlbumInfo albumInfo) {
        return albumInfo.getKey() + ".jpg";
    }

    static String getCoverFolder() {
        final File cacheDir = sApp.getExternalCacheDir();
        if (cacheDir == null) {
            return null;
        }
        return cacheDir.getAbsolutePath() + FOLDER_SUFFIX;
    }

    /**
     * This method takes a URL object and returns a HttpURLConnection object.
     *
     * @param url The URL object used to create the connection.
     * @return The connection which is returned; ensure this resource is disconnected after use.
     */
    public static HttpURLConnection getHttpConnection(final URL url) {
        HttpURLConnection connection = null;

        if (url == null) {
            if (DEBUG) {
                Log.d(TAG, "Cannot create a connection with a null URL");
            }
            return null;
        }

        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            Log.w(TAG, "Failed to execute cover get request.", e);
        }

        if (connection != null) {
            connection.setUseCaches(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
        }

        return connection;
    }

    public static synchronized CoverManager getInstance() {
        if (sInstance == null) {
            sInstance = new CoverManager();
        }
        return sInstance;
    }

    static AlbumInfo getNormalizedAlbumInfo(final AlbumInfo albumInfo) {
        final String artist = cleanGetRequest(albumInfo.getArtist());
        String album = cleanGetRequest(albumInfo.getAlbum());
        album = removeDiscReference(album);

        return new AlbumInfo(album, artist, albumInfo.getPath(), albumInfo.getFilename());
    }

    /**
     * Checks if device connected or connecting to wifi network
     */
    static boolean isWifi() {
        final ConnectivityManager conMan = (ConnectivityManager) sApp
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get status of wifi connection
        final NetworkInfo.State wifi = conMan.getNetworkInfo(1).getState();

        return (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING);
    }

    private static Map<String, String> loadCovers() {
        Map<String, String> wrongCovers = null;
        ObjectInputStream objectInputStream = null;

        try {
            final File file = new File(getCoverFolder(), COVERS_FILE_NAME);
            objectInputStream = new ObjectInputStream(new FileInputStream(file));
            wrongCovers = (Map<String, String>) objectInputStream.readObject();
        } catch (final Exception e) {
            Log.e(TAG, "Cannot load cover history file.", e);
            wrongCovers = new HashMap<>();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close cover history file.", e);

                }
            }
        }

        return wrongCovers;
    }

    private static MultiMap<String, String> loadWrongCovers() {
        MultiMap<String, String> wrongCovers = null;
        ObjectInputStream objectInputStream = null;

        try {
            final File file = new File(getCoverFolder(), WRONG_COVERS_FILE_NAME);
            objectInputStream = new ObjectInputStream(new FileInputStream(file));
            wrongCovers = (MultiMap<String, String>) objectInputStream.readObject();
        } catch (final Exception e) {
            Log.e(TAG, "Cannot load cover blacklist.", e);
            wrongCovers = new MultiMap<>();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close cover blacklist.", e);

                }
            }
        }

        return wrongCovers;
    }

    static byte[] readBytes(final InputStream inputStream) throws IOException {
        try {
            // this dynamically extends to take the bytes you read
            final ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            // this is storage overwritten on each iteration with bytes
            final int bufferSize = 1024;
            final byte[] buffer = new byte[bufferSize];

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
    static String removeDiscReference(final String album) {
        String cleanedAlbum = album.toLowerCase();
        for (final String discReference : DISC_REFERENCES) {
            cleanedAlbum = cleanedAlbum.replaceAll(discReference + "\\s*\\d+", " ");
        }
        return cleanedAlbum;
    }

    private static void saveCovers(final String fileName, final Object object) {
        ObjectOutputStream outputStream = null;
        try {
            final File file = new File(getCoverFolder(), fileName);
            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(object);
        } catch (final Exception e) {
            Log.e(TAG, "Cannot save covers.", e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close cover file.", e);

                }
            }
        }
    }

    /**
     * This method connects to the HTTP server URL, and gets a HTTP status code. If the
     * status code is OK or similar this method returns true, otherwise false.
     *
     * @param connection An HttpURLConnection object.
     * @return True if the URL exists, false otherwise.
     */
    public static boolean urlExists(final HttpURLConnection connection) {
        int statusCode = 0;

        if (connection == null) {
            Log.d(TAG, "Cannot find out if URL exists with a null connection.");
            return false;
        }

        try {
            statusCode = connection.getResponseCode();
        } catch (final IOException e) {
            if (DEBUG) {
                Log.e(TAG, "Failed to get a valid response code.", e);
            }
        }

        return urlExists(statusCode);
    }

    /**
     * This method connects to the HTTP server URL, gets a HTTP status code and if the
     * status code is OK or similar this method returns true, otherwise false.
     *
     * @param statusCode An HttpURLConnection object.
     * @return True if the URL exists, false otherwise.
     */
    public static boolean urlExists(final int statusCode) {
        final int temporaryRedirect = 307; /** No constant for 307 exists */

        return statusCode == HttpURLConnection.HTTP_OK ||
                statusCode == temporaryRedirect ||
                statusCode == HttpURLConnection.HTTP_MOVED_TEMP;
    }

    public void addCoverRequest(final CoverInfo coverInfo) {
        if (DEBUG) {
            Log.d(TAG, "Looking for cover with artist=" + coverInfo.getArtist() + ", album="
                    + coverInfo.getAlbum());
        }
        mRequests.add(coverInfo);
    }

    public void clear() {
        final CachedCover cachedCover = getCacheRetriever();
        if (cachedCover != null) {
            cachedCover.clear();
        }
        initializeCoverData();
    }

    public void clear(final AlbumInfo albumInfo) {

        final CachedCover cachedCover = getCacheRetriever();
        if (cachedCover != null) {
            cachedCover.delete(albumInfo);
        }
        mCoverUrlMap.remove(albumInfo);
        mWrongCoverUrlMap.remove(albumInfo.getKey());
        mNotFoundAlbumKeys.remove(albumInfo.getKey());
    }

    @Override
    protected void finalize() throws Throwable {
        stopExecutors();
        sInstance = null;
        super.finalize();
    }

    private CachedCover getCacheRetriever() {
        for (final ICoverRetriever retriever : mCoverRetrievers) {
            if (retriever instanceof CachedCover && retriever.isCoverLocal()) {
                return (CachedCover) retriever;
            }
        }
        return null;
    }

    private CoverInfo getExistingRequest(final CoverInfo coverInfo) {
        return mRunningRequests.get(mRunningRequests.indexOf(coverInfo));
    }

    private void initializeCoverData() {
        mWrongCoverUrlMap = loadWrongCovers();
        mCoverUrlMap = loadCovers();
        mNotFoundAlbumKeys = new HashSet<>();
    }

    // The gracenote URLs change at every request. We match for this provider on
    // the URL prefix only.
    private boolean isBlacklistedCoverUrl(final String url, final String albumKey) {
        if (url.contains(GracenoteCover.URL_PREFIX)) {
            for (final String wrongUrl : mWrongCoverUrlMap.get(albumKey)) {
                if (wrongUrl.contains(GracenoteCover.URL_PREFIX)) {
                    return true;
                }
            }
            return false;
        } else {
            return mWrongCoverUrlMap.get(albumKey).contains(url);
        }
    }

    private boolean isLastCoverRetriever(final ICoverRetriever retriever) {

        for (int r = 0; r < mCoverRetrievers.length; r++) {
            if (mCoverRetrievers[r].equals(retriever)) {
                if (r < mCoverRetrievers.length - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private void logQueues() {
        if (DEBUG) {
            Log.d(TAG, "requests queue size : " + mRequests.size());
            Log.d(TAG, "running request queue size : " + mRunningRequests.size());
            for (final CoverInfo coverInfo : mRunningRequests) {
                Log.d(TAG, "Running request : " + coverInfo);
            }
            Log.d(TAG, "helpersByCoverInfo map size : " + mHelpersByCoverInfo.size());
        }
    }

    public void markWrongCover(final AlbumInfo albumInfo) {

        final CachedCover cacheCoverRetriever;
        final String wrongUrl;
        if (DEBUG) {
            Log.d(TAG, "Blacklisting cover for " + albumInfo);
        }

        if (!albumInfo.isValid()) {
            Log.w(TAG, "Cannot blacklist cover, missing artist or album : " + albumInfo);
            return;
        }

        wrongUrl = mCoverUrlMap.get(albumInfo.getKey());
        // Do not blacklist cover if from local storage (url starts with /...)
        if (wrongUrl != null && !wrongUrl.startsWith("/")) {
            if (DEBUG) {
                Log.d(TAG, "Cover URL to be blacklisted  " + wrongUrl);
            }

            mWrongCoverUrlMap.put(albumInfo.getKey(), wrongUrl);

            cacheCoverRetriever = getCacheRetriever();
            if (cacheCoverRetriever != null) {
                if (DEBUG) {
                    Log.d(TAG, "Removing blacklisted cover from cache : ");
                }
                mCoverUrlMap.remove(albumInfo.getKey());
                cacheCoverRetriever.delete(albumInfo);
            }
        } else {
            Log.w(TAG, "Cannot blacklist the cover for album : " + albumInfo
                    + " because no cover URL has been recorded for it");

        }
    }

    private void notifyListeners(CoverInfo coverInfo) {

        if (mHelpersByCoverInfo.containsKey(coverInfo)) {
            final Iterator<CoverDownloadListener> listenerIterator = mHelpersByCoverInfo
                    .get(coverInfo)
                    .iterator();
            while (listenerIterator.hasNext()) {
                final CoverDownloadListener listener = listenerIterator.next();

                switch (coverInfo.getState()) {
                    case COVER_FOUND:
                        removeRequest(coverInfo);
                        if (DEBUG) {
                            Log.d(TAG, "Cover found for " + coverInfo.getAlbum());
                        }
                        listener.onCoverDownloaded(coverInfo);
                        // Do a copy for the other listeners (not to share
                        // bitmaps between views because of the recycling)
                        if (listenerIterator.hasNext()) {
                            coverInfo = new CoverInfo(coverInfo);
                            final Bitmap copyBitmap = coverInfo.getBitmap()[0].copy(
                                    coverInfo.getBitmap()[0].getConfig(),
                                    coverInfo.getBitmap()[0].isMutable());
                            coverInfo.setBitmap(new Bitmap[]{
                                    copyBitmap
                            });
                        }
                        break;
                    case COVER_NOT_FOUND:
                        // Re-try the cover art download
                        // if the request has been given up or if the path is
                        // missing (like in artist view)
                        if (!coverInfo.isRequestGivenUp() && !isEmpty(coverInfo.getPath())) {
                            mNotFoundAlbumKeys.add(coverInfo.getKey());
                        }
                        removeRequest(coverInfo);
                        if (DEBUG) {
                            Log.d(TAG, "Cover not found for " + coverInfo.getAlbum());
                        }
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

    private void removeRequest(final CoverInfo coverInfo) {
        mRunningRequests.remove(coverInfo);
        mHelpersByCoverInfo.remove(coverInfo);
        logQueues();
    }

    private void saveCovers() {
        saveCovers(COVERS_FILE_NAME, mCoverUrlMap);
    }

    private void saveWrongCovers() {
        saveCovers(WRONG_COVERS_FILE_NAME, mWrongCoverUrlMap);
    }

    void setCoverRetrievers(final List<CoverRetrievers> whichCoverRetrievers) {
        if (whichCoverRetrievers == null) {
            mCoverRetrievers = new ICoverRetriever[0];
        }
        mCoverRetrievers = new ICoverRetriever[whichCoverRetrievers.size()];
        for (int i = 0; i < whichCoverRetrievers.size(); i++) {
            switch (whichCoverRetrievers.get(i)) {
                case CACHE:
                    mCoverRetrievers[i] = new CachedCover();
                    break;
                case LASTFM:
                    mCoverRetrievers[i] = new LastFMCover();
                    break;
                case LOCAL:
                    mCoverRetrievers[i] = new LocalCover();
                    break;
                case GRACENOTE:
                    if (GracenoteCover.isClientIdAvailable()) {
                        mCoverRetrievers[i] = new GracenoteCover();
                    }
                    break;
                case DEEZER:
                    mCoverRetrievers[i] = new DeezerCover();
                    break;
                case MUSICBRAINZ:
                    mCoverRetrievers[i] = new MusicBrainzCover();
                    break;
                case DISCOGS:
                    mCoverRetrievers[i] = new DiscogsCover();
                    break;
                case SPOTIFY:
                    mCoverRetrievers[i] = new SpotifyCover();
                    break;
                case ITUNES:
                    mCoverRetrievers[i] = new ItunesCover();
                    break;
            }
        }
    }

    public void setCoverRetrieversFromPreferences() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(sApp);
        final List<CoverRetrievers> enabledRetrievers = new ArrayList<>();
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
            Log.i(TAG, "Shutting down cover executors");
            mActive = false;
            mPriorityCoverFetchExecutor.shutdown();
            mRequestExecutor.shutdown();
            mCreateBitmapExecutor.shutdown();
            mCoverFetchExecutor.shutdown();
            mCacheCoverFetchExecutor.shutdown();
        } catch (final Exception ex) {
            Log.e(TAG, "Failed to shutdown cover executors.", ex);
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
        SPOTIFY,
        ITUNES
    }

    private class CreateBitmapTask implements Runnable {

        private final CoverInfo mCoverInfo;

        private CreateBitmapTask(final CoverInfo coverInfo) {
            super();
            mCoverInfo = coverInfo;
        }

        @Override
        public void run() {

            final Bitmap[] bitmaps;

            if (DEBUG) {
                Log.d(TAG, "Making cover bitmap for " + mCoverInfo.getAlbum());
            }

            if (mCoverInfo.getCoverRetriever().isCoverLocal()) {
                int maxSize = mCoverInfo.getCoverMaxSize();
                if (mCoverInfo.getCachedCoverMaxSize() != CoverInfo.MAX_SIZE) {
                    maxSize = mCoverInfo.getCachedCoverMaxSize();
                }
                if (maxSize == CoverInfo.MAX_SIZE) {
                    bitmaps = new Bitmap[]{
                            BitmapFactory.decodeByteArray(mCoverInfo.getCoverBytes(), 0,
                                    mCoverInfo.getCoverBytes().length)
                    };
                    mCoverInfo.setBitmap(bitmaps);
                } else {
                    bitmaps = new Bitmap[]{
                            Tools.decodeSampledBitmapFromBytes(mCoverInfo.getCoverBytes(), maxSize,
                                    maxSize, false)
                    };
                    mCoverInfo.setBitmap(bitmaps);
                }
            } else {
                final BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(mCoverInfo.getCoverBytes(), 0,
                        mCoverInfo.getCoverBytes().length, o);

                int scale = 1;
                if (mCoverInfo.getCoverMaxSize() != CoverInfo.MAX_SIZE
                        || o.outHeight > mCoverInfo.getCoverMaxSize()
                        || o.outWidth > mCoverInfo.getCoverMaxSize()) {
                    scale = (int) Math.pow(2.0,
                            (double) (int) Math.round(Math.log(
                                    (double) mCoverInfo.getCoverMaxSize() /
                                            (double) Math.max(o.outHeight, o.outWidth)) / Math
                                    .log(0.5)));
                }

                o.inSampleSize = 1;
                o.inJustDecodeBounds = false;
                final Bitmap fullBmp = BitmapFactory.decodeByteArray(mCoverInfo.getCoverBytes(), 0,
                        mCoverInfo.getCoverBytes().length, o);
                final Bitmap bmp;
                if (scale == 1) {
                    // This can cause some problem (a bitmap being freed will
                    // free both references)
                    // But the only use is to save it in the cache so it's okay.
                    bmp = fullBmp;
                } else {
                    o.inSampleSize = scale;
                    o.inJustDecodeBounds = false;
                    bmp = BitmapFactory.decodeByteArray(mCoverInfo.getCoverBytes(), 0,
                            mCoverInfo.getCoverBytes().length, o);
                }
                bitmaps = new Bitmap[]{
                        bmp, fullBmp
                };
                mCoverInfo.setBitmap(bitmaps);
                mCoverInfo.setCoverBytes(null);

                final ICoverRetriever cacheRetriever;
                cacheRetriever = getCacheRetriever();
                if (cacheRetriever != null && !mCoverInfo.getCoverRetriever()
                        .equals(cacheRetriever)) {
                    if (DEBUG) {
                        Log.i(TAG, "Saving cover art to cache");
                    }
                    // Save the fullsize bitmap
                    getCacheRetriever().save(mCoverInfo, fullBmp);

                    // Release the cover immediately if not used
                    if (!bitmaps[0].equals(bitmaps[1])) {
                        bitmaps[1].recycle();
                        bitmaps[1] = null;
                    }
                }
            }

            mRequests.addLast(mCoverInfo);
        }

    }

    private class FetchCoverTask implements Runnable

    {

        private final CoverInfo mCoverInfo;

        private FetchCoverTask(final CoverInfo coverInfo) {
            super();
            mCoverInfo = coverInfo;
        }

        @Override
        public void run() {
            String[] coverUrls;
            boolean remote;
            boolean local;
            boolean canStart = true;
            byte[] coverBytes;

            if (mCoverInfo.getState() != WEB_COVER_FETCH
                    || mCoverFetchExecutor.getQueue().size() < MAX_REQUESTS) {

                // If the coverretriever is defined in the coverInfo
                // that means that a previous cover fetch failed with this
                // retriever
                // We just start after this retriever to try a cover.
                if (mCoverInfo.getCoverRetriever() != null) {
                    canStart = false;
                }

                for (final ICoverRetriever coverRetriever : mCoverRetrievers) {
                    try {

                        if (coverRetriever == null) {
                            continue;
                        }

                        if (canStart) {

                            remote = mCoverInfo.getState() == WEB_COVER_FETCH
                                    && !coverRetriever.isCoverLocal();
                            local = mCoverInfo.getState() == CACHE_COVER_FETCH
                                    && coverRetriever.isCoverLocal();
                            if (remote || local) {
                                if (DEBUG) {
                                    Log.d(TAG, "Looking for cover "
                                            + mCoverInfo.getArtist() + ", " + mCoverInfo.getAlbum()
                                            + " with " + coverRetriever.getName());
                                }
                                mCoverInfo.setCoverRetriever(coverRetriever);
                                coverUrls = coverRetriever.getCoverUrl(mCoverInfo);

                                // Normalize (remove special characters ...) the
                                // artist and album names if no result has been
                                // found.
                                if (!(coverUrls != null && coverUrls.length > 0)
                                        && remote
                                        && !(coverRetriever.getName()
                                        .equals(LocalCover.RETRIEVER_NAME))) {
                                    final AlbumInfo normalizedAlbumInfo = getNormalizedAlbumInfo(
                                            mCoverInfo);
                                    if (!normalizedAlbumInfo.equals(mCoverInfo)) {
                                        if (DEBUG) {
                                            Log.d(TAG,
                                                    "Retry to fetch cover with normalized names for "
                                                            + normalizedAlbumInfo);
                                        }
                                        coverUrls = coverRetriever.getCoverUrl(normalizedAlbumInfo);
                                    }
                                }

                                if (coverUrls != null && coverUrls.length > 0) {
                                    final List<String> wrongUrlsForCover =
                                            mWrongCoverUrlMap.get(mCoverInfo.getKey());

                                    if (wrongUrlsForCover == null
                                            || !isBlacklistedCoverUrl(coverUrls[0],
                                            mCoverInfo.getKey())) {

                                        if (DEBUG) {
                                            Log.d(TAG, "Cover found for  " + mCoverInfo.getAlbum()
                                                    + " with " + coverRetriever.getName()
                                                    + " : " + coverUrls[0]);
                                        }
                                        coverBytes = getCoverBytes(coverUrls, mCoverInfo);
                                        if (coverBytes != null && coverBytes.length > 0) {
                                            if (!coverRetriever.isCoverLocal()) {
                                                mCoverUrlMap.put(mCoverInfo.getKey(), coverUrls[0]);
                                            }
                                            mCoverInfo.setCoverBytes(coverBytes);
                                            mRequests.addLast(mCoverInfo);
                                            return;
                                        } else {
                                            if (DEBUG) {
                                                Log.d(TAG, "The cover URL for album "
                                                        + mCoverInfo.getAlbum()
                                                        + " did not work : "
                                                        + coverRetriever.getName());
                                            }
                                        }

                                    } else {
                                        if (DEBUG) {
                                            Log.d(TAG, "Blacklisted cover url found for "
                                                    + mCoverInfo.getAlbum() + " : "
                                                    + coverUrls[0]);
                                        }
                                    }
                                }

                            }
                        } else {
                            if (DEBUG) {
                                Log.d(TAG, "Bypassing the retriever " + coverRetriever.getName()
                                        + " for album " + mCoverInfo.getAlbum()
                                        + ", already asked.");
                            }
                            canStart = coverRetriever.equals(mCoverInfo.getCoverRetriever());
                        }

                    } catch (final Exception e) {
                        Log.e(TAG, "Fetch cover failure.", e);
                    }

                }
            } else {
                mCoverInfo.setRequestGivenUp(true);
                Log.w(TAG, "Too many requests, giving up this one : " + mCoverInfo.getAlbum());
            }

            mRequests.addLast(mCoverInfo);
        }

    }

    private class RequestProcessorTask implements Runnable {

        @Override
        public void run() {

            CoverInfo coverInfo;

            while (mActive) {

                try {
                    coverInfo = mRequests.take();

                    if (coverInfo == null || coverInfo.getListener() == null) {
                        return;
                    }

                    switch (coverInfo.getState()) {
                        case NEW:
                            // Do not create a new request if a similar one
                            // already exists
                            // Just register the new cover listener and update
                            // the request priority.
                            mHelpersByCoverInfo.put(coverInfo, coverInfo.getListener());
                            if (mRunningRequests.contains(coverInfo)) {
                                final CoverInfo existingRequest = getExistingRequest(coverInfo);
                                existingRequest.setPriority(existingRequest.isPriority()
                                        || coverInfo.isPriority());
                                notifyListeners(existingRequest);
                                break;
                            } else {

                                if (!coverInfo.isValid()
                                        || mNotFoundAlbumKeys.contains(coverInfo.getKey())) {
                                    if (DEBUG) {
                                        Log.d(TAG, "Incomplete cover request or already not found "
                                                + "cover with artist=" + coverInfo.getArtist()
                                                + ", album=" + coverInfo.getAlbum());
                                    }
                                    coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                                    notifyListeners(coverInfo);
                                } else {
                                    mRunningRequests.add(coverInfo);
                                    coverInfo.setState(CACHE_COVER_FETCH);
                                    mCacheCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                }
                                break;

                            }
                        case CACHE_COVER_FETCH:
                            if (coverInfo.getCoverBytes() == null
                                    || coverInfo.getCoverBytes().length == 0) {
                                coverInfo.setState(WEB_COVER_FETCH);
                                notifyListeners(coverInfo);
                                if (coverInfo.isPriority()) {
                                    mPriorityCoverFetchExecutor
                                            .submit(new FetchCoverTask(coverInfo));
                                } else {
                                    mCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                }
                                break;
                            } else {
                                coverInfo.setState(CREATE_BITMAP);
                                mCreateBitmapExecutor.submit(new CreateBitmapTask(coverInfo));
                                break;
                            }
                        case WEB_COVER_FETCH:
                            if (coverInfo.getCoverBytes() != null
                                    && coverInfo.getCoverBytes().length > 0) {
                                coverInfo.setState(CREATE_BITMAP);
                                notifyListeners(coverInfo);
                                mCreateBitmapExecutor.submit(new CreateBitmapTask(coverInfo));
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
                                if (DEBUG) {
                                    Log.d(TAG,
                                            "The cover has not been downloaded correctly for album "
                                                    + coverInfo.getAlbum()
                                                    + " with this retriever : "
                                                    + coverInfo.getCoverRetriever()
                                                    + ", trying the next ones ...");
                                }
                                mCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                            } else {
                                coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                                notifyListeners(coverInfo);
                            }
                            break;
                        default:
                            Log.e(TAG, "Unknown request : " + coverInfo);
                            coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                            notifyListeners(coverInfo);
                            break;
                    }

                    if (mRunningRequests.isEmpty()) {
                        saveCovers();
                        saveWrongCovers();
                    }

                } catch (final Exception e) {
                    if (DEBUG) {
                        Log.e(TAG, "Cover request processing failure.", e);
                    }
                }
            }

        }
    }
}
