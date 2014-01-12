
package com.namelessdev.mpdroid.data.dao.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.namelessdev.mpdroid.data.model.ConnectionProfile;

import java.util.HashMap;
import java.util.Map;

public class ConnectionProfileDao extends BaseDBHelper<ConnectionProfile> implements
        com.namelessdev.mpdroid.data.dao.ConnectionProfileDao {

    private static final String TABLE_CONNECTION_PROFILE = "connection_profiles";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_HOSTNAME = "hostname";
    private static final String COL_PORT = "port";
    private static final String COL_PASSWORD = "password";
    private static final String COL_STREAMING_HOSTNAME = "streamingHostname";
    private static final String COL_STREAMING_PORT = "streamingPort";
    private static final String COL_STREAMING_SUFFIX = "streamingSuffix";
    private static final String COL_MUSIC_PATH = "musicPath";
    private static final String COL_COVER_FILENAME = "coverFilename";
    private static final String COL_USE_DATABASE_CACHE = "useDatabaseCache";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL(String
                .format("CREATE TABLE %s (%s INTEGER PRIMARY KEY, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
                        TABLE_CONNECTION_PROFILE, COL_ID, COL_NAME, COL_HOSTNAME, COL_PORT,
                        COL_PASSWORD, COL_STREAMING_HOSTNAME,
                        COL_STREAMING_PORT, COL_STREAMING_SUFFIX, COL_MUSIC_PATH,
                        COL_COVER_FILENAME, COL_USE_DATABASE_CACHE));
    }

    private Map<String, Integer> columnIds;

    @Override
    public ConnectionProfile cursorToObject(Cursor cursor) {
        final ConnectionProfile profile = new ConnectionProfile();
        profile.setId(cursor.getLong(columnIds.get(COL_ID)));
        profile.setName(cursor.getString(columnIds.get(COL_NAME)));
        profile.setHostname(cursor.getString(columnIds.get(COL_HOSTNAME)));
        profile.setPort(cursor.getInt(columnIds.get(COL_PORT)));
        profile.setPassword(cursor.getString(columnIds.get(COL_PASSWORD)));
        profile.setStreamingHostname(cursor.getString(columnIds.get(COL_STREAMING_HOSTNAME)));
        profile.setStreamingPort(cursor.getInt(columnIds.get(COL_STREAMING_PORT)));
        profile.setStreamingSuffix(cursor.getString(columnIds.get(COL_STREAMING_SUFFIX)));
        profile.setMusicPath(cursor.getString(columnIds.get(COL_MUSIC_PATH)));
        profile.setCoverFilename(cursor.getString(columnIds.get(COL_COVER_FILENAME)));
        profile.setUseDatabaseCache(cursor.getInt(columnIds.get(COL_USE_DATABASE_CACHE)) > 0 ? true
                : false);
        return profile;
    }

    @Override
    public String getMainTableName() {
        return TABLE_CONNECTION_PROFILE;
    }

    @Override
    public void mapColumnIdFromCursor(Cursor cursor) {
        columnIds = new HashMap<String, Integer>();
        columnIds.put(COL_ID, -1);
        columnIds.put(COL_NAME, -1);
        columnIds.put(COL_HOSTNAME, -1);
        columnIds.put(COL_PORT, -1);
        columnIds.put(COL_PASSWORD, -1);
        columnIds.put(COL_STREAMING_HOSTNAME, -1);
        columnIds.put(COL_STREAMING_PORT, -1);
        columnIds.put(COL_STREAMING_SUFFIX, -1);
        columnIds.put(COL_MUSIC_PATH, -1);
        columnIds.put(COL_COVER_FILENAME, -1);
        columnIds.put(COL_USE_DATABASE_CACHE, -1);
        fillColumnMap(cursor, columnIds);
    }

    @Override
    public ContentValues objectToContentValues(ConnectionProfile object) {
        final ContentValues content = new ContentValues();
        content.put(COL_ID, object.getId());
        content.put(COL_NAME, object.getName());
        content.put(COL_HOSTNAME, object.getHostname());
        content.put(COL_PORT, object.getPort());
        content.put(COL_PASSWORD, object.getPassword());
        content.put(COL_STREAMING_HOSTNAME, object.getStreamingHostname());
        content.put(COL_STREAMING_PORT, object.getStreamingPort());
        content.put(COL_STREAMING_SUFFIX, object.getStreamingSuffix());
        content.put(COL_MUSIC_PATH, object.getMusicPath());
        content.put(COL_COVER_FILENAME, object.getCoverFilename());
        content.put(COL_USE_DATABASE_CACHE, object.usesDatabaseCache() ? 1 : 0);
        return content;
    }

}
