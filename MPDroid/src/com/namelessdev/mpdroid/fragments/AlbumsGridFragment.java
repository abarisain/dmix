package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Artist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayAdapter;
import com.namelessdev.mpdroid.views.AlbumGridDataBinder;

public class AlbumsGridFragment extends AlbumsFragment {

    // Minimum number of songs in the queue before the fastscroll thumb is shown
    private static final int MIN_ITEMS_BEFORE_FASTSCROLL = 6;

    public AlbumsGridFragment(Artist artist) {
        super(artist);
        isCountPossiblyDisplayed = false;
    }

    public AlbumsGridFragment() {
        this(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browsegrid, container, false);
        list = (GridView) view.findViewById(R.id.grid);
        registerForContextMenu(list);
        list.setOnItemClickListener(this);
        loadingView = view.findViewById(R.id.loadingLayout);
        loadingTextView = (TextView) view.findViewById(R.id.loadingText);
        noResultView = view.findViewById(R.id.noResultLayout);
        loadingTextView.setText(getLoadingText());
        coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshFastScrollStyle();
    }

    @Override
    protected ListAdapter getCustomListAdapter() {
        if (items != null) {
            return new ArrayAdapter(getActivity(),
                    new AlbumGridDataBinder(app, artist == null ? null : artist.getName(), app.isLightThemeSelected()), items);
        }
        return super.getCustomListAdapter();
    }

    @Override
    public void updateFromItems() {
        super.updateFromItems();
        refreshFastScrollStyle();
    }

    private void refreshFastScrollStyle() {
        if (items != null && items.size() >= MIN_ITEMS_BEFORE_FASTSCROLL) {
            // No need to enable FastScroll, this setter enables it.
            list.setFastScrollAlwaysVisible(true);
            list.setScrollBarStyle(AbsListView.SCROLLBARS_INSIDE_INSET);
        } else {
            list.setFastScrollAlwaysVisible(false);
            list.setFastScrollEnabled(false);
            // Matches the XML style
            list.setScrollBarStyle(AbsListView.SCROLLBARS_OUTSIDE_OVERLAY);
        }
    }
}
