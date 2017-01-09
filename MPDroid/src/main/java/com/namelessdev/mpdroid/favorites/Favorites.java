/*
 * Copyright (C) 2010-2017 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.favorites;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.anpmech.mpd.MPD;
import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.Music;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Favorites {

    /**
     * Sticker name for album favorites or just its prefix, if a personalization key is available.
     */
    private static final String STICKER_ALBUM_FAVORITE = "albumfav";

    /**
     * Preference key of the personalization key.
     */
    private static final String PREFERENCE_FAVORITE_KEY = "favoriteKey";

    /**
     * Preference key of the activation of favorites.
     */
    private static final String PREFERENCE_USE_FAVORITE = "useFavorites";

    /**
     * MPD server
     */
    private final MPD mMPD;

    public Favorites(final MPD mpd) {
        this.mMPD = mpd;
    }

    /**
     * Marks an album as a favorite.
     * @param album Favored album
     * @throws IOException
     * @throws MPDException
     */
    public void addAlbum(final Album album) throws IOException, MPDException {
        mMPD.getStickerManager().set(computeFavoriteStickerKey(), "Y", mMPD.getSongs(album));
        Tools.notifyUser(R.string.addToFavorites, album.getName());
    }

    /**
     * Removes an album from favorites.
     * @param album Album to remove
     * @throws IOException
     * @throws MPDException
     */
    public void removeAlbum(final Album album) throws IOException, MPDException {
        mMPD.getStickerManager().delete(computeFavoriteStickerKey(), mMPD.getSongs(album));
        Tools.notifyUser(R.string.removeFromFavorites, album.getName());
    }

    /**
     * Determines if an album is favored.
     * @param album Album to check
     * @return true, if album is favored
     * @throws IOException
     * @throws MPDException
     */
    public boolean isFavorite(final Album album) throws IOException, MPDException {
        final List<Music> songs = mMPD.getSongs(album);
        if (songs.isEmpty()) {
            return false;
        }
        final String favorite = mMPD.getStickerManager().get(songs.get(0),
                computeFavoriteStickerKey());
        return favorite != null && favorite.length() > 0;
    }

    /**
     * Determine all favored albums.
     * @return all favored albums
     * @throws IOException
     * @throws MPDException
     */
    public Collection<Album> getAlbums() throws IOException, MPDException {
        final Set<Music> songs =
                mMPD.getStickerManager().find("", computeFavoriteStickerKey()).keySet();
        final Set<Album> albums = new HashSet<>();
        for (final Music song : songs) {
            albums.add(song.getAlbum());
        }
        return albums;
    }

    /**
     * Computes the sticker name for album favorites incl. the personalization key.
     * @return Sticker name for album favorites
     */
    private static String computeFavoriteStickerKey() {
        final SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());
        final String personalizationKey = settings.getString(PREFERENCE_FAVORITE_KEY, "").trim();
        return STICKER_ALBUM_FAVORITE +
                (!personalizationKey.isEmpty() ? "-" + personalizationKey : "");
    }

    /**
     * Are favorites activated in preferences?
     * @return true, if activated.
     */
    public static boolean areFavoritesActivated() {
        final SharedPreferences settings =
                PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());
        return settings.getBoolean(PREFERENCE_USE_FAVORITE, false);
    }

}
