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

import org.a0z.mpd.item.Music;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Felipe Gustavo de Almeida, Stefan Agner
 */

/**
 * These lists store the internal structure store of the playlist. All modifications are
 * synchronized, iterating over the list will still require manual locking.
 */
final class MusicList implements Iterable<Music> {

    /** The debug flag, change to true for debugging log output. */
    private static final boolean DEBUG = false;

    /** The debug log identifier. */
    private static final String TAG = "MusicList";

    /** The playlist store in positional order. */
    private final List<Music> mList;

    /** A list of songIDs in songPos order. */
    private final List<Integer> mSongID;

    MusicList() {
        super();

        mList = Collections.synchronizedList(new ArrayList<Music>());
        mSongID = Collections.synchronizedList(new ArrayList<Integer>());
    }

    /**
     * Adds music to {@code MusicList}.
     *
     * @param music music to be added.
     */
    private void add(final Music music) {
        synchronized (mList) {
            final int songPos = music.getPos();

            if (DEBUG) {
                Log.debug(TAG, "listSize: " + mList.size() + " songPos: " + songPos);
            }
            if (mList.size() == songPos) {
                if (DEBUG) {
                    Log.debug(TAG, "Adding music to the end");
                }
                mList.add(music);
                mSongID.add(music.getSongId());
            } else {
                if (DEBUG) {
                    Log.debug(TAG,
                            "Adding beyond the end, or setting within the current playlist.");
                }
                /**
                 * Grow the list to the size of the songPos, THEN set it to the position necessary.
                 * The while loop shouldn't be necessary at all, unless, the result response is out
                 * of positional order.
                 */
                while (mList.size() <= songPos) {
                    mList.add(null);
                    mSongID.add(null);
                }

                if (songPos == -1) {
                    throw new IllegalStateException("Media server protocol error: songPos not " +
                            "included with the playlist changes included with the following " +
                            "music. Path:" + music.getFullPath() + " Name: " + music.getName());
                }

                mList.set(songPos, music);
                mSongID.set(songPos, music.getSongId());
            }
        }
    }

    /**
     * Retrieves a {@code Music} object by its songId.
     *
     * @param songId songId from the music to be retrieved.
     * @return a Music with given songId or {@code null} if it is not
     * present on this {@code MusicList}.
     */
    Music getById(final int songId) {
        final int songPos = Integer.valueOf(mSongID.indexOf(songId));

        return mList.get(songPos);
    }

    /**
     * Retrieves a {@code Music} object by its position on playlist.
     *
     * @param index position of the music to be retrieved.
     * @return a Music with given position or {@code null} if it is not
     * present on this {@code MusicList}.
     */
    Music getByIndex(final int index) {
        Music result = null;

        if (index >= 0 && mList.size() > index) {
            result = mList.get(index);
        }

        return result;
    }

    /**
     * Retrieves a List containing all {@code Music} objects from this {@code MusicList}.
     *
     * @return Retrieves a List containing all {@code Music} objects from this {@code MusicList}.
     */
    List<Music> getMusic() {
        return Collections.unmodifiableList(mList);
    }

    /**
     * Returns an {@link java.util.Iterator} for the music list.
     *
     * @return An {@code Iterator} instance.
     */
    @Override
    public Iterator<Music> iterator() {
        return mList.iterator();
    }

    /**
     * Modifies the list to reflect the changes coming in from the {@code playlist}. Lock to an
     * object prior to running this method.
     *
     * @param musicList    The changes to make to the backing stores.
     * @param listCapacity The size of the resulting list.
     */
    void manipulate(final Iterable<Music> musicList, final int listCapacity) {
        for (final Music music : musicList) {
            /**
             * Do not remove from either list. it will be removed by range.
             */
            add(music);
        }

        /**
         * Consistency checks and cleanups.
         */
        synchronized (mList) {
            final int listSize = mList.size();
            final int songIDSize = mSongID.size();
            if (listSize < listCapacity) {
                throw new IllegalStateException(
                        "List store: " + listSize + " and playlistLength: " + listCapacity +
                                " size differs.");
            }

            mList.subList(listCapacity, listSize).clear();
            mSongID.subList(listCapacity, songIDSize).clear();

            if (songIDSize != listSize) {
                throw new IllegalStateException("List store: " + listSize +
                        " and SongID: " + songIDSize + " size differs.");
            }
        }
    }

    /**
     * Replace all elements in this object.
     *
     * @param collection The {@code Music} collection to replace the {@code MusicList} with.
     */
    void replace(final Collection<Music> collection) {

        synchronized (mList) {
            mList.clear();
            mList.addAll(collection);

            mSongID.clear();
            for (final Music track : collection) {
                mSongID.add(track.getSongId());
            }
        }
    }

    /**
     * Retrieves this {@code MusicList} size.
     *
     * @return {@code MusicList} size.
     */
    int size() {
        return mList.size();
    }
}
