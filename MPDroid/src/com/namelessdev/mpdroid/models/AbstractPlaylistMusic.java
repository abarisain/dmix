package com.namelessdev.mpdroid.models;

import org.a0z.mpd.Directory;
import org.a0z.mpd.Music;

public abstract class AbstractPlaylistMusic extends Music {
    private int currentSongIconRefID;
    private boolean forceCoverRefresh = false;

    protected AbstractPlaylistMusic(String album, String artist, String albumartist, String fullpath, int disc, long date, long time, Directory parent, String title, int totalTracks, int track, int songId, int pos, String name) {
        super(album, artist, albumartist, fullpath, disc, date, time, parent, title, totalTracks, track, songId, pos, name);
    }

    public boolean isForceCoverRefresh() {
        return forceCoverRefresh;
    }

    public void setForceCoverRefresh(boolean forceCoverRefresh) {
        this.forceCoverRefresh = forceCoverRefresh;
    }

    public int getCurrentSongIconRefID() {
        return currentSongIconRefID;
    }

    public void setCurrentSongIconRefID(int currentSongIconRefID) {
        this.currentSongIconRefID = currentSongIconRefID;
    }

    public abstract String getPlayListMainLine();

    public abstract String getPlaylistSubLine();
}
