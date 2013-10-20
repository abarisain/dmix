package org.a0z.mpd;

import android.os.Parcel;
import android.os.Parcelable;

public class Album extends Item implements Parcelable {
    public static String singleTrackFormat = "%1 Track (%2)";
    public static String multipleTracksFormat = "%1 Tracks (%2)";

    private final String name;
    private final long songCount;
    private final long duration;
    private final long year;
    private String artist;

    public Album(String name, long songCount, long duration, long year, String artist) {
        this.name = name;
        this.songCount = songCount;
        this.duration = duration;
        this.year = year;
        this.artist = artist;
    }

    public Album(String name, String artist) {
        this.name = name;
        this.songCount = 0;
        this.duration = 0;
        this.year = 0;
        this.artist = artist;
    }

    protected Album(Parcel in) {
        this.name = in.readString();
        this.songCount = in.readLong();
        this.duration = in.readLong();
        this.year = in.readLong();
        this.artist = in.readString();
    }

    public String getName() {
        return name;
    }

    public long getSongCount() {
        return songCount;
    }

    public long getYear() {
        return year;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public String mainText() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
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
        if (MPD.sortAlbumsByYear() && (o instanceof Album)) {
            Album oa = (Album) o;
            if (year != oa.year) {
                return year < oa.year ? -1 : 1;
            }
        }
        return super.compareTo(o);
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
        dest.writeString(this.artist);
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

}
