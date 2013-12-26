package org.a0z.mpd;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Album extends Item implements Parcelable {
    public static String singleTrackFormat = "%1 Track (%2)";
    public static String multipleTracksFormat = "%1 Tracks (%2)";

    private final String name;
    private long songCount;
    private long duration;
    private long year;
    private String path;
    private Artist artist;

    public Album(String name, long songCount, long duration, long year, Artist artist) {
        this(name, songCount, duration, year, artist, "");
    }

    public Album(String name, long songCount, long duration, long year,
                 Artist artist, String path) {
        this.name = name;
        this.songCount = songCount;
        this.duration = duration;
        this.year = year;
        this.artist = artist;
        this.path = path;
    }

    public Album(String name, Artist artist) {
        this(name, 0, 0, 0, artist, "");
    }

    public Album(String name, Artist artist, String path) {
        this(name, 0, 0, 0, artist, path);
    }

    public Album(Album a) {
        this(a.name, a.songCount, a.duration, a.year, new Artist(a.artist), a.path);
    }

    protected Album(Parcel in) {
        this.name = in.readString();
        this.songCount = in.readLong();
        this.duration = in.readLong();
        this.year = in.readLong();
        this.path = in.readString();
        this.artist = new Artist(in.readString());
        this.artist.setIsAlbumArtist(in.readInt()>0?true:false);
    }

    public String getName() {
        return name;
    }

    public long getSongCount() {
        return songCount;
    }

    public void setSongCount(long sc) {
        songCount = sc;
    }

    public long getYear() {
        return year;
    }

    public void setYear(long y) {
        year = y;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long d) {
        duration = d;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String p) {
        path = p;
    }

    @Override
    public String mainText() {
        return name;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    @Override
    public String subText() {
        String construct = null;
        if (MPD.sortAlbumsByYear() && 0 != year) {
            construct = Long.toString(year);
        }
        if (0 != songCount) {
            if (construct != null)
                construct += " - ";
            construct += String.format(1 == songCount ? singleTrackFormat : multipleTracksFormat, songCount, Music.timeToString(duration));
        }
        return construct;
    }

    @Override
    public int compareTo(Item o) {
        if (o instanceof Album) {
            Album oa = (Album) o;
            if (MPD.sortAlbumsByYear()) {
                if (year != oa.year) {
                    return year < oa.year ? -1 : 1;
                }
            }
            int comp = super.compareTo(o);
            if (comp == 0 && artist != null) { // same album name, check artist
                comp = artist.compareTo(oa.artist);
            }
        }
        return super.compareTo(o);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Album) {
            Album a = (Album) o;
            return (year == a.year && duration == a.duration &&
                    songCount == a.songCount &&
                    path.equals(a.path) &&
                    name.equals(a.name) && artist.equals(a.artist));
        }
        return false;
    }

    public boolean isSameOnList(Item o) {
        if (null == o) {
            return false;
        }
        Album a = (Album)o;
        return (name.equals(a.getName()) &&
                artist.isSameOnList(a.getArtist()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeLong(this.songCount);
        dest.writeLong(this.duration);
        dest.writeLong(this.year);
        dest.writeString(this.path);
        dest.writeString(this.artist == null ? "" : this.artist.getName());
        dest.writeInt(this.artist == null ? 0 : this.artist.isAlbumArtist()?1:0);
    }

    public static final Parcelable.Creator<Album> CREATOR =
            new Parcelable.Creator<Album>() {
                public Album createFromParcel(Parcel in) {
                    return new Album(in);
                }

                public Album[] newArray(int size) {
                    return new Album[size];
                }
            };

    public AlbumInfo getAlbumInfo() {
        return new AlbumInfo(this);
    }

    public String info() {
        return (artist == null ? "null" : artist.info()) + " // " + name +
            ("".equals(path) ? "" : " ("+path+")");
    }

}
