
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
