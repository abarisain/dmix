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
import org.a0z.mpd.exception.MPDServerException;

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
 * Class representing a directory.
 *
 * @author Felipe Gustavo de Almeida
 */
public final class Directory extends Item implements FilesystemTreeEntry {

    private static final Directory ROOT;

    private final Map<String, Directory> mDirectoryEntries;

    private final Map<String, Music> mFileEntries;

    private final String mFilename;

    private final String mName; // name to display, usually = filename

    private final Directory mParent;

    private final Map<String, PlaylistFile> mPlaylistEntries;

    static {
        ROOT = new Directory(null, null);
    }

    /**
     * Creates a new directory.
     *
     * @param parent   mParent directory.
     * @param filename directory filename.
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

    public static Directory getRoot() {
        return ROOT;
    }

    /**
     * Check if a given directory exists as a sub-directory.
     *
     * @param filename sub-directory filename.
     * @return true if sub-directory exists, false if not.
     */
    public boolean containsDir(final String filename) {
        return mDirectoryEntries.containsKey(filename);
    }

    /**
     * Retrieves sub-directories.
     *
     * @return sub-directories.
     */
    public Collection<Directory> getDirectories() {
        final Collection<Directory> directoriesCompared = new TreeSet<>(
                new Comparator<Directory>() {
                    public int compare(final Directory lhs, final Directory rhs) {
                        return StringComparators.compareNatural(lhs.getName(), rhs.getName());
                    }
                });

        for (final Directory item : mDirectoryEntries.values()) {
            directoriesCompared.add(item);
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
     * Gets Music object by title
     *
     * @param title title of the file to be returned
     * @return Returns null if title not found
     */
    public Music getFileByTitle(final String title) {
        for (final Music music : mFileEntries.values()) {
            if (music.getTitle().equals(title)) {
                return music;
            }
        }
        return null;
    }

    /**
     * Retrieves file name.
     *
     * @return filename
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * Retrieves files from directory.
     *
     * @return files from directory.
     */
    public Collection<Music> getFiles() {
        final Collection<Music> filesCompared = new TreeSet<>(new Comparator<Music>() {
            @Override
            public int compare(final Music lhs, final Music rhs) {
                return StringComparators.compareNatural(lhs.getFilename(), rhs.getFilename());
            }
        });

        for (final Music item : mFileEntries.values()) {
            filesCompared.add(item);
        }
        return filesCompared;
    }

    /**
     * Retrieves directory's full path (does not start with /)
     *
     * @return full path
     */
    @Override
    public String getFullPath() {
        if (mParent != null && mParent.mParent != null) {
            return mParent.getFullPath() + '/' + mFilename;
        } else {
            return mFilename;
        }
    }

    /**
     * Retrieves directory name.
     *
     * @return directory name.
     */
    @Override
    public String getName() {
        return mName;
    }

    public Collection<PlaylistFile> getPlaylistFiles() {
        final Collection<PlaylistFile> playlistFilesCompared = new TreeSet<>(
                new Comparator<PlaylistFile>() {
                    @Override
                    public int compare(final PlaylistFile lhs, final PlaylistFile rhs) {
                        return StringComparators
                                .compareNatural(lhs.getFullPath(), rhs.getFullPath());
                    }
                });

        for (final PlaylistFile item : mPlaylistEntries.values()) {
            playlistFilesCompared.add(item);
        }
        return playlistFilesCompared;
    }

    /**
     * Given a path not starting or ending with '/', creates all directories on
     * this path.
     *
     * @param subPath path, must not start or end with '/'.
     * @return the last component of the path created.
     */
    public Directory makeDirectory(final String subPath) {
        final String name;
        final String remainingPath;
        final int slashIndex = subPath.indexOf('/');

        if (slashIndex == 0) {
            throw new IllegalArgumentException("name starts with '/'");
        }

        // split path
        if (slashIndex == -1) {
            name = subPath;
            remainingPath = null;
        } else {
            name = subPath.substring(0, slashIndex);
            remainingPath = subPath.substring(slashIndex + 1);
        }

        // create directory
        final Directory dir;
        if (!mDirectoryEntries.containsKey(name)) {
            dir = new Directory(this, name);
            mDirectoryEntries.put(dir.mFilename, dir);
        } else {
            dir = mDirectoryEntries.get(name);
        }

        // create remainder
        if (remainingPath != null) {
            return dir.makeDirectory(remainingPath);
        }
        return dir;
    }

    /**
     * Gives the parent to this directory object with the name given in the parameter.
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
     */
    public void refresh(final MPDConnection connection)
            throws MPDServerException {
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
                    final Directory dir = ROOT.makeDirectory(pair[VALUE]);

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
