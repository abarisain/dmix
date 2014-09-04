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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author Felipe Gustavo de Almeida, Stefan Agner
 */

/** The skeleton of the playlist. */
final class MusicList {

    private static final String TAG = "MusicList";

    private final List<Music> mList;

    private final AbstractMap<Integer, Music> mSongIDMap;

    MusicList() {
        super();

        mSongIDMap = new HashMap<>();
        mList = Collections.synchronizedList(new ArrayList<Music>());
    }

    /**
<<<<<<< HEAD
     * Constructs a new {@code MusicList} containing all music from
     * {@code list}.
     *
     * @param list a {@code MusicList}
     */
    MusicList(final MusicList list) {
        this();

        addAll(list.getMusic());
    }

    /**
     * Adds music to {@code MusicList}.
     *
     * @param music music to be added.
     */
    final void add(final Music music) {
        if (getById(music.getSongId()) != null) {
            throw new IllegalArgumentException("Music is already on list");
        }

        // add it to the map and at the right position to the list
        mSongIDMap.put(Integer.valueOf(music.getSongId()), music);
        while (mList.size() < music.getPos() + 1) {
            mList.add(null);
        }
        mList.set(music.getPos(), music);
    }

    /**
     * Adds all Musics from {@code playlist} to this {@code MusicList}
     * .
     *
     * @param playlist {@code Collection} of {@code Music} to be added
     *                 to this {@code MusicList}.
     * @throws ClassCastException when {@code playlist} contains elements
     *                            not assignable to {@code Music}.
     */
    final void addAll(final Collection<Music> playlist) {
        mList.addAll(playlist);
    }

    /** Removes all {@code Music} objects from this {@code MusicList}. */
    final void clear() {
        synchronized (mList) {
            mList.clear();
            mSongIDMap.clear();
        }
    }

    /**
     * Retrieves a {@code Music} object by its songId.
     *
     * @param songId songId from the music to be retrieved.
     * @return a Music with given songId or {@code null} if it is not
     * present on this {@code MusicList}.
     */
    final Music getById(final int songId) {
        return mSongIDMap.get(Integer.valueOf(songId));
    }

    /**
     * Retrieves a {@code Music} object by its position on playlist.
     *
     * @param index position of the music to be retrieved.
     * @return a Music with given position or {@code null} if it is not
     * present on this {@code MusicList}.
     */
    final Music getByIndex(final int index) {
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
    final List<Music> getMusic() {
        return Collections.unmodifiableList(mList);
    }

    /**
     * Remove a {@code Music} object with given {@code songId}
     * from this {@code MusicList}, if it is present.
     * Adds or moves a Music item on the {@code MusicList}.
     *
     * @param music {@code Music} to be added or moved.
     */
    final void manipulate(final Music music) {
        final int songId = music.getSongId();
        final Integer songIdInteger = Integer.valueOf(songId);
        final int songPos = music.getPos();

        /** SongIDs can't exist in two places at the same time, remove it prior to adding it. */
        if (getById(songId) != null) {
            mSongIDMap.remove(songIdInteger);
            mList.remove(music);
        }

        synchronized (mList) {
            // add it to the map and at the right position to the list
            mSongIDMap.put(songIdInteger, music);
            while (mList.size() < songPos + 1) {
                mList.add(null);
            }
            mList.set(songPos, music);
        }
    }

    /**
     * Remove music with given {@code songId} from this
     * {@code MusicList}, if it is present.
     *
     * @param songId songId of the {@code Music} to be removed from this
     *               {@code MusicList}.
     */
    final void removeById(final int songId) {
        final Music music = getById(songId);

        synchronized (mList) {
            if (music != null) {
                mSongIDMap.remove(Integer.valueOf(songId));
                mList.remove(music);
            }
        }
    }

    /**
     * Removes a {@code Music} object at {@code position} from this {@code MusicList},
     * if it is present.
     *
     * @param index position of the {@code Music} to be removed from this
     *              {@code MusicList}.
     */
    final void removeByIndex(final int index) {
        synchronized (mList) {
            final Music music = getByIndex(index);

            if (music != null) {
                mList.remove(index);
                mSongIDMap.remove(Integer.valueOf(music.getSongId()));
            }
        }
    }

    /**
     * Removes all items sequentially within a specified range from the {@code MusicList}.
     *
     * @param start The position with which to start removing.
     * @param end   The final position to remove from the list.
     */
    final void removeByRange(final int start, final int end) {
        if (start < -1 || start > mList.size()) {
            throw new IndexOutOfBoundsException("Attempted invalid start range value: " + start +
                    ", list size: " + mList.size() + '.');
        }

        if (end < -1 || end > mList.size()) {
            throw new IndexOutOfBoundsException("Attempted invalid end range value: " + end +
                    ", list size: " + mList.size() + '.');
        }

        int iterator = start;
        synchronized (mList) {
            while (iterator < end) {
                removeByIndex(start);
                iterator++;
            }
        }
    }

    /**
     * Replace the current {@code MusicList} object.
     *
     * @param collection The {@code Music} collection to replace the {@code MusicList} with.
     */
    final void replace(final Collection<Music> collection) {
        synchronized (mList) {
            clear();
            mList.addAll(collection);

            for (final Music track : collection) {
                mSongIDMap.put(track.getSongId(), track);
            }
        }
    }

    /**
     * Retrieves this {@code MusicList} size.
     *
     * @return {@code MusicList} size.
     */
    final int size() {
        return mList.size();
    }
}
