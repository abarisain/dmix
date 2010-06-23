package org.a0z.mpd;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.a0z.mpd.url.UnsupportedMimeTypeException;

/**
 * MPD Playlist controller.
 * @author Felipe Gustavo de Almeida, Stefan Agner
 * @version $Id: MPDPlaylist.java 2716 2004-11-20 17:37:20Z galmeida $
 */
public class MPDPlaylist {
    private static final String MPD_CMD_PLAYLIST_ADD = "add";

    private static final String MPD_CMD_PLAYLIST_CLEAR = "clear";

    private static final String MPD_CMD_PLAYLIST_DELETE = "rm";

    private static final String MPD_CMD_PLAYLIST_LIST = "playlistid";

    private static final String MPD_CMD_PLAYLIST_CHANGES = "plchanges";

    private static final String MPD_CMD_PLAYLIST_LOAD = "load";

    private static final String MPD_CMD_PLAYLIST_MOVE = "move";

    private static final String MPD_CMD_PLAYLIST_MOVE_ID = "moveid";

    private static final String MPD_CMD_PLAYLIST_REMOVE = "delete";

    private static final String MPD_CMD_PLAYLIST_REMOVE_ID = "deleteid";

    private static final String MPD_CMD_PLAYLIST_SAVE = "save";

    private static final String MPD_CMD_PLAYLIST_SHUFFLE = "shuffle";

    private static final String MPD_CMD_PLAYLIST_SWAP = "swap";

    private static final String MPD_CMD_PLAYLIST_SWAP_ID = "swapid";

    private MusicList list;

    private int lastPlaylistVersion = -1;

    private boolean firstRefreash = true;

    private MPD mpd;

    /**
     * Creates a new playlist.
     */
    MPDPlaylist(MPD mpd) {
        this.list = new MusicList();
        this.mpd = mpd;
    }

    /**
     * Adds a <code>Collection</code> of <code>Music</code> to playlist.
     * @param c <code>Collection</code> of <code>Music</code> to be added to playlist.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     */
    public void add(Collection c) throws MPDServerException {
        Iterator it = c.iterator();

        while (it.hasNext()) {
            Music music = (Music) it.next();
            String[] args = new String[1];
            args[0] = music.getFullpath();
            this.mpd.getMpdConnection().queueCommand(MPD_CMD_PLAYLIST_ADD, args);
        }
        this.mpd.getMpdConnection().sendCommandQueue();
        this.refresh();
    }

    /**
     * Adds a music to playlist.
     * @param music music to be added.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void add(Music music) throws MPDServerException {
        String[] args = new String[1];
        args[0] = music.getFullpath();
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_ADD, args);
        this.refresh();
    }
    
    /**
     * Adds a music to playlist recursivly.
     * @param music music to be added.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void add(Directory music) throws MPDServerException {
        String[] args = new String[1];
        args[0] = music.getFullpath();
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_ADD, args);
        this.refresh();
    }

    /**
     * Adds a stream to playlist.
     * @param url streams's url
     * @throws MPDServerException on server error.
     * @throws MPDClientException on client error.
     */
    public void add(URL url) throws MPDServerException, MPDClientException {
        List urlContent;
        try {
            urlContent = (List) url.openConnection().getContent();
        } catch (UnsupportedMimeTypeException e) {
            //if mimetype is not supported by JMPDComm try to pass it directly to MPD.
            urlContent = new LinkedList();
            urlContent.add(url.toString());
        } catch (IOException e) {
            throw new MPDClientException("Unable to fetch " + url.toString(), e);
        }
        if (urlContent.size() > 0) {
            String[] args = new String[1];
            Iterator it = urlContent.iterator();
            while (it.hasNext()) {
                args[0] = (String) it.next();
                this.mpd.getMpdConnection().queueCommand(MPD_CMD_PLAYLIST_ADD, args);
            }
            this.mpd.getMpdConnection().sendCommandQueue();
            this.refresh();
        }
    }

    /**
     * Clears playlist content.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void clear() throws MPDServerException {
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_CLEAR);
        list.clear();
    }

    /**
     * Retrieves music at position idx in playlist. Operates on local copy of playlist, may not reflect server's current
     * playlist. You may call refresh() before calling get().
     * @param idx position.
     * @return music at position idx.
     */
    public Music getMusic(int idx) {
        return (Music) list.getByIndex(idx);
    }

    /**
     * Load playlist file.
     * @param file playlist filename without .m3u extension.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void load(String file) throws MPDServerException {
        String[] args = new String[1];
        args[0] = file;
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_LOAD, args);
    }

    /**
     * Moves song at position <code>from</code> to position <code>to</code>.
     * @param from current position of the song to be moved.
     * @param to target position of the song to be moved.
     * @throws MPDServerException if an error occur while contacting server.
     * @deprecated use <code>move</code>
     * @see #move(int, int)
     */
    public void moveByPosition(int from, int to) throws MPDServerException {
        String[] args = new String[2];
        args[0] = Integer.toString(from);
        args[0] = Integer.toString(to);
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_MOVE, args);
        this.refresh();
    }

    /**
     * Moves song with specified id to position <code>to</code>.
     * @param songId Id of the song to be moved.
     * @param to target position of the song to be moved.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void move(int songId, int to) throws MPDServerException {
        String[] args = new String[2];
        args[0] = Integer.toString(songId);
        args[0] = Integer.toString(to);
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_MOVE_ID, args);
        this.refresh();
    }

    /**
     * Reload playlist content. <code>refresh</code> has better performance and is more server friendly, use it
     * whenever possible.
     * @see #refresh(int)
     * @throws MPDServerException if an error occur while contacting server.
     * @return current playlist version.
     */
    public int refresh() throws MPDServerException {
        if (firstRefreash) {
            this.lastPlaylistVersion = this.reload();
            firstRefreash = false;
        } else {
            this.lastPlaylistVersion = this.refresh(lastPlaylistVersion);
        }
        return this.lastPlaylistVersion;
    }

    /**
     * Reload playlist content. <code>refresh</code> has better performance and is more server friendly, use it
     * whenever possible.
     * @see #refresh(int)
     * @throws MPDServerException if an error occur while contacting server.
     * @return current playlist version.
     */
    private int reload() throws MPDServerException {
    	LinkedList<Music> playlist = new LinkedList<Music>();
    	LinkedList<String> file = new LinkedList<String>();
        //TODO should be atomic
        MPDStatus status = this.mpd.getStatus();
        List<String> response = this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_LIST);
        int index = 0;
        for (String line : response) {
            if (line.startsWith("file: ")) {
                if (file.size() != 0) {
                    playlist.add(new Music(file));
                    index++;
                    file.clear();
                }
            }
            file.add(line);
        }

        if (file.size() != 0) {
            playlist.add(new Music(file));
        }

        this.list.clear();
        this.list.addAll(playlist);
        return status.getPlaylistVersion();
    }

    /**
     * Do incremental update of playlist contents.
     * @param playlistVersion last read playlist version
     * @throws MPDServerException if an error occur while contacting server.
     * @return current playlist version.
     */
    public int refresh(int playlistVersion) throws MPDServerException {
        MusicList playlist = new MusicList(list);
        List file = new LinkedList();
        String[] args = new String[1];
        args[0] = Integer.toString(playlistVersion);
        // TODO should be atomic
        MPDStatus status = this.mpd.getStatus();
        List response = this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_CHANGES, args);
        Iterator it = response.iterator();
        while (it.hasNext()) {
            String line = (String) it.next();

            if (line.startsWith("file: ")) {
                if (file.size() != 0) {
                    playlist.add(new Music(file));
                    file.clear();
                }
            }
            file.add(line);
        }

        if (file.size() != 0) {
            playlist.add(new Music(file));
        }

        this.list.clear();
        this.list.addAll(playlist.subList(0, status.getPlaylistLength()));
        return status.getPlaylistVersion();
    }

    /**
     * Removes playlist file.
     * @param file playlist filename without .m3u extension.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void removePlaylist(String file) throws MPDServerException {
        String[] args = new String[1];
        args[0] = file;
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_DELETE, args);
    }

    /**
     * Remove playlist entry at position idx.
     * @param position position of the entry to be removed.
     * @throws MPDServerException if an error occur while contacting server.
     * @deprecated use <code>removeSong</code>.
     * @see #removeSong(int)
     */
    public void removeSongByPosition(int position) throws MPDServerException {
        String[] args = new String[1];
        args[0] = Integer.toString(position);
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_REMOVE, args);
        list.removeByPosition(position);
    }

    /**
     * Remove playlist entry at position idx.
     * @param songId id of the entry to be removed.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void removeSong(int songId) throws MPDServerException {
        String[] args = new String[1];
        args[0] = Integer.toString(songId);
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_REMOVE_ID, args);
        list.removeBySongId(songId);
    }

    /**
     * Removes entries from playlist.
     * @param songs entries positions.
     * @throws MPDServerException if an error occur while contacting server.
     * @deprecated use <code>removeSongs</code>
     * @see #removeSongs(int[])
     */
    public void removeSongsByPosition(int[] songs) throws MPDServerException {
        java.util.Arrays.sort(songs);
        for (int i = songs.length - 1; i >= 0; i--) {
            String[] args = new String[1];
            args[0] = Integer.toString(songs[i]);
            this.mpd.getMpdConnection().queueCommand(MPD_CMD_PLAYLIST_REMOVE, args);
        }
        this.mpd.getMpdConnection().sendCommandQueue();

        for (int i = songs.length - 1; i >= 0; i--) {
            list.removeByPosition(songs[i]);
        }
    }

    /**
     * Removes entries from playlist.
     * @param songIds entries Ids.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void removeSongs(int[] songIds) throws MPDServerException {

        for (int i = songIds.length - 1; i >= 0; i--) {
            String[] args = new String[1];
            args[0] = Integer.toString(songIds[i]);
            this.mpd.getMpdConnection().queueCommand(MPD_CMD_PLAYLIST_REMOVE_ID, args);
        }
        this.mpd.getMpdConnection().sendCommandQueue();

        for (int i = songIds.length - 1; i >= 0; i--) {
            list.removeBySongId(songIds[i]);
        }
    }

    /**
     * Save playlist file.
     * @param file playlist filename without .m3u extension.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void save(String file) throws MPDServerException {
        String[] args = new String[1];
        args[0] = file;
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SAVE, args);
    }

    /**
     * Shuffles playlist content.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void shuffle() throws MPDServerException {
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SHUFFLE);
    }

    /**
     * Retrieves playlist size. Operates on local copy of playlist, may not reflect server's current playlist. You may
     * call refresh() before calling size().
     * @return playlist size.
     */
    public int size() {
        return list.size();
    }

    /**
     * Swap positions of song1 and song2.
     * @param song1 position of song1 in playlist.
     * @param song2 position of song2 in playlist
     * @throws MPDServerException if an error occur while contacting server.
     * @deprecated use <code>swap</code>.
     * @see #swap(int, int)
     */
    public void swapByPosition(int song1, int song2) throws MPDServerException {
        String[] args = new String[2];
        args[0] = Integer.toString(song1);
        args[0] = Integer.toString(song2);
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SWAP, args);
        this.refresh();
    }

    /**
     * Swap positions of song1 and song2.
     * @param song1Id id of song1 in playlist.
     * @param song2Id id of song2 in playlist.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void swap(int song1Id, int song2Id) throws MPDServerException {
        String[] args = new String[2];
        args[0] = Integer.toString(song1Id);
        args[1] = Integer.toString(song2Id);
        this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SWAP_ID, args);
        this.refresh();
    }

    /**
     * Retrieves all songs as an <code>List</code> of <code>Music</code>.
     * @return all songs as an <code>List</code> of <code>Music</code>.
     * @see Music
     */
    public List<Music> getMusics() {
        return this.list.getMusics();
    }

    /**
     * Retrieves a string representation of the object.
     * @return a string representation of the object.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Music m : list.getMusics()) {
            sb.append(m.toString() + "\n");
        }
        return sb.toString();
    }

}