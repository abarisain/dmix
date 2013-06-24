package com.namelessdev.mpdroid.helpers;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.ICoverRetriever;
import com.namelessdev.mpdroid.cover.LastFMCover;
import com.namelessdev.mpdroid.cover.LocalCover;
import com.namelessdev.mpdroid.tools.Tools;

/**
 * Download Covers Asynchronous with Messages
 * 
 * @author Stefan Agner
 * @version $Id: $
 */
public class CoverAsyncHelper extends Handler {
	public static final int EVENT_DOWNLOADCOVER = 0;
	public static final int EVENT_COVERDOWNLOADED = 1;
	public static final int EVENT_COVERNOTFOUND = 2;
	public static final int MAX_SIZE = 0;

	public static final String PREFERENCE_CACHE = "enableLocalCoverCache";
	public static final String PREFERENCE_LASTFM = "enableLastFM";
	public static final String PREFERENCE_LOCALSERVER = "enableLocalCover";
	public static final String PREFERENCE_ONLY_WIFI = "enableCoverOnlyOnWifi";

	private MPDApplication app = null;
	private SharedPreferences settings = null;

	private int coverMaxSize = MAX_SIZE;
	private int cachedCoverMaxSize = MAX_SIZE;
	private boolean cacheWritable = true;

	private ICoverRetriever[] coverRetrievers = null;

	public static ExecutorService threadPool;

	static {
		threadPool = Executors.newCachedThreadPool();
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

	public void downloadCover(String artist, String album, String path, String filename) {
		final CoverInfo info = new CoverInfo();
		info.sArtist = artist;
		info.sAlbum = album;
		info.sPath = path;
		info.sFilename = filename;
		threadPool.execute(new CoverAsyncWorker(info));
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

		public Bitmap getBitmapForRetriever(ICoverRetriever retriever) {
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

			Bitmap downloadedCover = null;
			for (String url : urls) {
				if (url == null)
					continue;
				Log.i(MPDApplication.TAG, "Downloading cover art at url : " + url);
				if (retriever.isCoverLocal()) {
					int maxSize = coverMaxSize;
					if (cachedCoverMaxSize != MAX_SIZE) {
						maxSize = cachedCoverMaxSize;
					}
					if (maxSize == MAX_SIZE) {
						downloadedCover = BitmapFactory.decodeFile(url);
					} else {
						downloadedCover = Tools.decodeSampledBitmapFromPath(url, maxSize, maxSize, false);
					}
				} else {
					downloadedCover = download(url);
				}

				if (downloadedCover != null) {
					break;
				}
			}
			return downloadedCover;
		}

		public boolean fillEmptyArtist() {
			if (info.sArtist != null)
				return true;
			try {
				// load songs for this album
				final List<? extends Item> songs = app.oMPDAsyncHelper.oMPD.getSongs(null, new Album(info.sAlbum));

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
			Bitmap cover = null;
			if (fillEmptyArtist()) {
				for (ICoverRetriever coverRetriever : coverRetrievers) {
					cover = getBitmapForRetriever(coverRetriever);
					if (cover != null) {
						Log.i(MPDApplication.TAG, "Found cover art using retriever : " + coverRetriever.getName());
						// if cover is not read from cache and saving is enabled
						if (cacheWritable && !(coverRetriever instanceof CachedCover)) {
							// Save this cover into cache, if it is enabled.
							for (ICoverRetriever coverRetriever1 : coverRetrievers) {
								if (coverRetriever1 instanceof CachedCover) {
									Log.i(MPDApplication.TAG, "Saving cover art to cache");
									((CachedCover) coverRetriever1).save(info.sArtist, info.sAlbum, cover);
								}
							}
						}
						CoverAsyncHelper.this.obtainMessage(EVENT_COVERDOWNLOADED, cover).sendToTarget();
						break;
					}
				}
			}

			if (cover == null) {
				Log.i(MPDApplication.TAG, "No cover art found");
				CoverAsyncHelper.this.obtainMessage(EVENT_COVERNOTFOUND).sendToTarget();
			}
		}
	}

	private Bitmap download(String url) {
		URL myFileUrl = null;
		HttpURLConnection conn = null;
		try {
			// Download Cover File...
			myFileUrl = new URL(url);
			conn = (HttpURLConnection) myFileUrl.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(false);
			conn.connect();
			BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 8192);
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int len;
				while ((len = bis.read(buffer)) > -1) {
					baos.write(buffer, 0, len);
				}
				baos.flush();
				InputStream is = new ByteArrayInputStream(baos.toByteArray());

				BitmapFactory.Options o = new BitmapFactory.Options();
				o.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(is, null, o);

				int scale = 1;
				if (coverMaxSize != MAX_SIZE || o.outHeight > coverMaxSize || o.outWidth > coverMaxSize) {
					scale = (int) Math.pow(2, (int) Math.round(Math.log(coverMaxSize /
							(double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
				}

				o.inSampleSize = scale;
				o.inJustDecodeBounds = false;
				is.reset();
				Bitmap bmp = BitmapFactory.decodeStream(is, null, o);
				is.close();
				conn.disconnect();

				return bmp;
			} catch (Exception e) {
				return null;
			}

		} catch (MalformedURLException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	private class CoverInfo {
		public String sArtist;
		public String sAlbum;
		public String sPath;
		public String sFilename;
	}

	public enum CoverRetrievers {
		CACHE,
		LASTFM,
		LOCAL;
	}

}
