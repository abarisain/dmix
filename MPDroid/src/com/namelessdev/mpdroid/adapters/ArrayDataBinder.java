
package com.namelessdev.mpdroid.adapters;

import android.content.Context;
import android.view.View;

import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;

import org.a0z.mpd.Item;

import java.util.List;

public interface ArrayDataBinder {
    public AbstractViewHolder findInnerViews(View targetView);

    public int getLayoutId();

    public boolean isEnabled(int position, List<? extends Item> items, Object item);

    public void onDataBind(Context context, View targetView, AbstractViewHolder viewHolder,
            List<? extends Item> items, Object item, int position);

    public View onLayoutInflation(Context context, View targetView, List<? extends Item> items);
}
