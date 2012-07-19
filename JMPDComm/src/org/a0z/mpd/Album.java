package org.a0z.mpd;

public class Album extends Item {
	public static String singleTrackFormat="%1 Track (%2)";
	public static String multipleTracksFormat="%1 Tracks (%2)";

	private final String name;
	private final int songCount;
	private final int duration;
	private final int year;
	private final String artist;

	public Album(String name, int songCount, int duration, int year) {
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

	public int getSongCount() {
		return songCount;
	}
	
	public int getYear() {
		return year;
	}

	public int getDuration() {
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
			construct = Integer.toString(year);
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
