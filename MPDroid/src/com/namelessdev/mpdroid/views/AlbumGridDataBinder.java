package com.namelessdev.mpdroid.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
import com.namelessdev.mpdroid.helpers.CoverManager;
import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;
import org.a0z.mpd.Album;
import org.a0z.mpd.Item;

import java.util.List;

public class AlbumGridDataBinder extends AlbumDataBinder {
    SharedPreferences settings;

    public AlbumGridDataBinder(MPDApplication app, String artist, boolean isLightTheme) {
        super(app, artist, isLightTheme);
        settings = PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Override
    public void onDataBind(final Context context, final View targetView, final AbstractViewHolder viewHolder, List<? extends Item> items, Object item, int position) {
        AlbumViewHolder holder = (AlbumViewHolder) viewHolder;

        final Album album = (Album) item;

        // Caching must be switch on to use this view
        final CoverAsyncHelper coverHelper = new CoverAsyncHelper(app, settings);
        final int height = holder.albumCover.getHeight();
        // If the list is not displayed yet, the height is 0. This is a problem, so set a fallback one.
        coverHelper.setCoverMaxSize(height == 0 ? 256 : height);

        // display the album title
        holder.albumName.setText(album.getName());

        // listen for new artwork to be loaded
        final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(context, holder.albumCover, holder.coverArtProgress, lightTheme, false);
        final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) holder.albumCover.getTag(R.id.AlbumCoverDownloadListener);
        if (oldAcd != null)
            oldAcd.detach();
        holder.albumCover.setTag(R.id.AlbumCoverDownloadListener, acd);
        holder.albumCover.setTag(R.id.CoverAsyncHelper, coverHelper);
        coverHelper.addCoverDownloadListener(acd);

        loadPlaceholder(coverHelper);

        // Can't get artwork for missing album name
        if (CoverManager.isValidArtistOrAlbum(album.getArtist().getName()) && CoverManager.isValidArtistOrAlbum(album.getName()) && enableCache) {
            holder.albumCover.setTag(album.getAlbumInfo().getKey());
            loadArtwork(coverHelper, album.getAlbumInfo());
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.album_grid_item;
    }

}
