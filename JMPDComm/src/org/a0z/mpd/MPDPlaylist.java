package org.a0z.mpd;

import android.util.Log;
import org.a0z.mpd.event.AbstractStatusChangeListener;
import org.a0z.mpd.exception.MPDClientException;
import org.a0z.mpd.exception.MPDServerException;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MPD Playlist controller.
 *
 * @version $Id: MPDPlaylist.java 2716 2004-11-20 17:37:20Z galmeida $
 */
public class MPDPlaylist extends AbstractStatusChangeListener {
	public static final String MPD_CMD_PLAYLIST_ADD = "add";
	public static final String MPD_CMD_PLAYLIST_CLEAR = "clear";
	public static final String MPD_CMD_PLAYLIST_DELETE = "rm";
	public static final String MPD_CMD_PLAYLIST_LIST = "playlistid";
	public static final String MPD_CMD_PLAYLIST_CHANGES = "plchanges";
	public static final String MPD_CMD_PLAYLIST_LOAD = "load";
	public static final String MPD_CMD_PLAYLIST_MOVE = "move";
	public static final String MPD_CMD_PLAYLIST_MOVE_ID = "moveid";
	public static final String MPD_CMD_PLAYLIST_REMOVE = "delete";
	public static final String MPD_CMD_PLAYLIST_REMOVE_ID = "deleteid";
	public static final String MPD_CMD_PLAYLIST_SAVE = "save";
	public static final String MPD_CMD_PLAYLIST_SHUFFLE = "shuffle";
	public static final String MPD_CMD_PLAYLIST_SWAP = "swap";
	public static final String MPD_CMD_PLAYLIST_SWAP_ID = "swapid";

	private MPD mpd;
	private MusicList list;
	private int lastPlaylistVersion = -1;
	private boolean firstRefreash = true;
    private static final boolean DEBUG = false;

	/**
	 * Creates a new playlist.
	 */
	MPDPlaylist(MPD mpd) {
		this.mpd = mpd;

		this.list = new MusicList();
	}

	/**
	 * Adds a <code>Collection</code> of <code>Music</code> to playlist.
	 *
	 * @param c
	 *           <code>Collection</code> of <code>Music</code> to be added to playlist.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see Music
	 */
	public void addAll(Collection<Music> c) throws MPDServerException {
		for (Music m : c)
			this.mpd.getMpdConnection().queueCommand(MPD_CMD_PLAYLIST_ADD, m.getFullpath());

		this.mpd.getMpdConnection().sendCommandQueue();
		this.refresh();
	}

	/**
	 * Adds a music to playlist.
	 *
	 * @param entry
	 *           music/directory/playlist to be added.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void add(FilesystemTreeEntry entry) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_ADD, entry.getFullpath());
		this.refresh();
	}

	/**
	 * Adds a stream to playlist.
	 *
	 * @param url streams's URL
	 * @throws MPDServerException
	 * @throws MPDClientException.
	 */
	public void add(URL url) throws MPDServerException, MPDClientException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_ADD, url.toString());
		this.refresh();
	}

	/**
	 * Clears playlist content.
	 *
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void clear() throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_CLEAR);
		list.clear();
	}

	/**
	 * Retrieves all songs as an <code>List</code> of <code>Music</code>.
	 *
	 * @return all songs as an <code>List</code> of <code>Music</code>.
	 * @see Music
	 */
	public List<Music> getMusicList() {
		return this.list.getMusic();
	}

	/**
	 * Retrieves music at position index in playlist. Operates on local copy of playlist, may not reflect server's current playlist.
	 *
	 * @param index
	 *           position.
	 * @return music at position index.
	 */
	public Music getByIndex(int index) {
		return list.getByIndex(index);
	}

	/**
	 * Load playlist file.
	 *
	 * @param file
	 *           playlist filename without .m3u extension.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void load(String file) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_LOAD, file);
		this.refresh();
	}

	/**
	 * Moves song at position <code>from</code> to position <code>to</code>.
	 *
	 * @param from
	 *           current position of the song to be moved.
	 * @param to
	 *           target position of the song to be moved.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see #move(int, int)
	 */
	public void moveByPosition(int from, int to) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_MOVE, Integer.toString(from), Integer.toString(to));
		this.refresh();
	}

	/**
	 * Moves song with specified id to position <code>to</code>.
	 *
	 * @param songId
	 *           Id of the song to be moved.
	 * @param to
	 *           target position of the song to be moved.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void move(int songId, int to) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_MOVE_ID, Integer.toString(songId), Integer.toString(to));
		this.refresh();
	}

	/*
	 * React to playlist change on server and refresh ourself
	 * @see org.a0z.mpd.event.AbstractStatusChangeListener#playlistChanged(org.a0z.mpd.MPDStatus, int)
	 */
	@Override
	public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
		try {
			refresh(oldPlaylistVersion);
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reload playlist content. <code>refresh</code> has better performance and is more server friendly, use it whenever possible.
	 *
	 * @see #refresh(int)
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @return current playlist version.
	 */
	private int refresh() throws MPDServerException {
		if (firstRefreash) {
			// TODO should be atomic
			MPDStatus status = this.mpd.getStatus();
			List<String> response = this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_LIST);
			List<Music> playlist = Music.getMusicFromList(response, false);

			list.clear();
			list.addAll(playlist);

			lastPlaylistVersion = status.getPlaylistVersion();
			firstRefreash = false;
		} else {
			this.lastPlaylistVersion = this.refresh(lastPlaylistVersion);
		}
		return this.lastPlaylistVersion;
	}

	/**
	 * Do incremental update of playlist contents.
	 *
	 * @param playlistVersion
	 *           last read playlist version
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @return current playlist version.
	 */
	private int refresh(int playlistVersion) throws MPDServerException {
		// TODO should be atomic
		MPDStatus status = this.mpd.getStatus();
		List<String> response = this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_CHANGES, Integer.toString(playlistVersion));
		List<Music> changes = Music.getMusicFromList(response, false);

		int newLength = status.getPlaylistLength();
		int oldLength = this.list.size();
		List<Music> newPlaylist = new ArrayList<Music>(newLength+1);

		newPlaylist.addAll(this.list.subList(0 , newLength < oldLength ? newLength : oldLength));

		for(int i = newLength - oldLength; i > 0; i--)
			newPlaylist.add(null);

		for( Music song : changes ) {
            if (newPlaylist.size() > song.getPos() && song.getPos() > -1) {
			newPlaylist.set(song.getPos(), song);
            }
		}

		this.list.clear();
		this.list.addAll(newPlaylist);

		return status.getPlaylistVersion();
	}

	/**
	 * Remove playlist entry at position index.
	 *
	 * @param position
	 *           position of the entry to be removed.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see #removeId(int)
	 */
	public void removeByIndex(int position) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_REMOVE, Integer.toString(position));
		list.removeByIndex(position);
	}

	/**
	 * Removes entries from playlist.
	 *
	 * @param songs
	 *           entries positions.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see #removeById(int[])
	 */
	public void removeByIndex(int[] songs) throws MPDServerException {
		java.util.Arrays.sort(songs);

		for (int i = songs.length - 1; i >= 0; i--)
			this.mpd.getMpdConnection().queueCommand(MPD_CMD_PLAYLIST_REMOVE, Integer.toString(songs[i]));
		this.mpd.getMpdConnection().sendCommandQueue();

		for (int i = songs.length - 1; i >= 0; i--)
			list.removeByIndex(songs[i]);
	}

	/**
	 * Remove playlist entry with ID songId
	 *
	 * @param songId
	 *           id of the entry to be removed.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void removeById(int songId) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_REMOVE_ID, Integer.toString(songId));
		list.removeById(songId);
	}

	/**
	 * Removes entries from playlist.
	 *
	 * @param songIds
	 *           entries IDs.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void removeById(int[] songIds) throws MPDServerException {
		for (int id : songIds)
			this.mpd.getMpdConnection().queueCommand(MPD_CMD_PLAYLIST_REMOVE_ID, Integer.toString(id));
		this.mpd.getMpdConnection().sendCommandQueue();

		for (int id : songIds)
			list.removeById(id);
	}

	/**
	 * Removes album of given ID from playlist.
	 *
	 * @param songs
	 *           entries positions.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see #removeById(int[])
	 */
	public void removeAlbumById(int songId) throws MPDServerException {
            List<Music> songs = getMusicList();
            // Better way to get artist of given songId?
            String artist = "";
            String album = "";
            boolean usingAlbumArtist = true;
            for (Music song : songs)
                if (song.getSongId() == songId) {
                    artist = song.getAlbumArtist();
                    if (artist == null || artist.equals("")) {
                        usingAlbumArtist = false;
                        artist = song.getArtist();
                    }
                    album = song.getAlbum();
                    break;
                }
            if (artist == null || album == null)
                return;
            if (DEBUG)
                Log.d("MPD", "Remove album "+album + " of "+artist);

            // Have artist & album, remove matching:
            int num=0;
            for (Music song : songs)
                if (album.equals(song.getAlbum())) {
                    if (usingAlbumArtist && artist.equals(song.getAlbumArtist()) ||
                        !usingAlbumArtist && artist.equals(song.getArtist())) {
                        int id = song.getSongId();
                        this.mpd.getMpdConnection().queueCommand(MPD_CMD_PLAYLIST_REMOVE_ID, Integer.toString(id));
                        list.removeById(id);
                        num++;
                    }
                }
            if (DEBUG)
                Log.d("MPD", "Removed "+num + " songs");
            this.mpd.getMpdConnection().sendCommandQueue();
	}

	/**
	 * Removes playlist file.
	 *
	 * @param file
	 *           playlist filename without .m3u extension.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void removePlaylist(String file) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_DELETE, file);
	}

	/**
	 * Save playlist file.
	 *
	 * @param file
	 *           playlist filename without .m3u extension.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void savePlaylist(String file) throws MPDServerException {
		// If the playlist already exists, save will fail. So, just remove it first!
		try {
			this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_DELETE, file);
		} catch (MPDServerException e) {
			// Guess the file did not exist???
		}
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SAVE, file);
	}

	/**
	 * Shuffles playlist content.
	 *
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void shuffle() throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SHUFFLE);
	}

	/**
	 * Retrieves playlist size. Operates on local copy of playlist, may not reflect server's current playlist. You may call refresh() before
	 * calling size().
	 *
	 * @return playlist size.
	 */
	public int size() {
		return list.size();
	}

	/**
	 * Swap positions of song1 and song2.
	 *
	 * @param song1
	 *           position of song1 in playlist.
	 * @param song2
	 *           position of song2 in playlist
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see #swap(int, int)
	 */
	public void swapByPosition(int song1, int song2) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SWAP, Integer.toString(song1), Integer.toString(song2));
		this.refresh();
	}

	/**
	 * Swap positions of song1 and song2.
	 *
	 * @param song1Id
	 *           id of song1 in playlist.
	 * @param song2Id
	 *           id of song2 in playlist.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void swap(int song1Id, int song2Id) throws MPDServerException {
		this.mpd.getMpdConnection().sendCommand(MPD_CMD_PLAYLIST_SWAP_ID, Integer.toString(song1Id), Integer.toString(song2Id));
		this.refresh();
	}

	/**
	 * Retrieves a string representation of the object.
	 *
	 * @return a string representation of the object.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Music m : list.getMusic()) {
			sb.append(m.toString() + "\n");
		}
		return sb.toString();
	}

}