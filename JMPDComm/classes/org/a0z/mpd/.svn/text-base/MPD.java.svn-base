package org.a0z.mpd;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.a0z.mpd.url.M3uContentHandler;
import org.a0z.mpd.url.MpdContentHandlerFactory;

/**
 * MPD Server controller.
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPD.java 2716 2004-11-20 17:37:20Z galmeida $
 */
public class MPD {
    /**
     * Max volume level.
     */
    public static final int MIN_VOLUME = 0;

    /**
     * Min volume level.
     */
    public static final int MAX_VOLUME = 100;

    private static final String MPD_CMD_CLEARERROR = "clearerror";

    private static final String MPD_CMD_CLOSE = "close";

    private static final String MPD_CMD_CROSSFADE = "crossfade";

    private static final String MPD_CMD_FIND = "find";

    private static final String MPD_CMD_KILL = "kill";

    private static final String MPD_CMD_LIST_TAG = "list";

    private static final String MPD_CMD_LISTALL = "listall";

    private static final String MPD_CMD_LSDIR = "lsinfo";

    private static final String MPD_CMD_NEXT = "next";

    private static final String MPD_CMD_PAUSE = "pause";

    private static final String MPD_CMD_PASSWORD = "password";

    private static final String MPD_CMD_PLAY = "play";

    private static final String MPD_CMD_PLAY_ID = "playid";

    private static final String MPD_CMD_PREV = "previous";

    private static final String MPD_CMD_REFRESH = "update";

    private static final String MPD_CMD_REPEAT = "repeat";

    private static final String MPD_CMD_RANDOM = "random";

    private static final String MPD_CMD_SEARCH = "search";

    private static final String MPD_CMD_SEEK = "seek";

    private static final String MPD_CMD_SEEK_ID = "seekid";

    private static final String MPD_CMD_STATISTICS = "stats";

    private static final String MPD_CMD_STATUS = "status";

    private static final String MPD_CMD_STOP = "stop";

    private static final String MPD_CMD_VOLUME = "volume";

    private static final String MPD_CMD_SET_VOLUME = "setvol";
    
    private static final String MPD_CMD_OUTPUTS = "outputs";

    private static final String MPD_CMD_OUTPUTENABLE = "enableoutput";
    
    private static final String MPD_CMD_OUTPUTDISABLE = "disableoutput";

    /**
     * MPD default TCP port.
     */
    public static final int DEFAULT_MPD_PORT = 6600;

    /**
     * Find music by album name.
     */
    public static final String MPD_FIND_ALBUM = "album";

    /**
     * Find music by artist name.
     */
    public static final String MPD_FIND_ARTIST = "artist";

    /**
     * Search music by album name.
     */
    public static final String MPD_SEARCH_ALBUM = "album";

    /**
     * Search music by artist name.
     */
    public static final String MPD_SEARCH_ARTIST = "artist";

    /**
     * Search music by filename.
     */
    public static final String MPD_SEARCH_FILENAME = "filename";

    /**
     * Search music by title.
     */
    public static final String MPD_SEARCH_TITLE = "title";

    /**
     * List albums.
     */
    public static final String MPD_TAG_ALBUM = "album";

    /**
     * List artist.
     */
    public static final String MPD_TAG_ARTIST = "artist";

    /**
     * List album artist.
     */
    public static final String MPD_TAG_ALBUM_ARTIST = "albumartist";

    private static MpdContentHandlerFactory contentHandlerFactory = registerContentHandlerFactory();

    private MPDConnection mpdConnection;
    
    private MPDStatus mpdStatus;

    private MPDPlaylist playlist;

    private Directory rootDirectory;

    /**
     * Constructs a new MPD server controller without connection.
     */
    public MPD() {
        this.playlist = new MPDPlaylist(this);
        this.mpdStatus = new MPDStatus();
        this.rootDirectory = Directory.makeRootDirectory(this);
    }

    private static MpdContentHandlerFactory registerContentHandlerFactory() {
        MpdContentHandlerFactory factory = new MpdContentHandlerFactory();
        factory.registerContentHandler("audio/mpegurl", M3uContentHandler.class);
        factory.registerContentHandler("audio/x-mpegurl", M3uContentHandler.class);
        URLConnection.setContentHandlerFactory(factory);
        return factory;
    }

    /**
     * Retrieves MPD <code>ContentHandlerFactory</code>, allowing users to register new <code>ContentHandler</code>s.
     * @return MPD <code>ContentHandlerFactory</code>.
     */
    public static MpdContentHandlerFactory getContentHandlerFactory() {
        return contentHandlerFactory;
    }

    /**
     * Contructs a new MPD server controller.
     * @param server server address or hostname
     * @param port server port
     * @throws MPDServerException if an error occur while contacting server
     */
    public MPD(String server, int port) throws MPDServerException {
        this();
        this.connect(server, port);
    }

    /**
     * Retrieves <code>MPDConnection</code>.
     * @return <code>MPDConnection</code>.
     */
    MPDConnection getMpdConnection() {
        return this.mpdConnection;
    }

    /**
     * Increases or decreases volume by <code>modifier</code> amount.
     * @param modifier volume adjustment
     * @throws MPDServerException if an error occur while contacting server
     */
    public void adjustVolume(int modifier) throws MPDServerException {
        String[] args = new String[1];
        args[0] = Integer.toString(modifier);
        mpdConnection.sendCommand(MPD_CMD_VOLUME, args);
    }

    /**
     * Clears error message.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void clearError() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_CLEARERROR);
    }

    /**
     * Connects to a MPD server.
     * @param server server address or hostname
     * @param port server port
     * @throws MPDServerException if an error occur while contacting server
     */
    public final void connect(String server, int port) throws MPDServerException {
        this.mpdConnection = new MPDConnection(server, port);
    }

    /**
     * Connects to a MPD server.
     * @param server server address or hostname and port (server:port)
     * @throws MPDServerException if an error occur while contacting server
     */
    public final void connect(String server) throws MPDServerException {
        int port = DEFAULT_MPD_PORT;
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
     * Disconects from server.
     * @throws MPDServerException if an error occur while closing connection
     */
    public void disconnect() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_CLOSE);
        try {
            mpdConnection.disconnect();
        } catch (IOException e) {
            throw new MPDServerException(e.getMessage(), e);
        }
    }

    /**
     * Similar to <code>serach</code>,<code>find</code> looks for exact matches in the MPD database.
     * @param type type of search. Should be one of the following constants: MPD_FIND_ARTIST, MPD_FIND_ALBUM
     * @param string case-insensitive locator string. Anything that exactly matches <code>string</code> will be
     * returned in the results.
     * @return a Collection of <code>Music</code>
     * @throws MPDServerException if an error occur while contacting server
     * @see org.a0z.mpd.Music
     */
    public LinkedList<Music> find(String type, String string) throws MPDServerException {
        return genericSearch(MPD_CMD_FIND, type, string);
    }

    private LinkedList<Music> genericSearch(String searchCommand, String type, String string) throws MPDServerException {
        String[] args = new String[2];
        args[0] = type;
        args[1] = string;
        
        List<String> list = mpdConnection.sendCommand(searchCommand, args);
        LinkedList<String> file = new LinkedList<String>();
        LinkedList<Music> result = new LinkedList<Music>();
        for (String line : list) {
            if (line.startsWith("file: ")) {
                if (file.size() != 0) {
                    result.add(new Music(file));
                    file.clear();
                }
            }
            file.add(line);
        }

        if (file.size() > 0) {
            result.add(new Music(file));
        }

        return result;
    }

    /**
     * Retrieves a database directory listing of the base of the database directory path.
     * @return a <code>Collection</code> of <code>Music</code> and <code>Directory</code> representing directory
     * entries.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     * @see Directory
     */
    public LinkedList<FilesystemTreeEntry> getDir() throws MPDServerException {
        return getDir("");
    }

    /**
     * Retrieves a database directory listing of <code>dir</code> directory.
     * @param dir Directory to be listed.
     * @return a <code>Collection</code> of <code>Music</code> and <code>Directory</code> representing directory
     * entries.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     * @see Directory
     */
    public LinkedList<FilesystemTreeEntry> getDir(String dir) throws MPDServerException {
        String[] args = new String[1];
        args[0] = dir;

        List<String> list  = mpdConnection.sendCommand(MPD_CMD_LSDIR, args);
        LinkedList<String> file = new LinkedList<String>();
        LinkedList<FilesystemTreeEntry> result = new LinkedList<FilesystemTreeEntry>();
        for (String line : list) {
            if (line.startsWith("file: ") || line.startsWith("directory: ") || line.startsWith("playlist: ")) {
                if (file.size() != 0) {
                    result.add(new Music(file));
                    file.clear();
                }
            }
            if (line.startsWith("playlist: ")) {
                // TODO: implement playlist
            } else if (line.startsWith("directory: ")) {
                line = line.substring("directory: ".length());
                result.add(rootDirectory.makeDirectory(line));
            } else if (line.startsWith("file: ")) {
                file.add(line);
            }

        }
        if (file.size() > 0) {
            result.add(new Music(file));
        }

        return result;
    }

    

    /**
     * Retrieves a database directory listing of <code>dir</code> directory recursive!
     * @param dir Directory to be listed.
     * @return a <code>Collection</code> of <code>Music</code> and <code>Directory</code> representing directory
     * entries.
     * @throws MPDServerException if an error occur while contacting server.
     * @see Music
     * @see Directory
     */
    public Collection getDirRecursive(String dir) throws MPDServerException {
        String[] args = new String[1];
        args[0] = dir;
        //return mpdConnection.sendCommand(MPD_CMD_LSDIR, args);
        Collection result = new LinkedList();
        Iterator it = mpdConnection.sendCommand(MPD_CMD_LSDIR, args).iterator();

        List file = new LinkedList();
        while (it.hasNext()) {
            String line = (String) it.next();

            if (line.startsWith("file: ") || line.startsWith("directory: ") || line.startsWith("playlist: ")) {
                if (file.size() != 0) {
                    result.add(new Music(file));
                    file.clear();
                }
            }
            if (line.startsWith("playlist: ")) {
                //TODO implementar playlist
            } else if (line.startsWith("directory: ")) {
                line = line.substring("directory: ".length());
                result.add(rootDirectory.makeDirectory(line));
            } else {
                file.add(line);
            }

        }
        if (file.size() > 0) {
            result.add(new Music(file));
        }

        return result;
    }
    
    /**
     * Returns MPD server version.
     * @return MPD Server version.
     */
    public String getMpdVersion() {
        int[] version = mpdConnection.getMpdVersion();
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < version.length; i++) {
            sb.append(version[i]);
            if (i < (version.length - 1)) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    /**
     * Retrieves <code>playlist</code>.
     * @return playlist.
     */
    public MPDPlaylist getPlaylist() {
        return this.playlist;
    }

    /**
     * Retrieves statistics for the connected serevr.
     * @return statistics for the connected serevr.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public MPDStatistics getStatistics() throws MPDServerException {
        List response = mpdConnection.sendCommand(MPD_CMD_STATISTICS);
        return new MPDStatistics(response);
    }

    /**
     * Retrieves status of the connected server.
     * @return status of the connected server.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public MPDStatus getStatus() throws MPDServerException {
        List<String> response = mpdConnection.sendCommand(MPD_CMD_STATUS);
        mpdStatus.updateStatus(response);
        return mpdStatus;
    }

    /**
     * Retrieves currente volume.
     * @return current volume.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public int getVolume() throws MPDServerException {
        return this.getStatus().getVolume();
    }

    /**
     * Returns true when connected and false when not connected.
     * @return true when connected and false when not connected
     */
    public boolean isConnected() {
        if (mpdConnection == null) {
            return false;
        } else {
            return mpdConnection.isConnected();
        }
    }

    /**
     * List all albums from database.
     * @return <code>Collection</code> with all album names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public LinkedList<String> listAlbums() throws MPDServerException {
        return listAlbums(null);
    }

    /**
     * List all albums from a given artist.
     * @param artist artist to list albums
     * @return <code>Collection</code> with all album names from the given artist present in database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public LinkedList<String> listAlbums(String artist) throws MPDServerException {
        String[] args = null;
        if (artist == null) {
            args = new String[1];
        } else {
            args = new String[2];
            args[1] = artist;
        }
        args[0] = MPD_TAG_ALBUM;
        List<String> list = mpdConnection.sendCommand(MPD_CMD_LIST_TAG, args);
        LinkedList<String> result = new LinkedList<String>();
        for (String line : list) {
        	String[] arr = line.split(": ");
        	if(arr.length > 1)
        		result.add(arr[1]);
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Recursively retrieves all songs and directories in dir.
     * @param dir directory to list.
     * @throws MPDServerException if an error occur while contacting server.
     * @return <code>FileStorage</code> with all songs and directories.
     */
    public Directory listAllFiles(String dir) throws MPDServerException {
        String[] args = new String[1];
        args[0] = dir;
        List<String> list = mpdConnection.sendCommand(MPD_CMD_LISTALL, args);
        for (String line : list) {
            if (line.startsWith("directory: ")) {
                rootDirectory.makeDirectory(line.substring("directory: ".length()));
            } else if (line.startsWith("file: ")) {
                rootDirectory.addFile(new Music(line.substring("file: ".length())));
            }
        }
        return rootDirectory;
    }

    /**
     * List all artist names from database.
     * @return artist names from database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public LinkedList<String> listArtists() throws MPDServerException {
        String[] args = new String[1];
        args[0] = MPD_TAG_ARTIST;
        List<String> list = mpdConnection.sendCommand(MPD_CMD_LIST_TAG, args);
        LinkedList<String> result = new LinkedList<String>();
        for (String s : list) {
			String[] ss = s.split(": ");
			if (ss.length > 1)
				result.add(ss[1]);
        }
        Collections.sort(result);
        return result;
    }

	    /**
	     * List all album artist names from database.
	     * @return album artist names from database.
	     * @throws MPDServerException if an error occur while contacting server.
	     */
	    public LinkedList<String> listAlbumArtists() throws MPDServerException {
	        String[] args = new String[1];
	        args[0] = MPD_TAG_ALBUM_ARTIST;
	        List<String> list = mpdConnection.sendCommand(MPD_CMD_LIST_TAG, args);
	        LinkedList<String> result = new LinkedList<String>();
	        for (String s : list) {
				String[] ss = s.split(": ");
				if (ss.length > 1)
					result.add(ss[1]);
	        }
	        Collections.sort(result);
	        return result;
	    }

    /**
     * Jumps to next playlist track.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void next() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_NEXT);
    }

    /**
     * Authenticate using password.
     * @param password password.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void password(String password) throws MPDServerException {
        String[] args = new String[1];
        args[0] = password;
        mpdConnection.sendCommand(MPD_CMD_PASSWORD, args);
    }

    /**
     * Pauses/Resumes music playing.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void pause() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_PAUSE);
    }

    /**
     * Starts playing music.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void play() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_PLAY);
    }

    /**
     * Plays previous plyalist music.
     * @throws MPDServerException if an error occur while contacting server..
     */
    public void previous() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_PREV);
    }

    /**
     * Tells server to refreash database.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void refreshDatabase() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_REFRESH);
    }

    /**
     * Similar to <code>find</code>,<code>search</code> looks for partial matches in the MPD database.
     * @param type type of search. Should be one of the following constants: MPD_SEARCH_ARTIST, MPD_SEARCH_TITLE,
     * MPD_SEARCH_ALBUM, MPD_SEARCG_FILENAME
     * @param string case-insensitive locator string. Anything that contains <code>string</code> will be returned in
     * the results.
     * @return a Collection of <code>Music</code>.
     * @throws MPDServerException if an error occur while contacting server.
     * @see org.a0z.mpd.Music
     */
    public Collection search(String type, String string) throws MPDServerException {
        return genericSearch(MPD_CMD_SEARCH, type, string);
    }

    /**
     * Seeks music to the position.
     * @param songId music id in playlist.
     * @param position song position in seconds.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seek(int songId, long position) throws MPDServerException {
        String[] args = new String[2];
        args[0] = Integer.toString(songId);
        args[1] = Long.toString(position);
        mpdConnection.sendCommand(MPD_CMD_SEEK_ID, args);
        mpdStatus.songId = songId;
        mpdStatus.elapsedTime = position;
    }

    /**
     * Seeks current music to the position.
     * @param position song position in seconds
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void seek(long position) throws MPDServerException {
        seek(this.getStatus().getSongId(), position);
    }

    /**
     * Enabled or disable random.
     * @param random if true random will be enabled, if false random will be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setRandom(boolean random) throws MPDServerException {
        String[] args = new String[1];
        args[0] = random ? "1" : "0";
        mpdConnection.sendCommand(MPD_CMD_RANDOM, args);
    }

    /**
     * Enabled or disable repeating.
     * @param repeat if true repating will be enabled, if false repeating will be disabled.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setRepeat(boolean repeat) throws MPDServerException {
        String[] args = new String[1];
        args[0] = repeat ? "1" : "0";
        mpdConnection.sendCommand(MPD_CMD_REPEAT, args);
    }

    /**
     * Sets volume to <code>volume</code>.
     * @param volume new volume value, must be in 0-100 range.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setVolume(int volume) throws MPDServerException {
        int vol = volume;

        if (volume > MAX_VOLUME) {
            vol = MAX_VOLUME;
        } else if (volume < MIN_VOLUME) {
            vol = MIN_VOLUME;
        }

        String[] args = new String[1];
        args[0] = Integer.toString(vol);

        mpdConnection.sendCommand(MPD_CMD_SET_VOLUME, args);
        mpdStatus.volume = volume;
    }

    /**
     * Kills server.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void shutdown() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_KILL);
    }

    /**
     * Jumps to track <code>position</code> from playlist.
     * @param position track number.
     * @throws MPDServerException if an error occur while contacting server.
     * @deprecated use <code>skipTo</code>.
     * @see #skipTo(int)
     */
    public void skipToByPositon(int position) throws MPDServerException {
        String[] args = new String[1];
        args[0] = Integer.toString(position);
        mpdConnection.sendCommand(MPD_CMD_PLAY, args);
    }

    /**
     * Skip to song with specified <code>id</code>.
     * @param id song id.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void skipTo(int id) throws MPDServerException {
        String[] args = new String[1];
        args[0] = Integer.toString(id);
        mpdConnection.sendCommand(MPD_CMD_PLAY_ID, args);
    }

    /**
     * Stops music playing.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void stop() throws MPDServerException {
        mpdConnection.sendCommand(MPD_CMD_STOP);
    }

    /**
     * Retrieves root directory.
     * @return root directory.
     */
    public Directory getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Sets crossfade.
     * @param time crossfade time in seconds. 0 to disable crossfade.
     * @throws MPDServerException if an error occur while contacting server.
     */
    public void setCrossfade(int time) throws MPDServerException {
        int value = time;
        if (time < 0) {
            value = 0;
        }
        String[] args = new String[1];
        args[0] = Integer.toString(value);
        mpdConnection.sendCommand(MPD_CMD_CROSSFADE, args);
    }
    
    /**
     * 
     * 
     */
    public Collection<MPDOutput> getOutputs() throws MPDServerException {
    	Collection<MPDOutput> list = new LinkedList<MPDOutput>();
        Iterator it = mpdConnection.sendCommand(MPD_CMD_OUTPUTS).iterator();
        while (it.hasNext()) {
        	MPDOutput out = new MPDOutput();
        	out.setId(Integer.parseInt(((String) it.next()).split(": ")[1]));
        	out.setName(((String) it.next()).split(": ")[1]);
        	out.setEnabled(((String) it.next()).split(": ")[1].equals("1"));
        	list.add(out);
        }
    	return list;
    }

    public void enableOutput(int id) throws MPDServerException {
        String[] args = new String[1];
        args[0] = Integer.toString(id);
        mpdConnection.sendCommand(MPD_CMD_OUTPUTENABLE, args);
    }
    
    public void disableOutput(int id) throws MPDServerException {
        String[] args = new String[1];
        args[0] = Integer.toString(id);
        mpdConnection.sendCommand(MPD_CMD_OUTPUTDISABLE, args);
    }
    
}