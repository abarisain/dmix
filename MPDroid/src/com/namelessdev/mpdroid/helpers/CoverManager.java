package com.namelessdev.mpdroid.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.*;
import com.namelessdev.mpdroid.tools.MultiMap;
import com.namelessdev.mpdroid.tools.StringUtils;
import com.namelessdev.mpdroid.tools.Tools;
import org.a0z.mpd.UnknownAlbum;
import org.a0z.mpd.UnknownArtist;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import static com.namelessdev.mpdroid.helpers.CoverInfo.STATE.*;
import static com.namelessdev.mpdroid.tools.StringUtils.isNullOrEmpty;
import static com.namelessdev.mpdroid.tools.StringUtils.trim;

/**
 */
public class CoverManager {
    public static final String PREFERENCE_CACHE = "enableLocalCoverCache";
    public static final String PREFERENCE_LASTFM = "enableLastFM";
    public static final String PREFERENCE_LOCALSERVER = "enableLocalCover";
    public static final String PREFERENCE_ONLY_WIFI = "enableCoverOnlyOnWifi";
    private static final boolean DEBUG = false;
    public static final int MAX_REQUESTS = 25;
    private MPDApplication app = null;
    private SharedPreferences settings = null;
    private static CoverManager instance = null;
    private BlockingDeque<CoverInfo> requests = new LinkedBlockingDeque<CoverInfo>();
    private List<CoverInfo> runningRequests = Collections.synchronizedList(new ArrayList<CoverInfo>());
    private ExecutorService requestExecutor = Executors.newFixedThreadPool(1);
    private ExecutorService coverFetchExecutor = Executors.newFixedThreadPool(2);
    private ExecutorService priorityCoverFetchExecutor = Executors.newFixedThreadPool(1);
    private ExecutorService cacheCoverFetchExecutor = Executors.newFixedThreadPool(1);
    private ExecutorService createBitmapExecutor = cacheCoverFetchExecutor;
    private MultiMap<CoverInfo, CoverDownloadListener> helpersByCoverInfo = new MultiMap<CoverInfo, CoverDownloadListener>();
    private ICoverRetriever[] coverRetrievers = null;
    private boolean active = true;

    public synchronized static CoverManager getInstance(MPDApplication app, SharedPreferences settings) {
        if (instance == null) {
            instance = new CoverManager(app, settings);
        }
        return instance;
    }

    private CoverManager(MPDApplication app, SharedPreferences settings) {
        this.app = app;
        this.settings = settings;

        requestExecutor.submit(new RequestProcessorTask());
        this.setCoverRetrieversFromPreferences();
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

    private ICoverRetriever getCacheRetriever() {
        for (ICoverRetriever retriever : coverRetrievers) {
            if (retriever.isCoverLocal()) {
                return retriever;
            }
        }
        return null;
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
        NetworkInfo.State wifi = conMan.getNetworkInfo(1).getState();
        if (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING) {
            return true;
        } else {
            return false;
        }
    }

    public void addCoverRequest(CoverInfo coverInfo) {
        if (DEBUG) {
            Log.d(CoverManager.class.getSimpleName(), "Looking for cover with artist=" + coverInfo.getArtist() + ", album=" + coverInfo.getAlbum());
        }
        this.requests.add(coverInfo);
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
                            helpersByCoverInfo.put(coverInfo, coverInfo.getListener());
                            if (runningRequests.contains(coverInfo)) {
                                break;
                            } else {

                                if (!isValidCoverInfo(coverInfo)) {
                                    if (DEBUG) {
                                        Log.d(CoverManager.class.getSimpleName(), "Incomplete cover request  with artist=" + coverInfo.getArtist() + ", album=" + coverInfo.getAlbum());
                                    }
                                    notifyListeners(false, coverInfo);
                                } else {
                                    runningRequests.add(coverInfo);
                                    coverInfo.setState(CACHE_COVER_FETCH);
                                    cacheCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                }
                                break;

                            }
                        case CACHE_COVER_FETCH:
                            if (coverInfo.getCoverBytes() == null || coverInfo.getCoverBytes().length == 0) {
                                if (runningRequests.size() < MAX_REQUESTS) {
                                    coverInfo.setState(WEB_COVER_FETCH);
                                    if (coverInfo.isPriority()) {
                                        priorityCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                    } else {
                                        coverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                    }
                                    break;
                                } else {
                                    Log.w(CoverManager.class.getSimpleName(), "Too many requests, giving up this one : " + coverInfo.getAlbum());
                                    notifyListeners(false, coverInfo);
                                    break;
                                }
                            } else {
                                coverInfo.setState(CREATE_BITMAP);
                                createBitmapExecutor.submit(new CreateBitmapTask(coverInfo));
                                break;
                            }
                        case WEB_COVER_FETCH:
                            if (coverInfo.getCoverBytes() != null && coverInfo.getCoverBytes().length > 0) {
                                coverInfo.setState(CREATE_BITMAP);
                                createBitmapExecutor.submit(new CreateBitmapTask(coverInfo));
                                break;
                            } else {
                                notifyListeners(false, coverInfo);
                                break;
                            }
                        case CREATE_BITMAP:
                            if (coverInfo.getBitmap() != null) {
                                notifyListeners(true, coverInfo);
                            } else if (isLastCoverRetriever(coverInfo.getCoverRetriever())) {
                                if (DEBUG)
                                    Log.d(CoverManager.class.getSimpleName(), "The cover has not been downloaded correctly for album " + coverInfo.getAlbum() + " with this retriever : " + coverInfo.getCoverRetriever() + ", trying the next ones ...");
                                coverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                            } else {
                                notifyListeners(false, coverInfo);
                            }
                            break;
                        default:
                            Log.e(CoverManager.class.getSimpleName(), "Unknown request : " + coverInfo);
                            notifyListeners(false, coverInfo);
                            break;
                    }


                } catch (Exception e) {
                    Log.e(CoverManager.class.getSimpleName(), "Cover request processing failure : " + e);
                }
            }

        }
    }

    public static boolean isValidCoverInfo(CoverInfo coverInfo) {
        return isValidArtistOrAlbum(coverInfo.getAlbum()) && isValidArtistOrAlbum(coverInfo.getArtist());
    }

    public static boolean isValidArtistOrAlbum(String artistOrAlbum) {
        return !StringUtils.isNullOrEmpty(artistOrAlbum) && !artistOrAlbum.equals("-") &&
                !artistOrAlbum.equals(UnknownArtist.instance.getName()) && !artistOrAlbum.equals(UnknownAlbum.instance.getName());
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

    private void notifyListeners(boolean found, CoverInfo coverInfo) {

        try {
            if (DEBUG)
                Log.d(CoverManager.class.getSimpleName(), "End of cover lookup for " + coverInfo.getAlbum() + ", did we find it ? " + found);
            if (helpersByCoverInfo.containsKey(coverInfo)) {
                Iterator<CoverDownloadListener> listenerIterator = helpersByCoverInfo.get(coverInfo).iterator();
                while (listenerIterator.hasNext()) {
                    CoverDownloadListener listener = listenerIterator.next();

                    if (found) {
                        listener.onCoverDownloaded(coverInfo);

                    } else {
                        listener.onCoverNotFound(coverInfo);
                    }

                    if (listenerIterator.hasNext()) {
                        // Do a copy for the other listeners (not to share bitmaps between views because of the recycling)
                        coverInfo = new CoverInfo(coverInfo);
                        if (coverInfo.getBitmap() != null && coverInfo.getBitmap().length > 0) {
                            Bitmap copyBitmap = coverInfo.getBitmap()[0].copy(coverInfo.getBitmap()[0].getConfig(), coverInfo.getBitmap()[0].isMutable() ? true : false);
                            coverInfo.setBitmap(new Bitmap[]{copyBitmap});
                        }
                    }
                }
            }
        } finally {
            runningRequests.remove(coverInfo);
            helpersByCoverInfo.remove(coverInfo);
            logQueues();
        }
    }

    private void logQueues() {
        if (DEBUG) {
            Log.d(CoverManager.class.getSimpleName(), "requests queue size : " + requests.size());
            Log.d(CoverManager.class.getSimpleName(), "running request queue size : " + runningRequests.size());
            for (CoverInfo coverInfo : runningRequests) {
                Log.d(CoverManager.class.getSimpleName(), "Running request : " + coverInfo.toString());
            }
            Log.d(CoverManager.class.getSimpleName(), "helpersByCoverInfo map size : " + helpersByCoverInfo.size());
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

            // If the coverretriever is defined in the coverInfo
            // that means that a previous cover fetch failed with this retriever
            // We just start after this retriever to try a cover.
            if (coverInfo.getCoverRetriever() != null) {
                canStart = false;
            }

            for (ICoverRetriever coverRetriever : coverRetrievers) {
                try {

                    if (canStart) {

                        remote = coverInfo.getState() == WEB_COVER_FETCH && !coverRetriever.isCoverLocal() && !coverInfo.isCacheOnly();
                        local = coverInfo.getState() == CACHE_COVER_FETCH && coverRetriever.isCoverLocal();
                        if (remote || local) {
                            if (DEBUG) {
                                Log.d(CoverManager.class.getSimpleName(), "Looking for cover " + coverInfo.getArtist() + ", " + coverInfo.getAlbum() + " with " + coverRetriever.getName());
                            }
                            coverInfo.setCoverRetriever(coverRetriever);
                            coverUrls = coverRetriever.getCoverUrl(coverInfo.getArtist(), coverInfo.getAlbum(), coverInfo.getPath(), coverInfo.getFilename());
                            if (coverUrls != null && coverUrls.length > 0) {
                                if (DEBUG)
                                    Log.d(CoverManager.class.getSimpleName(), "Cover found for  " + coverInfo.getAlbum() + " with " + coverRetriever.getName());
                                coverBytes = getCoverBytes(coverUrls, coverInfo);
                                if (coverBytes != null && coverBytes.length > 0) {
                                    coverInfo.setCoverBytes(coverBytes);
                                    requests.addLast(coverInfo);
                                    return;
                                } else {
                                    if (DEBUG)
                                        Log.d(CoverManager.class.getSimpleName(), "The cover URL for album " + coverInfo.getAlbum() + " did not work : " + coverRetriever.getName());
                                }

                            }

                        }
                    } else {
                        if (DEBUG)
                            Log.d(CoverManager.class.getSimpleName(), "Bypassing the retriever " + coverRetriever.getName() + " for album " + coverInfo.getAlbum() + ", already asked.");
                        canStart = coverRetriever == coverInfo.getCoverRetriever();
                    }

                } catch (Exception e) {
                    Log.e(CoverManager.class.getSimpleName(), "Fetch cover failure : " + e);
                }

            }
            requests.addLast(coverInfo);
        }


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
                Log.d(CoverManager.class.getSimpleName(), "Making cover bitmap for " + coverInfo.getAlbum());

            if (coverInfo.getCoverRetriever().isCoverLocal()) {
                int maxSize = coverInfo.getCoverMaxSize();
                if (coverInfo.getCachedCoverMaxSize() != coverInfo.MAX_SIZE) {
                    maxSize = coverInfo.getCachedCoverMaxSize();
                }
                if (maxSize == coverInfo.MAX_SIZE) {
                    bitmaps = new Bitmap[]{BitmapFactory.decodeByteArray(coverInfo.getCoverBytes(), 0, coverInfo.getCoverBytes().length)};
                    coverInfo.setBitmap(bitmaps);
                } else {
                    bitmaps = new Bitmap[]{Tools.decodeSampledBitmapFromBytes(coverInfo.getCoverBytes(), maxSize, maxSize, false)};
                    coverInfo.setBitmap(bitmaps);
                }
            } else {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(coverInfo.getCoverBytes(), 0, coverInfo.getCoverBytes().length, o);

                int scale = 1;
                if (coverInfo.getCoverMaxSize() != CoverInfo.MAX_SIZE || o.outHeight > coverInfo.getCoverMaxSize() || o.outWidth > coverInfo.getCoverMaxSize()) {
                    scale = (int) Math.pow(2, (int) Math.round(Math.log(coverInfo.getCoverMaxSize() /
                            (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
                }

                o.inSampleSize = 1;
                o.inJustDecodeBounds = false;
                Bitmap fullBmp = BitmapFactory.decodeByteArray(coverInfo.getCoverBytes(), 0, coverInfo.getCoverBytes().length, o);
                Bitmap bmp = null;
                if (scale == 1) {
                    // This can cause some problem (a bitmap being freed will free both references)
                    // But the only use is to save it in the cache so it's okay.
                    bmp = fullBmp;
                } else {
                    o.inSampleSize = scale;
                    o.inJustDecodeBounds = false;
                    bmp = BitmapFactory.decodeByteArray(coverInfo.getCoverBytes(), 0, coverInfo.getCoverBytes().length, o);
                }
                bitmaps = new Bitmap[]{bmp, fullBmp};
                coverInfo.setBitmap(bitmaps);
                coverInfo.setCoverBytes(null);

                ICoverRetriever cacheRetriever;
                cacheRetriever = getCacheRetriever();
                if (cacheRetriever != null && coverInfo.getCoverRetriever() != cacheRetriever) {
                    if (DEBUG)
                        Log.i(MPDApplication.TAG, "Saving cover art to cache");
                    // Save the fullsize bitmap
                    ((CachedCover) getCacheRetriever()).save(coverInfo.getArtist(), coverInfo.getAlbum(), fullBmp);

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


    private byte[] getCoverBytes(String[] coverUrls, CoverInfo coverInfo) {

        byte[] coverBytes = null;

        for (String url : coverUrls) {

            try {
                if (DEBUG)
                    Log.d(CoverManager.class.getSimpleName(), "Downloading cover for " + coverInfo.getAlbum() + " from " + url);
                if (coverInfo.getState() == CACHE_COVER_FETCH) {

                    coverBytes = readBytes(new URL("file://" + url).openStream());

                } else if (coverInfo.getState() == WEB_COVER_FETCH) {
                    coverBytes = download(url);
                }
                if (coverBytes != null) {
                    if (DEBUG)
                        Log.d(CoverManager.class.getSimpleName(), "Cover downloaded for " + coverInfo.getAlbum() + " from " + url + ", size=" + coverBytes.length);
                    return coverBytes;
                }
            } catch (Exception e) {
                Log.w(CoverManager.class.getSimpleName(), "Cover get bytes failure : " + e);
            }
        }
        return coverBytes;
    }

    public byte[] readBytes(InputStream inputStream) throws IOException {
        try {
            // this dynamically extends to take the bytes you read
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            // this is storage overwritten on each iteration with bytes
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            // we need to know how may bytes were read to write them to the byteBuffer
            int len = 0;
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

    private byte[] download(String textUrl) {

        URL url;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        int statusCode;
        BufferedInputStream bis;
        ByteArrayOutputStream baos;
        byte[] buffer;
        int len;

        try {
            textUrl = trim(textUrl);
            if (isNullOrEmpty(textUrl)) {
                return null;
            }
            // Download Cover File...
            textUrl = textUrl.replace(" ", "%20");
            url = new URL(textUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            statusCode = connection.getResponseCode();
            inputStream = connection.getInputStream();
            if (statusCode != 200) {
                Log.w(CoverAsyncHelper.class.getName(), "This URL does not exist : Status code : " + statusCode + ", " + textUrl);
                return null;
            }
            bis = new BufferedInputStream(inputStream, 8192);
            baos = new ByteArrayOutputStream();
            buffer = new byte[1024];
            while ((len = bis.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            Log.e(CoverAsyncHelper.class.getSimpleName(), "Failed to download cover :" + e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //Nothing to do
                }
            }

        }
    }

    @Override
    protected void finalize() throws Throwable {
        stopExecutors();
        instance = null;
        super.finalize();
    }

    private void stopExecutors() {
        try {
            Log.i(CoverManager.class.getSimpleName(), "Shutting down cover executors");
            active = false;
            this.priorityCoverFetchExecutor.shutdown();
            this.requestExecutor.shutdown();
            this.createBitmapExecutor.shutdown();
            this.coverFetchExecutor.shutdown();
            this.cacheCoverFetchExecutor.shutdown();
        } catch (Exception ex) {
            Log.e(CoverAsyncHelper.class.getSimpleName(), "Failed to shutdown cover executors :" + ex);
        }


    }
}
