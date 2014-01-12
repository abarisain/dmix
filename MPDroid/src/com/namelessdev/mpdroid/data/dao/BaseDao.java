
package com.namelessdev.mpdroid.data.dao;

import java.util.List;

public interface BaseDao<T> {

    public long add(T object);

    public long[] addAll(List<T> list);

    public void delete(long id);

    public T get(long id);

    public List<T> getAll();

    public List<T> getAll(List<Long> idList);

    /**
     * Get the table row count
     * 
     * @return The row count
     */
    public int getItemCount();

    /**
     * Checks if an item is in database. Faster than get because it does not
     * bind the data.
     * 
     * @param id Id of the target item
     * @return If the item is found or not
     */
    public boolean isInDatabase(long id);

    /**
     * Truncates the table
     * 
     * @return The number of deleted lines
     */
    public int truncate();
}
