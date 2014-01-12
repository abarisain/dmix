
package com.namelessdev.mpdroid.adapters;

import android.content.Context;
import android.view.View;

import java.util.List;

public interface SeparatedListDataBinder {
    public boolean isEnabled(int position, List<? extends Object> items, Object item);

    public void onDataBind(Context context, View targetView, List<? extends Object> items,
            Object item, int position);
}
