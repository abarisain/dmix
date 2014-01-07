package com.namelessdev.mpdroid.helpers;

import org.a0z.mpd.*;
import org.a0z.mpd.exception.MPDServerException;

import com.namelessdev.mpdroid.tools.MultiMap;
import com.namelessdev.mpdroid.tools.Tools;
import com.namelessdev.mpdroid.R;

import android.content.Context;
import android.util.Log;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.*;
import java.net.InetAddress;

public class AlbumCache
{
    static final boolean GZIP = false;

    protected boolean enabled = true;

    protected String server;
    protected int port;
    protected File filesdir;

    protected CachedMPD mpd;
    protected MPDConnection mpdconnection;
    protected Date lastUpdate = null;

    // list of albumname, artist, albumartist including ""
    protected Set< List<String>> albumSet;
    // albums that have an albumartist get an empty artist:
    protected Set< List<String>> uniqueAlbumSet;

    class AlbumDetails implements Serializable {
        String path = null;
        //        List<Long> times = null;
        long numtracks = 0;
        long totaltime = 0;
        long date = 0;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException{
            out.writeUTF(path);
            //out.writeObject(times);
            out.writeLong(numtracks);
            out.writeLong(totaltime);
            out.writeLong(date);
        }
        private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
            path = in.readUTF();
            //times = (List<Long>)in.readObject();
            numtracks = in.readLong();
            totaltime = in.readLong();
            date = in.readLong();
        }
        private void readObjectNoData() throws ObjectStreamException {
        }
     }
    protected Map<String, AlbumDetails> albumDetails; // "artist///album" -> details


    protected static AlbumCache instance = null;

    public static AlbumCache getInstance(CachedMPD mpd) {
        if (instance == null) {
            instance = new AlbumCache(mpd);
        } else {
            instance.setMPD(mpd);
        }
        return instance;
    }

    protected AlbumCache(CachedMPD _mpd) {
        Log.d("MPD ALBUMCACHE", "Starting ...");
        setMPD(_mpd);
    }

    protected void setMPD(CachedMPD _mpd){
        enabled = true;
        try {
            Log.d("MPD ALBUMCACHE", "set MPD");
            this.mpd = _mpd;
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateConnection();
    }


    protected synchronized boolean updateConnection() {
        // get server/port from mpd
        if (!enabled) {
            Log.d("MPD ALBUMCACHE", "is disabled");
            return false;
        }
        if (mpd == null) {
            Log.d("MPD ALBUMCACHE", "no MPD! ");
            return false;
        }
        mpdconnection = mpd.getMpdConnection();
        if (mpdconnection == null) {
            Log.d("MPD ALBUMCACHE", "no MPDConnection! ");
            return false;
        }
        if (this.server == null) {
            this.server = mpdconnection.getHostAddress().getHostName();
            this.port   = mpdconnection.getHostPort();
            this.filesdir = mpd.getApplicationContext().getCacheDir();
            Log.d("MPD ALBUMCACHE", "server "+server +" port "+ port + " dir " + filesdir);
            if (!load()) {
                refresh(true);
            }
        }
        return true;
    }

    protected void makeUniqueAlbumSet() {
        uniqueAlbumSet = new HashSet<List<String>>();
        for (List<String> ai : albumSet) {
            if ("".equals(ai.get(2))) { // no albumartist
                uniqueAlbumSet.add(Arrays.asList(ai.get(0), ai.get(1),""));
            } else { // with albumartist set artist to ""
                uniqueAlbumSet.add(Arrays.asList(ai.get(0), "", ai.get(2)));
            }
        }
    }

    /*
     * reloads info from MPD if it is not up to date
     */
    public synchronized boolean refresh() {
        return refresh(false);
    }
    /*
     * reloads info from MPD if it is not up to date or if forced
     */
    public synchronized boolean refresh(boolean force) {
        if (!enabled) return false;
        if (!updateConnection()) return false;

        if (!force && isUpToDate()) {
            Log.d("MPD ALBUMCACHE", "Cache is up to date");
            return true;
        }
        Log.d("MPD ALBUMCACHE", "Cache is NOT up to date. fetching ...");
        lastUpdate = Calendar.getInstance().getTime();

        Context context = mpd.getApplicationContext();
        Tools.notifyUser(context.getResources().getString
                         (R.string.updatingLocalAlbumCacheNote),context);

        Date oldUpdate = lastUpdate;
        albumDetails        = new HashMap<String, AlbumDetails>();
        albumSet            = new HashSet<List<String>>();

        List<Music> allmusic;
        try {
            allmusic = Music.getMusicFromList(mpdconnection.sendCommand(MPDCommand.MPD_CMD_LISTALLINFO), false);
            Log.d("MPD ALBUMCACHE", "allmusic " + allmusic.size());
        } catch (MPDServerException e) {
            enabled = false;
            lastUpdate = null;
            e.printStackTrace();
            updateConnection();
            Log.d("MPD ALBUMCACHE", "disabled AlbumCache");
            Tools.notifyUser("Error with the 'listallinfo' command. Probably you have to adjust your server's 'max_output_buffer_size'", context);
            return false;
        }

        try {
            for (Music m : allmusic) {
                String albumartist = m.getAlbumArtist();
                String artist = m.getArtist();
                String album = m.getAlbum();
                if (album == null) {
                    album = "";
                }
                List<String> albuminfo = Arrays.asList
                    (album, artist == null ? "": artist,
                     albumartist == null ? "": albumartist);
                albumSet.add(albuminfo);

                boolean isAA = albumartist != null && !("".equals(albumartist));
                String thisalbum =
                    albumCode(isAA ? albumartist : artist, album, isAA);
                AlbumDetails details;
                if (albumDetails.containsKey(thisalbum)){
                    details = albumDetails.get(thisalbum);
                } else {
                    details = new AlbumDetails();
                    albumDetails.put(thisalbum, details);
                }
                if (details.path == null) {
                    details.path = m.getPath();
                }
                // if (details.times == null)
                //     details.times = new ArrayList<Long>();
                // details.times.add((Long)m.getTime());
                details.numtracks += 1;
                details.totaltime += m.getTime();
                if (details.date == 0)
                    details.date = m.getDate();
            }
            Log.d("MPD ALBUMCACHE", "albumDetails: " + albumDetails.size() );
            Log.d("MPD ALBUMCACHE", "albumSet: " + albumSet.size() );
            makeUniqueAlbumSet();
            Log.d("MPD ALBUMCACHE", "uniqueAlbumSet: " + uniqueAlbumSet.size() );
            if (!save()) {
                lastUpdate = oldUpdate;
                return false;
            }
        } catch (Exception e) {
            Tools.notifyUser("Error updating Album Cache", context);
            lastUpdate = oldUpdate;
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected synchronized boolean isUpToDate() {
        try {
            Date mpdlast = mpd.getStatistics().getDbUpdate();
            Log.d("MPD ALBUMCACHE", "lastupdate "+ lastUpdate  + " mpd date " + mpdlast);
            return (null != lastUpdate && null != mpdlast &&
                    lastUpdate.after(mpdlast));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected synchronized boolean load() {
        File file = new File(filesdir, getFilename()+(GZIP?".gz":""));
        if (!file.exists()) {
            return false;
        }
        Log.d("MPD ALBUMCACHE", "Loading "+file);
        ObjectInputStream restore = null;
        boolean loaded_ok = false;
        try {
            if (GZIP) {
                restore = new ObjectInputStream(new GZIPInputStream
                                                (new FileInputStream(file)));
            } else {
                restore = new ObjectInputStream(new FileInputStream(file));
            }
            lastUpdate          = (Date)restore.readObject();
            albumDetails        = (HashMap<String, AlbumDetails>)restore.readObject();
            albumSet            = (Set<List<String>>)restore.readObject();
            restore.close();
            makeUniqueAlbumSet();
            loaded_ok = true;
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (loaded_ok) Log.d("MPD ALBUMCACHE", cacheInfo());
        else  Log.d("MPD ALBUMCACHE", "Error on load");
        return loaded_ok;
    }

    protected synchronized boolean save() {
        File file = new File(filesdir, getFilename()+(GZIP?".gz":""));
        Log.d("MPD ALBUMCACHE", "Saving to "+file);
        File backupfile = new File(file.getAbsolutePath()+".bak");
        if (file.exists()) {
            if (backupfile.exists())
                backupfile.delete();
            file.renameTo(backupfile);
        }
        ObjectOutputStream save = null;
        boolean error = false;
        try {
            if (GZIP) {
                save = new ObjectOutputStream(new GZIPOutputStream
                                              (new FileOutputStream(file)));
            } else {
                save = new ObjectOutputStream(new BufferedOutputStream
                                              (new FileOutputStream(file)));
            }
            save.writeObject(lastUpdate);
            save.writeObject(albumDetails);
            save.writeObject(albumSet);
            save.close();
            Log.d("MPD ALBUMCACHE", "saved to "+ file);
        } catch (Exception e) {
            error = true;
            e.printStackTrace();
        }
        if (error) {
            file.delete();
            backupfile.renameTo(file);
        }
        return !error;
    }

    protected synchronized void deleteFile() {
        File file = new File(filesdir, getFilename());
        Log.d("MPD ALBUMCACHE", "Deleting "+file);
        if (file.exists())
            file.delete();
    }

    protected String getFilename(){
        return server+"_"+port;
    }

    public String cacheInfo(){
        return "AlbumCache: " +
            albumSet.size() + " album/artist combinations, " +
            uniqueAlbumSet.size() + " unique album/artist combinations, " +
            "Date: "+lastUpdate;
    }

    public Set<List<String>> getAlbumSet() {
        return albumSet;
    }
    public Set<List<String>> getUniqueAlbumSet() {
        return uniqueAlbumSet;
    }

    public Set<String> getAlbumArtists(String album, String artist) {
        Set<String> aartists = new HashSet<String>();
        for (List<String> ai : albumSet) {
            if (ai.get(0).equals(album) &&
                ai.get(1).equals(artist)) {
                aartists.add(ai.get(2));
            }
        }
        return aartists;
    }

    protected Set<String> getKeysByValue(Map<String,Set<String>> map, String val) {
        Set<String> result = new HashSet<String>();
        Set<String> keys = map.keySet();
        for (String k : keys) {
            Set<String> values = map.get(k);
            if (val == null || val == "" || values.contains(val)) {
                result.add(k);
            }
        }
        return result;
    }

    public List<String> getArtistsByAlbum(String album, boolean albumArtist) {
        Set<String> artists = new HashSet<String>();
        for (List<String> ai : albumSet) {
            if (ai.get(0).equals(album)) {
                if (albumArtist) {
                    artists.add(ai.get(2));
                } else {
                    artists.add(ai.get(1));
                }
            }
        }
        List<String> result;
        if (artists != null && artists.size() > 0) {
            result = new ArrayList<String>(artists);
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        }
        else {
            result = new ArrayList<String>();
        }
        return result;
    }


    public Set<String> getAlbums(String artist, boolean albumArtist){
        Set<String> albums = new HashSet<String>();
        for (List<String> ai : albumSet) {
            if (albumArtist && ai.get(2).equals(artist) ||
                !albumArtist && ai.get(1).equals(artist)) {
                albums.add(ai.get(0));
            }
        }
        return albums;
    }

    public String getDirByArtistAlbum(String artist, String album, boolean isAlbumArtist) {
        String aa = albumCode(artist, album, isAlbumArtist);
        String result = albumDetails.get(aa).path;
        Log.d("MPD ALBUMCACHE", "key "+aa +" - "+result );
        return result;
    }

    public AlbumDetails getAlbumDetails(String artist, String album, boolean isAlbumArtist) {
        return albumDetails.get(albumCode(artist, album, isAlbumArtist));
    }

    public String albumCode(String artist, String album, boolean isAlbumArtist) {
        return (artist != null ? artist : "") + "//"+
            (isAlbumArtist?"AA":"A") +
            "//" + (album != null ? album : "");
    }

}
