package org.a0z.mpd;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Context;

/**
 * MPD Server controller.
 * 
 * @version $Id: MPD.java 2716 2004-11-20 17:37:20Z galmeida $
 */
public class MPD {
	
	private MPDConnection mpdConnection;
	private MPDConnection mpdIdleConnection;
	private MPDStatus mpdStatus;
	private MPDPlaylist playlist;
	private Directory rootDirectory;

	static private boolean useAlbumArtist = false;
	static private boolean sortByTrackNumber = true;
	static private boolean sortAlbumsByYear = false;
	static private boolean showArtistAlbumCount = false;
	static private boolean showAlbumTrackCount = true;

	static private Context applicationContext = null;

	static public Context getApplicationContext() {
		return applicationContext;
	}

	static public void setApplicationContext(Context context) {
		applicationContext = context;
	}

    static public boolean useAlbumArtist() {
            return useAlbumArtist;
    }

    static public boolean sortAlbumsByYear() {
            return sortAlbumsByYear;
    }

    static public boolean sortByTrackNumber() {
            return sortByTrackNumber;
    }

    static public boolean showArtistAlbumCount() {
            return showArtistAlbumCount;
    }

    static public boolean showAlbumTrackCount() {
            return showAlbumTrackCount;
    }

    static public void setUseAlbumArtist(boolean v) {
            useAlbumArtist=v;
    }

    static public void setSortByTrackNumber(boolean v) {
            sortByTrackNumber=v;
    }

    static public void setSortAlbumsByYear(boolean v) {
            sortAlbumsByYear=v;
    }

    static public void setShowArtistAlbumCount(boolean v) {
            showArtistAlbumCount=v;
    }

    static public void setShowAlbumTrackCount(boolean v) {
            showAlbumTrackCount=v;
    }

	/**
	 * Constructs a new MPD server controller without connection.
	 */
	public MPD() {
		this.playlist = new MPDPlaylist(this);
		this.mpdStatus = new MPDStatus();
		this.rootDirectory = Directory.makeRootDirectory(this);
	}

	/**
	 * Constructs a new MPD server controller.
	 * 
	 * @param server
	 *           server address or host name
	 * @param port
	 *           server port
	 * @throws MPDServerException
	 *            if an error occur while contacting server
	 * @throws UnknownHostException 
	 */
	public MPD(String server, int port) throws MPDServerException, UnknownHostException {
		this();
		connect(server, port);
	}
	
	/**
	 * Constructs a new MPD server controller.
	 * 
	 * @param server
	 *           server address or host name
	 * @param port
	 *           server port
	 * @throws MPDServerException
	 *            if an error occur while contacting server
	 */
	public MPD(InetAddress server, int port) throws MPDServerException {
		this();
		connect(server, port);
	}

	/**
	 * Retrieves <code>MPDConnection</code>.
	 * 
	 * @return <code>MPDConnection</code>.
	 */
	MPDConnection getMpdConnection() {
		return this.mpdConnection;
	}
	
	MPDConnection getMpdIdleConnection() {
		return this.mpdIdleConnection;
	}
	
    /**
     * Wait for server changes using "idle" command on the dedicated connection.
     * 
     * @return Data readed from the server.
     * @throws MPDServerException if an error occur while contacting server
     */
    public List<String> waitForChanges() throws MPDServerException {

        while (mpdIdleConnection != null && mpdIdleConnection.isConnected()) {
            List<String> data = mpdIdleConnection
                    .sendAsyncCommand(MPDCommand.MPD_CMD_IDLE);
            if (data.isEmpty()) {
                continue;
            }
            return data;
        }
        throw new MPDConnectionException("IDLE connection lost");
    }

	public boolean isMpdConnectionNull() {
		return (this.mpdConnection == null);
	}

	/**
	 * Increases or decreases volume by <code>modifier</code> amount.
	 * 
	 * @param modifier
	 *           volume adjustment
	 * @throws MPDServerException
	 *            if an error occur while contacting server
	 */
	public void adjustVolume(int modifier) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");

		// calculate final volume (clip value with [0, 100])
		int vol = getVolume() + modifier;
		vol = Math.max(MPDCommand.MIN_VOLUME, Math.min(MPDCommand.MAX_VOLUME, vol));

		mpdConnection.sendCommand(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
	}

	/**
	 * Clears error message.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void clearError() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_CLEARERROR);
	}

	/**
	 * Connects to a MPD server.
	 * 
	 * @param server
	 *           server address or host name
	 * @param port
	 *           server port
	 * @throws MPDServerException
	 *            if an error occur while contacting server
	 * @throws UnknownHostException 
	 */
	public final void connect(String server, int port) throws MPDServerException, UnknownHostException {
		InetAddress adress = InetAddress.getByName(server);
		connect(adress, port);
	}
	
	/**
	 * Connects to a MPD server.
	 * 
	 * @param server
	 *           server address or host name
	 * @param port
	 *           server port
	 */
	public final void connect(InetAddress server, int port) throws MPDServerException {
		this.mpdConnection = new MPDConnection(server, port);
		this.mpdIdleConnection = new MPDConnection(server, port, 1000);
	}

	/**
	 * Connects to a MPD server.
	 * 
	 * @param server
	 *           server address or host name and port (server:port)
	 * @throws MPDServerException
	 *            if an error occur while contacting server
	 * @throws UnknownHostException 
	 */
	public final void connect(String server) throws MPDServerException, UnknownHostException {
		int port = MPDCommand.DEFAULT_MPD_PORT;
		String host = null;
		if (server.indexOf(':') != -1) {
			host = server.substring(0, server.lastIndexOf(':'));
			port = Integer.parseInt(server.substring(server.lastIndexOf(':') + 1));
		} else {
			host = server;
		}
		connect(host, port);
	}

	/**
	 * Disconnects from server.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while closing connection
	 */
	public void disconnect() throws MPDServerException {
        MPDServerException ex = null;
        if (mpdConnection != null && mpdConnection.isConnected()) {
            try {
                mpdConnection.sendCommand(MPDCommand.MPD_CMD_CLOSE);
            } catch (MPDServerException e) {
                ex = e;
            }
        }
        if (mpdConnection != null && mpdConnection.isConnected()) {
            try {
                mpdConnection.disconnect();
            } catch (MPDServerException e) {
                ex = (ex != null) ? ex : e;// Always keep first non null
                                           // exception
            }
        }
        if (mpdIdleConnection != null && mpdIdleConnection.isConnected()) {
            try {
                mpdIdleConnection.disconnect();
            } catch (MPDServerException e) {
                ex = (ex != null) ? ex : e;// Always keep non null first
                                           // exception
            }
        }

        if (ex != null) {
            throw ex;
        }
	}

	/**
	 * Similar to <code>search</code>,<code>find</code> looks for exact matches in the MPD database.
	 * 
	 * @param type
	 *           type of search. Should be one of the following constants: MPD_FIND_ARTIST, MPD_FIND_ALBUM
	 * @param string
	 *           case-insensitive locator string. Anything that exactly matches <code>string</code> will be returned in the results.
	 * @return a Collection of <code>Music</code>
	 * @throws MPDServerException
	 *            if an error occur while contacting server
	 * @see org.a0z.mpd.Music
	 */
	public List<Music> find(String type, String string) throws MPDServerException {
		return genericSearch(MPDCommand.MPD_CMD_FIND, type, string);
	}
    public List<Music> find(String[] args) throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_FIND, args, true);
    }

	// Returns a pattern where all punctuation characters are escaped. 

	private List<Music> genericSearch(String searchCommand, String type, String strToFind) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		List<String> response = mpdConnection.sendCommand(searchCommand, type, strToFind);
		return Music.getMusicFromList(response, true);
	}

    private List<Music> genericSearch(String searchCommand, String args[], boolean sort) throws MPDServerException {
        if (!isConnected())
        	throw new MPDServerException("MPD Connection is not established");

		return Music.getMusicFromList(mpdConnection.sendCommand(searchCommand, args), sort);
    }

	/**
	 * Retrieves a database directory listing of the base of the database directory path.
	 * 
	 * @return a <code>Collection</code> of <code>Music</code> and <code>Directory</code> representing directory entries.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see Music
	 * @see Directory
	 */
	public List<FilesystemTreeEntry> getDir() throws MPDServerException {
		return getDir(null);
	}

	/**
	 * Retrieves a database directory listing of <code>path</code> directory.
	 * 
	 * @param path Directory to be listed.
	 * @return a <code>Collection</code> of <code>Music</code> and <code>Directory</code> representing directory entries.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see Music
	 * @see Directory
	 */
	public List<FilesystemTreeEntry> getDir(String path) throws MPDServerException {
		if(!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		List<String> resonse = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LSDIR, path);
		
		LinkedList<String> lineCache = new LinkedList<String>();
		LinkedList<FilesystemTreeEntry> result = new LinkedList<FilesystemTreeEntry>();
		for (String line : resonse) {
			// file-elements are the only ones using fileCache, therefore if something new begins and the cache contains data, its music
			if ((line.startsWith("file: ") || line.startsWith("directory: ") || line.startsWith("playlist: ")) && lineCache.size() > 0) {
				result.add(new Music(lineCache));
				lineCache.clear();
			}
			
			if (line.startsWith("playlist: ")) {
				line = line.substring("playlist: ".length());
				result.add(new PlaylistFile(line));
			} else if (line.startsWith("directory: ")) {
				line = line.substring("directory: ".length());
				result.add(rootDirectory.makeDirectory(line));
			} else if (line.startsWith("file: ")) {
				lineCache.add(line);
			}

		}
		if (lineCache.size() > 0) {
			result.add(new Music(lineCache));
		}

		return result;
	}

	/**
	 * Returns MPD server version.
	 * 
	 * @return MPD Server version.
	 */
	public String getMpdVersion() throws MPDServerException {
		if(!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		int[] version = mpdConnection.getMpdVersion();
				
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < version.length; i++) {
			sb.append(version[i]);
			if (i < (version.length - 1))
				sb.append(".");
		}
		return sb.toString();
	}

	/**
	 * Retrieves <code>playlist</code>.
	 * 
	 * @return playlist.
	 */
	public MPDPlaylist getPlaylist() {
		return this.playlist;
	}

	/**
	 * Retrieves statistics for the connected server.
	 * 
	 * @return statistics for the connected server.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public MPDStatistics getStatistics() throws MPDServerException {
		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_STATISTICS);
		return new MPDStatistics(response);
	}

	/**
	 * Retrieves status of the connected server.
	 * 
	 * @return status of the connected server.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public MPDStatus getStatus() throws MPDServerException {
		if(!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_STATUS);
		mpdStatus.updateStatus(response);
		return mpdStatus;
	}

	/**
	 * Retrieves current volume.
	 * 
	 * @return current volume.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public int getVolume() throws MPDServerException {
		return this.getStatus().getVolume();
	}

	/**
	 * Returns true when connected and false when not connected.
	 * 
	 * @return true when connected and false when not connected
	 */
	public boolean isConnected() {
		if (mpdConnection == null)
			return false;
		return mpdConnection.isConnected() ;
	}


	/**
	 * List all albums from database.
	 * 
	 * @return <code>Collection</code> with all album names from database.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public List<String> listAlbums() throws MPDServerException {
		return listAlbums(null, false, false);
	}

	/**
	 * List all albums from database.
	 * 
	 * @param useAlbumArtist
	 * 			 use AlbumArtist instead of Artist
	 * @return <code>Collection</code> with all album names from database.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public List<String> listAlbums(boolean useAlbumArtist) throws MPDServerException {
		return listAlbums(null, useAlbumArtist, false);
	}

	/**
	 * List all albums from a given artist, including an entry for songs with no album tag.
	 * 
	 * @param artist
	 *           artist to list albums
	 * @param useAlbumArtist
	 * 			 use AlbumArtist instead of Artist
	 * @return <code>Collection</code> with all album names from database.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public List<String> listAlbums(String artist, boolean useAlbumArtist) throws MPDServerException {
		return listAlbums(artist, useAlbumArtist, true);
	}

	/**
	 * List all albums from a given artist.
	 * 
	 * @param artist
	 *           artist to list albums
	 * @param useAlbumArtist
	 * 			 use AlbumArtist instead of Artist
	 * @param includeUnknownAlbum
	 * 			 include an entry for songs with no album tag
	 * @return <code>Collection</code> with all album names from the given artist present in database.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public List<String> listAlbums(String artist, boolean useAlbumArtist, boolean includeUnknownAlbum) throws MPDServerException {
		if(!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		boolean foundSongWithoutAlbum = false;

		List<String> response;
		if (useAlbumArtist)
		    response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM, MPDCommand.MPD_TAG_ALBUM_ARTIST, artist);
		else
		    response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM, artist);
		ArrayList<String> result = new ArrayList<String>();
		for (String line : response) {
			String name = line.substring("Album: ".length());
			if (name.length() > 0) {
				result.add(name);
			}else{
				foundSongWithoutAlbum = true;
			}
		}

		Collections.sort(result);
		
		// add a single blank entry to host all songs without an album set
		if((includeUnknownAlbum == true) && (foundSongWithoutAlbum == true)) {
			result.add("");
		}

		return result;
	}

	private Long[] getAlbumDetails(String artist, String album, boolean useAlbumArtistTag) throws MPDServerException {
		if (!isConnected()) {
			throw new MPDServerException("MPD Connection is not established");
		}
		Long[] result = new Long[3];
		result[0] = 0l;
		result[1] = 0l;
		result[2] = 0l;
		if (MPD.showAlbumTrackCount()) {
			String[] args = new String[4];
			args[0] = useAlbumArtistTag ? MPDCommand.MPD_TAG_ALBUM_ARTIST : MPDCommand.MPD_TAG_ARTIST;
			args[1] = artist;
			args[2] = MPDCommand.MPD_TAG_ALBUM;
			args[3] = album;
			List<String> list = mpdConnection.sendCommand("count", args);
			for (String line : list) {
				if (line.startsWith("songs: ")) {
					result[0] = Long.parseLong(line.substring("songs: ".length()));
				} else if (line.startsWith("playtime: ")) {
					result[1] = Long.parseLong(line.substring("playtime: ".length()));
				}
			}
		}
		if (MPD.sortAlbumsByYear()) {
			String[] args = new String[6];
			args[0] = useAlbumArtistTag ? MPDCommand.MPD_TAG_ALBUM_ARTIST : MPDCommand.MPD_TAG_ARTIST;
			args[1] = artist;
			args[2] = MPDCommand.MPD_TAG_ALBUM;
			args[3] = album;
			args[4] = "track";
			args[5] = "1";
			List<Music> songs = find(args);
			if (null==songs || songs.isEmpty()) {
				args[5] = "01";
				songs = find(args);
			}
			if (null==songs || songs.isEmpty()) {
				args[5] = "1";
				songs = search(args);
			}
			if (null!=songs && !songs.isEmpty()) {
				result[2]=songs.get(0).getDate();
			}
		}
		return result;
	}

	public int getAlbumCount(Artist artist, boolean useAlbumArtistTag) throws MPDServerException {
		return listAlbums(artist.getName(), useAlbumArtistTag).size();
	}

	public int getAlbumCount(String artist, boolean useAlbumArtistTag) throws MPDServerException {
		if (mpdConnection == null) {
			throw new MPDServerException("MPD Connection is not established");
		}
		return listAlbums(artist, useAlbumArtistTag).size();
	}

	/**
	 * Recursively retrieves all songs and directories.
	 * 
	 * @param dir
	 *           directory to list.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @return <code>FileStorage</code> with all songs and directories.
	 */
	/*public Directory listAllFiles(String dir) throws MPDServerException {
		if(!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		List<String> list = mpdConnection.sendCommand(MPD_CMD_LISTALL, dir);
		
		for (String line : list) {
			if (line.startsWith("directory: ")) {
				rootDirectory.makeDirectory(line.substring("directory: ".length()));
			} else if (line.startsWith("file: ")) {
				rootDirectory.addFile(new Music(line.substring("file: ".length())));
			}
		}
		return rootDirectory;
	}*/
	
	/**
	 * List all genre names from database.
	 * 
	 * @return artist names from database.
	 * 
	 * @throws MPDServerException if an error occur while contacting server.
	 */
	public List<String> listGenres() throws MPDServerException {
		return listGenres(true);
	}

	/**
	 * List all genre names from database.
	 * 
	 * @param sortInsensitive
	 *            boolean for insensitive sort when true
	 * @return artist names from database.
	 * @throws MPDServerException
	 *             if an error occur while contacting server.
	 */
	public List<String> listGenres(boolean sortInsensitive) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");

		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_GENRE);

		ArrayList<String> result = new ArrayList<String>();
		for (String s : response) {
			String name = s.substring("Genre: ".length());
			if (name.length() > 0)
				result.add(name);
		}
		if (sortInsensitive)
			Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
		else
			Collections.sort(result);

		return result;
	}

	/**
	 * List all artist names from database.
	 * 
	 * @return artist names from database.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public List<String> listArtists() throws MPDServerException {
		return listArtists(true);
	}
	/**
	 * List all artist names from database.
	 * 
	 * @param sortInsensitive
	 *           boolean for insensitive sort when true
	 * @return artist names from database.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public List<String> listArtists(boolean sortInsensitive) throws MPDServerException {
		if(!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ARTIST);
		
		ArrayList<String> result = new ArrayList<String>();
		for (String s : response) {
			String name = s.substring("Artist: ".length());
			if (name.length() > 0)
				result.add(name);
		}
		if (sortInsensitive)
			Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
		else
			Collections.sort(result);
		
		return result;
	}
	
	/**
	 * List all artist names from database.
	 * 
	 * @return artist names from database.
	 * @throws MPDServerException
	 *             if an error occur while contacting server.
	 */
	public List<String> listArtists(String genre) throws MPDServerException {
		return listArtists(genre, true);
	}

	/**
	 * List all artist names from database.
	 * 
	 * @param sortInsensitive
	 *            boolean for insensitive sort when true
	 * @return artist names from database.
	 * @throws MPDServerException
	 *             if an error occur while contacting server.
	 */
	public List<String> listArtists(String genre, boolean sortInsensitive) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");

		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ARTIST, MPDCommand.MPD_TAG_GENRE, genre);

		ArrayList<String> result = new ArrayList<String>();
		for (String s : response) {
			String name = s.substring("Artist: ".length());
			if (name.length() > 0)
				result.add(name);
		}
		if (sortInsensitive)
			Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
		else
			Collections.sort(result);

		return result;
	}

	/**
	 * List all album artist names from database.
	 * 
	 * @return album artist names from database.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public List<String> listAlbumArtists() throws MPDServerException {
		if(!isConnected())
			throw new MPDServerException("MPD Connection is not established");

		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM_ARTIST);
		
		ArrayList<String> result = new ArrayList<String>();
		for (String s : response) {
			String name = s.substring("albumartist: ".length());
			if (name.length() > 0)
				result.add(name);
		}
		
		Collections.sort(result);
		
		return result;
	}

	/**
	 * List all album artist names from database.
	 * 
	 * @return album artist names from database.
	 * @throws MPDServerException
	 *             if an error occur while contacting server.
	 */
	public List<String> listAlbumArtists(Genre genre) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");

		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LIST_TAG, MPDCommand.MPD_TAG_ALBUM_ARTIST, MPDCommand.MPD_TAG_GENRE,
				genre.getName());

		ArrayList<String> result = new ArrayList<String>();
		for (String s : response) {
			String name = s.substring("albumartist: ".length());
			if (name.length() > 0)
				result.add(name);
		}

		Collections.sort(result);

		return result;
	}

	/**
	 * Jumps to next playlist track.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void next() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_NEXT);
	}

	/**
	 * Authenticate using password.
	 * 
	 * @param password
	 *           password.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void password(String password) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_PASSWORD, password);
		mpdIdleConnection.sendCommand(MPDCommand.MPD_CMD_PASSWORD, password);
	}

	/**
	 * Pauses/Resumes music playing.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void pause() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_PAUSE);
	}

	/**
	 * Starts playing music.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void play() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY);
	}

	/**
	 * Plays previous playlist music.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while contacting server..
	 */
	public void previous() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_PREV);
	}

	/**
	 * Tells server to refresh database.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void refreshDatabase() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_REFRESH);
	}

	/**
	 * Similar to <code>find</code>,<code>search</code> looks for partial matches in the MPD database.
	 * 
	 * @param type
	 *           type of search. Should be one of the following constants: MPD_SEARCH_ARTIST, MPD_SEARCH_TITLE, MPD_SEARCH_ALBUM,
	 *           MPD_SEARCG_FILENAME
	 * @param string
	 *           case-insensitive locator string. Anything that contains <code>string</code> will be returned in the results.
	 * @return a Collection of <code>Music</code>.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see org.a0z.mpd.Music
	 */
	public Collection<Music> search(String type, String string) throws MPDServerException {
		return genericSearch(MPDCommand.MPD_CMD_SEARCH, type, string);
	}

    public List<Music> search(String[] args) throws MPDServerException {
        return genericSearch(MPDCommand.MPD_CMD_SEARCH, args, true);
    }

	/**
	 * Seeks music to the position.
	 * 
	 * @param songId
	 *           music id in playlist.
	 * @param position
	 *           song position in seconds.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void seekById(int songId, long position) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_SEEK_ID, Integer.toString(songId), Long.toString(position));
	}
	
	/**
	 * Seeks music to the position.
	 * 
	 * @param index
	 *           music position in playlist.
	 * @param position
	 *           song position in seconds.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void seekByIndex(int index, long position) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_SEEK, Integer.toString(index), Long.toString(position));
	}

	/**
	 * Seeks current music to the position.
	 * 
	 * @param position
	 *           song position in seconds
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void seek(long position) throws MPDServerException {
		seekById(this.getStatus().getSongId(), position);
	}

	/**
	 * Enabled or disable random.
	 * 
	 * @param random
	 *           if true random will be enabled, if false random will be disabled.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void setRandom(boolean random) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_RANDOM, random ? "1" : "0");
	}

	/**
	 * Enabled or disable repeating.
	 * 
	 * @param repeat
	 *           if true repeating will be enabled, if false repeating will be disabled.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void setRepeat(boolean repeat) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_REPEAT, repeat ? "1" : "0");
	}

	/**
	 * Enabled or disable single mode.
	 * 
	 * @param single
	 *            if true single mode will be enabled, if false single mode will be disabled.
	 * @throws MPDServerException
	 *             if an error occur while contacting server.
	 */
	public void setSingle(boolean single) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");

		mpdConnection.sendCommand(MPDCommand.MPD_CMD_SINGLE, single ? "1" : "0");
	}

	/**
	 * Enabled or disable consuming.
	 * 
	 * @param consume
	 *            if true song consuming will be enabled, if false song consuming will be disabled.
	 * @throws MPDServerException
	 *             if an error occur while contacting server.
	 */
	public void setConsume(boolean consume) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");

		mpdConnection.sendCommand(MPDCommand.MPD_CMD_CONSUME, consume ? "1" : "0");
	}

	/**
	 * Sets volume to <code>volume</code>.
	 * 
	 * @param volume
	 *           new volume value, must be in 0-100 range.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void setVolume(int volume) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		int vol = Math.max(MPDCommand.MIN_VOLUME, Math.min(MPDCommand.MAX_VOLUME, volume));
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_SET_VOLUME, Integer.toString(vol));
	}

	/**
	 * Kills server.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void shutdown() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_KILL);
	}

	/**
	 * Jumps to track <code>position</code> from playlist.
	 * 
	 * @param position
	 *           track number.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 * @see #skipToId(int)
	 */
	public void skipToPositon(int position) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY, Integer.toString(position));
	}

	/**
	 * Skip to song with specified <code>id</code>.
	 * 
	 * @param id
	 *           song id.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void skipToId(int id) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_PLAY_ID, Integer.toString(id));
	}

	/**
	 * Stops music playing.
	 * 
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void stop() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_STOP);
	}

	/**
	 * Retrieves root directory.
	 * 
	 * @return root directory.
	 */
	public Directory getRootDirectory() {
		return rootDirectory;
	}

	/**
	 * Sets cross-fade.
	 * 
	 * @param time
	 *           cross-fade time in seconds. 0 to disable cross-fade.
	 * @throws MPDServerException
	 *            if an error occur while contacting server.
	 */
	public void setCrossfade(int time) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_CROSSFADE, Integer.toString(Math.max(0, time)));
	}

	/**
     * Returns the available outputs
     * 
     * @return List of available outputs
     */
	public List<MPDOutput> getOutputs() throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		List<MPDOutput> result = new LinkedList<MPDOutput>();
		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTS);
		
		LinkedList<String> lineCache = new LinkedList<String>();
		for (String line : response) {
			if (line.startsWith("outputid: ")) {
				if (lineCache.size() != 0) {
					result.add(new MPDOutput(lineCache));
					lineCache.clear();
				}
			}
			lineCache.add(line);
		}

		if (lineCache.size() != 0) {
			result.add(new MPDOutput(lineCache));
		}

		return result;
	}

	/**
	 * Returns a list of all available playlists
	 */
	public List<Item> getPlaylists() throws MPDServerException {
		return getPlaylists(false);
	}

	/**
	 * Returns a list of all available playlists
	 * 
	 * @param sort whether the return list should be sorted
	 */
	public List<Item> getPlaylists(boolean sort) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		List<Item> result = new ArrayList<Item>();
		List<String> response = mpdConnection.sendCommand(MPDCommand.MPD_CMD_LISTPLAYLISTS);
		for(String line : response) {
			if(line.startsWith("playlist"))
				result.add(new Playlist(line.substring("playlist: ".length())));
		}
		if (sort)
			Collections.sort(result);
		
		return result;
	}

	public void enableOutput(int id) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTENABLE, Integer.toString(id));
	}

	public void disableOutput(int id) throws MPDServerException {
		if (!isConnected())
			throw new MPDServerException("MPD Connection is not established");
		
		mpdConnection.sendCommand(MPDCommand.MPD_CMD_OUTPUTDISABLE, Integer.toString(id));
	}

	public List<Music> getSongs(Artist artist, Album album) throws MPDServerException {
		boolean haveArtist = (null != artist);
		boolean haveAlbum = (null != album) && !(album instanceof UnknownAlbum);
        String[] search = null;

		int pos=0;
        if (haveAlbum || haveArtist) {
        	search=new String[haveAlbum && haveArtist ? 4 : 2];
        	if (haveArtist) {
        		search[pos++]=MPD.useAlbumArtist() ? MPDCommand.MPD_TAG_ALBUM_ARTIST : MPDCommand.MPD_FIND_ARTIST;
        		search[pos++]=artist.getName();
        	}
        	if (haveAlbum) {
        		search[pos++]=MPDCommand.MPD_FIND_ALBUM;
        		search[pos++]=album.getName();
        	}
        }
        List<Music> songs=find(search);
		if(album instanceof UnknownAlbum) {
			// filter out any songs with which have the album tag set
			Iterator<Music> iter = songs.iterator();
			while (iter.hasNext()) {
				if (iter.next().getAlbum() != null) iter.remove();
			}
		}
        if (null!=songs) {
        	 Collections.sort(songs);
        }
        return songs;
	}

	public List<Album> getAlbums(Artist artist) throws MPDServerException {
		List<String> albumNames = null;
		List<Album> albums = null;

		if(artist != null) {
			albumNames = listAlbums(artist.getName(), useAlbumArtist);
		}else{
			albumNames = listAlbums(useAlbumArtist);
		}

		if (null!=albumNames && !albumNames.isEmpty()) {
			albums=new ArrayList<Album>();
			for (String album : albumNames) {
				if(album == "") {
					// add a blank entry to host all songs without an album set
					albums.add(new UnknownAlbum());
				}else{
					long songCount = 0;
					long duration = 0;
					long year = 0;
					if (null!=artist && (MPD.showAlbumTrackCount() || MPD.sortAlbumsByYear())) {
						try {
							Long[] albumDetails = getAlbumDetails(artist.getName(), album, MPD.useAlbumArtist());
							if (null!=albumDetails && 3==albumDetails.length) {
								songCount=albumDetails[0];
								duration=albumDetails[1];
								year=albumDetails[2];
							}
						} catch (MPDServerException e) {
						}
					}
					albums.add(new Album(album, songCount, duration, year));
				}
			}
		}
		if (null!=albums) {
       	 	Collections.sort(albums);
		}
		return albums;
	}

	public List<Genre> getGenres() throws MPDServerException {
		List<String> genreNames = listGenres();
		List<Genre> genres = null;

		if (null != genreNames && !genreNames.isEmpty()) {
			genres = new ArrayList<Genre>();
			for (String genre : genreNames) {
				genres.add(new Genre(genre));
			}
		}
		if (null != genres) {
			Collections.sort(genres);
		}
		return genres;
	}

	public List<Artist> getArtists() throws MPDServerException {
		List<String> artistNames=MPD.useAlbumArtist() ? listAlbumArtists() : listArtists(true);
		List<Artist> artists = null;

		if (null!=artistNames && !artistNames.isEmpty()) {
			artists=new ArrayList<Artist>();
			for (String artist : artistNames) {
				artists.add(new Artist(artist, MPD.showArtistAlbumCount() ? getAlbumCount(artist, useAlbumArtist) : 0));
			}
		}
		if (null!=artists) {
       	 	Collections.sort(artists);
		}
		return artists;
	}

	public List<Artist> getArtists(Genre genre) throws MPDServerException {
		List<String> artistNames = MPD.useAlbumArtist() ? listAlbumArtists(genre) : listArtists(genre.getName(), true);
		List<Artist> artists = null;

		if (null != artistNames && !artistNames.isEmpty()) {
			artists = new ArrayList<Artist>();
			for (String artist : artistNames) {
				artists.add(new Artist(artist, MPD.showArtistAlbumCount() ? getAlbumCount(artist, useAlbumArtist) : 0));
			}
		}
		if (null != artists) {
			Collections.sort(artists);
		}
		return artists;
	}

	public List<Music> getPlaylistSongs(String playlistName) throws MPDServerException {
		String args[]=new String[1];
		args[0]=playlistName;
		List<Music> music=genericSearch(MPDCommand.MPD_CMD_PLAYLIST_INFO, args, false);
		
		for (int i=0; i<music.size(); ++i) {
			music.get(i).setSongId(i);
		}

		return music;
	}

	public void movePlaylistSong(String playlistName, int from, int to) throws MPDServerException {
		getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_MOVE, playlistName, Integer.toString(from), Integer.toString(to));
	}

	public void removeFromPlaylist(String playlistName, Integer pos) throws MPDServerException {
		getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_DEL, playlistName, Integer.toString(pos));
	}
	
	public void addToPlaylist(String playlistName, Collection<Music> c) throws MPDServerException {
		if (null==c || c.size()<1) {
			return;
		}
		for (Music m : c) {
			getMpdConnection().queueCommand(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlistName, m.getFullpath());
		}
		getMpdConnection().sendCommandQueue();
	}

	public void addToPlaylist(String playlistName, FilesystemTreeEntry entry) throws MPDServerException {
		getMpdConnection().sendCommand(MPDCommand.MPD_CMD_PLAYLIST_ADD, playlistName, entry.getFullpath());
	}
}
