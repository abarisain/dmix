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

import org.a0z.mpd.MPD;
import org.a0z.mpd.Tools;
import org.a0z.mpd.exception.MPDServerException;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Class representing a directory.
 *
 * @author Felipe Gustavo de Almeida
 */
public final class Directory extends Item implements FilesystemTreeEntry {

    private final Map<String, Directory> mDirectoryEntries;

    private final Map<String, Music> mFileEntries;

    private final String mFilename;

    private final MPD mMPD;

    private final Directory mParent;

    private final Map<String, PlaylistFile> mPlayLists;

    private String mName; // name to display, usually = filename

    /**
     * Clones a directory.
     */
    public Directory(final Directory dir) {
        super();
        mMPD = dir.mMPD;
        mName = dir.mName;
        mFilename = dir.mFilename;
        mParent = dir.mParent;
        mFileEntries = dir.mFileEntries;
        mDirectoryEntries = dir.mDirectoryEntries;
        mPlayLists = dir.mPlayLists;
    }

    /**
     * Creates a new directory.
     *
     * @param mpd      MPD controller.
     * @param parent   mParent directory.
     * @param filename directory filename.
     */
    private Directory(final MPD mpd, final Directory parent, final String filename) {
        super();
        mMPD = mpd;
        mName = filename;
        mFilename = filename;
        mParent = parent;
        mFileEntries = new HashMap<>();
        mDirectoryEntries = new HashMap<>();
        mPlayLists = new HashMap<>();
    }

    /**
     * Retrieves a database directory listing of {@code path} directory.
     *
     * @param response The server response.
     * @param mpd      The {@code MPD} object instance.
     * @return a {@code Collection} of {@code Music} and
     * {@code Directory} representing directory entries.
     * @see Music
     */
    public static List<FilesystemTreeEntry> getDir(final List<String> response, final MPD mpd) {
        final LinkedList<String> lineCache = new LinkedList<>();
        final LinkedList<FilesystemTreeEntry> result = new LinkedList<>();

        // Read the response backwards so it is easier to parse
        for (int i = response.size() - 1; i >= 0; i--) {

            // If we hit anything we know is an item, consume the linecache
            final String line = response.get(i);
            final String[] lines = Tools.splitResponse(line);

            switch (lines[0]) {
                case "directory":
                    result.add(makeRootDirectory(mpd).makeDirectory(lines[1]));
                    lineCache.clear();
                    break;
                case "file":
                    // Music requires this line to be cached too.
                    // It could be done every time but it would be a waste to add and
                    // clear immediately when we're parsing a playlist or a directory
                    lineCache.add(line);
                    result.add(Music.build(lineCache));
                    lineCache.clear();
                    break;
                case "playlist":
                    result.add(new PlaylistFile(lines[1]));
                    lineCache.clear();
                    break;
                default:
                    // We're in something unsupported or in an item description, cache the lines
                    lineCache.add(line);
                    break;
            }
        }

        // Since we read the list backwards, reverse the results ordering.
        Collections.reverse(result);
        return result;
    }

    /**
     * Creates a new directory.
     *
     * @param mpd MPD controller.
     * @return last path component.
     */
    public static Directory makeRootDirectory(final MPD mpd) {
        return new Directory(mpd, null, "");
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
    public TreeSet<Directory> getDirectories() {
        final TreeSet<Directory> c = new TreeSet<>(new Comparator<Directory>() {
            public int compare(final Directory o1, final Directory o2) {
                return StringComparators.compareNatural(o1.getName(), o2.getName());
            }
        });

        for (final Directory item : mDirectoryEntries.values()) {
            c.add(item);
        }
        return c;
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
    public TreeSet<Music> getFiles() {
        final TreeSet<Music> c = new TreeSet<>(new Comparator<Music>() {
            public int compare(final Music o1, final Music o2) {
                return StringComparators.compareNatural(o1.getFilename(), o2.getFilename());
            }
        });

        for (final Music item : mFileEntries.values()) {
            c.add(item);
        }
        return c;
    }

    /**
     * Retrieves directory's full path (does not start with /)
     *
     * @return full path
     */
    public String getFullPath() {
        if (getParent() != null && getParent().getParent() != null) {
            return getParent().getFullPath() + '/' + getFilename();
        } else {
            return getFilename();
        }
    }

    /**
     * Retrieves directory name.
     *
     * @return directory name.
     */
    public String getName() {
        return mName;
    }

    /**
     * Retrieves mParent directory.
     *
     * @return mParent directory.
     */
    public Directory getParent() {
        return mParent;
    }

    public TreeSet<PlaylistFile> getPlaylistFiles() {
        final TreeSet<PlaylistFile> c = new TreeSet<>(new Comparator<PlaylistFile>() {
            public int compare(final PlaylistFile o1, final PlaylistFile o2) {
                return StringComparators.compareNatural(o1.getFullPath(), o2.getFullPath());
            }
        });

        for (final PlaylistFile item : mPlayLists.values()) {
            c.add(item);
        }
        return c;
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
            dir = new Directory(mMPD, this, name);
            mDirectoryEntries.put(dir.getFilename(), dir);
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
     * Refresh directory contents (not recursive).
     *
     * @throws MPDServerException if an error occurs while contacting server.
     */
    public void refreshData() throws MPDServerException {
        final List<FilesystemTreeEntry> c = mMPD.getDir(getFullPath());
        for (final FilesystemTreeEntry o : c) {
            if (o instanceof Directory) {
                final Directory dir = (Directory) o;
                if (!mDirectoryEntries.containsKey(dir.getFilename())) {
                    mDirectoryEntries.put(dir.getFilename(), dir);
                }
            } else if (o instanceof Music) {
                final Music music = (Music) o;
                final String filename = music.getFilename();

                if (mFileEntries.containsKey(filename)) {
                    final Music oldMusic = mFileEntries.get(filename);

                    if (!music.equals(oldMusic)) {
                        mFileEntries.put(filename, music);
                    }
                } else {
                    mFileEntries.put(filename, music);
                }
            } else if (o instanceof PlaylistFile) {
                final PlaylistFile pl = (PlaylistFile) o;
                if (!mPlayLists.containsKey(pl.getName())) {
                    mPlayLists.put(pl.getName(), pl);
                }
            }
        }
    }

    /**
     * Sets name.
     *
     * @param name name to be displayed
     */
    public void setName(final String name) {
        mName = name;
    }
}
