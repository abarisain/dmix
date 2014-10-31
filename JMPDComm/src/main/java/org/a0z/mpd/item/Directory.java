/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2014 The MPDroid Project
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

package org.a0z.mpd.item;

import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Tools;
import org.a0z.mpd.connection.MPDConnection;
import org.a0z.mpd.exception.MPDException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.a0z.mpd.Tools.KEY;
import static org.a0z.mpd.Tools.VALUE;

/**
 * A class representing a MPD protocol directory.
 *
 * @author Felipe Gustavo de Almeida
 */
public final class Directory extends Item implements FilesystemTreeEntry {

    /** The MPD protocol directory separator. */
    private static final char MPD_SEPARATOR = '/';

    /** The root directory object. */
    private static final Directory ROOT;

    /** A map of directory entries from the current directory on the media server. */
    private final Map<String, Directory> mDirectoryEntries;

    /** A map of file entries from the current directory on the media server. */
    private final Map<String, Music> mFileEntries;

    /** The filename of this directory. */
    private final String mFilename;

    /** The name to display for this directory, typically the filename. */
    private final String mName;

    /** The parent directory object relative to this object. */
    private final Directory mParent;

    /** A map of playlist file entries from the current directory on the media server. */
    private final Map<String, PlaylistFile> mPlaylistEntries;

    static {
        ROOT = new Directory(null, null);
    }

    /**
     * Creates a new directory.
     *
     * @param parent   The parent directory to this directory.
     * @param filename The filename of this directory.
     */
    private Directory(final Directory parent, final String filename) {
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
    private Directory(final Directory parent, final String filename, final String name,
            final Map<String, Directory> directoryEntries, final Map<String, Music> fileEntries,
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
     * Gets the root directory for this media server.
     *
     * @return The root directory for this media server.
     * @see #refresh(org.a0z.mpd.connection.MPDConnection)
     */
    public static Directory getRoot() {
        return ROOT;
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
     * Retrieves a non-recursive list of subdirectories of this directory in natural order.
     *
     * @return A non-recursive list of subdirectories of this directory in natural order.
     */
    public Collection<Directory> getDirectories() {
        final Collection<Directory> directoriesCompared = new TreeSet<>(
                new Comparator<Directory>() {
                    @Override
                    public int compare(final Directory lhs, final Directory rhs) {
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
    public Directory getDirectory(final String filename) {
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
                return StringComparators.compareNatural(lhs.getFilename(), rhs.getFilename());
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
     * Creates a child {@code Directory} object relative to this directory object.
     *
     * @param subdirectory The subdirectory path of the root to create a {@code Directory} for.
     * @return the last component of the path created.
     * @throws java.lang.IllegalArgumentException If {@code subdirectory} starts or ends with '/'
     * @see #getRoot()
     * @see #refresh(org.a0z.mpd.connection.MPDConnection)
     */
    public Directory makeChildDirectory(final String subdirectory) {
        final String name;
        final String remainingPath;
        final int slashIndex = subdirectory.indexOf(MPD_SEPARATOR);

        if (slashIndex == 0) {
            throw new IllegalArgumentException("name starts with '" + MPD_SEPARATOR + '\'');
        }

        if (slashIndex == subdirectory.length() - 1) {
            throw new IllegalArgumentException("name ends with " + MPD_SEPARATOR + '\'');
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
     * Retrieves a database directory listing of {@code path} directory.
     *
     * @param connection A connection to the server.
     * @throws IOException  Thrown upon a communication error with the server.
     * @throws MPDException Thrown if an error occurs as a result of command execution.
     */
    public void refresh(final MPDConnection connection) throws IOException, MPDException {
        final int cacheSize = 40; /** Approximate max number of lines per file entry. */
        final List<String> response =
                connection.sendCommand(MPDCommand.MPD_CMD_LSDIR, getFullPath());
        final Collection<String> lineCache = new ArrayList<>(cacheSize);

        final Map<String, Directory> directoryEntries = new HashMap<>(mDirectoryEntries.size());
        final Map<String, Music> fileEntries = new HashMap<>(mFileEntries.size());
        final Map<String, PlaylistFile> playlistEntries = new HashMap<>(mPlaylistEntries.size());

        // Read the response backwards so it is easier to parse
        for (int i = response.size() - 1; i >= 0; i--) {

            // If we hit anything we know is an item, consume the line cache
            final String line = response.get(i);
            final String[] pair = Tools.splitResponse(line);

            switch (pair[KEY]) {
                case "directory":
                    final Directory dir = ROOT.makeChildDirectory(pair[VALUE]);

                    directoryEntries.put(dir.mFilename, dir);
                    lineCache.clear();
                    break;
                case "file":
                    // Music requires this line to be cached too.
                    // It could be done every time but it would be a waste to add and
                    // clear immediately when we're parsing a playlist or a directory
                    lineCache.add(line);

                    final Music music = Music.build(lineCache);
                    fileEntries.put(music.getFilename(), music);

                    lineCache.clear();
                    break;
                case "playlist":
                    final PlaylistFile playlistFile = new PlaylistFile(pair[VALUE]);

                    playlistEntries.put(playlistFile.getName(), playlistFile);

                    lineCache.clear();
                    break;
                default:
                    // We're in something unsupported or in an item description, cache the lines
                    lineCache.add(line);
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
