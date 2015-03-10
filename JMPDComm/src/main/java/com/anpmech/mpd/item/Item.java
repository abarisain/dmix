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
import java.util.ResourceBundle;

/**
 * This is a generic representation of a Item "object" from the MPD protocol.
 *
 * @param <T> The Item type.
 */
public abstract class Item<T extends Item<T>> implements Comparable<Item<T>> {

    /**
     * This {@link Collator} is used for comparison and sorting of {@code Item}s.
     */
    private static final Collator COLLATOR = Collator.getInstance();

    /**
     * The ResourceBundle from which to retrieve any necessary translations.
     */
    private static final ResourceBundle RESOURCE;

    /**
     * The bundle name for the "Unknown" or empty metadata for an item.
     */
    private static final String UNKNOWN_METADATA = "UnknownMetadata";

    static {
        COLLATOR.setStrength(Collator.PRIMARY);
        RESOURCE = ResourceBundle.getBundle(UNKNOWN_METADATA);
    }

    Item() {
        super();
    }

    /**
     * This method merges two Item type lists.
     * <p/>
     * This method removes unknown items and items not of the same name, then adds the remainder to
     * {@code list1}.
     *
     * @param list1 The first list to merge, the list which will be merged into.
     * @param list2 The second list to merge, do not reuse this list after calling this method.
     */
    public static <T extends Item<T>> void merge(final List<T> list1, final List<T> list2) {
        Collections.sort(list1);
        Collections.sort(list2);

        final ListIterator<T> iterator2 = list2.listIterator();
        int position = 0;
        ListIterator<T> iterator1 = list1.listIterator(position);

        while (iterator2.hasNext()) {
            final Item<T> item2 = iterator2.next();

            if (position != iterator1.previousIndex()) {
                iterator1 = list1.listIterator(position);
            }
            while (iterator1.hasNext()) {
                final Item<T> item1 = iterator1.next();

                if (item1.isUnknown()) {
                    iterator1.remove();
                    continue;
                }

                if (item1.isNameSame(item2)) {
                    position = iterator1.previousIndex();
                    iterator2.remove();
                    break;
                }
            }
        }

        list1.addAll(list2);
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
        final String sorted = sortName();
        final String anotherSorted = another.sortName();

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
            comparisonResult = COLLATOR.compare(sorted, anotherSorted);
        }

        return comparisonResult;
    }

    /**
     * Compares an Item object with a general contract of comparison that is reflexive, symmetric
     * and transitive.
     *
     * @param o The object to compare this instance with.
     * @return True if the objects are equal with regard to te general contract, false otherwise.
     */
    public abstract boolean equals(final Object o);

    /**
     * Gets a generic representation of the Item's name.
     *
     * @return A generic representation of the Item's name.
     */
    public abstract String getName();

    /**
     * Returns an integer hash code for this Item. By contract, any two objects for which {@link
     * #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * @return This Item hash code.
     * @see Object#equals(Object)
     */
    public abstract int hashCode();

    /**
     * Checks the name of this item against the name of the item in the {@code other} parameter.
     *
     * @param otherItem The other item to check the name against.
     * @return True if both names are not null and one name is equal to the other.
     */
    public boolean isNameSame(final Item<T> otherItem) {
        boolean nameExists = false;
        final String name = getName();

        if (name != null && otherItem != null) {
            nameExists = name.equals(otherItem.getName());
        }

        return nameExists;
    }

    /**
     * This returns false if the name is unknown.
     *
     * @return True if the value from {@link #getName()} is empty or null, false otherwise.
     */
    public boolean isUnknown() {
        final String name = getName();

        return name == null || name.isEmpty();
    }

    /**
     * This returns the name of the item, without any case changes.
     *
     * @return The name manipulated for sorting, without case changes.
     */
    String sortName() {
        return getName();
    }

    /**
     * This returns the name of the item, with case change for sorting.
     *
     * @return The name manipulated for sorting with lowered case change lowered.
     */
    public String sortText() {
        String name = sortName();

        if (name != null) {
            name = name.toLowerCase();
        }

        return name;
    }

    /**
     * Returns the generated name for the Item, and if null or empty the output may be translated
     * by resource, if available.
     *
     * @return If empty or null a translated "UnknownMetadata" string, if available, otherwise the
     * given name of the item.
     */
    @Override
    public String toString() {
        String mainText = getName();

        if (mainText == null || mainText.isEmpty()) {
            final String key = UNKNOWN_METADATA + getClass().getSimpleName();

            if (RESOURCE.containsKey(key)) {
                mainText = RESOURCE.getString(key);
            }
        }

        return mainText;
    }
}
