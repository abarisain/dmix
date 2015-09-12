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

import com.anpmech.mpd.Log;
import com.anpmech.mpd.Tools;

import java.util.Comparator;

/**
 * Class representing a generic track entry in playlist, abstracted for backend.
 *
 * <p>This item is returned from methods of the
 * <A HREF="http://www.musicpd.org/doc/protocol/database.html">database</A>
 * subsystem of the <A HREF="http://www.musicpd.org/doc/protocol">MPD protocol</A>.</p>
 *
 * @param <T> The Music type.
 */
abstract class AbstractMusic<T extends Music> extends Item<Music> implements FilesystemTreeEntry {

    /**
     * The media server response key returned for a Album value.
     */
    public static final String RESPONSE_ALBUM = "Album";

    /**
     * The media server response key returned for a AlbumArtist value.
     */
    public static final String RESPONSE_ALBUM_ARTIST = "AlbumArtist";

    /**
     * The media server response key returned for a Artist value.
     */
    public static final String RESPONSE_ARTIST = "Artist";

    /**
     * The media server response key returned for a Composer value.
     */
    public static final String RESPONSE_COMPOSER = "Composer";

    /**
     * The media server response key returned for a Date value.
     */
    public static final String RESPONSE_DATE = "Date";

    /**
     * The media server response key returned for a Disc value.
     */
    public static final String RESPONSE_DISC = "Disc";

    /**
     * The media server response key returned for a file path value.
     */
    public static final String RESPONSE_FILE = "file";

    /**
     * The media server response key returned for a Genre value.
     */
    public static final String RESPONSE_GENRE = "Genre";

    /**
     * The media server response key returned for a Name (tag) value.
     */
    public static final String RESPONSE_NAME = "Name";

    /**
     * The media server response key returned for a playlist queue ID value.
     */
    public static final String RESPONSE_SONG_ID = "Id";

    /**
     * The media server response key returned for a playlist queue position value.
     */
    public static final String RESPONSE_SONG_POS = "Pos";

    /**
     * The media server response key returned for a Time (duration) value.
     */
    public static final String RESPONSE_TIME = "Time";

    /**
     * The media server response key returned for a Title value.
     */
    public static final String RESPONSE_TITLE = "Title";

    /**
     * The media server response key returned for a Track value.
     */
    public static final String RESPONSE_TRACK = "Track";

    /**
     * The media server response key returned for a Comment value.
     */
    public static final String RESPONSE_COMMENT = "Comment";

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
    private static final String TAG = "AbstractMusic";

    final String mResponse;

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

    /**
     * The copy constructor for this class.
     *
     * @param track The track to copy from.
     */
    protected AbstractMusic(final T track) {
        this(track.mResponse);
    }

    /**
     * The generator constructor for this class.
     *
     * @param response The MPD protocol response to create this object from.
     */
    AbstractMusic(final String response) {
        super();

        mResponse = response;
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

    /**
     * This method parses the date MPD protocol response by removing all non-digit characters then
     * parsing it as a long.
     *
     * @param dateResponse The date MPD protocol response.
     * @return The parsed date.
     */
    public static long parseDate(final CharSequence dateResponse) {
        final int length = dateResponse.length();
        final StringBuilder sb = new StringBuilder(length);
        long resultDate = Long.MIN_VALUE;

        for (int i = 0; i < length; i++) {
            final char c = dateResponse.charAt(i);

            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }

        try {
            resultDate = Long.parseLong(sb.toString());
        } catch (final NumberFormatException e) {
            Log.warning(TAG, "Not a valid date.", e);
        }

        return resultDate;
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
        int compareResult = compareIntegers(true, getSongId(), om.getSongId());

        if (withTrackNumber) {
            if (compareResult == 0) {
                /** Order by the disc number. */
                compareResult = compareIntegers(true, getDisc(), om.getDisc());
            }

            if (compareResult == 0) {
                /** Order by track number. */
                compareResult = compareIntegers(true, getTrack(), om.getTrack());
            }
        }

        if (compareResult == 0) {
            /** Order by song title (getTitle() fallback on file names). */
            compareResult = compareString(getTitle(), om.getTitle());
        }

        if (compareResult == 0) {
            /** Order by name (this is helpful for streams). */
            compareResult = compareString(getName(), om.getName());
        }

        if (compareResult == 0) {
            /** As a last resort, order by the full path. */
            compareResult = compareString(getFullPath(), om.getFullPath());
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
            final T track = (T) o;

            isEqual = Boolean.valueOf(track.mResponse.equals(mResponse));
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    /**
     * Returns a key's value from the {@link #mResponse}.
     *
     * @param key The key to get the value for.
     * @return The value paired to the key, null if not found.
     */
    private String findValue(final String key) {
        return Tools.findValue(mResponse, key);
    }


    /**
     * Retrieves an {@link Album} from this item.
     *
     * @return An {@link Album} object of the current track with the Album Artist (if available),
     * otherwise of the Artist.
     */
    public Album getAlbum() {
        final String albumArtistName = getAlbumArtistName();
        final boolean isAlbumArtist = !Tools.isEmpty(albumArtistName);
        final AlbumBuilder albumBuilder = new AlbumBuilder();

        albumBuilder.setName(getAlbumName());
        if (isAlbumArtist) {
            albumBuilder.setAlbumArtist(albumArtistName);
        } else {
            albumBuilder.setArtist(getArtistName());
        }

        albumBuilder.setSongDetails(getDate(), findValue(RESPONSE_FILE));

        return albumBuilder.build();
    }

    public Artist getAlbumArtist() {
        return new Artist(getAlbumArtistName());
    }

    /**
     * Retrieves the album artist name.
     *
     * @return The name of the album artist or null if it is not set.
     */
    public String getAlbumArtistName() {
        return findValue(RESPONSE_ALBUM_ARTIST);
    }

    /**
     * Retrieves the artist name.
     *
     * @return Returns the album artist if it exists, else it will return the album if it exists,
     * otherwise it will return the unknown album translation.
     */
    public String getAlbumArtistOrArtist() {
        final String albumArtistName = findValue(RESPONSE_ALBUM_ARTIST);
        final String artistName = findValue(RESPONSE_ARTIST);
        final String result;

        if (!Tools.isEmpty(albumArtistName)) {
            result = albumArtistName;
        } else if (!Tools.isEmpty(artistName)) {
            result = artistName;
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
        return findValue(RESPONSE_ALBUM);
    }

    /**
     * Retrieves an {@link Artist} item for this object.
     *
     * @return An {@link Artist} item for this object.
     */
    public Artist getArtist() {
        return new Artist(getArtistName());
    }

    /**
     * Retrieves artist name for this track.
     *
     * @return The artist name for this track.
     */
    public String getArtistName() {
        return findValue(RESPONSE_ARTIST);
    }

    /**
     * Retrieves the composer name for this track.
     *
     * @return The composer name for this track.
     */
    public String getComposerName() {
        return findValue(RESPONSE_COMPOSER);
    }

    /**
     * Retrieves the release date for the track.
     *
     * @return The release date for this track.
     */
    public long getDate() {
        final String date = findValue(RESPONSE_DATE);
        long parsedDate = Long.MIN_VALUE;

        if (date != null) {
            parsedDate = parseDate(date);
        }

        return parsedDate;
    }

    /**
     * Retrieves the disc number for the album this track was produced for.
     *
     * @return The disc number for the album this track was produced for.
     */
    public int getDisc() {
        return Tools.parseInteger(findValue(RESPONSE_DISC));
    }

    /**
     * Retrieves the duration of the track formatted as HH:MM:SS.
     *
     * @return The duration of the track formatted as HH:MM:SS.
     */
    public CharSequence getFormattedTime() {
        return Tools.timeToString(getTime());
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
        String filename = findValue(RESPONSE_FILE);

        if (isStream()) {
            final int pos = filename.indexOf('#');

            if (pos != -1) {
                filename = filename.substring(0, pos);
            }
        }

        return filename;
    }

    /**
     * Retrieves the genre name for this track.
     *
     * @return The genre name for this track.
     */
    public String getGenreName() {
        return findValue(RESPONSE_GENRE);
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
            name = findValue(RESPONSE_NAME);

            if (Tools.isEmpty(name)) {
                name = getFullPath();
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
        return findValue(RESPONSE_NAME);
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
        return Tools.parseInteger(findValue(RESPONSE_SONG_POS));
    }

    /**
     * Retrieves current song playlist id.
     *
     * @return current song playlist id.
     */
    public int getSongId() {
        return Tools.parseInteger(findValue(RESPONSE_SONG_ID));
    }

    /**
     * Retrieves playing time.
     *
     * @return playing time.
     */
    public long getTime() {
        return Tools.parseLong(findValue(RESPONSE_TIME));
    }

    /**
     * Retrieves the title if it exists, the full path otherwise.
     *
     * @return The title if it exists, the full path otherwise.
     */
    public String getTitle() {
        String title = findValue(RESPONSE_TITLE);

        if (Tools.isEmpty(title)) {
            title = getFullPath();
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
        final String value = findValue(RESPONSE_TRACK);
        int totalTracks = Integer.MIN_VALUE;

        if (value != null) {
            final int trackIndex = value.indexOf('/');

            try {
                if (trackIndex != -1) {
                    totalTracks = Integer.parseInt(value.substring(trackIndex + 1));
                }
            } catch (final NumberFormatException e) {
                Log.warning(TAG, "Not a valid track number.", e);
            }
        }

        return totalTracks;
    }

    /**
     * Retrieves track number. This can contain letters!
     *
     * @return track number.
     */
    public int getTrack() {
        final String value = findValue(RESPONSE_TRACK);

        int track = Integer.MIN_VALUE;
        if (value != null) {
            final int trackIndex = value.indexOf('/');

            try {
                if (trackIndex == -1) {
                    track = Integer.parseInt(value);
                } else {
                    track = Integer.parseInt(value.substring(0, trackIndex));
                }
            } catch (final NumberFormatException e) {
                Log.warning(TAG, "Not a valid track number.", e);
            }
        }

        return track;
    }

    /**
     * Returns the URI fragment if it exists.
     *
     * @return The URI fragment if it exists, null otherwise.
     */
    public String getURIFragment() {
        final String fullPath = findValue(RESPONSE_FILE);
        final int pos;
        String streamName = null;

        if (fullPath == null) {
            pos = 0;
        } else {
            pos = fullPath.indexOf('#');
        }

        if (pos > 1) {
            streamName = fullPath.substring(pos + 1, fullPath.length());
        }

        return streamName;
    }

    /**
     * Retrieves the comment
     *
     * @return  The comments or an empty string
     */
    public String getComments() {
        String comments = findValue(RESPONSE_COMMENT);
        comments = comments == null ? "" : comments;
        return comments;
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
        return mResponse.hashCode();
    }

    /**
     * Returns whether the full path of this item appears to be streaming media.
     *
     * @return True if this item appears to be streaming media, false otherwise.
     */
    public boolean isStream() {
        final String fullPath = findValue(RESPONSE_FILE);

        return fullPath != null && fullPath.contains("://");
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
