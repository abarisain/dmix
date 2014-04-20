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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Class representing a file/music entry in playlist.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id: Music.java 2940 2005-02-09 02:31:48Z galmeida $
 */
public class Music extends Item implements FilesystemTreeEntry {
    public static class MusicTitleComparator implements Comparator<Music> {
        public int compare(Music o1, Music o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getTitle(), o2.getTitle());
        }
    }

    /**
     * Used to indicate this <code>Music</code> is a stream.
     */
    public static final int STREAM = 1;

    /**
     * Used to indicate this <code>Music</code> is a file.
     */
    public static final int FILE = 0;
    // excluded artist names : in lower case
    private static final List<String> ARTIST_BLACK_LIST = Arrays.asList("various artists",
            "various artist");

    // Hack to discard some album artist names very long listing a long list of
    // people and not useful to fetch covers ...
    public static final int MAX_ARTIST_NAME_LENGTH = 40;

    public static int compare(String a, String b) {
        if (null == a) {
            return null == b ? 0 : -1;
        }
        if (null == b) {
            return 1;
        }
        return a.compareTo(b);
    }

    public static List<Music> getMusicFromList(List<String> response, boolean sort) {
        ArrayList<Music> result = new ArrayList<Music>();
        LinkedList<String> lineCache = new LinkedList<String>();

        for (String line : response) {
            if (line.startsWith("file: ")) {
                if (lineCache.size() != 0) {
                    result.add(new Music(lineCache));
                    lineCache.clear();
                }
            }
            lineCache.add(line);
        }

        if (lineCache.size() != 0) {
            result.add(new Music(lineCache));
        }

        if (sort) {
            Collections.sort(result);
        }

        return result;
    }

    private static boolean isEmpty(String s) {
        return null == s || 0 == s.length();
    }

    public static boolean isValidArtist(String artist) {
        return !isEmpty(artist) && !ARTIST_BLACK_LIST.contains(artist.toLowerCase())
                && artist.length() < MAX_ARTIST_NAME_LENGTH;
    }

    public static String timeToString(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }

        long hours = seconds / 3600;
        seconds -= 3600 * hours;
        long minutes = seconds / 60;
        seconds -= minutes * 60;
        if (hours == 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }

    private String album = "";

    private String artist = "";

    private String albumartist = "";

    private String fullpath;

    private int disc = -1;

    private long date = -1;

    private long time = -1;

    private Directory parent;

    private String title;

    private int totalTracks = -1;

    private int track = -1;

    private int songId = -1;

    private int pos = -1;

    private String name;

    public Music() {
    }

    /**
     * Constructs a new Music.
     * 
     * @param response server response, which gets parsed into the instance.
     */
    Music(List<String> response) {
        for (String line : response) {
            if (line.startsWith("file:")) {
                this.fullpath = line.substring("file: ".length());
                if(this.fullpath.contains("://")) {
                    String n = getStreamName();
                    if (null != n && !n.isEmpty()) {
                        this.name = n;
                    }
                }
            } else if (line.startsWith("Artist:")) {
                this.artist = line.substring("Artist: ".length());
            } else if (line.startsWith("AlbumArtist:")) {
                this.albumartist = line.substring("AlbumArtist: ".length());
            } else if (line.startsWith("Album:")) {
                this.album = line.substring("Album: ".length());
            } else if (line.startsWith("Title:")) {
                this.title = line.substring("Title: ".length());
            } else if (line.startsWith("Name:") && this.name == null) {
                /**
                 * this.name may already be assigned to the stream name in file conditional
                 */
                this.name = line.substring("Name: ".length());
            } else if (line.startsWith("Track:")) {
                String[] aux = line.substring("Track: ".length()).split("/");
                if (aux.length > 0) {
                    try {
                        this.track = Integer.parseInt(aux[0]);
                        if (aux.length > 1) {
                            this.totalTracks = Integer.parseInt(aux[1]);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            } else if (line.startsWith("Disc:")) {
                String[] aux = line.substring("Disc: ".length()).split("/");
                if (aux.length > 0) {
                    try {
                        this.disc = Integer.parseInt(aux[0]);
                    } catch (NumberFormatException e) {
                    }
                }
            } else if (line.startsWith("Time:")) {
                try {
                    this.time = Long.parseLong(line.substring("Time: ".length()));
                } catch (NumberFormatException e) {
                }
            } else if (line.startsWith("Id:")) {
                try {
                    this.songId = Integer.parseInt(line.substring("Id: ".length()));
                } catch (NumberFormatException e) {
                }
            } else if (line.startsWith("Pos:")) {
                try {
                    this.pos = Integer.parseInt(line.substring("Pos: ".length()));
                } catch (NumberFormatException e) {
                }
            } else if (line.startsWith("Date:")) {
                try {
                    this.date = Long.parseLong(line.substring("Date: ".length()).replaceAll("\\D+",
                            ""));
                } catch (NumberFormatException e) {
                }
            } /**
             * Ignore potential else block, there could be some id3 tags which are not common and
             * are, therefore, not implemented here.
             */
        }
    }

    protected Music(String album, String artist, String albumartist, String fullpath, int disc,
            long date, long time, Directory parent, String title, int totalTracks, int track,
            int songId, int pos, String name) {
        this.album = album;
        this.artist = artist;
        this.albumartist = albumartist;
        this.fullpath = fullpath;
        this.disc = disc;
        this.date = date;
        this.time = time;
        this.parent = parent;
        this.title = title;
        this.totalTracks = totalTracks;
        this.track = track;
        this.songId = songId;
        this.pos = pos;
        this.name = name;
    }

    @Override
    public int compareTo(Item o) {
        if (o instanceof Music) {
            Music om = (Music) o;
            int compare;

            // songId overrides every other sorting method. It's used for playlists/queue
            if (songId != om.songId) {
                return songId < om.songId ? -1 : 1;
            }

            // If enabled, sort by disc and track number
            if (MPD.sortByTrackNumber()) {
                if (disc != om.disc && disc != -1 && om.disc != -1) {
                    return disc < om.disc ? -1 : 1;
                }
                if (track != om.track && track != -1 && om.track != -1) {
                    return track < om.track ? -1 : 1;
                }
            }

            // Order by song title (getTitle() fallback on filenames)
            compare = compare(getTitle(), om.getTitle());
            if (0 != compare) {
                return compare;
            }

            // Then order by name (streams)
            compare = compare(name, om.name);
            if (0 != compare) {
                return compare;
            }

            // Last resort is to order by fullpath
            return compare(fullpath, om.fullpath);
        }
        return super.compareTo(o);
    }

    /**
     * Retrieves album name.
     * 
     * @return album name.
     */
    public String getAlbum() {
        return album;
    }

    /**
     * Retrieves the original album artist name.
     * 
     * @return album artist name or null if it is not set.
     */
    public String getAlbumArtist() {
        return albumartist;
    }

    public Artist getAlbumArtistAsArtist() {
        return new Artist(albumartist);
    }

    public String getAlbumArtistOrArtist() {
        return isEmpty(albumartist) ? artist : albumartist;
    }

    public Album getAlbumAsAlbum() {
        boolean is_aa = !isEmpty(albumartist);
        Artist art = new Artist((is_aa ? albumartist : artist));
        return new Album(album, art, is_aa);
    }

    public AlbumInfo getAlbumInfo() {
        return new AlbumInfo(getAlbumArtistOrArtist(), getAlbum(), getPath(), getFilename());
    }

    /**
     * Retrieves artist name.
     * 
     * @return artist name.
     */
    public String getArtist() {
        return artist;
    }

    public Artist getArtistAsArtist() {
        return new Artist(artist);
    }

    public long getDate() {
        return date;
    }

    public int getDisc() {
        return disc;
    }

    /**
     * TODO test this for streams Retrieves filename.
     * 
     * @return filename.
     */
    public String getFilename() {
        int pos = fullpath.lastIndexOf("/");
        if (pos == -1 || pos == fullpath.length() - 1) {
            return fullpath;
        } else {
            return fullpath.substring(pos + 1);
        }
    }

    /**
     * Retrieves album artist name but discard names like various ...
     * 
     * @return album artist name.
     */
    public String getFilteredAlbumArtist() {
        return isValidArtist(albumartist) ? albumartist : artist;
    }

    /**
     * Retrieves date as string (##:##).
     * 
     * @return date as string.
     */
    public String getFormattedTime() {
        return timeToString(time);
    }

    /**
     * Retrieves full path name.
     * 
     * @return full path name.
     */
    public String getFullpath() {
        return fullpath;
    }

    /**
     * Returns <code>Music.FILE</code> if this <code>Music</code> is a file and
     * <code>Music.STREAM</code> if it's a stream.
     * 
     * @return <code>Music.FILE</code> if this <code>Music</code> is a file and
     *         <code>Music.STREAM</code> if it's a stream.
     * @see Music#FILE
     * @see Music#STREAM
     */
    public int getMedia() {
        if (this.getFullpath().indexOf("://") == -1) {
            return FILE;
        } else {
            return STREAM;
        }
    }

    /**
     * Retrieves stream's name.
     * 
     * @return stream's name.
     */
    public String getName() {
        if (isEmpty(name)) {
            return isEmpty(getStreamName()) ? getFilename() : getStreamName();
        } else {
            return name;
        }
    }

    /**
     * Retrieves file's parent directory
     * 
     * @return file's parent directory
     */
    public String getParent() {
        if (fullpath == null) {
            return null;
        }
        int pos = fullpath.lastIndexOf("/");
        if (pos == -1) {
            return null;
        } else {
            return fullpath.substring(0, pos);
        }
    }

    /**
     * Retrieves file's parent directory
     * 
     * @return file's parent directory
     */
    public Directory getParentDirectory() {
        return parent;
    }

    /**
     * Retrieves path of music file (does not start or end with /)
     * 
     * @return path of music file.
     */
    public String getPath() {
        String result;
        if (null != fullpath && fullpath.length() > getFilename().length()) {
            result = fullpath.substring(0, fullpath.length() - getFilename().length() - 1);
        } else {
            result = "";
        }
        return result;
    }

    /**
     * Retrieves current song stopped on or playing, playlist song number.
     * 
     * @return current song stopped on or playing, playlist song number.
     */
    public int getPos() {
        return pos;
    }

    /**
     * Retrieves current song playlist id.
     * 
     * @return current song playlist id.
     */
    public int getSongId() {
        return songId;
    }

    private String getStreamName() {
        if (null != fullpath && !fullpath.isEmpty()) {
            int pos = fullpath.indexOf("#");
            if (pos > 1) {
                return fullpath.substring(pos + 1, fullpath.length());
            }
        }
        return null;
    }

    /**
     * Retrieves playing time.
     * 
     * @return playing time.
     */
    public long getTime() {
        return time;
    }

    /**
     * Retrieves title.
     * 
     * @return title.
     */
    public String getTitle() {
        if (isEmpty(title)) {
            return getFilename();
        } else {
            return title;
        }
    }

    /**
     * Retrieves total number of tracks from this music's album when available.
     * This can contain letters!
     * 
     * @return total number of tracks from this music's album when available.
     */
    public int getTotalTracks() {
        return totalTracks;
    }

    /**
     * Retrieves track number. This can contain letters!
     * 
     * @return track number.
     */
    public int getTrack() {
        return track;
    }

    public boolean haveTitle() {
        return null != title && title.length() > 0;
    }

    public boolean isStream() {
        return null != fullpath && fullpath.contains("://");
    }

    /**
     * Retrieves a string representation of the object.
     * 
     * @return a string representation of the object.
     * @see java.lang.Object#toString()
     */
    /*
     * public String toString() { return track + " - " + album + " - " + artist
     * + " - " + title + " (" + fullpath + ")"; }
     */
    @Override
    public String mainText() {
        return getTitle();
    }

    /**
     * Defines album name.
     * 
     * @param string album name.
     */
    public void setAlbum(String string) {
        album = string;
    }

    public void setAlbumArtist(String albumartist) {
        this.albumartist = albumartist;
    }

    /**
     * Defines artist name.
     * 
     * @param string artist name.
     */
    public void setArtist(String string) {
        artist = string;
    }

    public void setDate(long value) {
        date = value;
    }

    public void setDisc(int value) {
        disc = value;
    }

    /**
     * Set file's parent directory
     * 
     * @param directory file's parent directory
     */
    public void setParent(Directory directory) {
        parent = directory;
    }

    public void setSongId(int value) {
        songId = value;
    }

    /**
     * Defines playing time.
     * 
     * @param l playing time.
     */
    public void setTime(long l) {
        time = l;
    }

    /**
     * Defines title.
     * 
     * @param string title.
     */
    public void setTitle(String string) {
        title = string;
    }

    /**
     * Defines total number of tracks from this music's album when available.
     * 
     * @param total total number of tracks from this music's album when
     *            available.
     */
    public void setTotalTracks(int total) {
        totalTracks = total;
    }

    /**
     * Defines track number.
     * 
     * @param num track number.
     */
    public void setTrack(int num) {
        track = num;
    }

    @Override
    public String subText() {
        return timeToString(time);
    }

    /**
     * Copies, artist, album, title, time, totalTracks and track from another
     * <code>music</code>.
     * 
     * @param other <code>Music</code> to copy data from.
     */
    void update(Music other) {
        this.setArtist(other.getArtist());
        this.setAlbum(other.getAlbum());
        this.setTitle(other.getTitle());
        this.setTime(other.getTime());
        this.setTotalTracks(other.getTotalTracks());
        this.setTrack(other.getTrack());
        /*
         * this.setGenre(other.getGenre());
         * this.setSoundtrack(other.getSoundtrack());
         * this.setComposer(other.getComposer());
         */
        this.setDisc(other.getDisc());
        this.setDate(other.getDate());
    }
}
