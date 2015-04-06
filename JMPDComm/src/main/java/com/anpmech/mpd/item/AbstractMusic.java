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

import com.anpmech.mpd.Tools;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Class representing a generic track entry in playlist, abstracted for backend. This item is
 * returned from methods of the <A HREF="http://www.musicpd.org/doc/protocol/database.html">database</A>
 * subsystem of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.
 *
 * @author Felipe Gustavo de Almeida
 */
abstract class AbstractMusic<T extends Music> extends Item<Music> implements FilesystemTreeEntry {

    /** The media server response key returned for a {@link #mAlbumName} value. */
    public static final String RESPONSE_ALBUM = "Album";

    /**
     * The media server response key returned for a {@link #mAlbumArtistName} value.
     */
    public static final String RESPONSE_ALBUM_ARTIST = "AlbumArtist";

    /**
     * The media server response key returned for a {@link #mArtistName} value.
     */
    public static final String RESPONSE_ARTIST = "Artist";

    /**
     * The media server response key returned for a {@link #mComposerName} value.
     */
    public static final String RESPONSE_COMPOSER = "Composer";

    /**
     * The media server response key returned for a {@link #mDate} value.
     */
    public static final String RESPONSE_DATE = "Date";

    /**
     * The media server response key returned for a {@link #mDisc} value.
     */
    public static final String RESPONSE_DISC = "Disc";

    /**
     * The media server response key returned for a {@link #mFullPath} value.
     */
    public static final String RESPONSE_FILE = "file";

    /**
     * The media server response key returned for a {@link #mGenreName} value.
     */
    public static final String RESPONSE_GENRE = "Genre";

    /**
     * The media server response key returned for a {@link #mName} value.
     */
    public static final String RESPONSE_NAME = "Name";

    /**
     * The media server response key returned for a {@link #mSongId} value.
     */
    public static final String RESPONSE_SONG_ID = "Id";

    /**
     * The media server response key returned for a {@link #mSongPos} value.
     */
    public static final String RESPONSE_SONG_POS = "Pos";

    /**
     * The media server response key returned for a {@link #mTime} value.
     */
    public static final String RESPONSE_TIME = "Time";

    /**
     * The media server response key returned for a {@link #mTitle} value.
     */
    public static final String RESPONSE_TITLE = "Title";

    /**
     * The media server response key returned for a {@link #mTrack} and {@link #mTotalTracks}
     * values.
     */
    public static final String RESPONSE_TRACK = "Track";

    /**
     * The string used to refer to an album tag.
     */
    public static final String TAG_ALBUM = "album";

    /**
     * The string used to refer to an album artist tag.
     */
    public static final String TAG_ALBUM_ARTIST = "albumartist";

    /**
     * The string used to refer to an artist tag.
     */
    public static final String TAG_ARTIST = "artist";

    /**
     * The string used to refer to a composer tag.
     */
    public static final String TAG_COMPOSER = "composer";

    /**
     * The string used to refer to a date tag.
     */
    public static final String TAG_DATE = "date";

    /**
     * The string used to refer to an disc tag.
     */
    public static final String TAG_DISC = "disc";

    /**
     * The string used to refer to a genre tag.
     */
    public static final String TAG_GENRE = "genre";

    /**
     * The string used to refer to a name tag.
     */
    public static final String TAG_NAME = "name";

    /**
     * The string used to refer to a time tag.
     */
    public static final String TAG_TIME = "time";

    /**
     * The string used to refer to a title tag.
     */
    public static final String TAG_TITLE = "title";

    /**
     * The string used to refer to a track tag.
     */
    public static final String TAG_TRACK = "track";

    /**
     * This integer is used to detect integers that were unset during construction.
     */
    static final int UNDEFINED_INT = -1;

    /**
     * The log identifier for this class.
     */
    private static final String TAG = "Music";

    /**
     * This field is storage for the name of the artist of the album this track was published on.
     */
    final String mAlbumArtistName;

    /**
     * This field is storage for the name of the album this track was pushed on.
     */
    final String mAlbumName;

    /**
     * This field is storage for the name of the artist of this track.
     */
    final String mArtistName;

    /**
     * This field is storage for the name of the composer of this track.
     */
    final String mComposerName;

    /**
     * This field is storage for the date the release date of this track.
     */
    final long mDate;

    /**
     * This field is storage for the disc number of the album that this track was published on.
     */
    final int mDisc;

    /**
     * This field is storage for a representation of the source of the media, relative to the media
     * server.
     */
    final String mFullPath;

    /**
     * This field is storage for the name of the genre for this track.
     */
    final String mGenreName;

    /**
     * This field is storage for the Name tag for this track.
     */
    final String mName;

    /**
     * This field is storage for the song ID, relative to the playlist of the currently connected
     * media server.
     */
    final int mSongId;

    /**
     * This field is the storage for the song position, relative to the playlist of the currently
     * connected media server.
     */
    final int mSongPos;

    /**
     * This field is the storage for the time the current track has been playing.
     */
    final long mTime;

    /**
     * This field is the storage for the title of this track.
     */
    final String mTitle;

    /**
     * This field is the storage for the total duration of this track.
     */
    final int mTotalTracks;

    /**
     * This field is the storage for the track number of this track relative to the album it was
     * produced for.
     */
    final int mTrack;

    /**
     * Similar to the default {@code Comparable} for the Music class, but it compares without
     * taking disc and track numbers into account.
     */
    public static final Comparator<AbstractMusic<Music>> COMPARE_WITHOUT_TRACK_NUMBER =
            new Comparator<AbstractMusic<Music>>() {
                /**
                 * Compares the two specified objects to determine their relative ordering. The
                 * ordering implied by the return value of this method for all possible pairs of
                 * {@code (lhs, rhs)} should form an <i>equivalence relation</i>.
                 * This means that
                 * <ul>
                 * <li>{@code compare(a, a)} returns zero for all {@code a}</li>
                 * <li>the sign of {@code compare(a, b)} must be the opposite of the sign of {@code
                 * compare(b, a)} for all pairs of (a,b)</li>
                 * <li>From {@code compare(a, b) > 0} and {@code compare(b, c) > 0} it must
                 * follow {@code compare(a, c) > 0} for all possible combinations of {@code
                 * (a, b, c)}</li>
                 * </ul>
                 *
                 * @param lhs an {@code Object}.
                 * @param rhs a second {@code Object} to compare with {@code lhs}.
                 * @return an integer < 0 if {@code lhs} is less than {@code rhs}, 0 if they are
                 * equal, and > 0 if {@code lhs} is greater than {@code rhs}.
                 * @throws ClassCastException if objects are not of the correct type.
                 */
                @Override
                public int compare(final AbstractMusic<Music> lhs, final AbstractMusic<Music> rhs) {
                    int compare = 0;

                    if (lhs != null) {
                        compare = lhs.compareTo(rhs, false);
                    }

                    return compare;
                }
            };

    AbstractMusic(final T music) {
        this(music.mAlbumName, music.mAlbumArtistName, music.mArtistName, music.mComposerName,
                music.mDate, music.mDisc, music.mFullPath, music.mGenreName, music.mName,
                music.mSongId, music.mSongPos, music.mTime, music.mTitle, music.mTotalTracks,
                music.mTrack);
    }

    AbstractMusic(final String albumName, final String albumArtistName, final String artistName,
            final String composerName, final long date, final int disc, final String fullPath,
            final String genreName, final String name, final int songId, final int songPos,
            final long time, final String title, final int totalTracks, final int track) {
        super();

        mAlbumName = albumName;
        mArtistName = artistName;
        mAlbumArtistName = albumArtistName;
        mComposerName = composerName;
        mFullPath = fullPath;
        mDisc = disc;
        mDate = date;
        mGenreName = genreName;
        mTime = time;
        mTitle = title;
        mTotalTracks = totalTracks;
        mTrack = track;
        mSongId = songId;
        mSongPos = songPos;
        mName = name;
    }

    /**
     * This method extends Integer.compare() by adding a undefined integer comparison.
     *
     * @param compUndefined If true, will compare by {@code UNDEFINED_INT} value.
     * @param lhs           The first integer to compare.
     * @param rhs           The second integer to compare.
     * @return A negative integer, zero, or a positive integer as the first argument is less than,
     * equal to, or greater than the second.
     */
    private static int compareIntegers(final boolean compUndefined, final int lhs, final int rhs) {
        int result = 0;

        if (lhs != rhs) {

            /** Compare the two integers against the primitive undefined integer for this class. */
            if (compUndefined) {
                if (lhs == UNDEFINED_INT) {
                    result = -1;
                } else if (rhs == UNDEFINED_INT) {
                    result = 1;
                }
            }
        }

        if (result == 0) {
            result = Tools.compare(lhs, rhs);
        }

        return result;
    }

    /**
     * A null safe, case-insensitive comparator.
     *
     * @param lhs Left-hand side string to compare.
     * @param rhs Right hand size string to compare.
     * @return 0 if values are equal, 0 if lhs is less than rhs, 1 if lhs is greater than rhs.
     */
    private static int compareString(final String lhs, final String rhs) {
        final int result;

        if (lhs == null && rhs == null) {
            result = 0;
        } else if (lhs == null) {
            result = -1; // lhs < rhs
        } else if (rhs == null) {
            result = 1;  // lhs > rhs
        } else {
            result = lhs.compareToIgnoreCase(rhs);
        }

        return result;
    }

    private static boolean isEmpty(final String s) {
        return null == s || s.isEmpty();
    }

    /**
     * Defines a natural order to this object and another.
     *
     * @param another The other object to compare this to.
     * @return A negative integer if this instance is less than {@code another}; A positive integer
     * if this instance is greater than {@code another}; 0 if this instance has the same order as
     * {@code another}.
     */
    @Override
    public int compareTo(final Music another) {
        return compareTo(another, true);
    }

    /**
     * Defines a natural order to this object and another.
     *
     * @param another         The other object to compare this to.
     * @param withTrackNumber If true, compare tracks by Disc and Track number first
     * @return A negative integer if this instance is less than {@code another}; A positive integer
     * if this instance is greater than {@code another}; 0 if this instance has the same order as
     * {@code another}.
     */
    private int compareTo(final Item<Music> another, final boolean withTrackNumber) {
        final AbstractMusic<Music> om = (AbstractMusic<Music>) another;

        /** songId overrides every other sorting method. It's used for playlists/queue. */
        int compareResult = compareIntegers(true, mSongId, om.mSongId);

        if (withTrackNumber) {
            if (compareResult == 0) {
                /** Order by the disc number. */
                compareResult = compareIntegers(true, mDisc, om.mDisc);
            }

            if (compareResult == 0) {
                /** Order by track number. */
                compareResult = compareIntegers(true, mTrack, om.mTrack);
            }
        }

        if (compareResult == 0) {
            /** Order by song title (getTitle() fallback on file names). */
            compareResult = compareString(getTitle(), om.getTitle());
        }

        if (compareResult == 0) {
            /** Order by name (this is helpful for streams). */
            compareResult = compareString(mName, om.mName);
        }

        if (compareResult == 0) {
            /** As a last resort, order by the full path. */
            compareResult = compareString(mFullPath, om.mFullPath);
        }

        return compareResult;
    }

    /**
     * Compares a Music object with a general contract of comparison that is reflexive, symmetric
     * and transitive.
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
            final T music = (T) o;

            final Object[][] equalsObjects = {
                    {mAlbumName, music.mAlbumName},
                    {mAlbumArtistName, music.mAlbumArtistName},
                    {mArtistName, music.mArtistName},
                    {mComposerName, music.mComposerName},
                    {mGenreName, music.mGenreName},
                    {mName, music.mName},
                    {mTitle, music.mTitle}
            };

            final int[][] equalsInt = {
                    {mDisc, music.mDisc},
                    {mSongId, music.mSongId},
                    {mSongPos, music.mSongPos},
                    {mTotalTracks, music.mTotalTracks},
                    {mTrack, music.mTrack}
            };

            if (mDate != music.mDate || mTime != music.mTime || Tools.isNotEqual(equalsInt)) {
                isEqual = Boolean.FALSE;
            }

            if (!mFullPath.equals(music.mFullPath) || Tools.isNotEqual(equalsObjects)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    /**
     * Retrieves an {@link Album} from this item.
     *
     * @return An {@link Album} object of the current track with the Album Artist (if available),
     * otherwise of the Artist.
     */
    public Album getAlbum() {
        final boolean isAlbumArtist = !isEmpty(mAlbumArtistName);
        final AlbumBuilder albumBuilder = new AlbumBuilder();

        albumBuilder.setName(mAlbumName);
        if (isAlbumArtist) {
            albumBuilder.setAlbumArtist(mAlbumArtistName);
        } else {
            albumBuilder.setArtist(mArtistName);
        }

        albumBuilder.setSongDetails(mDate, mFullPath);

        return albumBuilder.build();
    }

    public Artist getAlbumArtist() {
        return new Artist(mAlbumArtistName);
    }

    /**
     * Retrieves the album artist name.
     *
     * @return The name of the album artist or null if it is not set.
     */
    public String getAlbumArtistName() {
        return mAlbumArtistName;
    }

    /**
     * Retrieves the artist name.
     *
     * @return Returns the album artist if it exists, else it will return the album if it exists,
     * otherwise it will return the unknown album translation.
     */
    public String getAlbumArtistOrArtist() {
        final String result;

        if (mAlbumArtistName != null && !mAlbumArtistName.isEmpty()) {
            result = mAlbumArtistName;
        } else if (mArtistName != null && !mArtistName.isEmpty()) {
            result = mArtistName;
        } else {
            result = getArtist().toString();
        }

        return result;
    }

    /**
     * Retrieves album name.
     *
     * @return album name.
     */
    public String getAlbumName() {
        return mAlbumName;
    }

    /**
     * Retrieves an {@link Artist} item for this object.
     *
     * @return An {@link Artist} item for this object.
     */
    public Artist getArtist() {
        return new Artist(mArtistName);
    }

    /**
     * Retrieves artist name for this track.
     *
     * @return The artist name for this track.
     */
    public String getArtistName() {
        return mArtistName;
    }

    /**
     * Retrieves the composer name for this track.
     *
     * @return The composer name for this track.
     */
    public String getComposerName() {
        return mComposerName;
    }

    /**
     * Retrieves the release date for the track.
     *
     * @return The release date for this track.
     */
    public long getDate() {
        return mDate;
    }

    /**
     * Retrieves the disc number for the album this track was produced for.
     *
     * @return The disc number for the album this track was produced for.
     */
    public int getDisc() {
        return mDisc;
    }

    /**
     * Retrieves the duration of the track formatted as HH:MM:SS.
     *
     * @return The duration of the track formatted as HH:MM:SS.
     */
    public CharSequence getFormattedTime() {
        return Tools.timeToString(mTime);
    }

    /**
     * This returns a representation of the source of the media, relative to the media server.
     *
     * <p>This can be a filename, a URL, etc. If this item's filename is a URL, the URI fragment is
     * removed.</p>
     *
     * @return The filename of this item without a URI fragment.
     * @see #getURIFragment()
     */
    @Override
    public String getFullPath() {
        String fileName = mFullPath;

        if (isStream()) {
            final int pos = mFullPath.indexOf('#');

            if (pos != -1) {
                fileName = mFullPath.substring(0, pos);
            }
        }

        return fileName;
    }

    /**
     * Retrieves the genre name for this track.
     *
     * @return The genre name for this track.
     */
    public String getGenreName() {
        return mGenreName;
    }

    /**
     * This method returns the stream name (if not empty), the Name tag, or the filename.
     *
     * @return The stream name, the name tag or the filename, which ever is available first,
     * respectively.
     */
    @Override
    public String getName() {
        String name = null;

        if (isStream()) {
            name = getURIFragment();
        }

        if (name == null) {
            if (isEmpty(mName)) {
                name = getFullPath();
            } else {
                name = mName;
            }
        }

        return name;
    }

    /**
     * This method returns the name tag as received from the MPD protocol key {@link #TAG_NAME}.
     *
     * @return The name tag as returned by the protocol.
     */
    public String getNameTag() {
        return mName;
    }

    /**
     * Retrieves file's parent directory
     *
     * @return file's parent directory
     */
    public String getParentDirectory() {
        String pathName = getFullPath();

        if (pathName != null) {
            int index = pathName.lastIndexOf('/');

            /** If it ends with a backslash, try again. */
            if (index == pathName.length() - 1) {
                index = pathName.lastIndexOf('/', index - 1);
            }

            if (index != -1) {
                pathName = pathName.substring(0, index);
            }
        }

        return pathName;
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

    /**
     * Retrieves playing time.
     *
     * @return playing time.
     */
    public long getTime() {
        return mTime;
    }

    /**
     * Retrieves the title if it exists, the full path otherwise.
     *
     * @return The title if it exists, the full path otherwise.
     */
    public String getTitle() {
        final String title;

        if (isEmpty(mTitle)) {
            title = getFullPath();
        } else {
            title = mTitle;
        }

        return title;
    }

    /**
     * Retrieves total number of tracks from this music's album when available. This can contain
     * letters!
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

    /**
     * Returns the URI fragment if it exists.
     *
     * @return The URI fragment if it exists, null otherwise.
     */
    public String getURIFragment() {
        final int pos = mFullPath.indexOf('#');
        String streamName = null;

        if (pos > 1) {
            streamName = mFullPath.substring(pos + 1, mFullPath.length());
        }

        return streamName;
    }

    /**
     * Returns an integer hash code for this Music item. By contract, any two objects for which
     * {@link #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * @return This Music item hash code.
     * @see Object#equals(Object)
     */
    @Override
    public int hashCode() {
        final Object[] objects = {mAlbumName, mArtistName, mAlbumArtistName, mGenreName, mName,
                mTitle};

        int result = 31 * mFullPath.hashCode();
        result = 31 * result + mDisc;
        result = 31 * result + (int) (mDate ^ (mDate >>> 32));
        result = 31 * result + mSongId;
        result = 31 * result + mSongPos;
        result = 31 * result + (int) (mTime ^ (mTime >>> 32));
        result = 31 * result + mTotalTracks;
        result = 31 * result + mTrack;

        return result + Arrays.hashCode(objects);
    }

    /**
     * Returns whether the full path of this item appears to be streaming media.
     *
     * @return True if this item appears to be streaming media, false otherwise.
     */
    public boolean isStream() {
        return mFullPath != null && mFullPath.contains("://");
    }

    /**
     * Retrieves the title of this item if it exists, the full path otherwise.
     *
     * @return The title of this item, the full path otherwise.
     */
    @Override
    public String toString() {
        return getTitle();
    }
}
