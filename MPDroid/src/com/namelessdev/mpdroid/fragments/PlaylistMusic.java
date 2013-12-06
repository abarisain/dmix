package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Music;

import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static android.text.TextUtils.join;
import static com.namelessdev.mpdroid.tools.StringUtils.getExtension;

public class PlaylistMusic extends Music {
    private int currentSongIconRefID;
    private boolean forceCoverRefresh = false;

    public PlaylistMusic(Music m) {
        super(m.getAlbum(), m.getArtist(), m.getAlbumArtist(), m.getFullpath(), m.getDisc(), m.getDate(), m.getTime(), m.getParentDirectory(), m.getTitle(), m.getTotalTracks(), m.getTrack(), m.getSongId(), m.getPos(), m.getName());
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

    public String getPlayListMainLine() {
        String line;
        if (isStream()) {
            line = getName().replace("." + getExtension(getName()), "");
        } else {
            line = getTitle();
        }
        return line;
    }

    public String getPlaylistSubLine() {
        if (isStream()) {
            return getFullpath();
        } else {
            List<String> sublineTexts = new ArrayList<String>();
            if (!isEmpty(getArtist())) {
                sublineTexts.add(getArtist());
            }
            if (!isEmpty(getAlbum())) {
                sublineTexts.add(getAlbum());
            }
            return join(" - ", sublineTexts);
        }
    }


}
