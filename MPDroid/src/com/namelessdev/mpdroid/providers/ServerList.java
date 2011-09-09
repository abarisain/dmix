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

import android.net.Uri;
import android.provider.BaseColumns;

public final class ServerList {
	public static final String AUTHORITY = "com.namelessdev.mpdroid.providers.serverlist";

	// This class cannot be instantiated
	private ServerList() {
	}

	public static final class ServerColumns implements BaseColumns {
		// This class cannot be instantiated
		private ServerColumns() {
		}

		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/servers");
		
		/**
		 * The MIME type of {@link #CONTENT_URI} a list of servers
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.namelessdev.server";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single server
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.namelessdev.server";

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "name ASC";

		/**
		 * The server name
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String NAME = "name";

		/**
		 * The server host
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String HOST = "host";

		/**
		 * The mpd control port
		 * <P>
		 * Type: TEXT (will be processed later)
		 * </P>
		 */
		public static final String PORT = "port";

		/**
		 * The mpd streaming port
		 * <P>
		 * Type: TEXT (will be processed later)
		 * </P>
		 */
		public static final String STREAMING_PORT = "streamingPort";

		/**
		 * The mpd streaming URL (overrides STREAMING_PORT if not null-
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String STREAMING_URL = "streamingURL";

		/**
		 * Server password
		 * <P>
		 * Type: TEXT
		 * </P>
		 */
		public static final String PASSWORD = "password";
	}
}