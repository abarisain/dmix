package com.namelessdev.mpdroid.helpers;

import android.graphics.Bitmap;
import com.namelessdev.mpdroid.cover.ICoverRetriever;

public class CoverInfo {
    public enum STATE {NEW, CACHE_COVER_FETCH, WEB_COVER_FETCH, CREATE_BITMAP}

    ;
    private STATE state = STATE.NEW;
    private String artist = "";
    private String album = "";
    private String path;
    private String filename;
    private Bitmap[] bitmap;
    private byte[] coverBytes;
    public static final int MAX_SIZE = 0;
    private int coverMaxSize = MAX_SIZE;
    private int cachedCoverMaxSize = MAX_SIZE;
    private ICoverRetriever coverRetriever;

    public CoverInfo() {
    }

    public CoverInfo(String artist, String album) {
        this.artist = artist;
        this.album = album;
    }

    public ICoverRetriever getCoverRetriever() {
        return coverRetriever;
    }

    public void setCoverRetriever(ICoverRetriever coverRetriever) {
        this.coverRetriever = coverRetriever;
    }

    public int getCoverMaxSize() {
        return coverMaxSize;
    }

    public void setCoverMaxSize(int coverMaxSize) {
        this.coverMaxSize = coverMaxSize;
    }

    public int getCachedCoverMaxSize() {
        return cachedCoverMaxSize;
    }

    public void setCachedCoverMaxSize(int cachedCoverMaxSize) {
        this.cachedCoverMaxSize = cachedCoverMaxSize;
    }

    public byte[] getCoverBytes() {
        return coverBytes;
    }

    public void setCoverBytes(byte[] coverBytes) {
        this.coverBytes = coverBytes;
    }

    public STATE getState() {
        return state;
    }

    public void setState(STATE state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoverInfo coverInfo = (CoverInfo) o;

        if (cachedCoverMaxSize != coverInfo.cachedCoverMaxSize) return false;
        if (coverMaxSize != coverInfo.coverMaxSize) return false;
        if (album != null ? !album.equals(coverInfo.album) : coverInfo.album != null) return false;
        if (artist != null ? !artist.equals(coverInfo.artist) : coverInfo.artist != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (album != null ? album.hashCode() : 0);
        result = 31 * result + coverMaxSize;
        result = 31 * result + cachedCoverMaxSize;
        return result;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Bitmap[] getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap[] bitmap) {
        this.bitmap = bitmap;
    }
}