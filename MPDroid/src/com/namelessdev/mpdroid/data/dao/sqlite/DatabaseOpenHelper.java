
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
