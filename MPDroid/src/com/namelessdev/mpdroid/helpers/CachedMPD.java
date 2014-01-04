package com.namelessdev.mpdroid.helpers;

import org.a0z.mpd.*;
import org.a0z.mpd.exception.MPDClientException;
import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Context;
import android.util.Log;

import com.namelessdev.mpdroid.tools.MultiMap;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

import android.util.Log;

/*
 * Cached Version of org.a0z.mpd.MPD
 *
 * Certain methods of MPD are overridden to call the cache.
 *
 * All public methods should call cache.refresh() to see whether the
 * cache is up to date.
 */
public class CachedMPD extends MPD
{

    boolean useCache = true;

    protected AlbumCache cache;

    public CachedMPD(boolean useCache) {
        cache = AlbumCache.getInstance(this);
        setUseCache(useCache);
    }

    public CachedMPD() {
        this(true);
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    /* force refreshing of the cache
     */
    public void clearCache() {
        if (useCache) {
            cache.refresh(true);
        }
    }

    /* is checked before requests
     */
    protected boolean cacheOK() {
        return (useCache && cache.refresh());
    }


    /*
     * List all albums of given artist from database.
     */
    @Override
    public List<String> listAlbums(String artist, boolean useAlbumArtist,
                                   boolean includeUnknownAlbum)
        throws MPDServerException {
        if (!cacheOK()) {
            return super.listAlbums(artist, useAlbumArtist, includeUnknownAlbum);
        }
        return new ArrayList(cache.getAlbums(artist, useAlbumArtist));
    }



    /*
     * List all albumartist or artist names of all given albums from database.
     *
     * @return list of array of artist names for each album.
     */
    @Override
    public List<String[]> listArtists(List<Album> albums, boolean useAlbumArtist)
        throws MPDServerException {
        if (!cacheOK()) {
            return super.listArtists(albums, useAlbumArtist);
        }
        ArrayList<String[]> result = new ArrayList<String[]>();
        for (Album album : albums) {
            String a = album.getName();
            result.add((String[])cache.getArtistsByAlbum
                       (album.getName(), useAlbumArtist).toArray(new String[0]));
        }
        return result;
    }

    protected List<String> listArtists(String album, boolean useAlbumArtist,
                                       boolean includeUnknownAlbum) {
        // called internally, refresh already done
        return new ArrayList(cache.getArtistsByAlbum(album, useAlbumArtist));
    }


    /*
     * with cache we can afford to get the paths for all albums
     */
    @Override
    public List<Album> getAllAlbums(boolean trackCountNeeded)
        throws MPDServerException {
        if (!cacheOK()) {
            return super.getAllAlbums(trackCountNeeded);
        }
        List<Album> albums = new ArrayList<Album>();
        Map<String, Set<String>> aa_albums = cache.getAlbumArtistsByAlbum();
        for (String album : aa_albums.keySet()) {
            for (String artist : aa_albums.get(album)) {
                albums.add(new Album(album, new Artist(artist, 0), true));
            }
        }
        Map<String, Set<String>> a_albums  = cache.getArtistsByAlbum();
        for (String album : a_albums.keySet()) {
            for (String artist : a_albums.get(album)) {
                albums.add(new Album(album, new Artist(artist, 0), false));
            }
        }
        Collections.sort(albums);
        // fixAlbumArtists(albums);
        getAlbumDetails(albums,true);
        return albums;
    }

    /*
     * add path info to all albums
     */
    @Override
    protected void addAlbumPaths(List<Album> albums) {
        if (!cacheOK()) {
            return;
        }
        for (Album a : albums) {
            Artist artist = a.getArtist();
            String aname = (artist==null?"":artist.getName());
            AlbumCache.AlbumDetails details
                = cache.getAlbumDetails(aname, a.getName(), a.hasAlbumArtist());
            if (details != null) {
                a.setPath(details.path);
            }
            Log.d("MPD CACHED","album " + a.info());
        }
        Log.d("MPD CACHED","addAlbumPaths " + albums.size());
    }

    /*
     * add detail info to all albums
     */
    @Override
    protected void getAlbumDetails(List<Album> albums, boolean findYear/*ignored*/)
        throws MPDServerException {
        if (!cacheOK()) {
            super.getAlbumDetails(albums, findYear);
            return;
        }
        for (Album a : albums) {
            Artist art = a.getArtist();
            String artist = (art == null ? "" : art.getName());
            //Log.d("MPD CACHED","Details  " + artist+" // "+a.getName());
            AlbumCache.AlbumDetails details =
                cache.getAlbumDetails(artist, a.getName(), a.hasAlbumArtist());
            if (null != details){
                a.setSongCount(details.numtracks);
                a.setDuration(details.totaltime);
                a.setYear(details.date);
                a.setPath(details.path);
            }
        }
        Log.d("MPD CACHED","Details of " + albums.size());
    }

}