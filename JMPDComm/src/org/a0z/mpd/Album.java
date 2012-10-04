package org.a0z.mpd;

public class Album extends Item {
	public static String singleTrackFormat="%1 Track (%2)";
	public static String multipleTracksFormat="%1 Tracks (%2)";

	private final String name;
	private final long songCount;
	private final long duration;
	private final long year;
	private final String artist;

	public Album(String name, long songCount, long duration, long year) {
		this.name=name;
		this.songCount=songCount;
		this.duration=duration;
		this.year=year;
		this.artist=null;
	}

	public Album(String name, String artist) {
		this.name=name;
		this.songCount=0;
		this.duration=0;
		this.year=0;
		this.artist=artist;
	}

	public String getName() {
		return name;
	}

	public String getArtist() {
		return artist;
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
}
