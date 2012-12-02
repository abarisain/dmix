package com.namelessdev.mpdroid.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;

/**
 * 
 * 
 * List adapter which uses an object list.
 * If the object is an instance of String, the list will display a separator.
 * If the object is of any other type, the binder will be called.
 * The binder should do what getView does when you extend BaseAdapter (except that you never inflate the view yourself)
 * 
 * There are many other implementations of this list on the internet,
 * this one has a lot of restrictions (which makes it simplier), but handles the separators so that you always get the right line number
 * when you select a line.
 *
 * The separator needs to have a TextView named "separator_title".
 *
 */

public class SeparatedListAdapter extends BaseAdapter {
	private static final int TYPE_CONTENT = 0;
	private static final int TYPE_SEPARATOR = 1;
	
	private List<Object> items; //Content
	private SeparatedListDataBinder binder; //The content -> view 'binding'
	private int viewId = -1; //The view to be displayed
	private LayoutInflater inflater;
	private Context context;
	private int separatorLayoutId;
	
	public SeparatedListAdapter(Context context, int viewId, SeparatedListDataBinder binder, List<Object> items) {
		init(context, viewId, R.layout.list_separator, binder, items);
	}
	
	public SeparatedListAdapter(Context context, int viewId, int separatorViewId, SeparatedListDataBinder binder, List<Object> items) {
		init(context, viewId, separatorViewId, binder, items);
	}
	
	private void init(Context context, int viewId, int separatorViewId, SeparatedListDataBinder binder, List<Object> items) {
		this.viewId = viewId;
		this.binder = binder;
		this.items = items;
		this.context = context;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		separatorLayoutId = separatorViewId;
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		if(items.get(position) instanceof String) {
			return TYPE_SEPARATOR;
		}
		return TYPE_CONTENT;
	}
	
	public int getCount() {
		return items.size();
	}

	public Object getItem(int position) {
		return items.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean areAllItemsEnabled(){
		return false;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		int itemType = getItemViewType(position);
		
		if(convertView == null) {
			if(itemType == TYPE_SEPARATOR) {
				convertView = inflater.inflate(separatorLayoutId,parent,false);
			} else {
				convertView = inflater.inflate(viewId,parent,false);
			}
		}
		
		if(itemType == TYPE_SEPARATOR) {
			((TextView) convertView.findViewById(R.id.separator_title)).setText((String) items.get(position));
		} else {
			binder.onDataBind(context, convertView, items, items.get(position), position);
		}
		
		return convertView;
	}

	
	@Override
	public boolean isEnabled(int position) {
		if(getItemViewType(position) == TYPE_SEPARATOR) {
			return false;
		}
		return binder.isEnabled(position, items, getItem(position));
	}
	
}
