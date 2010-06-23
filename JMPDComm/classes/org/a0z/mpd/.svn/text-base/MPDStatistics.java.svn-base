package org.a0z.mpd;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Class representing MPD Server statistics.
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDStatistics.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public class MPDStatistics {

    private static final int MILLI_TO_SEC = 1000;

    private long artists = -1;

    private long albums = -1;

    private long songs = -1;

    private long uptime = -1;

    private Date dbUpdate = null;

    private long playtime = -1;

    private long dbPlaytime = -1;

    private MPDStatistics() {
    }

    MPDStatistics(List response) {

        Iterator it = response.iterator();
        while (it.hasNext()) {
            String line = (String) it.next();
            if (line.startsWith("artists:")) {
                this.artists = Long.parseLong(line.substring("artists: ".length()));
            } else if (line.startsWith("albums:")) {
                this.albums = Long.parseLong(line.substring("albums: ".length()));
            } else if (line.startsWith("songs:")) {
                this.songs = Long.parseLong(line.substring("songs: ".length()));
            } else if (line.startsWith("uptime:")) {
                this.uptime = Long.parseLong(line.substring("uptime: ".length()));
            } else if (line.startsWith("db_update:")) {
                this.dbUpdate = new Date(Long.parseLong(line.substring("db_update: ".length())) * MILLI_TO_SEC);
            } else if (line.startsWith("playtime:")) {
                this.playtime = Long.parseLong(line.substring("playtime: ".length()));
            } else if (line.startsWith("db_playtime:")) {
                this.dbPlaytime = Long.parseLong(line.substring("db_playtime: ".length()));
            }
        }
    }

    /**
     * Retrieves total number of albums.
     * @return total number of albums.
     */
    public long getAlbums() {
        return albums;
    }

    /**
     * Retrieves total number of artists.
     * @return total number of artists.
     */
    public long getArtists() {
        return artists;
    }

    /**
     * Retrives last database update time.
     * @return last database update time.
     */
    public Date getDbUpdate() {
        return dbUpdate;
    }

    /**
     * Retrieves total number of songs.
     * @return total number of songs.
     */
    public long getSongs() {
        return songs;
    }

    /**
     * Retrieves server uptime.
     * @return server uptime.
     */
    public long getUptime() {
        return uptime;
    }

    /**
     * Retrieves time mpd has been playing music.
     * @return how long mpd has been actually playing music in seconds.
     */
    public long getPlaytime() {
        return playtime;
    }

    /**
     * Retrieves the amount of time mpd would take to play every song in the db
     * once.
     * @return Retrieves the amount of time (in seconds) mpd would take to play
     * every song in the db once.
     */
    public long getDbPlaytime() {
        return dbPlaytime;
    }

    /**
     * Retrieves a string representation of the object.
     * @return a string representation of the object.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "artists: " + this.artists + " albums: " + this.albums + " songs: " + this.songs + " uptime: " + this.uptime + " last db update: " + this.dbUpdate;
    }

}