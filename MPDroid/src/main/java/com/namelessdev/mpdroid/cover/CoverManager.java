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

package com.namelessdev.mpdroid.cover;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.retriever.CachedCover;
import com.namelessdev.mpdroid.cover.retriever.DeezerCover;
import com.namelessdev.mpdroid.cover.retriever.GracenoteCover;
import com.namelessdev.mpdroid.cover.retriever.ICoverRetriever;
import com.namelessdev.mpdroid.cover.retriever.ItunesCover;
import com.namelessdev.mpdroid.cover.retriever.JamendoCover;
import com.namelessdev.mpdroid.cover.retriever.LastFMCover;
import com.namelessdev.mpdroid.cover.retriever.LocalCover;
import com.namelessdev.mpdroid.cover.retriever.CoverArtArchiveCover;
import com.namelessdev.mpdroid.cover.retriever.SpotifyCover;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
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
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.concurrent.ThreadPoolExecutor;

import static com.namelessdev.mpdroid.cover.CoverInfo.STATE.CACHE_COVER_FETCH;
import static com.namelessdev.mpdroid.cover.CoverInfo.STATE.CREATE_BITMAP;
import static com.namelessdev.mpdroid.cover.CoverInfo.STATE.WEB_COVER_FETCH;

public final class CoverManager {

    public static final boolean DEBUG = false;

    public static final String FOLDER_SUFFIX = "/covers/";

    public static final String PREFERENCE_CACHE = "enableLocalCoverCache";

    public static final String PREFERENCE_LASTFM = "enableLastFM";

    public static final String PREFERENCE_LOCALSERVER = "enableLocalCover";

    private static final MPDApplication APP = MPDApplication.getInstance();

    private static final String COVERS_FILE_NAME = "covers.bin";

    private static final String PREFERENCE_ONLY_WIFI = "enableCoverOnlyOnWifi";

    private static final String TAG = "CoverManager";

    private static final String WRONG_COVERS_FILE_NAME = "wrong-covers.bin";

    private static CoverManager sInstance;

    private final ExecutorService mCacheCoverFetchExecutor = Executors.newSingleThreadExecutor();

    private final ThreadPoolExecutor mCoverFetchExecutor =
            (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    /**
     * This Collection stores all enabled cover retrievers.
     */
    private final List<ICoverRetriever> mCoverRetrievers = new ArrayList<>();

    private final HashMap<String, String> mCoverUrlMap = new HashMap<>();

    private final ExecutorService mCreateBitmapExecutor = mCacheCoverFetchExecutor;

    private final Map<CoverInfo, Collection<CoverDownloadListener>> mHelpersByCoverInfo
            = new HashMap<>();

    private final Set<String> mNotFoundAlbumKeys = new HashSet<>();

    private final ExecutorService mPriorityCoverFetchExecutor = Executors.newSingleThreadExecutor();

    private final ExecutorService mRequestExecutor = Executors.newSingleThreadExecutor();

    private final BlockingDeque<CoverInfo> mRequests = new LinkedBlockingDeque<>();

    private final List<CoverInfo> mRunningRequests = Collections
            .synchronizedList(new ArrayList<CoverInfo>());

    private final HashMap<String, Collection<String>> mWrongCoverUrlMap = new HashMap<>();

    private boolean mActive = true;

    private CoverManager() {
        super();

        mRequestExecutor.submit(new RequestProcessorTask());
        setCoverRetrieversFromPreferences();
        initializeCoverData();
    }

    /**
     * This method connects to the HTTP server URL, and gets a HTTP status code. If the status code
     * is OK or similar this method returns true, otherwise false.
     *
     * @param connection An HttpURLConnection object.
     * @return True if the URL exists, false otherwise.
     * @throws IOException Upon error retrieving a response code.
     */
    public static boolean doesUrlExist(final HttpURLConnection connection) throws IOException {
        final boolean doesUrlExist;

        if (connection == null) {
            Log.d(TAG, "Cannot find out if URL exists with a null connection.");
            doesUrlExist = false;
        } else {
            doesUrlExist = doesUrlExist(connection.getResponseCode());
        }

        return doesUrlExist;
    }

    /**
     * This method connects to the HTTP server URL, gets a HTTP status code and if the status code
     * is OK or similar this method returns true, otherwise false.
     *
     * @param statusCode An HttpURLConnection object.
     * @return True if the URL exists, false otherwise.
     */
    private static boolean doesUrlExist(final int statusCode) {
        final int temporaryRedirect = 307; /** No constant for 307 exists */

        return statusCode == HttpURLConnection.HTTP_OK ||
                statusCode == temporaryRedirect ||
                statusCode == HttpURLConnection.HTTP_MOVED_TEMP;
    }

    private static String getCoverFolder() {
        final File cacheDir = APP.getExternalCacheDir();
        final String coverFolder;

        if (cacheDir == null) {
            coverFolder = null;
        } else {
            coverFolder = cacheDir.getAbsolutePath() + FOLDER_SUFFIX;
        }

        return coverFolder;
    }

    /**
     * Returns a file from the base directory and filename.
     *
     * @param dirPath The base directory to get the {@code File} from.
     * @param name    The filename to get the {@code File} from.
     * @return A {@code File}.
     * @throws IOException If there was a problem creating a file from the parameters.
     */
    private static File getFile(final String dirPath, final String name)
            throws IOException {
        final File file = new File(dirPath, name);

        if (!file.exists()) {
            final File parent = file.getParentFile();

            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    Log.e(TAG, "Failed to create parent directories.");
                }
            }

            if (!file.createNewFile()) {
                Log.e(TAG, "Failed to create file: " + name);
            }
        }

        return file;
    }

    public static synchronized CoverManager getInstance() {
        if (sInstance == null) {
            sInstance = new CoverManager();
        }

        return sInstance;
    }

    /**
     * Checks if device connected to a WIFI network.
     *
     * <p>On Android SDK 21 and later this sets the first WIFI network found as the
     * default.</p>
     *
     * @return True if this device is connected to a WIFI network, false otherwise.
     */
    private static boolean isWifi() {
        final ConnectivityManager connectivityManager =
                (ConnectivityManager) APP.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifi = false;

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final Network[] networks = connectivityManager.getAllNetworks();

                for (final Network network : networks) {
                    if (isWifi(connectivityManager.getNetworkInfo(network))) {
                        /**
                         * Using non-depreciated method causes MethodNotFoundException on
                         * Android 5.1.1.
                         */
                        isWifi = ConnectivityManager.setProcessDefaultNetwork(network);
                        break;
                    }
                }
            } else {
                if (isWifi(connectivityManager.getNetworkInfo(1))) {
                    isWifi = true;
                }
            }
        }

        if (DEBUG) {
            final StringBuilder stringBuilder = new StringBuilder("Wifi network required and ");

            if (!isWifi) {
                stringBuilder.append("not ");
            }

            stringBuilder.append("found.");
            Log.d(TAG, stringBuilder.toString());
        }

        return isWifi;
    }

    /**
     * Checks if device connected to a WIFI network.
     *
     * @param networkInfo The {@link NetworkInfo} to check for connectivity.
     * @return True if this device is connected or connecting to a WIFI network, false otherwise.
     */
    private static boolean isWifi(final NetworkInfo networkInfo) {
        final boolean isWifiAndConnected;

        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                networkInfo.getState() == NetworkInfo.State.CONNECTED) {
            isWifiAndConnected = true;
        } else {
            isWifiAndConnected = false;
        }

        return isWifiAndConnected;
    }

    /**
     * This method loads a serialized object file into a raw {@link Map}.
     *
     * <p>This method suppresses raw types and unchecked warnings due to the required
     * serialization.</p>
     *
     * @param map      The map to put all objects from serialized file into.
     * @param filename The filename of the serialized object file.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void loadMapFromFile(final Map map, final String filename) {
        ObjectInputStream objectInputStream = null;

        try {
            final File file = getFile(getCoverFolder(), filename);
            final FileInputStream fis = new FileInputStream(file);

            if (fis.available() == 0) {
                fis.close();
                objectInputStream = null;
            } else {
                objectInputStream = new ObjectInputStream(fis);
                final Object oisObject = objectInputStream.readObject();

                map.putAll((Map) oisObject);
            }
        } catch (final ClassNotFoundException | IOException ignored) {
            Log.e(TAG, "Error loading file, removing.");
            new File(getCoverFolder(), WRONG_COVERS_FILE_NAME).delete();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close cover blacklist.", e);

                }
            }
        }
    }

    /**
     * This method creates stores a value in a list in a value of a key.
     *
     * @param map   The {@link Map} to store the value's list.
     * @param key   The key to store the value's list in.
     * @param value The value to store in a list in the {@link Map}.
     * @param <T>   The Map type.
     * @param <K>   The Map's key type.
     * @param <V>   The Map's list value type.
     */
    private static <T extends Map<K, Collection<V>>, K, V> void mapCollectionValue(final T map,
            final K key, final V value) {
        Collection<V> valueList = map.get(key);

        if (valueList == null) {
            valueList = new ArrayList<>();
            map.put(key, valueList);
        }

        valueList.add(value);
    }

    public void addCoverRequest(final CoverInfo coverInfo) {
        if (DEBUG) {
            Log.d(TAG, "Looking for cover with artist=" + coverInfo.getArtistName() + ", album="
                    + coverInfo.getAlbumName());
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
        CachedCover cachedRetriever = null;

        for (final ICoverRetriever retriever : mCoverRetrievers) {
            if (retriever instanceof CachedCover && retriever.isCoverLocal()) {
                cachedRetriever = (CachedCover) retriever;
            }
        }

        return cachedRetriever;
    }

    private void initializeCoverData() {
        mCoverUrlMap.clear();
        mWrongCoverUrlMap.clear();

        loadMapFromFile(mCoverUrlMap, COVERS_FILE_NAME);
        loadMapFromFile(mWrongCoverUrlMap, WRONG_COVERS_FILE_NAME);
        mNotFoundAlbumKeys.clear();
    }

    public void markWrongCover(final AlbumInfo albumInfo) {
        final CachedCover cacheCoverRetriever;
        final String wrongUrl;
        if (DEBUG) {
            Log.d(TAG, "Blacklisting cover for " + albumInfo);
        }

        if (albumInfo.isValid()) {
            wrongUrl = mCoverUrlMap.get(albumInfo.getKey());
            // Do not blacklist cover if from local storage (url starts with /...)
            if (wrongUrl != null && !wrongUrl.startsWith("/")) {
                if (DEBUG) {
                    Log.d(TAG, "Cover URL to be blacklisted  " + wrongUrl);
                }

                mapCollectionValue(mWrongCoverUrlMap, albumInfo.getKey(), wrongUrl);

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
        } else {
            Log.w(TAG, "Cannot blacklist cover, missing artist or album : " + albumInfo);
        }
    }

    public void setCoverRetrieversFromPreferences() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(APP);
        mCoverRetrievers.clear();

        // There is a cover provider order, respect it.
        // Cache -> MPD Server -> LastFM
        if (settings.getBoolean(PREFERENCE_CACHE, true)) {
            mCoverRetrievers.add(new CachedCover());
        }
        if (!settings.getBoolean(PREFERENCE_ONLY_WIFI, false) || isWifi()) {
            if (settings.getBoolean(PREFERENCE_LOCALSERVER, false)) {
                mCoverRetrievers.add(new LocalCover());
            }
            if (settings.getBoolean(PREFERENCE_LASTFM, true)) {
                mCoverRetrievers.add(new LastFMCover());
                mCoverRetrievers.add(new ItunesCover());
                mCoverRetrievers.add(new DeezerCover());
                mCoverRetrievers.add(new SpotifyCover());
                if (GracenoteCover.isClientIdAvailable()) {
                    mCoverRetrievers.add(new GracenoteCover());
                }
                mCoverRetrievers.add(new CoverArtArchiveCover());
                mCoverRetrievers.add(new JamendoCover());
            }
        }
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

    private final class CreateBitmapTask implements Runnable {

        private final CoverInfo mCoverInfo;

        private CreateBitmapTask(final CoverInfo coverInfo) {
            super();
            mCoverInfo = coverInfo;
        }

        private int calculateInSampleSize(final BitmapFactory.Options options, final int reqWidth,
                final int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                // Calculate ratios of height and width to requested height and
                // width
                final int heightRatio = Math.round((float) height / (float) reqHeight);
                final int widthRatio = Math.round((float) width / (float) reqWidth);

                // Choose the smallest ratio as inSampleSize value, this will
                // guarantee
                // a final image with both dimensions larger than or equal to the
                // requested height and width.
                inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }

            return inSampleSize;
        }

        private Bitmap decodeSampledBitmapFromBytes(
                final byte[] bytes, final int reqWidth, final int reqHeight,
                final boolean resizePerfectly) {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (resizePerfectly) {
                final Bitmap scaledBitmap = Bitmap
                        .createScaledBitmap(bitmap, reqWidth, reqHeight, true);
                bitmap.recycle();
                bitmap = scaledBitmap;
            }

            return bitmap;
        }

        @Override
        public void run() {

            final Bitmap[] bitmaps;

            if (DEBUG) {
                Log.d(TAG, "Making cover bitmap for " + mCoverInfo.getAlbumName());
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
                            decodeSampledBitmapFromBytes(mCoverInfo.getCoverBytes(), maxSize,
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

                final CachedCover cacheRetriever = getCacheRetriever();

                if (cacheRetriever != null && !mCoverInfo.getCoverRetriever()
                        .equals(cacheRetriever)) {
                    if (DEBUG) {
                        Log.i(TAG, "Saving cover art to cache");
                    }

                    // Save the fullsize bitmap
                    try {
                        cacheRetriever.save(mCoverInfo, fullBmp);
                    } catch (final IOException e) {
                        Log.e(TAG, "Failed to save cover art to cache.", e);
                    }

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

    private final class FetchCoverTask implements Runnable {

        private static final int MAX_REQUESTS = 20;

        private final CoverInfo mCoverInfo;

        private FetchCoverTask(final CoverInfo coverInfo) {
            super();
            mCoverInfo = coverInfo;
        }

        private byte[] download(final String textUrl) throws IOException {
            final URL url = URI.create(textUrl).toURL();
            final HttpURLConnection connection = getHTTPConnection(url);
            BufferedInputStream bis = null;
            ByteArrayOutputStream baos = null;
            byte[] buffer = null;
            int len;

            if (doesUrlExist(connection)) {
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
            }

            return buffer;
        }

        private byte[] getCoverBytes(final Iterable<String> coverUrls, final CoverInfo coverInfo) {

            byte[] coverBytes = null;

            for (final String url : coverUrls) {
                try {
                    if (DEBUG) {
                        Log.d(TAG, "Downloading cover (with maxsize " + coverInfo.getCoverMaxSize()
                                + ", " + coverInfo.getCachedCoverMaxSize() + ") for "
                                + coverInfo.getAlbumName() + " from " + url);
                    }
                    if (coverInfo.getState() == CACHE_COVER_FETCH) {

                        coverBytes = readBytes(new URL("file://" + url).openStream());

                    } else if (coverInfo.getState() == WEB_COVER_FETCH) {
                        coverBytes = download(url);
                    }
                    if (coverBytes != null) {
                        if (DEBUG) {
                            Log.d(TAG,
                                    "Cover downloaded for " + coverInfo.getAlbumName() + " from " +
                                            url + ", size=" + coverBytes.length);
                        }
                        break;
                    }
                } catch (final Exception e) {
                    Log.w(TAG, "Cover get bytes failure.", e);
                }
            }

            return coverBytes;
        }

        /**
         * This method takes a URL object and returns a HttpURLConnection object.
         *
         * @param url The URL object used to create the connection.
         * @return The connection which is returned; ensure this resource is disconnected after use.
         * @throws IOException Thrown upon a communication error with the server.
         */
        private HttpURLConnection getHTTPConnection(final URL url) throws IOException {
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            return connection;
        }

        // The gracenote URLs change at every request. We match for this provider on
        // the URL prefix only.
        private boolean isBlacklistedCoverUrl(final String url, final String albumKey) {
            boolean isBlacklisted = false;

            if (url.contains(GracenoteCover.URL_PREFIX)) {
                for (final String wrongUrl : mWrongCoverUrlMap.get(albumKey)) {
                    if (wrongUrl.contains(GracenoteCover.URL_PREFIX)) {
                        isBlacklisted = true;
                    }
                }
            } else {
                isBlacklisted = mWrongCoverUrlMap.get(albumKey).contains(url);
            }

            return isBlacklisted;
        }

        private byte[] readBytes(final InputStream inputStream) throws IOException {
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

        @Override
        public void run() {
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
                                    Log.d(TAG, "Looking for cover " + mCoverInfo.getArtistName()
                                            + ", " + mCoverInfo.getAlbumName() + " with "
                                            + coverRetriever.getName());
                                }
                                mCoverInfo.setCoverRetriever(coverRetriever);
                                final List<String> coverUrls =
                                        coverRetriever.getCoverUrls(mCoverInfo);

                                if (!coverUrls.isEmpty()) {
                                    final String firstCover = coverUrls.get(0);
                                    final Collection<String> wrongUrlsForCover =
                                            mWrongCoverUrlMap.get(mCoverInfo.getKey());

                                    if (wrongUrlsForCover == null
                                            || !isBlacklistedCoverUrl(firstCover,
                                            mCoverInfo.getKey())) {

                                        if (DEBUG) {
                                            Log.d(TAG, "Cover found for  " +
                                                    mCoverInfo.getAlbumName() + " with "
                                                    + coverRetriever.getName() + " : " +
                                                    firstCover);
                                        }
                                        coverBytes = getCoverBytes(coverUrls, mCoverInfo);
                                        if (coverBytes != null && coverBytes.length > 0) {
                                            if (!coverRetriever.isCoverLocal()) {
                                                mCoverUrlMap.put(mCoverInfo.getKey(), firstCover);
                                            }
                                            mCoverInfo.setCoverBytes(coverBytes);
                                            mRequests.addLast(mCoverInfo);
                                            return;
                                        } else {
                                            if (DEBUG) {
                                                Log.d(TAG, "The cover URL for album "
                                                        + mCoverInfo.getAlbumName()
                                                        + " did not work : "
                                                        + coverRetriever.getName());
                                            }
                                        }

                                    } else {
                                        if (DEBUG) {
                                            Log.d(TAG, "Blacklisted cover url found for "
                                                    + mCoverInfo.getAlbumName() + " : "
                                                    + firstCover);
                                        }
                                    }
                                }

                            }
                        } else {
                            if (DEBUG) {
                                Log.d(TAG, "Bypassing the retriever " + coverRetriever.getName()
                                        + " for album " + mCoverInfo.getAlbumName()
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
                Log.w(TAG, "Too many requests, giving up this one : " + mCoverInfo.getAlbumName());
            }

            mRequests.addLast(mCoverInfo);
        }

    }

    private final class RequestProcessorTask implements Runnable {

        private CoverInfo getExistingRequest(final CoverInfo coverInfo) {
            return mRunningRequests.get(mRunningRequests.indexOf(coverInfo));
        }

        private boolean isLastCoverRetriever(final ICoverRetriever retriever) {
            final boolean isLast;
            final int size = mCoverRetrievers.size();

            if (size > 0) {
                isLast = mCoverRetrievers.get(size - 1).getName().equals(retriever.getName());
            } else {
                isLast = false;
            }

            return isLast;
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
                                Log.d(TAG, "Cover found for " + coverInfo.getAlbumName());
                            }
                            listener.onCoverDownloaded(coverInfo,
                                    Arrays.asList(coverInfo.getBitmap()));
                            // Do a copy for the other listeners (not to share
                            // bitmaps between views because of the recycling)
                            if (listenerIterator.hasNext()) {
                                coverInfo = new CoverInfo(coverInfo);
                                final Bitmap copyBitmap = coverInfo.getBitmap()[0].copy(
                                        coverInfo.getBitmap()[0].getConfig(),
                                        coverInfo.getBitmap()[0].isMutable());
                                coverInfo.setBitmap(copyBitmap);
                            }
                            break;
                        case COVER_NOT_FOUND:
                            // Re-try the cover art download
                            // if the request has been given up or if the path is
                            // missing (like in artist view)
                            if (!coverInfo.isRequestGivenUp() &&
                                    !TextUtils.isEmpty(coverInfo.getParentDirectory())) {
                                mNotFoundAlbumKeys.add(coverInfo.getKey());
                            }
                            removeRequest(coverInfo);
                            if (DEBUG) {
                                Log.d(TAG, "Cover not found for " + coverInfo.getAlbumName());
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
                            mapCollectionValue(mHelpersByCoverInfo, coverInfo,
                                    coverInfo.getListener());
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
                                                + "cover with artist=" + coverInfo.getArtistName()
                                                + ", album=" + coverInfo.getAlbumName());
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
                                                    + coverInfo.getAlbumName()
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

        private void saveCovers() {
            saveCovers(COVERS_FILE_NAME, mCoverUrlMap);
        }

        private void saveCovers(final String fileName, final Serializable object) {
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
                    } catch (final IOException e) {
                        Log.e(TAG, "Cannot flush cover file.", e);

                    }

                    try {
                        outputStream.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Cannot close cover file.", e);
                    }
                }
            }
        }

        private void saveWrongCovers() {
            saveCovers(WRONG_COVERS_FILE_NAME, mWrongCoverUrlMap);
        }
    }
}
