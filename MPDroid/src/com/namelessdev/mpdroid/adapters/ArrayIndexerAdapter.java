package com.namelessdev.mpdroid.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.a0z.mpd.Item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;

import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;

//Stolen from http://www.anddev.org/tutalphabetic_fastscroll_listview_-_similar_to_contacts-t10123.html
//Thanks qlimax !

public class ArrayIndexerAdapter extends ArrayAdapter<Item> implements SectionIndexer {
	private static final int TYPE_DEFAULT = 0;
	
	HashMap<String, Integer> alphaIndexer;
	String[] sections;
	ArrayIndexerDataBinder dataBinder = null;
	LayoutInflater inflater;
	List<Item> items;
	Context context;

	@SuppressWarnings("unchecked")
	public ArrayIndexerAdapter(Context context, int textViewResourceId, List<? extends Item> items) {
		super(context, textViewResourceId, (List<Item>) items);
		dataBinder = null;
		init(context, items);
	}

	@SuppressWarnings("unchecked")
	public ArrayIndexerAdapter(Context context, ArrayIndexerDataBinder dataBinder, List<? extends Item> items) {
		super(context, 0, (List<Item>) items);
		this.dataBinder = dataBinder;
		init(context, items);
	}
	
	@SuppressWarnings("unchecked")
	private void init(Context context, List<? extends Item> items) {
		if (!(items instanceof ArrayList<?>))
			throw new RuntimeException("Items must be contained in an ArrayList<Item>");

		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.context = context;
		this.items = (List<Item>) items;
		
		// here is the tricky stuff
		alphaIndexer = new HashMap<String, Integer>(); 
		// in this hashmap we will store here the positions for
		// the sections

		int size = items.size();
		for (int i = size - 1; i >= 0; i--) {
			Item element = items.get(i);
			alphaIndexer.put(element.sort().substring(0, 1).toUpperCase(), i);
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
	}
	
	public ArrayIndexerDataBinder getDataBinder() {
		return dataBinder;
	}
	
	public void setDataBinder(ArrayIndexerDataBinder dataBinder) {
		this.dataBinder = dataBinder;
	}
	
	@Override
	public int getPositionForSection(int section) {
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
	
	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public int getItemViewType(int position) {
		return TYPE_DEFAULT;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		if(dataBinder == null) {
			return super.getView(position, convertView, parent);
		}

		// cache all inner view references with ViewHolder pattern
		AbstractViewHolder holder;

		if(convertView == null) {
			convertView = inflater.inflate(dataBinder.getLayoutId(), parent, false);
			convertView = dataBinder.onLayoutInflation(context, convertView, items);

			// use the databinder to look up all references to inner views
			holder = dataBinder.findInnerViews(convertView);
			convertView.setTag(holder);
		}else{
			holder = (AbstractViewHolder) convertView.getTag();
		}

		dataBinder.onDataBind(context, convertView, holder, items, items.get(position), position);
		return convertView;
	}

	@Override
	public boolean isEnabled(int position) {
		if(dataBinder == null) {
			return super.isEnabled(position);
		}
		return dataBinder.isEnabled(position, items, getItem(position));
	}
	
}
