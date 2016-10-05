/*
 * Copyright (C) 2010-2016 The MPDroid Project
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

import com.anpmech.mpd.item.Album;
import com.anpmech.mpd.item.AlbumBuilder;
import com.anpmech.mpd.item.Artist;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by thaag on 2/14/2016.
 */
public class Favorites {

    private final Context mContext;
    private Set<Album> mAlbums = new HashSet<>();

    public Favorites(Context context){
        this.mContext = context;
        loadFavorites();
    }

    public void addAlbum(final Album newAlbum) {
        boolean alreadyInList = false;
        for (Album album : mAlbums){
            if (album.equals(newAlbum)){
                alreadyInList = true;
                break;
            }
        }

        if (!alreadyInList){
            mAlbums.add(newAlbum);
        }

        saveFavorites();
    }

    public void removeAlbum(final Album remAlbum){
        Iterator<Album> iterator = mAlbums.iterator();
        while (iterator.hasNext()){
            Album album = iterator.next();
            if (album.equals(remAlbum)){
                iterator.remove();
                break;
            }
        }

        saveFavorites();
    }

    public Collection<Album> getAlbums() {
        loadFavorites();
        return mAlbums;
    }

    public void loadFavorites() {
        mAlbums.clear();

        SharedPreferences prefs = mContext.getSharedPreferences("favorites",0);
        JSONArray jsonFavorites = null;
        try {
            jsonFavorites = new JSONArray(prefs.getString("favorites", "{}"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (jsonFavorites != null){
            final AlbumBuilder albumBuilder = new AlbumBuilder();


            for (int i=0; i<jsonFavorites.length(); i++){
                try {
                    JSONObject jsonAlbum = jsonFavorites.getJSONObject(i);
                    albumBuilder.setBase(jsonAlbum.getString("albumName"), jsonAlbum.getString("albumArtist"), true);
                    mAlbums.add(albumBuilder.build());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    public void saveFavorites(){
        JSONArray jsonFavorites = new JSONArray();
        for (Album album : mAlbums){
            JSONObject jsonAlbum = new JSONObject();
            try {
                jsonAlbum.put("albumName", album.getName());
                jsonAlbum.put("albumArtist", album.getArtist().getName());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            jsonFavorites.put(jsonAlbum);
        }

        SharedPreferences prefs = mContext.getSharedPreferences("favorites",0);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("favorites", jsonFavorites.toString());
        editor.apply();

    }
}
