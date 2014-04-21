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

import static android.text.TextUtils.isEmpty;
import static org.a0z.mpd.StringsUtils.getHashFromString;

public class AlbumInfo {

    protected String artist = "";
    protected String album = "";
    protected String path = "";
    protected String filename = "";
    private static final String INVALID_ALBUM_KEY = "INVALID_ALBUM_KEY";

    public AlbumInfo() {
    }

    public AlbumInfo(Album album) {
        Artist a = album.getArtist();
        this.artist = (a == null ? "" : a.getName());
        this.album = album.getName();
        this.path = album.getPath();
    }

    public AlbumInfo(AlbumInfo a) {
        this.artist = a.artist;
        this.album = a.album;
        this.path = a.path;
        this.filename = a.filename;
    }

    public AlbumInfo(String artist, String album) {
        this.artist = artist;
        this.album = album;
    }

    public AlbumInfo(String artist, String album, String path, String filename) {
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.filename = filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AlbumInfo albumInfo = (AlbumInfo) o;

        if (album != null ? !album.equals(albumInfo.album) : albumInfo.album != null)
            return false;
        if (artist != null ? !artist.equals(albumInfo.artist) : albumInfo.artist != null)
            return false;

        return true;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getFilename() {
        return filename;
    }

    public String getKey() {
        return isValid() ? getHashFromString(artist + album) : INVALID_ALBUM_KEY;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (album != null ? album.hashCode() : 0);
        return result;
    }

    public boolean isValid() {
        return !isEmpty(artist) && !isEmpty(album);
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "AlbumInfo{" +
                "artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", path='" + path + '\'' +
                ", filename='" + filename + '\'' +
                '}';
    }
}
