package org.musicpd.android.cover;

public interface ICoverRetriever {

	public String[] getCoverUrl(String artist, String album, String path, String filename) throws Exception;

	public boolean isCoverLocal();

	public String getName();
}
