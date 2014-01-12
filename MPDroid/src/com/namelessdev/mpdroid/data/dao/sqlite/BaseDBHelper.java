
package com.namelessdev.mpdroid.data.dao.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.namelessdev.mpdroid.data.dao.BaseDao;
import com.namelessdev.mpdroid.data.dao.sqlite.exceptions.BaseDBHelperException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base SQLite interface code, which implements the most common usage of DAOs.
 * 
 * @param <T> The DAO target type
 */
public abstract class BaseDBHelper<T> implements BaseDao<T> {
    private String idColumnName;

    private static boolean manualTransactions = false;

    /**
     * Beings a transaction
     */
    public static void beginTransaction() {
        if (!manualTransactions) {
            throw new RuntimeException(
                    "Cannot begin a transaction if they are not set to manual mode.");
        }
        getDatabase().beginTransaction();
    }

    /**
     * Ends a transaction
     * 
     * @param successful True if the transaction is successful
     */
    public static void endTransaction(boolean successful) {
        if (!manualTransactions) {
            throw new RuntimeException(
                    "Cannot end a transaction if they are not set to manual mode.");
        }
        if (successful) {
            getDatabase().setTransactionSuccessful();
        }
        getDatabase().endTransaction();
    }

    /**
     * Get the application database instance. Do not close it.
     * 
     * @return The database instance
     */
    public static SQLiteDatabase getDatabase() {
        return DatabaseOpenHelper.getInstance(null).getDB();
    }

    /**
     * Sets if the transactions will be handled manually
     * 
     * @param value The parameter value
     */
    public static void setManualTransactions(boolean value) {
        manualTransactions = value;
    }

    public BaseDBHelper() {
        idColumnName = "id";
    }

    public long add(T object) {
        if (getMainTableName() == null) {
            throwMainNotSetException();
            return -1l;
        }
        if (object == null) {
            return -1l;
        }

        final ContentValues content = objectToContentValues(object);
        if (content == null) {
            throw new BaseDBHelperException("objectToContentValues returned null");
        }
        // We don't need update for now, and I don't think we will, but if we
        // do, it's already there.
        /*
         * if (isInDatabase(getObjectId(object))) { final StringBuilder builder
         * = new StringBuilder(idColumnName); builder.append("=");
         * builder.append(getObjectId(object)); return
         * getDatabase().update(getMainTableName(), content, builder.toString(),
         * null); }
         */
        return getDatabase().insert(getMainTableName(), null, content);
    }

    public long[] addAll(List<T> list) {
        final boolean lManualTransaction = manualTransactions;

        if (getMainTableName() == null) {
            throwMainNotSetException();
            return null;
        }
        if (list == null || list.size() == 0) {
            return new long[0];
        }
        if (!lManualTransaction) {
            getDatabase().beginTransaction();
        }
        long[] ids = new long[list.size()];
        try {
            for (int i = 0; i < list.size(); i++) {
                ids[i] = add(list.get(i));
            }
            if (!lManualTransaction) {
                getDatabase().setTransactionSuccessful();
            }
        } finally {
            if (!lManualTransaction) {
                getDatabase().endTransaction();
            }
        }
        return ids;
    }

    /**
     * Converts a cursor to an object instance. Note : You shall not call
     * cursor.moveToNext, or anything unrelated to getting values
     * 
     * @param cursor The cursor to convert from.
     * @return The object converted. Should never be null, otherwise
     *         BaseDBHelperException might be raised.
     */
    public abstract T cursorToObject(Cursor cursor);

    public void delete(long id) {
        if (getMainTableName() == null) {
            throwMainNotSetException();
            return;
        }
    }

    // See commented code in add(T object)
    // public abstract Long getObjectId(T object);

    /**
     * Fills a Map if column names with their IDs
     * 
     * @param columnMap The source map
     * @return The filled map
     */
    public Map<String, Integer> fillColumnMap(Cursor cursor, Map<String, Integer> columnMap) {
        for (String key : columnMap.keySet()) {
            columnMap.put(key, cursor.getColumnIndex(key));
        }
        return columnMap;
    }

    public T get(long id) {
        return get(idColumnName + " = ?", new String[] {
            Long.toString(id)
        });
    }

    /**
     * Helper get you can call for easily adding more get functions.
     */
    public T get(String whereClause, String[] whereArgs) {
        if (getMainTableName() == null) {
            throwMainNotSetException();
            return null;
        }
        final Cursor cursor = getDatabase().query(getMainTableName(), null, whereClause, whereArgs,
                null, null, null);
        if (cursor.moveToNext()) {
            mapColumnIdFromCursor(cursor);
            final T object = cursorToObject(cursor);
            if (object == null) {
                throw new BaseDBHelperException("cursorToObject returned null");
            }
            return object;
        }

        return null;
    }

    /**
     * Override this to add default values
     */
    public List<T> getAll() {
        return getAll(null, null, null);
    }

    public List<T> getAll(List<Long> idList) {
        final List<T> resultList = new ArrayList<T>();
        if (idList != null) {
            for (Long id : idList) {
                final T object = get(id);
                if (object != null) {
                    // Avoid dead relationships (should never happen by the way)
                    resultList.add(object);
                }
            }
        }
        return resultList;
    }

    /**
     * Helper getAll you can call for easily adding more getAll functions.
     */
    public List<T> getAll(String whereClause, String[] whereArgs, String orderBy) {
        if (getMainTableName() == null) {
            throwMainNotSetException();
            return null;
        }
        final List<T> resultList = new ArrayList<T>();
        final Cursor cursor = getDatabase().query(getMainTableName(), null, whereClause, whereArgs,
                null, null, orderBy);
        mapColumnIdFromCursor(cursor);
        T convertedObject = null;
        while (cursor.moveToNext()) {
            convertedObject = cursorToObject(cursor);
            if (convertedObject == null) {
                throw new BaseDBHelperException("cursorToObject returned null");
            }
            resultList.add(convertedObject);
        }
        return resultList;
    }

    public int getItemCount() {
        if (getMainTableName() == null) {
            throwMainNotSetException();
            return -1;
        }
        final Cursor cursor = getDatabase().query(getMainTableName(), null, null, null, null, null,
                null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Returns the main table name. Used for almost every function in
     * BaseDBHelper. Should NEVER return null (BaseDBHelperException will be
     * thrown if so).
     */
    public abstract String getMainTableName();

    /**
     * Gets the content of the link table for an ID
     */
    public List<Long> getRelationships(String tableName, String firstColumn, String secondColumn,
            Long targetId) {
        if (tableName == null || firstColumn == null || secondColumn == null) {
            throw new BaseDBHelperException("Invalid arguments");
        }
        final List<Long> resultList = new ArrayList<Long>();
        final Cursor cursor = getDatabase().query(tableName, null, firstColumn + " = ?",
                new String[] {
                    Long.toString(targetId)
                }, null,
                null, null);
        int secondColumnId = cursor.getColumnIndex(secondColumn);
        while (cursor.moveToNext()) {
            resultList.add(cursor.getLong(secondColumnId));
        }
        return resultList;
    }

    public boolean isInDatabase(long id) {
        if (getMainTableName() == null) {
            throwMainNotSetException();
            return false;
        }
        boolean found = false;
        final StringBuilder builder = new StringBuilder(idColumnName);
        builder.append("=");
        builder.append(id);

        final Cursor cursor = getDatabase().query(getMainTableName(), new String[] {
            idColumnName
        }, builder.toString(), null, null, null,
                null);

        while (cursor.moveToNext()) {
            found = true;
        }
        cursor.close();
        return found;
    }

    /**
     * Called when the DB Helper needs you to generate the column id <=> column
     * name mapping It is called once per get and getAll. This is for
     * optimization, you're free to do it manually in objectToContentValues, but
     * when objectToContentValues is called in a loop, you are assured that
     * mapColumnIdFromCursor is only called once.
     * 
     * @param cursor The database cursor you should use for mapping
     */
    public abstract void mapColumnIdFromCursor(Cursor cursor);

    /**
     * Converts an object to ContentValues for SQL insertion
     * 
     * @param object Object to convert, can be null.
     * @return Filled ContentValues. Must NOT be null.
     */
    public abstract ContentValues objectToContentValues(T object);

    /**
     * Sets the ID column name for delete, getItemCount, truncate and
     * isInDatabase Only necessary if the column isnt "id"
     * 
     * @param idColumnName The id column name to use
     */
    public void setIdColumnName(String idColumnName) {
        this.idColumnName = idColumnName;
    }

    /**************************
     * BaseDao implementation *
     **************************/

    private void throwMainNotSetException() {
        throw new BaseDBHelperException("Main table name not set.");
    }

    public int truncate() {
        if (getMainTableName() == null) {
            throwMainNotSetException();
            return 0;
        }
        // If WhereClause = 1 in SQLite, it returns us the number of deleted
        // rows
        return getDatabase().delete(getMainTableName(), "1", null);
    }

}
