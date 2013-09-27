package org.a0z.mpd;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class Album extends Item implements Parcelable, Serializable {
	public static String singleTrackFormat="%1 Track (%2)";
	public static String multipleTracksFormat="%1 Tracks (%2)";

	private final String name;
	private final long songCount;
	private final long duration;
	private final long year;

	public Album(String name, long songCount, long duration, long year) {
		this.name=name;
		this.songCount=songCount;
		this.duration=duration;
		this.year=year;
	}

	public Album(String name) {
		this.name=name;
		this.songCount=0;
		this.duration=0;
		this.year=0;
	}

	protected Album(Parcel in) {
		this.name=in.readString();
		this.songCount=in.readLong();
		this.duration=in.readLong();
		this.year=in.readLong();
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

	@Override
	public String subText() {
		String construct = null;
		if (MPD.sortAlbumsByYear() && 0!=year) {
			construct = Long.toString(year);
		}
		if (0!=songCount) {
			if(construct != null)
				construct += " - ";
			construct += String.format(1==songCount ? singleTrackFormat : multipleTracksFormat, songCount, Music.timeToString(duration));
		}
		return construct;
	}

    @Override
    public int compareTo(Item o) {
    	if (MPD.sortAlbumsByYear() && (o instanceof Album)) {
    		Album oa=(Album)o;
    		if (year!=oa.year) {
    			return year<oa.year ? -1 : 1;
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
	}

	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public Album createFromParcel(Parcel in) {
                return new Album(in);
            }
 
            public Album[] newArray(int size) {
                return new Album[size];
            }
        };

}
