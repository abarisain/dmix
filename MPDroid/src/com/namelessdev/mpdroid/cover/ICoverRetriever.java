package com.namelessdev.mpdroid.cover;

import org.a0z.mpd.AlbumInfo;

public interface ICoverRetriever {

    public String[] getCoverUrl(AlbumInfo albumInfo) throws Exception;

    public boolean isCoverLocal();

    public String getName();
}
