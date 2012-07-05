package org.a0z.mpd;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.a0z.mpd.exception.InvalidParameterException;
import org.a0z.mpd.exception.MPDServerException;

/**
 * Class representing a directory.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id: Directory.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public final class Directory extends Item implements FilesystemTreeEntry {
	private Map<String, Music> files;
	private Map<String, Directory> directories;
	private Directory parent;
	private String name;
	private MPD mpd;

	/**
	 * Creates a new directory.
	 * 
	 * @param mpd
	 *           MPD controller.
	 * @param parent
	 *           parent directory.
	 * @param name
	 *           directory name.
	 */
	private Directory(MPD mpd, Directory parent, String name) {
		this.mpd = mpd;
		this.name = name;
		this.parent = parent;
		this.files = new HashMap<String, Music>();
		this.directories = new HashMap<String, Directory>();
	}

	/**
	 * Creates a new directory.
	 * 
	 * @param mpd
	 *           MPD controller.
	 * @return last path component.
	 */
	public static Directory makeRootDirectory(MPD mpd) {
		return new Directory(mpd, null, "");
	}

	/**
	 * Retrieves directory name.
	 * 
	 * @return directory name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves files from directory.
	 * 
	 * @return files from directory.
	 */
	public TreeSet<Music> getFiles() {
		TreeSet<Music> c = new TreeSet<Music>(new Comparator<Music>() {
			public int compare(Music o1, Music o2) {
				return o1.getFilename().compareTo(o2.getFilename());
			}
		});
		
		for (Music item : files.values())
			c.add(item);
		return c;
	}

	/**
	 * Gets Music object by title
	 * 
	 * @param title title of the file to be returned
	 * @return Returns null if title not found
	 */
	public Music getFileByTitle(String title) {
		for (Music music : files.values()) {
			if (music.getTitle().equals(title))
				return music;
		}
		return null;
	}

	/**
	 * Retrieves sub-directories.
	 * 
	 * @return sub-directories.
	 */
	public TreeSet<Directory> getDirectories() {
		TreeSet<Directory> c = new TreeSet<Directory>(new Comparator<Directory>() {
			public int compare(Directory o1, Directory o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		for (Directory item : directories.values())
			c.add(item);
		return c;
	}
	
	/**
	 * Retrieves a sub-directory.
	 * 
	 * @param name
	 *           name of sub-directory to retrieve.
	 * @return a sub-directory.
	 */
	public Directory getDirectory(String name) {
		return directories.get(name);
	}
	
	/**
	 * Check if a given directory exists as a sub-directory.
	 * 
	 * @param name
	 *           sub-directory name.
	 * @return true if sub-directory exists, false if not.
	 */
	public boolean containsDir(String name) {
		return directories.containsKey(name);
	}
	
	/**
	 * Refresh directory contents (not recursive).
	 * 
	 * @throws MPDServerException
	 *            if an error occurs while contacting server.
	 */
	public void refreshData() throws MPDServerException {
		List<FilesystemTreeEntry> c = mpd.getDir(this.getFullpath());
		for (FilesystemTreeEntry o : c) {
			if (o instanceof Directory) {
				Directory dir = (Directory) o;
				if (!directories.containsKey(dir.getName())) {
					directories.put(dir.getName(), dir);
				}
			} else if (o instanceof Music) {
				Music music = (Music) o;
				if (!files.containsKey(music.getFilename())) {
					files.put(music.getFilename(), music);
				} else {
					Music old = files.get(music.getFilename());
					old.update(music);
				}
			}
		}
	}

	/**
	 * Given a path not starting or ending with '/', creates all directories on this path.
	 * 
	 * @param subPath
	 *           path, must not start or end with '/'.
	 * @return the last component of the path created.
	 */
	public Directory makeDirectory(String subPath) {
		String name;
		String remainingPath;
		int slashIndex = subPath.indexOf('/');

		if (slashIndex == 0)
			throw new InvalidParameterException("name starts with '/'");
		
		// split path
		if (slashIndex == -1) {
			name = subPath;
			remainingPath = null;
		} else {
			name = subPath.substring(0, slashIndex);
			remainingPath = subPath.substring(slashIndex + 1);
		}
		
		// create directory
		Directory dir;
		if (!directories.containsKey(name)) {
			dir = new Directory(mpd, this, name);
			directories.put(dir.getName(), dir);
		} else {
			dir = directories.get(name);
		}
		
		// create remainder
		if (remainingPath != null)
			return dir.makeDirectory(remainingPath);
		return dir;
	}

	/**
	 * Adds a file, creating path directories.
	 * 
	 * @param file
	 *           file to be added
	 */
	public void addFile(Music file) {
		if (getFullpath().compareTo(file.getPath()) == 0) {
			file.setParent(this);
			files.put(file.getFilename(), file);
		} else {
			Directory dir = makeDirectory(file.getPath());
			dir.addFile(file);
		}
	}

	/**
	 * Retrieves parent directory.
	 * 
	 * @return parent directory.
	 */
	public Directory getParent() {
		return parent;
	}

	/**
	 * Retrieves directory's full path (does not start with /)
	 * 
	 * @return full path
	 */
	public String getFullpath() {
		if (getParent() != null && getParent().getParent() != null) {
			return getParent().getFullpath() + "/" + getName();
		} else {
			return getName();
		}
	}
	
	/**
	 * Retrieves a textual representation of this object.
	 * 
	 * @return textual representation of this object.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("+" + getFullpath() + "\n");
		for (String file : files.keySet()) {
			sb.append(files.get(file).toString() + "\n");
		}

		for (String dir : directories.keySet()) {
			sb.append(directories.get(dir).toString() + "\n");
		}
		sb.append("-" + this.getFullpath() + "\n");
		return sb.toString();
	}
}