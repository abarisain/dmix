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

package com.namelessdev.mpdroid.cover;

import com.namelessdev.mpdroid.MPDApplication;

import org.a0z.mpd.AlbumInfo;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.util.Log.d;
import static android.util.Log.e;
import static com.namelessdev.mpdroid.helpers.CoverManager.getCoverFileName;

public class CachedCover implements ICoverRetriever {

    private final MPDApplication app = MPDApplication.getInstance();
    private static final String FOLDER_SUFFIX = "/covers/";

    public CachedCover() {
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
                    d(CachedCover.class.getSimpleName(), "Deleting cover : " + file.getName());
                }
                if (albumInfo == null || getCoverFileName(albumInfo).equals(file.getName())) {
                    file.delete();
                }
            }
        }
    }

    public String getAbsoluteCoverFolderPath() {
        final File cacheDir = app.getExternalCacheDir();
        if (cacheDir == null)
            return null;
        return cacheDir.getAbsolutePath() + FOLDER_SUFFIX;
    }

    public String getAbsolutePathForSong(AlbumInfo albumInfo) {
        final File cacheDir = app.getExternalCacheDir();
        if (cacheDir == null)
            return null;
        return getAbsoluteCoverFolderPath() + getCoverFileName(albumInfo);
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
    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception {
        final String storageState = Environment.getExternalStorageState();
        // If there is no external storage available, don't bother
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)
                || Environment.MEDIA_MOUNTED.equals(storageState)) {
            final String url = getAbsolutePathForSong(albumInfo);
            if (new File(url).exists())
                return new String[] {
                        url
                };
        }
        return null;
    }

    @Override
    public String getName() {
        return "SD Card Cache";
    }

    @Override
    public boolean isCoverLocal() {
        return true;
    }

    public void save(AlbumInfo albumInfo, Bitmap cover) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // External storage is not there or read only, don't do anything
            e(MPDApplication.TAG, "No writable external storage, not saving cover to cache");
            return;
        }
        FileOutputStream out = null;
        try {
            new File(getAbsoluteCoverFolderPath()).mkdirs();
            out = new FileOutputStream(getAbsolutePathForSong(albumInfo));
            cover.compress(Bitmap.CompressFormat.JPEG, 95, out);
        } catch (Exception e) {
            e(MPDApplication.TAG, "Cache cover write failure : " + e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e(MPDApplication.TAG, "Cannot close cover stream : " + e);
                }
            }
        }
    }

}
