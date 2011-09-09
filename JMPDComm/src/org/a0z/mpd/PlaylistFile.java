package org.a0z.mpd;

/**
 * Represents a playlist in the database
 *
 */
public class PlaylistFile implements FilesystemTreeEntry {
	private String fullpath;
	
	public PlaylistFile(String path) {
		fullpath = path;
	}
	
	@Override
	public String getFullpath() {
		return fullpath;
	}

}
