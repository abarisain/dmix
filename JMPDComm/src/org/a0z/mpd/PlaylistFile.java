package org.a0z.mpd;

/**
 * Represents a playlist in the database
 *
 */
public class PlaylistFile extends Item implements FilesystemTreeEntry {
	private String fullpath;
	
	public PlaylistFile(String path) {
		fullpath = path;
	}
	
	@Override
	public String getFullpath() {
		return fullpath;
	}

	@Override
	public String getName() {
		if (fullpath != null) {
			int index = fullpath.lastIndexOf('/');
			if (index > 0) {
				return fullpath.substring(index);
			} else {
				return fullpath;
			}
		}
		return "";
	}

}
