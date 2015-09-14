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

import com.anpmech.mpd.MPDCommand;
import com.anpmech.mpd.Tools;
import com.anpmech.mpd.concurrent.ResponseFuture;
import com.anpmech.mpd.connection.MPDConnection;
import com.anpmech.mpd.exception.MPDException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * This class is the generic base for the Directory items, abstracted for backend.
 *
 * <p>This item is returned from methods of the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html">database</A>
 * subsystem of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.</p>
 *
 * @param <T> The Directory type.
 */
abstract class AbstractDirectory<T extends Directory> extends Item<Directory>
        implements FilesystemTreeEntry {

    /**
     * The media server response key returned for a Directory filesystem entry.
     */
    public static final String RESPONSE_DIRECTORY = "directory";

    /** The MPD protocol directory separator. */
    protected static final char MPD_SEPARATOR = '/';

    /**
     * The class log identifier.
     */
    protected static final String TAG = "Directory";

    /** A map of directory entries from the current directory on the media server. */
    protected final Map<String, T> mDirectoryEntries;

    /** A map of file entries from the current directory on the media server. */
    protected final Map<String, Music> mFileEntries;

    /** The filename of this directory. */
    protected final String mFilename;

    /** The name to display for this directory, typically the filename. */
    protected final String mName;

    /** The parent directory object relative to this object. */
    protected final T mParent;

    /** A map of playlist file entries from the current directory on the media server. */
    protected final Map<String, PlaylistFile> mPlaylistEntries;

    /**
     * Creates a new directory.
     *
     * @param parent   The parent directory to this directory.
     * @param filename The filename of this directory.
     */
    protected AbstractDirectory(final T parent, final String filename) {
        this(parent, filename, filename, null, null, null);
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
    protected AbstractDirectory(final T parent, final String filename,
            final String name,
            final Map<String, T> directoryEntries,
            final Map<String, Music> fileEntries,
            final Map<String, PlaylistFile> playlistEntries) {
        super();

        mParent = parent;
        mFilename = filename;
        mName = name;

        if (fileEntries == null) {
            mFileEntries = new HashMap<>();
        } else {
            mFileEntries = fileEntries;
        }

        if (directoryEntries == null) {
            mDirectoryEntries = new HashMap<>();
        } else {
            mDirectoryEntries = directoryEntries;
        }

        if (playlistEntries == null) {
            mPlaylistEntries = new HashMap<>();
        } else {
            mPlaylistEntries = playlistEntries;
        }
    }

    /**
     * Check if a given directory exists as a subdirectory.
     *
     * @param filename The subdirectory filename.
     * @return True if subdirectory exists, false otherwise.
     */
    public boolean containsDir(final String filename) {
        return mDirectoryEntries.containsKey(filename);
    }

    /**
     * Compares a Directory object with a general contract of comparison that is reflexive,
     * symmetric and transitive.
     *
     * @param o The object to compare this instance with.
     * @return True if the objects are equal with regard to te general contract, false otherwise.
     */
    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            /** This has to be the same due to the class check above. */
            //noinspection unchecked
            final T directory = (T) o;

            final Object[][] equalsObjects = {
                    {mDirectoryEntries, directory.mDirectoryEntries},
                    {mFileEntries, directory.mFileEntries},
                    {mFilename, directory.mFilename},
                    {mParent, directory.mParent},
                    {mPlaylistEntries, directory.mPlaylistEntries}
            };

            if (Tools.isNotEqual(equalsObjects)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    /**
     * Retrieves a non-recursive list of subdirectories of this directory in natural order.
     *
     * @return A non-recursive list of subdirectories of this directory in natural order.
     */
    public Collection<T> getDirectories() {
        final Collection<T> directoriesCompared = new TreeSet<>(
                new Comparator<T>() {
                    @Override
                    public int compare(final T lhs, final T rhs) {
                        return StringComparators.compareNatural(lhs.getName(), rhs.getName());
                    }
                });

        synchronized (mDirectoryEntries) {
            directoriesCompared.addAll(mDirectoryEntries.values());
        }

        return directoriesCompared;
    }

    /**
     * Retrieves a sub-directory.
     *
     * @param filename name of sub-directory to retrieve.
     * @return a sub-directory.
     */
    public T getDirectory(final String filename) {
        return mDirectoryEntries.get(filename);
    }

    /**
     * Gets a {@code Music} object by the title of the music.
     *
     * @param title The title of the file of the music to be found.
     * @return Returns the {@code Music} object if found, null otherwise.
     */
    public Music getFileByTitle(final String title) {
        Music result = null;

        synchronized (mFileEntries) {
            for (final Music music : mFileEntries.values()) {
                if (music.getTitle().equals(title)) {
                    result = music;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Retrieves the filename of the current directory.
     *
     * @return The filename of the current directory.
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * Retrieves a collection of files from directory.
     *
     * @return A collection of files from this directory.
     */
    public Collection<Music> getFiles() {
        final Collection<Music> filesCompared = new TreeSet<>(new Comparator<Music>() {
            @Override
            public int compare(final Music lhs, final Music rhs) {
                return StringComparators.compareNatural(lhs.getFullPath(), rhs.getFullPath());
            }
        });

        synchronized (mFileEntries) {
            filesCompared.addAll(mFileEntries.values());
        }

        return filesCompared;
    }

    /**
     * Retrieves a full path without the forward slash prefix.
     *
     * @return The full path without the forward slash prefix.
     */
    @Override
    public String getFullPath() {
        final String fullPath;

        if (mParent == null || mParent.mParent == null) {
            fullPath = mFilename;
        } else {
            fullPath = mParent.getFullPath() + MPD_SEPARATOR + mFilename;
        }

        return fullPath;
    }

    /**
     * Retrieves the name of this directory.
     *
     * @return The name of this directory.
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * Creates a collection of playlist files from this directory in their natural order.
     *
     * @return A collection of playlist files from this directory in their natural order.
     */
    public Collection<PlaylistFile> getPlaylistFiles() {
        final Collection<PlaylistFile> playlistFilesCompared = new TreeSet<>(
                new Comparator<PlaylistFile>() {
                    @Override
                    public int compare(final PlaylistFile lhs, final PlaylistFile rhs) {
                        return StringComparators
                                .compareNatural(lhs.getFullPath(), rhs.getFullPath());
                    }
                });

        synchronized (mPlaylistEntries) {
            playlistFilesCompared.addAll(mPlaylistEntries.values());
        }

        return playlistFilesCompared;
    }

    /**
     * Returns an integer hash code for this Directory. By contract, any two objects for which
     * {@link #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * @return This Directory hash code.
     * @see Object#equals(Object)
     */
    @Override
    public int hashCode() {
        final Object[] objects = {mDirectoryEntries, mFileEntries, mFilename, mParent,
                mPlaylistEntries, mName};

        return Arrays.hashCode(objects);
    }

    /**
     * This class needs to be called from the child class to create a subdirectory from the root
     * directory.
     *
     * @param subdirectory The subdirectory from root to create.
     * @return A {@link Directory} made from the root and subdirectory.
     */
    protected abstract T makeSubdirectory(final String subdirectory);

    /**
     * Retrieves a database directory listing of {@code path} directory.
     *
     * @param connection A connection to the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void refresh(final MPDConnection connection) throws IOException, MPDException {
        final ResponseFuture future =
                connection.submit(MPDCommand.MPD_CMD_LSDIR, getFullPath());
        final ListIterator<Map.Entry<String, String>> iterator =
                future.get().reverseSplitListIterator();
        final StringBuilder lineCache = new StringBuilder();

        final Map<String, T> directoryEntries = new HashMap<>(mDirectoryEntries.size());
        final Map<String, Music> fileEntries = new HashMap<>(mFileEntries.size());
        final Map<String, PlaylistFile> playlistEntries = new HashMap<>(mPlaylistEntries.size());

        // Read the response backwards so it is easier to parse
        while (iterator.hasPrevious()) {

            // If we hit anything we know is an item, consume the line cache
            final Map.Entry<String, String> entry = iterator.previous();

            switch (entry.getKey()) {
                case RESPONSE_DIRECTORY:
                    final T dir = makeSubdirectory(entry.getValue());

                    directoryEntries.put(dir.mFilename, dir);
                    lineCache.setLength(0);
                    break;
                case AbstractMusic.RESPONSE_FILE:
                    // Music requires this line to be cached too.
                    // It could be done every time but it would be a waste to add and
                    // clear immediately when we're parsing a playlist or a directory
                    lineCache.append(entry.toString());
                    lineCache.append('\n');

                    final Music music = new Music(lineCache.toString());
                    fileEntries.put(music.getFullPath(), music);
                    lineCache.setLength(0);
                    break;
                case AbstractPlaylistFile.RESPONSE_PLAYLIST:
                    final PlaylistFile playlistFile = new PlaylistFile(entry.getValue());

                    playlistEntries.put(playlistFile.getName(), playlistFile);

                    lineCache.setLength(0);
                    break;
                default:
                    // We're in something unsupported or in an item description, cache the lines
                    lineCache.append(entry.toString());
                    lineCache.append('\n');
                    break;
            }
        }

        synchronized (mDirectoryEntries) {
            mDirectoryEntries.clear();
            mDirectoryEntries.putAll(directoryEntries);
        }

        synchronized (mFileEntries) {
            mFileEntries.clear();
            mFileEntries.putAll(fileEntries);
        }

        synchronized (mPlaylistEntries) {
            mPlaylistEntries.clear();
            mPlaylistEntries.putAll(playlistEntries);
        }
    }
}
