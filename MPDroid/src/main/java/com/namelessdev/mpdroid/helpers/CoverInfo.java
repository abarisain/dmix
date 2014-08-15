/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.helpers;

import android.graphics.Bitmap;

import com.namelessdev.mpdroid.cover.ICoverRetriever;

import org.a0z.mpd.AlbumInfo;

public class CoverInfo extends AlbumInfo {
    public enum STATE {
        NEW, CACHE_COVER_FETCH, WEB_COVER_FETCH, CREATE_BITMAP, COVER_FOUND, COVER_NOT_FOUND
    }

    private STATE state = STATE.NEW;
    private Bitmap[] bitmap = new Bitmap[0];
    private byte[] coverBytes = new byte[0];
    private boolean priority;
    public static final int MAX_SIZE = 0;
    private int coverMaxSize = MAX_SIZE;
    private int cachedCoverMaxSize = MAX_SIZE;
    private ICoverRetriever coverRetriever;
    private CoverDownloadListener listener;
    private boolean requestGivenUp = false;

    public CoverInfo() {
        super();
    }

    public CoverInfo(AlbumInfo albumInfo) {
        super();
        this.artist = albumInfo.getArtist();
        this.album = albumInfo.getAlbum();
        this.path = albumInfo.getPath();
        this.filename = albumInfo.getFilename();
    }

    public CoverInfo(CoverInfo coverInfo) {
        super();
        this.state = coverInfo.state;
        this.artist = coverInfo.artist;
        this.album = coverInfo.album;
        this.path = coverInfo.path;
        this.filename = coverInfo.filename;
        this.bitmap = coverInfo.bitmap;
        this.coverBytes = coverInfo.coverBytes;
        this.priority = coverInfo.priority;
        this.coverMaxSize = coverInfo.coverMaxSize;
        this.cachedCoverMaxSize = coverInfo.cachedCoverMaxSize;
        this.coverRetriever = coverInfo.coverRetriever;
        this.listener = coverInfo.getListener();
        this.requestGivenUp = coverInfo.requestGivenUp;
    }

    public CoverInfo(String artist, String album) {
        this.artist = artist;
        this.album = album;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CoverInfo coverInfo = (CoverInfo) o;

        if (album != null ? !album.equals(coverInfo.album) : coverInfo.album != null)
            return false;
        if (artist != null ? !artist.equals(coverInfo.artist) : coverInfo.artist != null)
            return false;

        return true;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public Bitmap[] getBitmap() {
        return bitmap;
    }

    public int getCachedCoverMaxSize() {
        return cachedCoverMaxSize;
    }

    public byte[] getCoverBytes() {
        return coverBytes;
    }

    public int getCoverMaxSize() {
        return coverMaxSize;
    }

    public ICoverRetriever getCoverRetriever() {
        return coverRetriever;
    }

    public String getFilename() {
        return filename;
    }

    public CoverDownloadListener getListener() {
        return listener;
    }

    public String getPath() {
        return path;
    }

    public STATE getState() {
        return state;
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (album != null ? album.hashCode() : 0);
        return result;
    }

    public boolean isPriority() {
        return priority;
    }

    public boolean isRequestGivenUp() {
        return requestGivenUp;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setBitmap(Bitmap[] bitmap) {
        this.bitmap = bitmap;
    }

    public void setCachedCoverMaxSize(int cachedCoverMaxSize) {
        this.cachedCoverMaxSize = cachedCoverMaxSize;
    }

    public void setCoverBytes(byte[] coverBytes) {
        this.coverBytes = coverBytes;
    }

    public void setCoverMaxSize(int coverMaxSize) {
        this.coverMaxSize = coverMaxSize;
    }

    public void setCoverRetriever(ICoverRetriever coverRetriever) {
        this.coverRetriever = coverRetriever;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setListener(CoverDownloadListener listener) {
        this.listener = listener;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public void setRequestGivenUp(boolean requestGivenUp) {
        this.requestGivenUp = requestGivenUp;
    }

    public void setState(STATE state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "CoverInfo{state=" + state + ", artist=" + artist == null ? "" : artist + ", album="
                + album == null ? "" : album + " priority=" + priority + "}";
    }
}
