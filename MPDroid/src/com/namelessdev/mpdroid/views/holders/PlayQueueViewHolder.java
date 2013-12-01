package com.namelessdev.mpdroid.views.holders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;

public class PlayQueueViewHolder extends AbstractViewHolder {
	public ImageView play;
	public TextView title;
	public TextView artist;
    public ImageView cover;
    public View menuButton;
    public View icon;
    public CoverAsyncHelper coverHelper;

}
