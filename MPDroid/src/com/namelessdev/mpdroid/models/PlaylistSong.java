
package com.namelessdev.mpdroid.models;

import static android.text.TextUtils.isEmpty;
import static android.text.TextUtils.join;

import org.a0z.mpd.Music;

import java.util.ArrayList;
import java.util.List;

public class PlaylistSong extends AbstractPlaylistMusic {

    public PlaylistSong(Music m) {
        super(m.getAlbum(), m.getArtist(), m.getAlbumArtist(), m.getFullpath(), m.getDisc(), m
                .getDate(), m.getTime(), m.getParentDirectory(), m.getTitle(), m.getTotalTracks(),
                m.getTrack(), m.getSongId(), m.getPos(), m.getName());
    }

    public String getPlayListMainLine() {
        return getTitle();
    }

    public String getPlaylistSubLine() {
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
