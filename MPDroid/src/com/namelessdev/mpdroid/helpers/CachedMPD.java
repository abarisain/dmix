
package com.namelessdev.mpdroid.helpers;

import android.util.Log;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.MPD;
import org.a0z.mpd.exception.MPDServerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public CachedMPD() {
        this(true);
    }

    public CachedMPD(boolean useCache) {
        cache = AlbumCache.getInstance(this);
        setUseCache(useCache);
    }

    /*
     * add path info to all albums
     */
    @Override
    protected void addAlbumPaths(List<Album> albums) {
        if (!cacheOK()) {
            super.addAlbumPaths(albums);
            return;
        }
        for (Album a : albums) {
            Artist artist = a.getArtist();
            String aname = (artist == null ? "" : artist.getName());
            AlbumCache.AlbumDetails details = cache.getAlbumDetails(aname, a.getName(),
                    a.hasAlbumArtist());
            if (details != null) {
                a.setPath(details.path);
            }
            // Log.d("MPD CACHED","album " + a.info());
        }
        Log.d("MPD CACHED", "addAlbumPaths " + albums.size());
    }

    /*
     * is checked before requests
     */
    protected boolean cacheOK() {
        return (useCache && cache.refresh());
    }

    /*
     * force refreshing of the cache
     */
    public void clearCache() {
        if (useCache) {
            cache.refresh(true);
        }
    }

    /*
     * add detail info to all albums
     */
    @Override
    protected void getAlbumDetails(List<Album> albums, boolean findYear/* ignored */)
            throws MPDServerException {
        if (!cacheOK()) {
            super.getAlbumDetails(albums, findYear);
            return;
        }
        for (Album a : albums) {
            Artist art = a.getArtist();
            String artist = (art == null ? "" : art.getName());
            // Log.d("MPD CACHED","Details for " + a.info());
            AlbumCache.AlbumDetails details =
                    cache.getAlbumDetails(artist, a.getName(), a.hasAlbumArtist());
            if (null != details) {
                a.setSongCount(details.numtracks);
                a.setDuration(details.totaltime);
                a.setYear(details.date);
                a.setPath(details.path);
            }
        }
        Log.d("MPD CACHED", "Details of " + albums.size());
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
        Set<Album> albums = new HashSet<Album>();
        Set<List<String>> albumset = cache.getUniqueAlbumSet();
        for (List<String> ai : albumset) {
            Album album;
            if ("".equals(ai.get(2))) { // no albumartist
                album = (new Album(ai.get(0), new Artist(ai.get(1)), false));
            } else {
                album = (new Album(ai.get(0), new Artist(ai.get(2)), true));
            }
            albums.add(album);
        }
        List<Album> result = new ArrayList<Album>(albums);
        Collections.sort(result);
        getAlbumDetails(result, true);
        return result;
    }

    @Override
    public List<String[]> listAlbumArtists(List<Album> albums) throws MPDServerException {
        if (!cacheOK()) {
            return super.listAlbumArtists(albums);
        }
        List<String[]> albumartists = new ArrayList<String[]>();
        for (Album a : albums) {
            Artist artist = a.getArtist();
            Set<String> aartists =
                    cache.getAlbumArtists(a.getName(),
                            (artist == null ? "" : artist.getName()));
            albumartists.add(aartists.toArray(new String[0]));
        }
        return albumartists;
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
            result.add((String[]) cache.getArtistsByAlbum
                    (album.getName(), useAlbumArtist).toArray(new String[0]));
        }
        return result;
    }

    protected List<String> listArtists(String album, boolean useAlbumArtist,
            boolean includeUnknownAlbum) {
        // called internally, refresh already done
        return new ArrayList(cache.getArtistsByAlbum(album, useAlbumArtist));
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

}
