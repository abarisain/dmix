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

import org.a0z.mpd.AlbumInfo;
import org.a0z.mpd.Log;
import org.a0z.mpd.MPD;
import org.a0z.mpd.Tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a file/music entry in playlist.
 *
 * @author Felipe Gustavo de Almeida
 */
public class Music extends Item implements FilesystemTreeEntry {

    // Hack to discard some album artist names very long listing a long list of
    // people and not useful to fetch covers ...
    public static final int MAX_ARTIST_NAME_LENGTH = 40;

    // excluded artist names : in lower case
    private static final List<String> ARTIST_BLACK_LIST = Arrays.asList("various artists",
            "various artist");

    /** The maximum number of key/value pairs for a music item response. */
    private static final int MUSIC_ATTRIBUTES = 30;

    private static final String TAG = "Music";

    /**
     * The date response has it's own delimiter.
     */
    private static final Pattern DATE_DELIMITER = Pattern.compile("\\D+");

    private final String mAlbum;

    private final String mArtist;

    private final String mAlbumArtist;

    private final String mGenre;

    private final String mFullPath;

    private final int mDisc;

    private final long mDate;

    private final String mName;

    private final int mSongPos;

    private final long mTime;

    private final String mTitle;

    private final int mTotalTracks;

    private final int mTrack;

    private int mSongId;

    public Music() {
        this(null, /** Album */
                null, /** Artist */
                null, /** AlbumArtist */
                null, /** FullPath */
                -1, /** Disc */
                -1L, /** Date */
                null, /** Genre */
                -1L, /** Time */
                null, /** Title */
                -1, /** TotalTracks*/
                -1, /** Track */
                -1, /** SongID */
                -1, /** SongPos */
                null); /** Name */

    }

    protected Music(final Music music) {
        this(music.mAlbum, music.mArtist, music.mAlbumArtist, music.mFullPath, music.mDisc,
                music.mDate, music.mGenre, music.mTime, music.mTitle,
                music.mTotalTracks, music.mTrack, music.mSongId, music.mSongPos, music.mName);
    }

    protected Music(final String album, final String artist, final String albumArtist,
            final String fullPath, final int disc, final long date, final String genre,
            final long time, final String title, final int totalTracks,
            final int track, final int songId, final int songPos, final String name) {
        super();

        mAlbum = album;
        mArtist = artist;
        mAlbumArtist = albumArtist;
        mFullPath = fullPath;
        mDisc = disc;
        mDate = date;
        mGenre = genre;
        mTime = time;
        mTitle = title;
        mTotalTracks = totalTracks;
        mTrack = track;
        mSongId = songId;
        mSongPos = songPos;
        mName = name;
    }

    static Music build(final Collection<String> response) {
        String album = null;
        String artist = null;
        String albumArtist = null;
        String fullPath = null;
        int disc = -1;
        long date = -1L;
        String genre = null;
        long time = -1L;
        String title = null;
        int totalTracks = -1;
        int track = -1;
        int songId = -1;
        int songPos = -1;
        String name = null;

        for (final String[] lines : Tools.splitResponse(response)) {

            switch (lines[0]) {
                case "file":
                    fullPath = lines[1];
                    if (!fullPath.isEmpty() && fullPath.contains("://")) {
                        final int pos = fullPath.indexOf('#');
                        if (pos > 1) {
                            name = fullPath.substring(pos + 1, fullPath.length());
                            fullPath = fullPath.substring(0, pos - 1);
                        }
                    }
                    break;
                case "Album":
                    album = lines[1];
                    break;
                case "AlbumArtist":
                    albumArtist = lines[1];
                    break;
                case "Artist":
                    artist = lines[1];
                    break;
                case "Date":
                    try {
                        final Matcher matcher = DATE_DELIMITER.matcher(lines[1]);
                        date = Long.parseLong(matcher.replaceAll(""));
                    } catch (final NumberFormatException e) {
                        Log.warning(TAG, "Not a valid date.", e);
                    }
                    break;
                case "Disc":
                    final int discIndex = lines[1].indexOf('/');

                    try {
                        if (discIndex == -1) {
                            disc = Integer.parseInt(lines[1]);
                        } else {
                            disc = Integer.parseInt(lines[1].substring(0, discIndex));
                        }
                    } catch (final NumberFormatException e) {
                        Log.warning(TAG, "Not a valid disc number.", e);
                    }
                    break;
                case "Genre":
                    genre = lines[1];
                    break;
                case "Id":
                    try {
                        songId = Integer.parseInt(lines[1]);
                    } catch (final NumberFormatException e) {
                        Log.error(TAG, "Not a valid song ID.", e);
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
                        songPos = Integer.parseInt(lines[1]);
                    } catch (final NumberFormatException e) {
                        Log.error(TAG, "Not a valid song position.", e);
                    }
                    break;
                case "Time":
                    try {
                        time = Long.parseLong(lines[1]);
                    } catch (final NumberFormatException e) {
                        Log.error(TAG, "Not a valid time number.", e);
                    }
                    break;
                case "Title":
                    title = lines[1];
                    break;
                case "Track":
                    final int trackIndex = lines[1].indexOf('/');

                    try {
                        if (trackIndex == -1) {
                            track = Integer.parseInt(lines[1]);
                        } else {
                            track = Integer.parseInt(lines[1].substring(0, trackIndex));
                            totalTracks = Integer.parseInt(lines[1].substring(trackIndex + 1));
                        }
                    } catch (final NumberFormatException e) {
                        Log.warning(TAG, "Not a valid track number.", e);
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

        return new Music(album, artist, albumArtist, fullPath, disc, date, genre, time, title,
                totalTracks, track, songId, songPos, name);
    }

    private static int compare(final String a, final String b) {
        final int result;

        if (a == null && b == null) {
            result = 0;
        } else if (a == null || b == null) {
            result = 1;
        } else {
            result = a.compareTo(b);
        }

        return result;
    }

    public static List<Music> getMusicFromList(final Collection<String> response,
            final boolean sort) {
        final List<Music> result = new ArrayList<>(response.size());
        final Collection<String> lineCache = new ArrayList<>(MUSIC_ATTRIBUTES);

        for (final String line : response) {
            if (line.startsWith("file: ")) {
                if (!lineCache.isEmpty()) {
                    result.add(build(lineCache));
                    lineCache.clear();
                }
            }
            lineCache.add(line);
        }

        if (!lineCache.isEmpty()) {
            result.add(build(lineCache));
        }

        if (sort) {
            Collections.sort(result);
        }

        return result;
    }

    private static boolean isEmpty(final String s) {
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

    public static String addStreamName(final String url, final String name) {
        if (null == name || name.isEmpty()) {
            return url;
        }
        try {
            final String path = new URL(url).getPath();
            if (null == path || path.isEmpty()) {
                return url + "/#" + name;
            }
        } catch (final MalformedURLException e) {
        }
        return url + "#" + name;
    }

    @Override
    public int compareTo(final Item another) {
        Integer compareResult = null;

        if (another instanceof Music) {
            final Music om = (Music) another;

            // songId overrides every other sorting method. It's used for playlists/queue
            if (mSongId < om.mSongId) {
                compareResult = Integer.valueOf(-1);
            } else if (mSongId > om.mSongId) {
                compareResult = 1;
            } else if (MPD.sortByTrackNumber()) {
                // If enabled, sort by mDisc and track number
                if (mDisc != om.mDisc && mDisc != -1 && om.mDisc != -1) {
                    if (mDisc < om.mDisc) {
                        compareResult = Integer.valueOf(-1);
                    } else {
                        compareResult = Integer.valueOf(1);
                    }
                } else if (mTrack != om.mTrack && mTrack != -1 && om.mTrack != -1) {
                    if (mTrack < om.mTrack) {
                        compareResult = Integer.valueOf(-1);
                    } else {
                        compareResult = Integer.valueOf(1);
                    }
                }
            }

            if (compareResult == null) {
                // Order by song title (getTitle() fallback on filenames)
                final int compare = compare(getTitle(), om.getTitle());
                if (compare != 0) {
                    compareResult = Integer.valueOf(compare);
                }
            }

            if (compareResult == null) {
                // Then order by name (streams)
                final int compare = compare(mName, om.mName);
                if (0 != compare) {
                    compareResult = Integer.valueOf(compare);
                }
            }

            if (compareResult == null) {
                // Last resort is to order by fullpath
                compareResult = Integer.valueOf(compare(mFullPath, om.mFullPath));
            }
        } else {
            compareResult = Integer.valueOf(super.compareTo(another));
        }

        return compareResult.intValue();
    }

    /**
     * Retrieves album name.
     *
     * @return album name.
     */
    public String getAlbum() {
        return mAlbum;
    }

    /**
     * Retrieves the original album artist name.
     *
     * @return album artist name or null if it is not set.
     */
    public String getAlbumArtist() {
        return mAlbumArtist;
    }

    public Artist getAlbumArtistAsArtist() {
        return new Artist(mAlbumArtist);
    }

    public String getAlbumArtistOrArtist() {
        return isEmpty(mAlbumArtist) ? mArtist : mAlbumArtist;
    }

    public Album getAlbumAsAlbum() {
        final boolean isAlbumArtist = !isEmpty(mAlbumArtist);
        final Artist artist = new Artist(isAlbumArtist ? mAlbumArtist : mArtist);
        return new Album(mAlbum, artist, isAlbumArtist);
    }

    public AlbumInfo getAlbumInfo() {
        return new AlbumInfo(getAlbumArtistOrArtist(), mAlbum, getPath(), getFilename());
    }

    /**
     * Retrieves artist name.
     *
     * @return artist name.
     */
    public String getArtist() {
        return mArtist;
    }

    public Artist getArtistAsArtist() {
        return new Artist(mArtist);
    }

    public String getGenre() {
        return mGenre;
    }

    public long getDate() {
        return mDate;
    }

    public int getDisc() {
        return mDisc;
    }

    /**
     * TODO test this for streams Retrieves filename.
     *
     * @return filename.
     */
    public String getFilename() {
        final int pos = mFullPath.lastIndexOf('/');
        if (pos == -1 || pos == mFullPath.length() - 1) {
            return mFullPath;
        } else {
            return mFullPath.substring(pos + 1);
        }
    }

    /**
     * Retrieves mDate as string (##:##).
     *
     * @return mDate as string.
     */
    public CharSequence getFormattedTime() {
        return timeToString(mTime);
    }

    /**
     * Retrieves full path name.
     *
     * @return full path name.
     */
    @Override
    public String getFullpath() {
        return mFullPath;
    }

    /**
     * Retrieves stream's name.
     *
     * @return stream's name.
     */
    @Override
    public String getName() {
        return isEmpty(mName) ? getFilename() : mName;
    }

    /**
     * Retrieves file's parent directory
     *
     * @return file's parent directory
     */
    public String getParent() {
        String parent = null;

        if (mFullPath != null) {
            final int pos = mFullPath.lastIndexOf('/');

            if (pos != -1) {
                parent = mFullPath.substring(0, pos);
            }
        }

        return parent;
    }

    /**
     * Retrieves path of music file (does not start or end with /)
     *
     * @return path of music file.
     */
    public String getPath() {
        final String result;
        if (null != mFullPath && mFullPath.length() > getFilename().length()) {
            result = mFullPath.substring(0, mFullPath.length() - getFilename().length() - 1);
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
        return mSongPos;
    }

    /**
     * Retrieves current song playlist id.
     *
     * @return current song playlist id.
     */
    public int getSongId() {
        return mSongId;
    }

    /** TODO: This needs to go away to make this class completely immutable. */
    public void setSongId(final int value) {
        mSongId = value;
    }

    /**
     * Retrieves playing time.
     *
     * @return playing time.
     */
    public long getTime() {
        return mTime;
    }

    /**
     * Retrieves title.
     *
     * @return title.
     */
    public String getTitle() {
        if (isEmpty(mTitle)) {
            return getFilename();
        } else {
            return mTitle;
        }
    }

    /**
     * Retrieves total number of tracks from this music's album when available.
     * This can contain letters!
     *
     * @return total number of tracks from this music's album when available.
     */
    public int getTotalTracks() {
        return mTotalTracks;
    }

    /**
     * Retrieves track number. This can contain letters!
     *
     * @return track number.
     */
    public int getTrack() {
        return mTrack;
    }

    public boolean hasTitle() {
        return null != mTitle && !mTitle.isEmpty();
    }

    public boolean isStream() {
        return null != mFullPath && mFullPath.contains("://");
    }

    @Override
    public String mainText() {
        return getTitle();
    }

    private static class MusicTitleComparator implements Comparator<Music> {

        @Override
        public int compare(final Music o1, final Music o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getTitle(), o2.getTitle());
        }
    }
}
