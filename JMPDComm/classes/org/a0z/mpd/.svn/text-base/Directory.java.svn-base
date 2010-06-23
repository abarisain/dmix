package org.a0z.mpd;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

/**
 * Class representing a directory.
 * @author Felipe Gustavo de Almeida
 * @version $Id: Directory.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public final class Directory implements FilesystemTreeEntry {
	private Map<String, Music> files;
	private Map<String, Directory> directories;
    private Directory parent;
    private String name;
    private MPD mpd;

    /**
     * Creates a new directory.
     * @param mpd MPD controller.
     * @param parent parent directory.
     * @param name directory name.
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
     * @param mpd MPD controller.
     * @return last path component.
     */
    public static Directory makeRootDirectory(MPD mpd) {
        return new Directory(mpd, null, "");
    }

    /**
     * Retrieves directory name.
     * @return directory name.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves files from directory.
     * @return files from directory.
     */
    public TreeSet<Music> getFiles() {
    	TreeSet<Music> c = new TreeSet<Music>(new Comparator<Music>() {
            public int compare(Music o1, Music o2) {
                return o1.getFilename().compareTo(o2.getFilename());
            }
        });
        for (String line : files.keySet()) {
            c.add(files.get(line));
        }
        return c;
    }

    /**
     * Gets Music object by title
     * @param Title
     * @return Returns null if title not found
     */
	public Music getFileByTitle(String Title)
	{
		for (Music music : files.values()) {
			if(music.getTitle().equals(Title))
				return music;
		}
		return null;
	}
    
    
    /**
     * Retrieves subdirectories.
     * @return subdirectories.
     */
    public TreeSet<Directory> getDirectories() {
    	TreeSet<Directory> c = new TreeSet<Directory>(new Comparator<Directory>() {
            public int compare(Directory o1, Directory o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (String line : directories.keySet()) {
            c.add(directories.get(line));
        }
        return c;
    }

    /**
     * Refresh directorie contents (not recursive).
     * @throws MPDServerException if an error occurs while contacting server.
     */
    public void refreshData() throws MPDServerException {
        LinkedList<FilesystemTreeEntry> c = mpd.getDir(this.getFullpath());
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
     * Given a path not starting or ending with '/', creates all dirs on this
     * path.
     * @param name path, must not start or end with '/'.
     * @return the last component of the path created.
     */
    public Directory makeDirectory(String name) {
        String firstName;
        String lastName;
        int slashIndex = name.indexOf('/');

        if (slashIndex == 0) {
            throw new InvalidParameterException("name starts with '/'");
        }
        if (slashIndex == -1) {
            firstName = name;
            lastName = null;
        } else {
            firstName = name.substring(0, slashIndex);
            lastName = name.substring(slashIndex + 1);
        }
        Directory dir;
        if (!directories.containsKey(firstName)) {
            dir = new Directory(mpd, this, firstName);
            directories.put(dir.getName(), dir);
        } else {
            dir = directories.get(firstName);
        }

        if (lastName != null) {
            return dir.makeDirectory(lastName);
        }
        return dir;
    }

    /**
     * Adds a file, creating path directories (mkdir -p).
     * @param file file to be added
     */
    public void addFile(Music file) {
        Directory dir = this;
        if (getFullpath().compareTo(file.getPath()) == 0) {
            file.setParent(this);
            files.put(file.getFilename(), file);
        } else {
            dir = makeDirectory(file.getPath());
            dir.addFile(file);
        }
    }

    /**
     * Check if a given directory existis as a subdir.
     * @param name subdir name.
     * @return true if subdir exists, false if not.
     */
    public boolean containsDir(String name) {
        return directories.containsKey(name);
    }

    /**
     * Retrieves a subdirectory.
     * @param name name of subdirectory to retrieve.
     * @return a subdirectory.
     */
    public Directory getDirectory(String name) {
        return directories.get(name);
    }

    /**
     * Retrieves a textual representation of this object.
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

    /**
     * Retrieves parent directory.
     * @return parent directory.
     */
    public Directory getParent() {
        return parent;
    }

    /**
     * Retrieves directory's fullpathname. (does not starts with /)
     * @return fullpathname of this directory.
     */
    public String getFullpath() {
        if (getParent() != null && getParent().getParent() != null) {
            return getParent().getFullpath() + "/" + getName();
        } else {
            return getName();
        }
    }
}