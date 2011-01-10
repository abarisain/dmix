/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.providers;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.namelessdev.mpdroid.providers.ServerList.ServerColumns;

/**
 * Provides access to a database of notes. Each note has a title, the note itself, a creation date and a modified data.
 */
public class ServerListProvider extends ContentProvider {

	private static final String TAG = "ServerListProvider";

	private static final String DATABASE_NAME = "servers.db";
	private static final int DATABASE_VERSION = 1;
	private static final String SERVERS_TABLE_NAME = "servers";

	private static HashMap<String, String> sServerListProjectionMap;

	private static final int SERVERS = 1;
	private static final int SERVER_ID = 2;

	private static final UriMatcher sUriMatcher;

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + SERVERS_TABLE_NAME + " (" + ServerColumns._ID + " INTEGER PRIMARY KEY," + ServerColumns.NAME
					+ " TEXT," + ServerColumns.HOST + " TEXT," + ServerColumns.PORT + " TEXT," + ServerColumns.STREAMING_PORT + " TEXT,"
					+ ServerColumns.STREAMING_URL + " TEXT," + ServerColumns.DEFAULT + " INTEGER" + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + SERVERS_TABLE_NAME);
			onCreate(db);
		}
	}

	private DatabaseHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(SERVERS_TABLE_NAME);
		qb.setProjectionMap(sServerListProjectionMap);

		switch (sUriMatcher.match(uri)) {
		case SERVERS:
			break;

		case SERVER_ID:
			qb.appendWhere(ServerColumns._ID + "=" + uri.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = ServerColumns.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case SERVERS:
			return ServerColumns.CONTENT_TYPE;

		case SERVER_ID:
			return ServerColumns.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// Validate the requested uri
		if (sUriMatcher.match(uri) != SERVERS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (values.containsKey(ServerColumns.NAME) == false) {
			// Should never happen
			Resources r = Resources.getSystem();
			values.put(ServerColumns.NAME, r.getString(android.R.string.untitled) + now.toString());
		}

		if (values.containsKey(ServerColumns.HOST) == false) {
			values.put(ServerColumns.HOST, "0.0.0.0");
		}

		if (values.containsKey(ServerColumns.PORT) == false) {
			values.put(ServerColumns.PORT, "6600");
		}

		if (values.containsKey(ServerColumns.STREAMING_PORT) == false) {
			values.put(ServerColumns.STREAMING_PORT, "8000");
		}

		if (values.containsKey(ServerColumns.STREAMING_URL) == false) {
			values.put(ServerColumns.STREAMING_URL, "");
		}

		if (values.containsKey(ServerColumns.DEFAULT) == false) {
			values.put(ServerColumns.DEFAULT, "0");
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(SERVERS_TABLE_NAME, "server", values);
		if (rowId > 0) {
			Uri noteUri = ContentUris.withAppendedId(ServerColumns.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(noteUri, null);
			return noteUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case SERVERS:
			count = db.delete(SERVERS_TABLE_NAME, where, whereArgs);
			break;

		case SERVER_ID:
			String noteId = uri.getPathSegments().get(1);
			count = db.delete(SERVERS_TABLE_NAME, ServerColumns._ID + "=" + noteId
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case SERVERS:
			count = db.update(SERVERS_TABLE_NAME, values, where, whereArgs);
			break;

		case SERVER_ID:
			String serverId = uri.getPathSegments().get(1);
			count = db.update(SERVERS_TABLE_NAME, values, ServerColumns._ID + "=" + serverId
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(ServerList.AUTHORITY, "servers", SERVERS);
		sUriMatcher.addURI(ServerList.AUTHORITY, "servers/#", SERVER_ID);

		sServerListProjectionMap = new HashMap<String, String>();
		sServerListProjectionMap.put(ServerColumns._ID, ServerColumns._ID);
		sServerListProjectionMap.put(ServerColumns.NAME, ServerColumns.NAME);
		sServerListProjectionMap.put(ServerColumns.HOST, ServerColumns.HOST);
		sServerListProjectionMap.put(ServerColumns.PORT, ServerColumns.PORT);
		sServerListProjectionMap.put(ServerColumns.STREAMING_PORT, ServerColumns.STREAMING_PORT);
		sServerListProjectionMap.put(ServerColumns.STREAMING_URL, ServerColumns.STREAMING_URL);
		sServerListProjectionMap.put(ServerColumns.DEFAULT, ServerColumns.DEFAULT);
	}
}