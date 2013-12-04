package com.namelessdev.mpdroid.helpers;

public interface CoverDownloadListener {

    public void onCoverDownloaded(CoverInfo cover);

    public void onCoverNotFound(CoverInfo coverInfo);

    public void onCoverDownloadStarted(CoverInfo cover);

}
