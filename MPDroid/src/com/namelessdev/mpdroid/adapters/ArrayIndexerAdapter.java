package com.namelessdev.mpdroid.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.Item;

import android.content.Context;
import android.widget.SectionIndexer;

//Stolen from http://www.anddev.org/tutalphabetic_fastscroll_listview_-_similar_to_contacts-t10123.html
//Thanks qlimax !

public class ArrayIndexerAdapter extends ArrayAdapter implements SectionIndexer {

    HashMap<String, Integer> alphaIndexer;
    String[] sections;
    ArrayDataBinder dataBinder = null;
    Context context;

    @SuppressWarnings("unchecked")
    public ArrayIndexerAdapter(Context context, int textViewResourceId, List<? extends Item> items) {
        super(context, textViewResourceId, (List<Item>) items);
    }

    @SuppressWarnings("unchecked")
    public ArrayIndexerAdapter(Context context, ArrayDataBinder dataBinder, List<? extends Item> items) {
        super(context, dataBinder, (List<Item>) items);
    }

    @Override
    protected void init(Context context, List<? extends Item> items) {
        super.init(context, items);

        // here is the tricky stuff
        alphaIndexer = new HashMap<String, Integer>();
        // in this hashmap we will store here the positions for
        // the sections

        int size = items.size();
        for (int i = size - 1; i >= 0; i--) {
            Item element = items.get(i);
            if (element.sort().length() > 0) {
                alphaIndexer.put(element.sort().substring(0, 1).toUpperCase(), i);
            } else {
                alphaIndexer.put("",i); // "Unknown" item
            }
            //We store the first letter of the word, and its index.
            //The Hashmap will replace the value for identical keys are putted in
        }

        // now we have an hashmap containing for each first-letter
        // sections(key), the index(value) in where this sections begins

        // we have now to build the sections(letters to be displayed)
        // array .it must contains the keys, and must (I do so...) be
        // ordered alphabetically

        ArrayList<String> keyList = new ArrayList<String>(alphaIndexer.keySet()); // list can be sorted
        Collections.sort(keyList);

        sections = new String[keyList.size()]; // simple conversion to an array of object
        keyList.toArray(sections);
    }

    @Override
    public int getPositionForSection(int section) {
        String letter = sections[section >= sections.length ? sections.length - 1 : section];
        return alphaIndexer.get(letter);
    }

    @Override
    public int getSectionForPosition(int position) {
        if (sections.length == 0)
            return -1;

        if (sections.length == 1)
            return 1;

        for (int i = 0; i < (sections.length - 1); i++) {
            int begin = alphaIndexer.get(sections[i]);
            int end = alphaIndexer.get(sections[i + 1]) - 1;
            if (position >= begin && position <= end)
                return i;
        }
        return sections.length - 1;
    }

    @Override
    public Object[] getSections() {
        return sections;
    }

}
