
package com.namelessdev.mpdroid.helpers;

import org.a0z.mpd.AlbumInfo;

public interface CoverDownloadListener {

    public void onCoverDownloaded(CoverInfo cover);

    public void onCoverDownloadStarted(CoverInfo cover);

    public void onCoverNotFound(CoverInfo coverInfo);

    public void tagAlbumCover(AlbumInfo albumInfo);

}
