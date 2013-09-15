package org.musicpd.android.cover;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.os.Environment;

import org.musicpd.android.MPDApplication;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.Tools;

public class CachedCover implements ICoverRetriever {

	private MPDApplication application;
	private static final String FOLDER_SUFFIX = "/covers/";

	public CachedCover(MPDApplication context) {
		if (context == null)
			throw new RuntimeException("Conext cannot be null");
		application = context;
	}

	@Override
	public String[] getCoverUrl(String artist, String album, String path, String filename) throws Exception {
		final String storageState = Environment.getExternalStorageState();
		// If there is no external storage available, don't bother
		if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState) || Environment.MEDIA_MOUNTED.equals(storageState)) {
			final String url = getAbsolutePathForSong(artist, album);
			if (new File(url).exists())
				return new String[] { url };
		}
		return null;
	}

	@Override
	public boolean isCoverLocal() {
		return true;
	}

	@Override
	public String getName() {
		return "SD Card Cache";
	}

	public long getCacheUsage() {
		long size = 0;
		try {
			for (File file : new File(getAbsoluteCoverFolderPath()).listFiles()) {
				if (file.isFile()) {
					size += file.length();
				}
			}
		} catch(Exception e) {
			Log.w(e);
		}
		return size;
	}

	public void save(String artist, String album, Bitmap cover) {
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			// External storage is not there or read only, don't do anything
			Log.e("No writable external storage, not saving cover to cache");
			return;
		}
		try {
			new File(getAbsoluteCoverFolderPath()).mkdirs();
			FileOutputStream out = new FileOutputStream(getAbsolutePathForSong(artist, album));
			cover.compress(Bitmap.CompressFormat.JPEG, 95, out);
		} catch (Exception e) {
			Log.w(e);
		}
	}

	public void clear() {
		final String cacheFolderPath = getAbsoluteCoverFolderPath();
		if (cacheFolderPath == null)
			return;
		final File cacheFolder = new File(cacheFolderPath);
		if (!cacheFolder.exists()) {
			return;
		}
		File[] files = cacheFolder.listFiles();
		if (files != null) {
			for (File f : files) {
				// No need to take care of subfolders, there won't be any.
				// (Or at least any that Mupeace cares about)
				f.delete();
			}
		}
	}

	public String getAbsoluteCoverFolderPath() {
		final File cacheDir = application.getExternalCacheDir();
		if (cacheDir == null)
			return null;
		return cacheDir.getAbsolutePath() + FOLDER_SUFFIX;
	}

	public String getAbsolutePathForSong(String artist, String album) {
		final File cacheDir = application.getExternalCacheDir();
		if (cacheDir == null)
			return null;
		String filename;
		if (artist == null) {
			filename = Tools.getHashFromString(artist + ";" + album) + ".jpg";
		}else{
			filename = Tools.getHashFromString(album) + ".jpg";
		}
		return getAbsoluteCoverFolderPath() + filename;
	}

}
