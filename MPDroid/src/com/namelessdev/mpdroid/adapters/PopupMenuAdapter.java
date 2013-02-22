package com.namelessdev.mpdroid.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PopupMenuAdapter extends ArrayAdapter<PopupMenuItem> {

	Context context;
	int layoutResourceId;
	PopupMenuItem data[] = null;

	public PopupMenuAdapter(Context context, int layoutResourceId, PopupMenuItem[] data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;

		if (view == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			view = inflater.inflate(layoutResourceId, parent, false);
		}

		PopupMenuItem pItem = data[position];
		TextView text = (TextView) view.findViewById(android.R.id.text1);
		text.setText(pItem.textId);
		return view;
	}
}

