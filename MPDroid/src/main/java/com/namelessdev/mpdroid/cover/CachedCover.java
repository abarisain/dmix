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
import com.namelessdev.mpdroid.helpers.AlbumInfo;
import com.namelessdev.mpdroid.helpers.CoverManager;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CachedCover implements ICoverRetriever {

    private static final String TAG = "CachedCover";

    private final MPDApplication mApp = MPDApplication.getInstance();

    private static String getAbsoluteCoverFolderPath(final File cacheDir) {
        final String folderPath;

        if (cacheDir == null) {
            folderPath = null;
        } else {
            folderPath = cacheDir.getAbsolutePath() + CoverManager.FOLDER_SUFFIX;
        }

        return folderPath;
    }

    private static String getCoverFileName(final AlbumInfo albumInfo) {
        return albumInfo.getKey() + ".jpg";
    }

    public void clear() {
        delete(null);
    }

    public void delete(final AlbumInfo albumInfo) {
        final File[] files = getAllCachedCoverFiles();

        if (files != null) {
            for (final File file : files) {
                // No need to take care of subfolders, there won't be any.
                // (Or at least any that MPDroid cares about)
                if (albumInfo != null && getCoverFileName(albumInfo).equals(file.getName())) {
                    Log.d(TAG, "Deleting cover : " + file.getName());
                }
                if (albumInfo == null || getCoverFileName(albumInfo).equals(file.getName())) {
                    file.delete();
                }
            }
        }
    }

    public String getAbsoluteCoverFolderPath() {
        return getAbsoluteCoverFolderPath(mApp.getExternalCacheDir());
    }

    public String getAbsolutePathForSong(final AlbumInfo albumInfo) {
        final File cacheDir = mApp.getExternalCacheDir();
        final String absolutePath;

        if (cacheDir == null) {
            absolutePath = null;
        } else {
            absolutePath = getAbsoluteCoverFolderPath(cacheDir) + getCoverFileName(albumInfo);
        }

        return absolutePath;
    }

    /**
     * Just as the name says, gets a list of all cached cover files.
     *
     * @return A array of cached cover files.
     */
    private File[] getAllCachedCoverFiles() {
        final String cacheFolderPath = getAbsoluteCoverFolderPath();
        File[] result = null;

        if (cacheFolderPath != null) {
            final File cacheFolder = new File(cacheFolderPath);
            if (cacheFolder.exists()) {
                result = cacheFolder.listFiles();
            }
        }

        return result;
    }

    public long getCacheUsage() {
        long size = 0L;
        final File[] files = getAllCachedCoverFiles();

        if (files != null) {
            for (final File file : files) {
                size += file.length();
            }
        }

        return size;
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) {
        final String storageState = Environment.getExternalStorageState();
        String[] coverUrl = null;

        // If there is no external storage available, don't bother
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)
                || Environment.MEDIA_MOUNTED.equals(storageState)) {
            final String url = getAbsolutePathForSong(albumInfo);

            if (url != null && new File(url).exists()) {
                coverUrl = new String[]{
                        url
                };
            }
        }

        return coverUrl;
    }

    @Override
    public String getName() {
        return "SD Card Cache";
    }

    @Override
    public boolean isCoverLocal() {
        return true;
    }

    public void save(final AlbumInfo albumInfo, final Bitmap cover) {
        final String absoluteCoverFolder = getAbsoluteCoverFolderPath();

        if (absoluteCoverFolder != null &&
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            FileOutputStream out = null;

            try {
                final File folder = new File(absoluteCoverFolder);

                if (folder.exists() || folder.mkdirs()) {
                    out = new FileOutputStream(getAbsolutePathForSong(albumInfo));
                    cover.compress(Bitmap.CompressFormat.JPEG, 95, out);
                } else {
                    Log.e(TAG, "Couldn't create directories for cached cover.");
                }
            } catch (final Exception e) {
                if (CoverManager.DEBUG) {
                    Log.e(TAG, "Cache cover write failure.", e);
                }
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Cannot close cover stream.", e);
                    }
                }
            }
        } else {
            // External storage is not there or read only, don't do anything
            Log.e(TAG, "No writable external storage, not saving cover to cache");
        }
    }
}
