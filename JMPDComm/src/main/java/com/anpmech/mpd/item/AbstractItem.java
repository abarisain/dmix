/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2016 The MPDroid Project
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
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class is the generic base for all Item Objects, a Object representation from the MPD
 * protocol, abstracted for backend.
 *
 * @param <T> The Item type.
 */
abstract class AbstractItem<T extends AbstractItem<T>> implements Comparable<T> {

    /**
     * This {@link Collator} is used for comparison and sorting of {@code Item}s.
     */
    static final Collator COLLATOR = Collator.getInstance();

    /**
     * The ResourceBundle from which to retrieve any necessary translations.
     */
    static final ResourceBundle RESOURCE;

    /**
     * The bundle name for the "Unknown" or empty metadata for an item.
     */
    static final String UNKNOWN_METADATA = "UnknownMetadata";

    static {
        COLLATOR.setStrength(Collator.PRIMARY);
        RESOURCE = ResourceBundle.getBundle(UNKNOWN_METADATA);
    }

    AbstractItem() {
        super();
    }

    /**
     * This method merges two Item type lists into {@code list1} and removes unknown items and
     * items of the same name.
     *
     * @param list1 The first list to merge, the list which will be merged into.
     * @param list2 The second list to merge.
     * @param <T>   Anything that extends an AbstractItem.
     */
    public static <T extends AbstractItem<T>> void merge(final List<T> list1, final List<T> list2) {
        list1.addAll(list2);
        Collections.sort(list1);

        for (int i = list1.size()-1; i>= 0; i--) {
            final T item = list1.get(i);
            if (item.isUnknown() || i>0 && item.isNameSame(list1.get(i-1))) {
                list1.remove(i);
            }
        }
    }

    /**
     * Compares this object to the specified object to determine their relative
     * order.
     *
     * @param another the object to compare to this instance.
     * @return a negative integer if this instance is less than {@code another};
     * a positive integer if this instance is greater than
     * {@code another}; 0 if this instance has the same order as
     * {@code another}.
     * @throws ClassCastException if {@code another} cannot be converted into something
     *                            comparable to {@code this} instance.
     */
    @Override
    public int compareTo(final T another) {
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
    public boolean isNameSame(final AbstractItem<T> otherItem) {
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
            name = name.toLowerCase(Locale.getDefault());
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
