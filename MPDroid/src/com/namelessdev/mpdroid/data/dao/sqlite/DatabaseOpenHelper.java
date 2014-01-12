/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
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

package com.namelessdev.mpdroid.data.dao.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static DatabaseOpenHelper _instance;
    private static final String DB_NAME = "digmaat.db";
    private static final int DB_VERSION = 1;

    public static void destroyInstance() {
        if (_instance != null) {
            _instance.closeDB();
            _instance = null;
        }
    }

    public static DatabaseOpenHelper getInstance(Context context) {
        if (_instance == null) {
            _instance = new DatabaseOpenHelper(context);
        }
        return _instance;
    }

    private SQLiteDatabase db;

    public DatabaseOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        openDB();
    }

    public void closeDB() {
        if (this.db != null) {
            this.db.close();
        }
    }

    public SQLiteDatabase getDB() {
        return db;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the databases using the DAOs here
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // Ne sera jamais implémenté
    }

    public void openDB() {
        this.db = this.getWritableDatabase();
    }

}
