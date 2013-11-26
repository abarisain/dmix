package org.a0z.mpd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			Matcher matcher = Pattern.compile("^.*/(.+)\\.(\\w+)$").matcher(fullpath);
			if (matcher.matches()) {
				return matcher.replaceAll("[$2] $1.$2");
			} else {
				return fullpath;
			}
		}
		return "";
	}

}
