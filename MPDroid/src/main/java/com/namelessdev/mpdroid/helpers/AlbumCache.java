/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.tools.Tools;

import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Music;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class AlbumCache {

    static final boolean GZIP = false;

    private static final String TAG = "AlbumCache";

    protected static AlbumCache sInstance = null;

    protected Map<String, AlbumDetails> mAlbumDetails; // "artist///album" ->

    // list of albumname, artist, albumartist including ""
    protected Set<List<String>> mAlbumSet;

    protected boolean mEnabled = true;

    protected File mFilesDir;

    protected Date mLastUpdate = null;

    protected CachedMPD mMPD;

    protected int mPort;

    protected String mServer;

    // albums that have an albumartist get an empty artist:
    protected Set<List<String>> mUniqueAlbumSet;

    protected AlbumCache(final CachedMPD mpd) {
        super();
        Log.d(TAG, "Starting ...");
        setMPD(mpd);
    }
    // details

    public static String albumCode(final String artist, final String album,
            final boolean isAlbumArtist) {
        return (artist != null ? artist : "") + "//" +
                (isAlbumArtist ? "AA" : "A") +
                "//" + (album != null ? album : "");
    }

    public static AlbumCache getInstance(final CachedMPD mpd) {
        if (sInstance == null) {
            sInstance = new AlbumCache(mpd);
        } else {
            sInstance.setMPD(mpd);
        }
        return sInstance;
    }

    protected static Set<String> getKeysByValue(final Map<String, Set<String>> map,
            final String val) {
        final Set<String> result = new HashSet<>();

        for (final Map.Entry<String, Set<String>> stringSetEntry : map.entrySet()) {
            final Set<String> values = stringSetEntry.getValue();
            if (val != null && val.isEmpty() || values.contains(val)) {
                result.add(stringSetEntry.getKey());
            }
        }
        return result;
    }

    public String cacheInfo() {
        return "AlbumCache: " +
                mAlbumSet.size() + " album/artist combinations, " +
                mUniqueAlbumSet.size() + " unique album/artist combinations, " +
                "Date: " + mLastUpdate;
    }

    protected synchronized void deleteFile() {
        final File file = new File(mFilesDir, getFilename());
        Log.d(TAG, "Deleting " + file);
        if (file.exists()) {
            file.delete();
        }
    }

    public Set<String> getAlbumArtists(final String album, final String artist) {
        final Set<String> aartists = new HashSet<>();
        for (final List<String> ai : mAlbumSet) {
            if (ai.get(0).equals(album) &&
                    ai.get(1).equals(artist)) {
                aartists.add(ai.get(2));
            }
        }
        return aartists;
    }

    public AlbumDetails getAlbumDetails(final String artist, final String album,
            final boolean isAlbumArtist) {
        return mAlbumDetails.get(albumCode(artist, album, isAlbumArtist));
    }

    public Set<List<String>> getAlbumSet() {
        return mAlbumSet;
    }

    public Set<String> getAlbums(final String artist, final boolean albumArtist) {
        final Set<String> albums = new HashSet<>();
        for (final List<String> ai : mAlbumSet) {
            if (albumArtist && ai.get(2).equals(artist) ||
                    !albumArtist && ai.get(1).equals(artist)) {
                albums.add(ai.get(0));
            }
        }
        return albums;
    }

    public List<String> getArtistsByAlbum(final String album, final boolean albumArtist) {
        final Set<String> artists = new HashSet<>();
        for (final List<String> ai : mAlbumSet) {
            if (ai.get(0).equals(album)) {
                if (albumArtist) {
                    artists.add(ai.get(2));
                } else {
                    artists.add(ai.get(1));
                }
            }
        }
        final List<String> result;
        if (artists != null && !artists.isEmpty()) {
            result = new ArrayList<>(artists);
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        } else {
            result = new ArrayList<>();
        }
        return result;
    }

    public String getDirByArtistAlbum(final String artist, final String album,
            final boolean isAlbumArtist) {
        final String albumCode = albumCode(artist, album, isAlbumArtist);
        final String result = mAlbumDetails.get(albumCode).mPath;
        Log.d(TAG, "key " + albumCode + " - " + result);
        return result;
    }

    protected String getFilename() {
        return mServer + '_' + mPort;
    }

    public Set<List<String>> getUniqueAlbumSet() {
        return mUniqueAlbumSet;
    }

    protected synchronized boolean isUpToDate() {
        final Date mpdlast = mMPD.getStatistics().getDbUpdate();
        Log.d(TAG, "lastupdate " + mLastUpdate + " mpd date " + mpdlast);
        return (null != mLastUpdate && null != mpdlast && mLastUpdate.after(mpdlast));
    }

    protected synchronized boolean load() {
        final File file = new File(mFilesDir, getFilename() + (GZIP ? ".gz" : ""));
        if (!file.exists()) {
            return false;
        }
        Log.d(TAG, "Loading " + file);
        final ObjectInputStream restore;
        boolean loadedOk = false;
        try {
            if (GZIP) {
                restore = new ObjectInputStream(new GZIPInputStream
                        (new FileInputStream(file)));
            } else {
                restore = new ObjectInputStream(new FileInputStream(file));
            }
            mLastUpdate = (Date) restore.readObject();
            mAlbumDetails = (Map<String, AlbumDetails>) restore.readObject();
            mAlbumSet = (Set<List<String>>) restore.readObject();
            restore.close();
            makeUniqueAlbumSet();
            loadedOk = true;
        } catch (final FileNotFoundException ignored) {
        } catch (final Exception e) {
            Log.e(TAG, "Exception.", e);
        }
        if (loadedOk) {
            Log.d(TAG, cacheInfo());
        } else {
            Log.d(TAG, "Error on load");
        }
        return loadedOk;
    }

    protected void makeUniqueAlbumSet() {
        mUniqueAlbumSet = new HashSet<>(mAlbumSet.size());
        for (final List<String> ai : mAlbumSet) {
            final String album = ai.get(2);
            if (album != null && album.isEmpty()) { // no albumartist
                mUniqueAlbumSet.add(Arrays.asList(ai.get(0), ai.get(1), ""));
            } else { // with albumartist set artist to ""
                mUniqueAlbumSet.add(Arrays.asList(ai.get(0), "", ai.get(2)));
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
    public synchronized boolean refresh(final boolean force) {
        if (!mEnabled) {
            return false;
        }
        if (!updateConnection()) {
            return false;
        }

        if (!force && isUpToDate()) {
            Log.d(TAG, "Cache is up to date");
            return true;
        }
        Log.d(TAG, "Cache is NOT up to date. fetching ...");
        mLastUpdate = Calendar.getInstance().getTime();

        Tools.notifyUser(R.string.updatingLocalAlbumCacheNote);

        final Date oldUpdate = mLastUpdate;
        mAlbumDetails = new HashMap<>();
        mAlbumSet = new HashSet<>();

        final List<Music> allmusic;
        try {
            allmusic = mMPD.listAllInfo();
            Log.d(TAG, "allmusic " + allmusic.size());
        } catch (final IOException | MPDException e) {
            mEnabled = false;
            mLastUpdate = null;
            updateConnection();
            Log.d(TAG, "disabled AlbumCache", e);
            Tools.notifyUser(
                    "Error with the 'listallinfo' command. Probably you have to adjust your server's 'max_output_buffer_size'"
            );
            return false;
        }

        try {
            for (final Music music : allmusic) {
                final String albumArtist = music.getAlbumArtist();
                final String artist = music.getArtist();
                String album = music.getAlbum();
                if (album == null) {
                    album = "";
                }
                final List<String> albumInfo = Arrays.asList
                        (album, artist == null ? "" : artist,
                                albumArtist == null ? "" : albumArtist);
                mAlbumSet.add(albumInfo);

                final boolean isAlbumArtist = albumArtist != null && !albumArtist.isEmpty();
                final String thisAlbum =
                        albumCode(isAlbumArtist ? albumArtist : artist, album, isAlbumArtist);
                final AlbumDetails details;
                if (mAlbumDetails.containsKey(thisAlbum)) {
                    details = mAlbumDetails.get(thisAlbum);
                } else {
                    details = new AlbumDetails();
                    mAlbumDetails.put(thisAlbum, details);
                }
                if (details.mPath == null) {
                    details.mPath = music.getPath();
                }
                // if (details.times == null)
                // details.times = new ArrayList<Long>();
                // details.times.add((Long)m.getTime());
                details.mNumTracks += 1;
                details.mTotalTime += music.getTime();
                if (details.mDate == 0) {
                    details.mDate = music.getDate();
                }
            }
            Log.d(TAG, "albumDetails: " + mAlbumDetails.size());
            Log.d(TAG, "albumSet: " + mAlbumSet.size());
            makeUniqueAlbumSet();
            Log.d(TAG, "uniqueAlbumSet: " + mUniqueAlbumSet.size());
            if (!save()) {
                mLastUpdate = oldUpdate;
                return false;
            }
        } catch (final Exception e) {
            Tools.notifyUser("Error updating Album Cache");
            mLastUpdate = oldUpdate;
            Log.e(TAG, "Error updating Album Cache.", e);
            return false;
        }
        return true;
    }

    protected synchronized boolean save() {
        final File file = new File(mFilesDir, getFilename() + (GZIP ? ".gz" : ""));
        Log.d(TAG, "Saving to " + file);
        final File backupfile = new File(file.getAbsolutePath() + ".bak");
        if (file.exists()) {
            if (backupfile.exists()) {
                backupfile.delete();
            }
            file.renameTo(backupfile);
        }
        final ObjectOutputStream save;
        boolean error = false;
        try {
            if (GZIP) {
                save = new ObjectOutputStream(new GZIPOutputStream
                        (new FileOutputStream(file)));
            } else {
                save = new ObjectOutputStream(new BufferedOutputStream
                        (new FileOutputStream(file)));
            }
            save.writeObject(mLastUpdate);
            save.writeObject(mAlbumDetails);
            save.writeObject(mAlbumSet);
            save.close();
            Log.d(TAG, "saved to " + file);
        } catch (final Exception e) {
            error = true;
            Log.e(TAG, "Failed to save.", e);
        }
        if (error) {
            file.delete();
            backupfile.renameTo(file);
        }
        return !error;
    }

    protected void setMPD(final CachedMPD mpd) {
        mEnabled = true;
        try {
            Log.d(TAG, "set MPD");
            mMPD = mpd;
        } catch (final Exception e) {
            Log.e(TAG, "Failed to setMPD.", e);
        }
        updateConnection();
    }

    protected synchronized boolean updateConnection() {
        // get server/port from mpd
        if (!mEnabled) {
            Log.d(TAG, "is disabled");
            return false;
        }
        if (mMPD == null) {
            Log.d(TAG, "no MPD! ");
            return false;
        }

        if (!mMPD.isConnected()) {
            Log.d(TAG, "no MPDConnection! ");
            return false;
        }
        if (mServer == null) {
            mServer = mMPD.getHostAddress().getHostName();
            mPort = mMPD.getHostPort();
            mFilesDir = MPDApplication.getInstance().getCacheDir();
            Log.d(TAG, "server " + mServer + " port " + mPort + " dir " + mFilesDir);
            if (!load()) {
                refresh(true);
            }
        }
        return true;
    }

    static class AlbumDetails implements Serializable {

        private static final long serialVersionUID = 2465675380232237273L;

        long mDate = 0;

        // List<Long> times = null;
        long mNumTracks = 0;

        String mPath = null;

        long mTotalTime = 0;

        private void readObject(final DataInput in)
                throws IOException, ClassNotFoundException {
            mPath = in.readUTF();
            // times = (List<Long>)in.readObject();
            mNumTracks = in.readLong();
            mTotalTime = in.readLong();
            mDate = in.readLong();
        }

        private void writeObject(final DataOutput out) throws IOException {
            out.writeUTF(mPath);
            // out.writeObject(times);
            out.writeLong(mNumTracks);
            out.writeLong(mTotalTime);
            out.writeLong(mDate);
        }
    }

}
