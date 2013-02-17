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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.cover.CachedCover;
import com.namelessdev.mpdroid.cover.ICoverRetriever;
import com.namelessdev.mpdroid.cover.LastFMCover;
import com.namelessdev.mpdroid.cover.LocalCover;

/**
 * Download Covers Asynchronous with Messages
 * 
 * @author Stefan Agner
 * @version $Id: $
 */
public class CoverAsyncHelper extends Handler {
	private static final int EVENT_DOWNLOADCOVER = 0;
	private static final int EVENT_COVERDOWNLOADED = 1;
	private static final int EVENT_COVERNOTFOUND = 2;
	private static final int MAX_SIZE = 0;

	public static final String PREFERENCE_CACHE = "enableLocalCoverCache";
	public static final String PREFERENCE_LASTFM = "enableLastFM";
	public static final String PREFERENCE_LOCALSERVER = "enableLocalCover";

	private MPDApplication app = null;
	private SharedPreferences settings = null;
	
	private int coverMaxSize = 0;

	private ICoverRetriever[] coverRetrievers = null;
	
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

	private CoverAsyncWorker oCoverAsyncWorker;

	public CoverAsyncHelper(MPDApplication app, SharedPreferences settings) {
		this.app = app;
		this.settings = settings;

		HandlerThread oThread = new HandlerThread("CoverAsyncWorker");
		oThread.start();
		oCoverAsyncWorker = new CoverAsyncWorker(oThread.getLooper());
		coverDownloadListener = new LinkedList<CoverDownloadListener>();
	}

	public void setCoverRetrieversFromPreferences() {
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
		final List<CoverRetrievers> enabledRetrievers = new ArrayList<CoverRetrievers>();
		//There is a cover provider order, respect it.
		//Cache -> LastFM -> MPD Server
		if(settings.getBoolean(PREFERENCE_CACHE, true)) {
			enabledRetrievers.add(CoverRetrievers.CACHE);
		}
		if (settings.getBoolean(PREFERENCE_LASTFM, true)) {
			enabledRetrievers.add(CoverRetrievers.LASTFM);
		}
		if (settings.getBoolean(PREFERENCE_LOCALSERVER, true)) {
			enabledRetrievers.add(CoverRetrievers.LOCAL);
		}
		setCoverRetrievers(enabledRetrievers);
	}
	
	public void setCoverMaxSize(int size) {
		coverMaxSize = size;
	}


	public void addCoverDownloadListener(CoverDownloadListener listener) {
		coverDownloadListener.add(listener);
	}

	public void downloadCover(String artist, String album, String path, String filename) {
		downloadCover(artist, album, path, filename, MAX_SIZE);
	}

	public void downloadCover(String artist, String album, String path, String filename, int targetSize) {
		CoverInfo info = new CoverInfo();
		info.sArtist = artist;
		info.sAlbum = album;
		info.sPath = path;
		info.sFilename = filename;
		info.iSize = targetSize;
		oCoverAsyncWorker.obtainMessage(EVENT_DOWNLOADCOVER, info).sendToTarget();
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

	private class CoverAsyncWorker extends Handler {
		public CoverAsyncWorker(Looper looper) {
			super(looper);
		}

		public Bitmap getBitmapForRetriever(CoverInfo info, ICoverRetriever retriever) {
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
					downloadedCover = BitmapFactory.decodeFile(url);
				} else {
					downloadedCover = download(url);
				}

				if (downloadedCover != null) {
					break;
				}
			}
			return downloadedCover;
		}

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_DOWNLOADCOVER:
				Bitmap cover = null;
				for (ICoverRetriever coverRetriever : coverRetrievers) {
					cover = getBitmapForRetriever((CoverInfo) msg.obj, coverRetriever);
					if (cover != null) {
						Log.i(MPDApplication.TAG, "Found cover art using retriever : " + coverRetriever.getName());
						// if cover is not read from cache
						if (!(coverRetriever instanceof CachedCover)) {
							// Save this cover into cache, if it is enabled.
							for (ICoverRetriever coverRetriever1 : coverRetrievers) {
								if (coverRetriever1 instanceof CachedCover) {
									Log.i(MPDApplication.TAG, "Saving cover art to cache");
									((CachedCover) coverRetriever1).save(((CoverInfo) msg.obj).sArtist, ((CoverInfo) msg.obj).sAlbum, cover);
								}
							}	
						}
						CoverAsyncHelper.this.obtainMessage(EVENT_COVERDOWNLOADED, cover).sendToTarget();
						break;
					}	
				}
				if (cover == null) {
					Log.i(MPDApplication.TAG, "No cover art found");
					CoverAsyncHelper.this.obtainMessage(EVENT_COVERNOTFOUND).sendToTarget();
				}
				break;
			default:
			}
		}	

	}
/*
	private Bitmap download(String url) {
		URL myFileUrl = null;
		try {
			// Download Cover File...
			HttpGet httpRequest = null;
			httpRequest = new HttpGet(url);

			final HttpClient httpclient = new DefaultHttpClient();
			final HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
			final HttpEntity entity = response.getEntity();
			final BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
			final InputStream instream = bufHttpEntity.getContent();
			return BitmapFactory.decodeStream(instream);
		} catch (IllegalArgumentException e) {
			Log.e(MPDApplication.TAG, "Cover URI is invalid : " + url);
			return null;
		} catch (IOException e) {
			return null;
		}
		
	}
*/

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
			BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
			try {
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        byte[] buffer = new byte[1024];
		        int len;
		        while ((len = bis.read(buffer)) > -1 ) {
		           baos.write(buffer, 0, len);
		        }
		        baos.flush();
		        InputStream is = new ByteArrayInputStream(baos.toByteArray()); 

		        BitmapFactory.Options o = new BitmapFactory.Options();
		        o.inJustDecodeBounds = true;
		        BitmapFactory.decodeStream(is,null,o);

			    System.out.println("orig.height:" + o.outHeight + " orig.width:" + o.outWidth);
			    System.out.println("max.size:" + coverMaxSize);
			    int scale = 1;
			    if (o.outHeight > coverMaxSize || o.outWidth > coverMaxSize) {
			         scale = (int)Math.pow(2, (int) Math.round(Math.log(coverMaxSize / 
			         (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
			    }
			    System.out.println("scale factor:" + scale);

			    o.inSampleSize = scale;
		        o.inJustDecodeBounds = false;
			    is.reset();  
			    Bitmap bmp = BitmapFactory.decodeStream(is,null,o);
			    System.out.println("new.height:" + bmp.getHeight() + " new.width:" + bmp.getWidth());
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
		public int iSize;
	}

	public enum CoverRetrievers {
		CACHE,
		LASTFM,
		LOCAL;
	}

}
