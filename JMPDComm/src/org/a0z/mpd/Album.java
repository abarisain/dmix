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
    private Artist artist;

    public Album(String name, long songCount, long duration, long year, Artist artist) {
        this.name = name;
        this.songCount = songCount;
        this.duration = duration;
        this.year = year;
        this.artist = artist;
    }

    public Album(String name, Artist artist) {
        this(name, 0, 0, 0, artist);
    }

    protected Album(Parcel in) {
        this.name = in.readString();
        this.songCount = in.readLong();
        this.duration = in.readLong();
        this.year = in.readLong();
        this.artist = new Artist(in.readString());
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
            if (comp == 0) { // same album name, check artist
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
        dest.writeString(this.artist.getName());
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
        return new AlbumInfo(getArtist().getName(), getName());
    }

}
