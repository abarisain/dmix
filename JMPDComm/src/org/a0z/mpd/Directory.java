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

package org.a0z.mpd;

import org.a0z.mpd.exception.InvalidParameterException;
import org.a0z.mpd.exception.MPDServerException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Class representing a directory.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id: Directory.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public final class Directory extends Item implements FilesystemTreeEntry {
    /**
     * Creates a new directory.
     * 
     * @param mpd MPD controller.
     * @return last path component.
     */
    public static Directory makeRootDirectory(MPD mpd) {
        return new Directory(mpd, null, "");
    }
    private Map<String, Music> files;
    private Map<String, Directory> directories;
    private Map<String, PlaylistFile> playlists;
    private Directory parent;
    private String filename;
    private String name; // name to display, usually = filename

    private MPD mpd;

    /**
     * Clones a directory.
     */
    public Directory(Directory dir) {
        this.mpd = dir.mpd;
        this.name = dir.name;
        this.filename = dir.filename;
        this.parent = dir.parent;
        this.files = dir.files;
        this.directories = dir.directories;
        this.playlists = dir.playlists;
    }

    /**
     * Creates a new directory.
     * 
     * @param mpd MPD controller.
     * @param parent parent directory.
     * @param filename directory filename.
     */
    private Directory(MPD mpd, Directory parent, String filename) {
        this.mpd = mpd;
        this.name = filename;
        this.filename = filename;
        this.parent = parent;
        this.files = new HashMap<String, Music>();
        this.directories = new HashMap<String, Directory>();
        this.playlists = new HashMap<String, PlaylistFile>();
    }

    /**
     * Adds a file, creating path directories.
     * 
     * @param file file to be added
     */
    public void addFile(Music file) {
        if (getFullpath().compareTo(file.getPath()) == 0) {
            file.setParent(this);
            files.put(file.getFilename(), file);
        } else {
            Directory dir = makeDirectory(file.getPath());
            dir.addFile(file);
        }
    }

    /**
     * Check if a given directory exists as a sub-directory.
     * 
     * @param filename sub-directory filename.
     * @return true if sub-directory exists, false if not.
     */
    public boolean containsDir(String filename) {
        return directories.containsKey(filename);
    }

    /**
     * Retrieves sub-directories.
     * 
     * @return sub-directories.
     */
    public TreeSet<Directory> getDirectories() {
        TreeSet<Directory> c = new TreeSet<Directory>(new Comparator<Directory>() {
            public int compare(Directory o1, Directory o2) {
                return StringComparators.compareNatural(o1.getName(), o2.getName());
            }
        });

        for (Directory item : directories.values())
            c.add(item);
        return c;
    }

    /**
     * Retrieves a sub-directory.
     * 
     * @param filename name of sub-directory to retrieve.
     * @return a sub-directory.
     */
    public Directory getDirectory(String filename) {
        return directories.get(filename);
    }

    /**
     * Gets Music object by title
     * 
     * @param title title of the file to be returned
     * @return Returns null if title not found
     */
    public Music getFileByTitle(String title) {
        for (Music music : files.values()) {
            if (music.getTitle().equals(title))
                return music;
        }
        return null;
    }

    /**
     * Retrieves file name.
     * 
     * @return filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Retrieves files from directory.
     * 
     * @return files from directory.
     */
    public TreeSet<Music> getFiles() {
        TreeSet<Music> c = new TreeSet<Music>(new Comparator<Music>() {
            public int compare(Music o1, Music o2) {
                return StringComparators.compareNatural(o1.getFilename(), o2.getFilename());
            }
        });

        for (Music item : files.values())
            c.add(item);
        return c;
    }

    /**
     * Retrieves directory's full path (does not start with /)
     * 
     * @return full path
     */
    public String getFullpath() {
        if (getParent() != null && getParent().getParent() != null) {
            return getParent().getFullpath() + "/" + getFilename();
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
        return name;
    }

    /**
     * Retrieves parent directory.
     * 
     * @return parent directory.
     */
    public Directory getParent() {
        return parent;
    }

    public TreeSet<PlaylistFile> getPlaylistFiles() {
        TreeSet<PlaylistFile> c = new TreeSet<PlaylistFile>(new Comparator<PlaylistFile>() {
            public int compare(PlaylistFile o1, PlaylistFile o2) {
                return StringComparators.compareNatural(o1.getFullpath(), o2.getFullpath());
            }
        });

        for (PlaylistFile item : playlists.values())
            c.add(item);
        return c;
    }

    /**
     * Given a path not starting or ending with '/', creates all directories on
     * this path.
     * 
     * @param subPath path, must not start or end with '/'.
     * @return the last component of the path created.
     */
    public Directory makeDirectory(String subPath) {
        String name;
        String remainingPath;
        int slashIndex = subPath.indexOf('/');

        if (slashIndex == 0)
            throw new InvalidParameterException("name starts with '/'");

        // split path
        if (slashIndex == -1) {
            name = subPath;
            remainingPath = null;
        } else {
            name = subPath.substring(0, slashIndex);
            remainingPath = subPath.substring(slashIndex + 1);
        }

        // create directory
        Directory dir;
        if (!directories.containsKey(name)) {
            dir = new Directory(mpd, this, name);
            directories.put(dir.getFilename(), dir);
        } else {
            dir = directories.get(name);
        }

        // create remainder
        if (remainingPath != null)
            return dir.makeDirectory(remainingPath);
        return dir;
    }

    /**
     * Refresh directory contents (not recursive).
     * 
     * @throws MPDServerException if an error occurs while contacting server.
     */
    public void refreshData() throws MPDServerException {
        List<FilesystemTreeEntry> c = mpd.getDir(this.getFullpath());
        for (FilesystemTreeEntry o : c) {
            if (o instanceof Directory) {
                Directory dir = (Directory) o;
                if (!directories.containsKey(dir.getFilename())) {
                    directories.put(dir.getFilename(), dir);
                }
            } else if (o instanceof Music) {
                Music music = (Music) o;
                if (!files.containsKey(music.getFilename())) {
                    files.put(music.getFilename(), music);
                } else {
                    Music old = files.get(music.getFilename());
                    old.update(music);
                }
            } else if (o instanceof PlaylistFile) {
                PlaylistFile pl = (PlaylistFile) o;
                if (!playlists.containsKey(pl.getName())) {
                    playlists.put(pl.getName(), pl);
                }
            }
        }
    }

    /**
     * Sets name.
     * 
     * @param name name to be displayed
     */
    public void setName(String name) {
        this.name = name;
    }
}
