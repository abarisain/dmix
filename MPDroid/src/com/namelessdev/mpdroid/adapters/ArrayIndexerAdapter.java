package com.namelessdev.mpdroid.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

//Stolen from http://www.anddev.org/tutalphabetic_fastscroll_listview_-_similar_to_contacts-t10123.html
//Thanks qlimax !

public class ArrayIndexerAdapter<T> extends ArrayAdapter<T> implements SectionIndexer {

	ArrayList<String> elements;
	HashMap<String, Integer> alphaIndexer;

	String[] sections;

	@SuppressWarnings("unchecked")
	public ArrayIndexerAdapter(Context context, int textViewResourceId,
			List<T> objects) {
		super(context, textViewResourceId, objects);
		if(!(objects instanceof ArrayList<?>))
			throw new RuntimeException("Items must be contained in an ArrayList<String>");
		elements = (ArrayList<String>) objects;
		// here is the tricky stuff
		alphaIndexer = new HashMap<String, Integer>(); 
		// in this hashmap we will store here the positions for
		// the sections

		int size = elements.size();
		for (int i = size - 1; i >= 0; i--) {
			String element = elements.get(i);
			alphaIndexer.put(element.substring(0, 1).toUpperCase(), i); 
		//We store the first letter of the word, and its index.
		//The Hashmap will replace the value for identical keys are putted in
		} 

		// now we have an hashmap containing for each first-letter
		// sections(key), the index(value) in where this sections begins

		// we have now to build the sections(letters to be displayed)
		// array .it must contains the keys, and must (I do so...) be
		// ordered alphabetically

		Set<String> keys = alphaIndexer.keySet(); // set of letters ...sets
		// cannot be sorted...

		Iterator<String> it = keys.iterator();
		ArrayList<String> keyList = new ArrayList<String>(); // list can be
		// sorted

		while (it.hasNext()) {
			String key = it.next();
			keyList.add(key);
		}

		Collections.sort(keyList);

		sections = new String[keyList.size()]; // simple conversion to an
		// array of object
		keyList.toArray(sections);

		// ooOO00K !

	}

	@Override
	public int getPositionForSection(int section) {
		// Log.v("getPositionForSection", ""+section);
		String letter = sections[section >= sections.length ? sections.length - 1 : section];

		return alphaIndexer.get(letter);
	}

	@Override
	public int getSectionForPosition(int position) {
		if(sections.length == 0)
			return -1;
		
		if(sections.length == 1)
			return 1;
		
		for(int i = 0; i < (sections.length - 1); i ++) {
			int begin = alphaIndexer.get(sections[i]);
			int end = alphaIndexer.get(sections[i]) - 1;
			if(position >= begin && position < end)
				return i;
		}
		return sections.length - 1;
	}

	@Override
	public Object[] getSections() {

		return sections; // to string will be called each object, to display
		// the letter
	}

}
