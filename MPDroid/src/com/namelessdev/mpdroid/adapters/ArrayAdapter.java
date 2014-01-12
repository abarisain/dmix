
package com.namelessdev.mpdroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;

import org.a0z.mpd.Item;

import java.util.ArrayList;
import java.util.List;

//Stolen from http://www.anddev.org/tutalphabetic_fastscroll_listview_-_similar_to_contacts-t10123.html
//Thanks qlimax !

public class ArrayAdapter extends android.widget.ArrayAdapter<Item> {
    private static final int TYPE_DEFAULT = 0;

    ArrayDataBinder dataBinder = null;
    LayoutInflater inflater;
    List<Item> items;
    Context context;

    @SuppressWarnings("unchecked")
    public ArrayAdapter(Context context, ArrayDataBinder dataBinder, List<? extends Item> items) {
        super(context, 0, (List<Item>) items);
        this.dataBinder = dataBinder;
        init(context, items);
    }

    @SuppressWarnings("unchecked")
    public ArrayAdapter(Context context, int textViewResourceId, List<? extends Item> items) {
        super(context, textViewResourceId, (List<Item>) items);
        dataBinder = null;
        init(context, items);
    }

    public ArrayDataBinder getDataBinder() {
        return dataBinder;
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_DEFAULT;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (dataBinder == null) {
            return super.getView(position, convertView, parent);
        }

        // cache all inner view references with ViewHolder pattern
        AbstractViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(dataBinder.getLayoutId(), parent, false);
            convertView = dataBinder.onLayoutInflation(context, convertView, items);

            // use the databinder to look up all references to inner views
            holder = dataBinder.findInnerViews(convertView);
            convertView.setTag(holder);
        } else {
            holder = (AbstractViewHolder) convertView.getTag();
        }

        dataBinder.onDataBind(context, convertView, holder, items, items.get(position), position);
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    protected void init(Context context, List<? extends Item> items) {
        if (!(items instanceof ArrayList<?>))
            throw new RuntimeException("Items must be contained in an ArrayList<Item>");

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
        this.items = (List<Item>) items;
    }

    @Override
    public boolean isEnabled(int position) {
        if (dataBinder == null) {
            return super.isEnabled(position);
        }
        return dataBinder.isEnabled(position, items, getItem(position));
    }

    public void setDataBinder(ArrayDataBinder dataBinder) {
        this.dataBinder = dataBinder;
    }

}
