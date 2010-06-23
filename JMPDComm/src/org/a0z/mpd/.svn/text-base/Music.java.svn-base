package org.a0z.mpd;

import java.util.Iterator;
import java.util.List;

/**
 * Class representing a file/music entry in playlist.
 * @author Felipe Gustavo de Almeida
 * @version $Id: Music.java 2940 2005-02-09 02:31:48Z galmeida $
 */
public class Music implements FilesystemTreeEntry{
    /**
     * Used to indicate this <code>Music</code> is a stream.
     */
    public static final int STREAM = 1;

    /**
     * Used to indicate this <code>Music</code> is a file.
     */
    public static final int FILE = 0;

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int TWO_DIGIT = 10;

    private String album;

    private String artist;

    private String fullpath;
    
    private String genre;
    
    private String date;

    private long time;

    private String title;

    private String totalTracks;

    private String track;

    private Directory parent;

    private int songId = -1;

    private int pos = -1;

    private String name;
    /**
     * Constructs a new Music.
     * @param response server response.
     */
    Music(List response) {
        Iterator it = response.iterator();
        while (it.hasNext()) {
            String line = (String) it.next();
            if (line.startsWith("file:")) {
                this.fullpath = line.substring("file: ".length());
            } else if (line.startsWith("Artist:")) {
                this.artist = line.substring("Artist: ".length());
            } else if (line.startsWith("Album:")) {
                this.album = line.substring("Album: ".length());
            } else if (line.startsWith("Title:")) {
                this.title = line.substring("Title: ".length());
            } else if (line.startsWith("Name:")) {
                this.name = line.substring("Name: ".length());
            } else if (line.startsWith("Track:")) {
                String[] aux = line.substring("Track: ".length()).split("/");
                this.track = aux[0];
                if (aux.length > 1) {
                    this.totalTracks = aux[1];
                }
            } else if (line.startsWith("Time:")) {
                this.time = Long.parseLong(line.substring("Time: ".length()));
            } else if (line.startsWith("Id:")) {
                this.songId = Integer.parseInt(line.substring("Id: ".length()));
            } else if (line.startsWith("Pos:")) {
                this.pos = Integer.parseInt(line.substring("Pos: ".length()));
            } else if (line.startsWith("Date:")) {
            	this.date = line.substring("Date: ".length());
            } else if (line.startsWith("Genre:")) {
            	this.genre = line.substring("Genre: ".length());
            } else if (line.startsWith("Soundtrack:")) {
            	line.substring("Soundtrack: ".length()); //TODO: Soundtrack
            } else if (line.startsWith("Composer:")) {
            	line.substring("Composer: ".length()); //TODO: Composer
            } else if (line.startsWith("Disc:")) {
            	line.substring("Disc: ".length()); //TODO: Composer
            } else {
            	// Ignore this case, there could be some id3 tags which are not common and therefor not implemented here...
                //(new InvalidResponseException("unknown response: " + line)).printStackTrace();
            }
        }
        this.parent = null;
    }

    Music(String fullpath) {
        if (fullpath.startsWith("/")) {
            throw new InvalidParameterException("invalid file path, starts with /");
        }
        this.fullpath = fullpath;
        this.parent = null;
    }

    /**
     * Retrieves album name.
     * @return album name.
     */
    public String getAlbum() {
        return album;
    }

    /**
     * Retrieves artist name.
     * @return artist name.
     */
    public String getArtist() {
        return artist;
    }

    /**
     * TODO test this for streams
     * Retrieves filename.
     * @return filename.
     */
    public String getFilename() {
        int pos = fullpath.lastIndexOf("/");
        if (pos == -1) {
            return fullpath;
        } else {
            return fullpath.substring(pos + 1);
        }
    }

    /**
     * Retrives full path name.
     * @return full path name.
     */
    public String getFullpath() {
        return fullpath;
    }

    /**
     * Retrieves current song playlist id.
     * @return current song playlist id.
     */
    public int getSongId() {
        return songId;
    }

    /**
     * Retrieves current song stopped on or playing, playlist song number.
     * @return current song stopped on or playing, playlist song number.
     */
    public int getPos() {
        return pos;
    }

    /**
     * Retrieves path of music file. (does not start or end with /)
     * @return path of music file.
     */
    public String getPath() {
        String result;
        if (getFullpath().length() > getFilename().length()) {
            result = fullpath.substring(0, getFullpath().length() - getFilename().length() - 1);
        } else {
            result = "";
        }
        return result;
    }

    /**
     * Retrieves playing time.
     * @return playing time.
     */
    public long getTime() {
        return time;
    }

    /**
     * Retrieves title.
     * @return title.
     */
    public String getTitle() {
        if (title == null || title.length() == 0) {
            return getFilename();
        } else {
            return title;
        }
    }

    /**
     * Retrieves stream's name.
     * @return stream's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrives total number of tracks from this music's album when available. This can contain letters!
     * @return total number of tracks from this music's album when available.
     */
    public String getTotalTracks() {
        return totalTracks;
    }

    /**
     * Retrieves track number. This can contain letters!
     * @return track number.
     */
    public String getTrack() {
        return track;
    }

    /**
     * Retrieves date as string (##:##).
     * @return date as string.
     */
    public String getFormatedTime() {
        String result = "00:00";
        if (time > 0) {
            int secs = (int) (time % SECONDS_PER_MINUTE);
            int mins = (int) (time / SECONDS_PER_MINUTE);
            result = (mins < TWO_DIGIT ? "0" + mins : Integer.toString(mins)) + ":" + (secs < TWO_DIGIT ? "0" + secs : Integer.toString(secs));
        }
        return result;
    }

    /**
     * Retrieves a string representation of the object.
     * @return a string representation of the object.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return track + " - " + album + " - " + artist + " - " + title + " (" + fullpath + ")";
    }

    /**
     * Defines album name.
     * @param string album name.
     */
    public void setAlbum(String string) {
        album = string;
    }

    /**
     * Defines artist name.
     * @param string artist name.
     */
    public void setArtist(String string) {
        artist = string;
    }

    /**
     * Defines playing time.
     * @param l playing time.
     */
    public void setTime(long l) {
        time = l;
    }

    /**
     * Defines title.
     * @param string title.
     */
    public void setTitle(String string) {
        title = string;
    }

    /**
     * Defines total number of tracks from this music's album when available.
     * @param i total number of tracks from this music's album when available.
     */
    public void setTotalTracks(String str) {
        totalTracks = str;
    }

    /**
     * Defines track number.
     * @param i track number.
     */
    public void setTrack(String str) {
        track = str;
    }

    /**
     * Copies, artist, album, title, time, totalTracks and track from another <code>music</code>.
     * @param music <code>Music</code> to copy data from.
     */
    void update(Music music) {
        this.setArtist(music.getArtist());
        this.setAlbum(music.getAlbum());
        this.setTitle(music.getTitle());
        this.setTime(music.getTime());
        this.setTotalTracks(music.getTotalTracks());
        this.setTrack(music.getTrack());
    }

    /**
     * Retrieves file's parent dir.
     * @return file's parent dir
     */
    public Directory getParent() {
        return parent;
    }

    /**
     * Defines file's parent dir.
     * @param directory file's parent dir
     */
    public void setParent(Directory directory) {
        parent = directory;
    }

    /**
     * Returns <code>Music.FILE</code> if this <code>Music</code> is a file and <code>Music.STREAM</code> if it's a stream.
     * @return <code>Music.FILE</code> if this <code>Music</code> is a file and <code>Music.STREAM</code> if it's a stream.
     * @see Music#FILE
     * @see Music#STREAM
     */
    public int getMedia() {
        if (this.getFullpath().indexOf("://") == -1) {
            return FILE;
        } else {
            return STREAM;
        }
    }
}