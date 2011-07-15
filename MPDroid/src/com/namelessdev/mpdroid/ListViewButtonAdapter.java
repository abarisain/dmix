package com.namelessdev.mpdroid;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class ListViewButtonAdapter<T> extends ArrayAdapter<T> {
	private final Context m_Context;
	private LayoutInflater inflater;

	public ListViewButtonAdapter(Context context, int textViewResourceId) {
		this(context, 0, textViewResourceId, (T[]) null);
	}

	public ListViewButtonAdapter(Context context, int resource, int textViewResourceId) {
		this(context, resource, textViewResourceId, (T[]) null);
	}

	public ListViewButtonAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
		super(context, resource, textViewResourceId, objects);
		m_Context = context;
	}

	public ListViewButtonAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
		super(context, resource, textViewResourceId, objects);
		m_Context = context;
	}

	public ListViewButtonAdapter(Context context, int textViewResourceId, List<T> objects) {
		this(context, 0, textViewResourceId, objects);
	}

	public ListViewButtonAdapter(Context context, int textViewResourceId, T[] objects) {
		this(context, 0, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (inflater == null) {
			inflater = (LayoutInflater) m_Context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.artist_row, null);
		}
		((TextView) convertView).setText(getItem(position).toString());
		return convertView;
	}
}
