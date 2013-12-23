package org.a0z.mpd;

import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class MPDCommand {
    private static final boolean DEBUG = false;

    public static final int MIN_VOLUME = 0;
    public static final int MAX_VOLUME = 100;

    public static final String MPD_CMD_CLEARERROR = "clearerror";
    public static final String MPD_CMD_CLOSE = "close";
    public static final String MPD_CMD_COUNT = "count";
    public static final String MPD_CMD_CROSSFADE = "crossfade";
    public static final String MPD_CMD_FIND = "find";
    public static final String MPD_CMD_KILL = "kill";
    public static final String MPD_CMD_LIST_TAG = "list";
    public static final String MPD_CMD_LISTALL = "listall";
    public static final String MPD_CMD_LISTALLINFO = "listallinfo";
    public static final String MPD_CMD_LISTPLAYLISTS = "listplaylists";
    public static final String MPD_CMD_LSDIR = "lsinfo";
    public static final String MPD_CMD_NEXT = "next";
    public static final String MPD_CMD_PAUSE = "pause";
    public static final String MPD_CMD_PASSWORD = "password";
    public static final String MPD_CMD_PLAY = "play";
    public static final String MPD_CMD_PLAY_ID = "playid";
    public static final String MPD_CMD_PREV = "previous";
    public static final String MPD_CMD_REFRESH = "update";
    public static final String MPD_CMD_REPEAT = "repeat";
    public static final String MPD_CMD_CONSUME = "consume";
    public static final String MPD_CMD_SINGLE = "single";
    public static final String MPD_CMD_RANDOM = "random";
    public static final String MPD_CMD_SEARCH = "search";
    public static final String MPD_CMD_SEEK = "seek";
    public static final String MPD_CMD_SEEK_ID = "seekid";
    public static final String MPD_CMD_STATISTICS = "stats";
    public static final String MPD_CMD_STATUS = "status";
    public static final String MPD_CMD_STOP = "stop";
    public static final String MPD_CMD_SET_VOLUME = "setvol";
    public static final String MPD_CMD_OUTPUTS = "outputs";
    public static final String MPD_CMD_OUTPUTENABLE = "enableoutput";
    public static final String MPD_CMD_OUTPUTDISABLE = "disableoutput";
    public static final String MPD_CMD_PLAYLIST_INFO = "listplaylistinfo";
    public static final String MPD_CMD_PLAYLIST_ADD = "playlistadd";
    public static final String MPD_CMD_PLAYLIST_MOVE = "playlistmove";
    public static final String MPD_CMD_PLAYLIST_DEL = "playlistdelete";

    public static final List<String> NON_RETRYABLE_COMMANDS = Arrays.asList(MPD_CMD_NEXT,MPD_CMD_PREV,MPD_CMD_PLAYLIST_ADD,MPD_CMD_PLAYLIST_MOVE,MPD_CMD_PLAYLIST_DEL) ;
    private boolean sentToServer = false;
    public static final String MPD_CMD_IDLE="idle";
    public static final String MPD_CMD_PING = "ping";

    // deprecated commands
    public static final String MPD_CMD_VOLUME = "volume";

    /**
     * MPD default TCP port.
     */
    public static final int DEFAULT_MPD_PORT = 6600;

    public static final String MPD_FIND_ALBUM = "album";
    public static final String MPD_FIND_ARTIST = "artist";

    public static final String MPD_SEARCH_ALBUM = "album";
    public static final String MPD_SEARCH_ARTIST = "artist";
    public static final String MPD_SEARCH_FILENAME = "filename";
    public static final String MPD_SEARCH_TITLE = "title";
    public static final String MPD_SEARCH_GENRE = "genre";

    public static final String MPD_TAG_ALBUM = "album";
    public static final String MPD_TAG_ARTIST = "artist";
    public static final String MPD_TAG_ALBUM_ARTIST = "albumartist";
    public static final String MPD_TAG_GENRE = "genre";

    String command = null;
    String[] args = null;

    private boolean synchronous = true;

    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    public MPDCommand(String _command, String... _args) {
        this.command = _command;
        this.args = _args;
    }

    public String toString() {
        StringBuffer outBuf = new StringBuffer();
        outBuf.append(command);
        for (String arg : args) {
            if(arg == null)
                continue;
            arg = arg.replaceAll("\"", "\\\\\"");
            outBuf.append(" \"" + arg + "\"");
        }
        outBuf.append("\n");
        final String outString = outBuf.toString();
        if (DEBUG)
            Log.d("JMPDComm", "Mpd command : " + (outString.startsWith("password ") ? "password **censored**" : outString));
        return outString;
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    public MPDCommand(String command, String[] args, boolean synchronous) {
        this.command = command;
        this.args = args;
        this.synchronous = synchronous;
    }

    public static boolean isRetryable(String command) {
        return !NON_RETRYABLE_COMMANDS.contains(command);
    }

    public boolean isSentToServer() {
        return sentToServer;
    }

    public void setSentToServer(boolean sentToServer) {
        this.sentToServer = sentToServer;
    }
}
