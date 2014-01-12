
package com.namelessdev.mpdroid.models;

import static com.namelessdev.mpdroid.tools.StringUtils.getExtension;

import org.a0z.mpd.Music;

public class PlaylistStream extends AbstractPlaylistMusic {

    public PlaylistStream(Music m) {
        super(m.getAlbum(), m.getArtist(), m.getAlbumArtist(), m.getFullpath(), m.getDisc(), m
                .getDate(), m.getTime(), m.getParentDirectory(), m.getTitle(), m.getTotalTracks(),
                m.getTrack(), m.getSongId(), m.getPos(), m.getName());
    }

    public String getPlayListMainLine() {
        return getName().replace("." + getExtension(getName()), "");
    }

    public String getPlaylistSubLine() {
        return getFullpath();
    }

}
