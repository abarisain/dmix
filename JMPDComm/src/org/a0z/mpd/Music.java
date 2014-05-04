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

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        final List<Music> result = new ArrayList<>(response.size());
        LinkedList<String> lineCache = new LinkedList<>();

        for (String line : response) {
            if (line.startsWith("file: ")) {
                if (!lineCache.isEmpty()) {
                    result.add(new Music(lineCache));
                    lineCache.clear();
                }
            }
            lineCache.add(line);
        }

        if (!lineCache.isEmpty()) {
            result.add(new Music(lineCache));
        }

        if (sort) {
            Collections.sort(result);
        }

        return result;
    }

    private static boolean isEmpty(String s) {
        return null == s || s.isEmpty();
    }

    public static boolean isValidArtist(String artist) {
        return !isEmpty(artist) && !ARTIST_BLACK_LIST.contains(artist.toLowerCase())
                && artist.length() < MAX_ARTIST_NAME_LENGTH;
    }

    /**
     * This method takes seconds and converts it into HH:MM:SS
     *
     * @param totalSeconds Seconds to convert to a string.
     * @return Returns time formatted from the {@code totalSeconds} in format HH:MM:SS.
     */
    public static String timeToString(final long totalSeconds) {
        long seconds = totalSeconds < 0L ? 0L : totalSeconds;
        final String result;

        final long secondsInHour = 3600L;
        final long secondsInMinute = 60L;

        final long hours = seconds / secondsInHour;
        seconds -= secondsInHour * hours;

        final long minutes = seconds / secondsInMinute;
        seconds -= minutes * secondsInMinute;

        if (hours == 0) {
            result = String.format("%02d:%02d", minutes, seconds);
        } else {
            result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        return result;
    }

    private String album = "";

    private String artist = "";

    private String albumartist = "";

    private String fullpath;

    private int disc = -1;

    private long date = -1L;

    private long time = -1L;

    private Directory parent;

    private String title;

    private int totalTracks = -1;

    private int track = -1;

    private int songId = -1;

    private int pos = -1;

    private String name;

    private static final String TAG = "org.a0z.mpd.Music";

    public Music() {
    }

    /**
     * This is a regular expression pattern matcher
     * for the MPD protocol delimiter ": ".
     */
    private static final Pattern MPD_DELIMITER = Pattern.compile(": ");

    /**
     * The time response has it's own delimiter.
     */
    private static final Pattern FORWARD_SLASH_DELIMITER = Pattern.compile("/");

    /**
     * The date response has it's own delimiter.
     */
    private static final Pattern DATE_DELIMITER = Pattern.compile("\\D+");

    /**
     * Constructs a new Music.
     *
     * @param response server response, which gets parsed into the instance.
     */
    Music(final List<String> response) {
        super();
        for (final String line : response) {
            final String[] lines = MPD_DELIMITER.split(line);

            switch (lines[0]) {
                case "file":
                    fullpath = lines[1];
                    if (fullpath.contains("://")) {
                        extractStreamName();
                    }
                    break;
                case "Album":
                    album = lines[1];
                    break;
                case "AlbumArtist":
                    albumartist = lines[1];
                    break;
                case "Artist":
                    artist = lines[1];
                    break;
                case "Date":
                    try {
                        final Matcher matcher = DATE_DELIMITER.matcher(lines[1]);
                        date = Long.parseLong(matcher.replaceAll(""));
                    } catch (final NumberFormatException e) {
                        Log.w(TAG, "Not a valid date.", e);
                    }
                    break;
                case "Disc":
                    final String[] discs = FORWARD_SLASH_DELIMITER.split(lines[1]);
                    if (discs.length > 0) {
                        try {
                            disc = Integer.parseInt(discs[0]);
                        } catch (final NumberFormatException e) {
                            Log.w(TAG, "Not a valid disc number.", e);
                        }
                    }
                    break;
                case "Id":
                    try {
                        songId = Integer.parseInt(lines[1]);
                    } catch (final NumberFormatException e) {
                        Log.e(TAG, "Not a valid song ID.", e);
                    }
                    break;
                case "Name":
                    /**
                     * name may already be assigned to the stream name in file conditional
                     */
                    if (name == null) {
                        name = lines[1];
                    }
                    break;
                case "Pos":
                    try {
                        pos = Integer.parseInt(lines[1]);
                    } catch (final NumberFormatException e) {
                        Log.e(TAG, "Not a valid song position.", e);
                    }
                    break;
                case "Time":
                    try {
                        time = Long.parseLong(lines[1]);
                    } catch (final NumberFormatException e) {
                        Log.e(TAG, "Not a valid time number.", e);
                    }
                    break;
                case "Title":
                    title = lines[1];
                    break;
                case "Track":
                    final String[] tracks = FORWARD_SLASH_DELIMITER.split(lines[1]);
                    if (tracks.length > 0) {
                        try {
                            track = Integer.parseInt(tracks[0]);
                            if (tracks.length > 1) {
                                totalTracks = Integer.parseInt(tracks[1]);
                            }
                        } catch (final NumberFormatException e) {
                            Log.e(TAG, "Not a valid track number", e);
                        }
                    }
                    break;
                default:
                    /**
                     * Ignore everything else, there are a lot of
                     * uninteresting blocks the server might send.
                     */
                    break;
            }
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
        int pos = fullpath.lastIndexOf('/');
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
     * Retrieves stream's name.
     *
     * @return stream's name.
     */
    public String getName() {
        return isEmpty(name) ? getFilename() : name;
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
        int pos = fullpath.lastIndexOf('/');
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

    private void extractStreamName() {
        if (null != fullpath && !fullpath.isEmpty()) {
            int pos = fullpath.indexOf("#");
            if (pos > 1) {
                name=fullpath.substring(pos + 1, fullpath.length());
                name=name.replace("${hash}", "#");
                fullpath=fullpath.substring(0, pos-1);
            }
        }
    }

    public static String addStreamName(String url, String name) {
        if (null == name || name.isEmpty()) {
            return url;
        }
        String fixed = name.replace("#", "${hash}");
        try {
            String path = new URL(url).getPath();
            if (null == path || path.isEmpty()) {
                return url + "/#" + fixed;
            }
        } catch (MalformedURLException e) {
        }
        return url + "#" + fixed;
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
        return null != title && !title.isEmpty();
    }

    public boolean isStream() {
        return null != fullpath && fullpath.contains("://");
    }

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
     *              available.
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

    /** Do not implement toString(), JMPDComm is dependent on the implementation in Item.java. */

    /**
     * Copies, artist, album, title, time, totalTracks and track from another
     * {@code music}.
     *
     * @param other {@code Music} to copy data from.
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
