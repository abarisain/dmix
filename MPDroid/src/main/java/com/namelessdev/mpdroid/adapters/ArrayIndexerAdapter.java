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
import android.widget.SectionIndexer;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

//Stolen from http://www.anddev.org/tutalphabetic_fastscroll_listview_-_similar_to_contacts-t10123.html
//Thanks qlimax !

public class ArrayIndexerAdapter extends ArrayAdapter implements SectionIndexer {

    static final Comparator localeComp = new LocaleComparator();

    HashMap<String, Integer> alphaIndexer;

    Context context;

    ArrayDataBinder dataBinder = null;

    String[] sections;

    @SuppressWarnings("unchecked")
    public ArrayIndexerAdapter(Context context, ArrayDataBinder dataBinder,
            List<? extends Item> items) {
        super(context, dataBinder, items);
    }

    @SuppressWarnings("unchecked")
    public ArrayIndexerAdapter(Context context, int textViewResourceId,
            List<? extends Item> items) {
        super(context, textViewResourceId, items);
    }

    @Override
    public int getPositionForSection(int section) {
        String letter = sections[section >= sections.length ? sections.length - 1 : section];
        return alphaIndexer.get(letter);
    }

    @Override
    public int getSectionForPosition(int position) {
        if (sections.length == 0) {
            return -1;
        }

        if (sections.length == 1) {
            return 1;
        }

        for (int i = 0; i < (sections.length - 1); i++) {
            int begin = alphaIndexer.get(sections[i]);
            int end = alphaIndexer.get(sections[i + 1]) - 1;
            if (position >= begin && position <= end) {
                return i;
            }
        }
        return sections.length - 1;
    }

    @Override
    public Object[] getSections() {
        return sections;
    }

    @Override
    protected void init(Context context, List<? extends Item> items) {
        super.init(context, items);

        // here is the tricky stuff
        alphaIndexer = new HashMap<String, Integer>();
        // in this hashmap we will store here the positions for
        // the sections

        int size = items.size();
        int unknownPos = -1; // "Unknown" item
        for (int i = size - 1; i >= 0; i--) {
            Item element = items.get(i);
            if (element.sortText().length() > 0) {
                alphaIndexer.put(element.sortText().substring(0, 1).toUpperCase(), i);
            } else {
                unknownPos = i; // save position
            }
            // We store the first letter of the word, and its index.
            // The Hashmap will replace the value for identical keys are putted
            // in
        }

        // now we have an hashmap containing for each first-letter
        // sections(key), the index(value) in where this sections begins

        // we have now to build the sections(letters to be displayed)
        // array .it must contains the keys, and must (I do so...) be
        // ordered alphabetically

        ArrayList<String> keyList = new ArrayList<String>(alphaIndexer.keySet()); // list
        // can
        // be
        // sorted
        Collections.sort(keyList, localeComp);

        // add "Unknown" at the end after sorting
        if (unknownPos >= 0) {
            alphaIndexer.put("", unknownPos);
            keyList.add("");
        }

        sections = new String[keyList.size()]; // simple conversion to an array
        // of object
        keyList.toArray(sections);
    }

}

/**
 * Locale-aware comparator
 */
class LocaleComparator implements Comparator {

    static final Collator defaultCollator = Collator.getInstance(Locale.getDefault());

    public int compare(Object str1, Object str2) {
        return defaultCollator.compare((String) str1, (String) str2);
    }
}
