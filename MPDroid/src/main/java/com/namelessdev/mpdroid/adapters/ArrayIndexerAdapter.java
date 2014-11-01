/*
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.adapters;

import org.a0z.mpd.item.Item;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.widget.SectionIndexer;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

//Stolen from http://www.anddev.org/tutalphabetic_fastscroll_listview_-_similar_to_contacts-t10123.html
//Thanks qlimax !

public class ArrayIndexerAdapter extends ArrayAdapter implements SectionIndexer {

    private static final Comparator<String> LOCALE_COMPARATOR = new LocaleComparator();

    private final HashMap<String, Integer> mAlphaIndexer;

    private final String[] mSections;

    public ArrayIndexerAdapter(final Context context, final ArrayDataBinder dataBinder,
            final List<? extends Item> items) {
        super(context, dataBinder, items);

        // in this HashMap we will store here the positions for the sections
        mAlphaIndexer = new HashMap<>();
        mSections = init(items);
    }

    public ArrayIndexerAdapter(final Context context, @LayoutRes final int textViewResourceId,
            final List<? extends Item> items) {
        super(context, textViewResourceId, items);

        // in this HashMap we will store here the positions for the sections
        mAlphaIndexer = new HashMap<>();
        mSections = init(items);
    }

    @Override
    public int getPositionForSection(final int sectionIndex) {
        final String letter;

        if (sectionIndex >= mSections.length) {
            letter = mSections[mSections.length - 1];
        } else {
            letter = mSections[sectionIndex];
        }

        return mAlphaIndexer.get(letter);
    }

    @Override
    public int getSectionForPosition(final int position) {
        Integer section = null;

        if (mSections.length == 0) {
            section = Integer.valueOf(-1);
        } else if (mSections.length == 1) {
            section = Integer.valueOf(1);
        } else {
            for (int i = 0; i < mSections.length - 1; i++) {
                final int begin = mAlphaIndexer.get(mSections[i]);
                final int end = mAlphaIndexer.get(mSections[i + 1]) - 1;
                if (position >= begin && position <= end) {
                    section = Integer.valueOf(i);
                    break;
                }
            }

            if (section == null) {
                section = Integer.valueOf(mSections.length - 1);
            }
        }

        return section.intValue();
    }

    @Override
    public Object[] getSections() {
        return mSections.clone();
    }

    private String[] init(final List<? extends Item> items) {
        final String[] sections;

        // here is the tricky stuff
        final int size = items.size();
        int unknownPos = -1; // "Unknown" item
        for (int i = size - 1; i >= 0; i--) {
            final Item element = items.get(i);
            final String sorted = element.sortText();

            if (sorted.isEmpty()) {
                unknownPos = i; // save position
            } else {
                mAlphaIndexer.put(sorted.substring(0, 1).toUpperCase(), i);
            }
            /**
             * We store the first letter of the word, and its index. The HashMap will replace the
             * value for identical keys are putted in.
             */
        }

        /**
         * Now we have an HashMap containing for each first-letter sections(key), the index(value)
         * in where this sections begins
         */

        /**
         * We have now to built the sections (letters to be displayed) array. This array must
         * contain the keys, and must (I do so...) be ordered alphabetically.
         */
        final ArrayList<String> keyList = new ArrayList<>(mAlphaIndexer.keySet());
        // list can be sorted
        Collections.sort(keyList, LOCALE_COMPARATOR);

        // add "Unknown" at the end after sorting
        if (unknownPos >= 0) {
            mAlphaIndexer.put("", unknownPos);
            keyList.add("");
        }

        sections = new String[keyList.size()]; // simple conversion to an array of object
        keyList.toArray(sections);

        return sections;
    }

    /**
     * Locale-aware comparator
     */
    @SuppressWarnings("ComparatorNotSerializable")
    private static class LocaleComparator implements Comparator<String> {

        @Override
        public int compare(final String lhs, final String rhs) {
            return Collator.getInstance().compare(lhs, rhs);
        }
    }
}