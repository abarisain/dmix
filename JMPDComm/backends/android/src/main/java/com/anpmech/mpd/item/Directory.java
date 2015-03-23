/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.anpmech.mpd.item;

import android.os.Bundle;
import android.os.Parcel;

import java.io.Serializable;
import java.util.Map;

/**
 * This is the Android backend {@code Directory} item.
 *
 * @see AbstractDirectory
 */
public class Directory extends AbstractDirectory<Directory> {

    public static final String EXTRA = TAG;

    private static final String DIRECTORY_FILENAME_KEY = "DIRECTORY_FILENAME";

    private static final String DIRECTORY_MAP_KEY = "DIRECTORY_ENTRIES";

    private static final String DIRECTORY_NAME_KEY = "DIRECTORY_NAME";

    private static final String FILE_MAP_KEY = "FILE_ENTRIES";

    private static final String PARENT_FILENAME_KEY = "PARENT_FILENAME";

    private static final String PLAYLIST_MAP_KEY = "PLAYLIST_ENTRIES";

    /** The root directory object. */
    private static final Directory ROOT = new Directory(null, null);

    public static final Creator<Directory> CREATOR = new Creator<Directory>() {
        @Override
        public Directory createFromParcel(final Parcel source) {
            final Bundle bundle = source.readBundle();

            return new Directory(
                    /** TODO: This might be wrong. */
                    ROOT.getDirectory(bundle.getString(PARENT_FILENAME_KEY)),
                    bundle.getString(DIRECTORY_FILENAME_KEY),
                    bundle.getString(DIRECTORY_NAME_KEY),
                    (Map<String, Directory>) bundle.getSerializable(DIRECTORY_MAP_KEY),
                    (Map<String, Music>) bundle.getSerializable(FILE_MAP_KEY),
                    (Map<String, PlaylistFile>) bundle.getSerializable(PLAYLIST_MAP_KEY)
            );
        }

        @Override
        public Directory[] newArray(final int size) {
            return new Directory[size];
        }
    };

    /**
     * Creates a new directory.
     *
     * @param parent   The parent directory to this directory.
     * @param filename The filename of this directory.
     */
    private Directory(final Directory parent, final String filename) {
        super(parent, filename);
    }

    /**
     * The base constructor.
     *
     * @param parent           The parent directory to this directory.
     * @param filename         The filename of this directory.
     * @param name             The name of this directory.
     * @param directoryEntries Children directories to this directory.
     * @param fileEntries      Children files to this directory.
     * @param playlistEntries  Children playlists to this directory.
     */
    private Directory(final Directory parent, final String filename,
            final String name,
            final Map<String, Directory> directoryEntries,
            final Map<String, Music> fileEntries,
            final Map<String, PlaylistFile> playlistEntries) {
        super(parent, filename, name, directoryEntries, fileEntries, playlistEntries);
    }

    /**
     * Gets the root directory for this media server.
     *
     * @return The root directory for this media server.
     * @see #refresh(com.anpmech.mpd.connection.MPDConnection)
     */
    public static Directory getRoot() {
        return ROOT;
    }

    /**
     * Creates a child {@code Directory} object relative to this directory object.
     *
     * @param subdirectory The subdirectory path of the root to create a {@code Directory} for.
     * @return the last component of the path created.
     * @throws IllegalArgumentException If {@code subdirectory} starts or ends with '/'
     * @see #getRoot()
     * @see #refresh(com.anpmech.mpd.connection.MPDConnection)
     */
    public Directory makeChildDirectory(final String subdirectory) {
        final String name;
        final String remainingPath;
        final int slashIndex = subdirectory.indexOf(MPD_SEPARATOR);

        if (slashIndex == 0 || slashIndex == subdirectory.length() - 1) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Subdirectory: ");
            stringBuilder.append(subdirectory);
            stringBuilder.append(" illegally");
            if (slashIndex == 0) {
                stringBuilder.append(" begins");
            } else {
                stringBuilder.append(" ends");
            }
            stringBuilder.append(" with '");
            stringBuilder.append(MPD_SEPARATOR);
            stringBuilder.append('\'');

            throw new IllegalArgumentException(stringBuilder.toString());
        }

        // split path
        if (slashIndex == -1) {
            name = subdirectory;
            remainingPath = null;
        } else {
            name = subdirectory.substring(0, slashIndex);
            remainingPath = subdirectory.substring(slashIndex + 1);
        }

        // create directory
        final Directory dir;
        if (mDirectoryEntries.containsKey(name)) {
            dir = mDirectoryEntries.get(name);
        } else {
            dir = new Directory(this, name);
            mDirectoryEntries.put(dir.mFilename, dir);
        }

        // create remainder
        if (remainingPath != null) {
            return dir.makeChildDirectory(remainingPath);
        }
        return dir;
    }

    /**
     * Makes an object which is the immediate parent relative to this directory object with the
     * name given in the parameter.
     *
     * @param name The string identifier used for the name of the parent directory.
     * @return The parent directory object of this object.
     */
    public Directory makeParentDirectory(final String name) {
        return new Directory(mParent.mParent, mParent.mFilename, name, mDirectoryEntries,
                mFileEntries, mPlaylistEntries);
    }

    /**
     * This class needs to be called from the child class to create a subdirectory from the root
     * directory.
     *
     * @param subdirectory The subdirectory from root to create.
     * @return A {@code Directory} made from the root and subdirectory.
     */
    @Override
    protected Directory makeSubdirectory(final String subdirectory) {
        return ROOT.makeChildDirectory(subdirectory);
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        final Bundle bundle = new Bundle();

        bundle.putString(PARENT_FILENAME_KEY, mParent.getFilename());
        bundle.putString(DIRECTORY_FILENAME_KEY, mFilename);
        bundle.putString(DIRECTORY_NAME_KEY, mName);
        bundle.putSerializable(DIRECTORY_MAP_KEY, (Serializable) mDirectoryEntries);
        bundle.putSerializable(FILE_MAP_KEY, (Serializable) mFileEntries);
        bundle.putSerializable(PLAYLIST_MAP_KEY, (Serializable) mPlaylistEntries);

        dest.writeBundle(bundle);
    }
}
