/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2015 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.anpmech.mpd.item;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class Item<T extends Item<T>> implements Comparable<Item<T>> {

    /**
     * This method merges two Item type lists.
     *
     * @param list1 The first list to merge, the list which will be merged into.
     * @param list2 The second list to merge, don't use this list after calling this method.
     */
    public static <T extends Item<T>> void merge(final List<T> list1, final List<T> list2) {
        removeUnknown(list1);

        Collections.sort(list1);
        Collections.sort(list2);

        int jStart = list1.size() - 1;
        for (int i = list2.size() - 1; i >= 0; i--) { // artists
            for (int j = jStart; j >= 0; j--) { // album artists
                if (list1.get(j).doesNameExist(list2.get(i))) {
                    jStart = j;
                    list2.remove(i);
                    break;
                }
            }
        }

        list1.addAll(list2);
    }

    /**
     * Removes any unknown entries from a list.
     *
     * @param items The items to search for an unknown item.
     */
    private static <T extends Item<T>> void removeUnknown(final List<T> items) {
        final ListIterator<T> iterator = items.listIterator(items.size());

        // remove "" from items, because the Unknown
        // AlbumArtist would fall back to an Artist, the "Unknown"
        // Entry must come from the Artists.
        while (iterator.hasPrevious()) {
            if (iterator.previous().isUnknown()) {
                iterator.remove();
            }
        }
    }

    /**
     * Defines a natural order to this object and another.
     *
     * @param another The other object to compare this to.
     * @return A negative integer if this instance is less than {@code another}; A positive integer
     * if this instance is greater than {@code another}; 0 if this instance has the same order as
     * {@code another}.
     */
    @Override
    public int compareTo(final Item<T> another) {
        final int comparisonResult;
        final String sorted = sortText();
        final String anotherSorted = another.sortText();

        // sort "" behind everything else
        if (sorted == null || sorted.isEmpty()) {
            if (anotherSorted == null || anotherSorted.isEmpty()) {
                comparisonResult = 0;
            } else {
                comparisonResult = 1;
            }
        } else if (anotherSorted == null || anotherSorted.isEmpty()) {
            comparisonResult = -1;
        } else {
            comparisonResult = Collator.getInstance().compare(sorted, anotherSorted);
        }

        return comparisonResult;
    }

    public boolean doesNameExist(final Item<T> o) {
        boolean nameExists = false;
        final String name = getName();

        if (name != null && o != null) {
            nameExists = name.equals(o.getName());
        }

        return nameExists;
    }

    public abstract String getName();

    public boolean isUnknown() {
        final String name = getName();

        return name == null || name.isEmpty();
    }

    /**
     * Returns the generated name for the item, and if null or empty the output may be translated
     * by resource, if available.
     *
     * @return If empty or null a translated "UnknownMetadata" string, if available, otherwise the
     * given name of the item.
     */
    public String mainText() {
        String mainText = getName();

        if (mainText == null || mainText.isEmpty()) {
            final String unknownKey = "UnknownMetadata";
            final String key = unknownKey + getClass().getSimpleName();
            ResourceBundle labels;

            try {
                labels = ResourceBundle.getBundle(unknownKey);
            } catch (final MissingResourceException ignored) {
                labels = ResourceBundle.getBundle(unknownKey, Locale.ENGLISH);
            }

            if (labels.containsKey(key)) {
                mainText = (String) labels.getObject(key);
            }
        }

        return mainText;
    }

    public String sortText() {
        String name = getName();

        if (name != null) {
            name = name.toLowerCase(Locale.getDefault());
        }

        return name;
    }

    @Override
    public String toString() {
        return mainText();
    }

}
