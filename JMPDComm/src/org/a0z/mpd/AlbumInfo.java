package org.a0z.mpd;

public class AlbumInfo {

    protected String artist = "";
    protected String album = "";
    protected String path = "";
    protected String filename = "";

    public AlbumInfo() {
    }

    public AlbumInfo(String artist, String album, String path, String filename) {
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.filename = filename;
    }

    public AlbumInfo(String artist, String album) {
        this.artist = artist;
        this.album = album;
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

    @Override
    public String toString() {
        return "AlbumInfo{" +
                "artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", path='" + path + '\'' +
                ", filename='" + filename + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlbumInfo albumInfo = (AlbumInfo) o;

        if (album != null ? !album.equals(albumInfo.album) : albumInfo.album != null) return false;
        if (artist != null ? !artist.equals(albumInfo.artist) : albumInfo.artist != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (album != null ? album.hashCode() : 0);
        return result;
    }

    public String getKey() {
        return artist+album;
    }

    public boolean isValid() {
        return !StringsUtils.isNullOrEmpty(artist) && !StringsUtils.isNullOrEmpty(album);
    }
}
