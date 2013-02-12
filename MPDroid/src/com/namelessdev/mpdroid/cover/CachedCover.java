package com.namelessdev.mpdroid.cover;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.os.Environment;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.tools.Tools;

public class CachedCover implements ICoverRetriever {

	private MPDApplication application;
	private static final String FOLDER_SUFFIX = "/covers/";

	public CachedCover(MPDApplication context) {
		if (context == null)
			throw new RuntimeException("Conext cannot be null");
		application = context;
	}

	@Override
	public String[] getCoverUrl(String artist, String album, String path) throws Exception {
		final String storageState = Environment.getExternalStorageState();
		// If there is no external storage available, don't bother
		if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState) || Environment.MEDIA_MOUNTED.equals(storageState)) {
			return new String[] { getAbsolutePathForSong(application, artist, album) };
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

	public static void save(MPDApplication context, String artist, String album, Bitmap cover) {
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			// External storage is not there or read only, don't do anything
			return;
		}
		try {
			new File(getAbsoluteCoverFolderPath(context)).mkdirs();
			FileOutputStream out = new FileOutputStream(getAbsolutePathForSong(context, artist, album));
			cover.compress(Bitmap.CompressFormat.JPEG, 100, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void clear(MPDApplication context) {
		final File cacheFolder = new File(getAbsoluteCoverFolderPath(context));
		if (!cacheFolder.exists()) {
			return;
		}
		File[] files = cacheFolder.listFiles();
		if (files != null) {
			for (File f : files) {
				// No need to take care of subfolders, there won't be any.
				// (Or at least any that MPDroid cares about)
				f.delete();
			}
		}
	}

	public static String getFilenameForSong(String artist, String album) {
		return Tools.getHashFromString(artist + ";" + album) + ".jpg";
	}
	
	public static String getAbsoluteCoverFolderPath(MPDApplication context) {
		final File cacheDir = context.getExternalCacheDir();
		if (cacheDir == null)
			return null;
		return cacheDir.getAbsolutePath() + FOLDER_SUFFIX;
	}

	public static String getAbsolutePathForSong(MPDApplication context, String artist, String album) {
		final File cacheDir = context.getExternalCacheDir();
		if (cacheDir == null)
			return null;
		return getAbsoluteCoverFolderPath(context) + getFilenameForSong(artist, album);
	}

}
