package com.namelessdev.mpdroid.views;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.SeparatedListDataBinder;
import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Music;

import java.util.List;

public class SearchResultDataBinder implements SeparatedListDataBinder {

    public static final String SEPARATOR = " - ";

    public void onDataBind(Context context, View targetView, List<Object> items, Object item, int position) {
        final TextView text1 = (TextView) targetView.findViewById(R.id.text1);
        String formattedResult = "";

        if (item instanceof Music) {
            Music music;
            music = (Music) item;
            formattedResult = join(music.getTitle(), music.getAlbum(), music.getArtist());
        } else if (item instanceof Artist) {
            formattedResult = (((Artist) item).getName());
        } else if (item instanceof Album) {
            Album album;
            album = (Album) item;
            formattedResult = join(album.getName(), album.getArtist());
        }
        text1.setText(formattedResult);
    }

    /**
     * Join not empty strings
     *
     * @param parts : parts to join
     * @return the formatted result
     */
    public static String join(String... parts) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part != null && part.length() > 0) {
                result.append(part);
                if (SEPARATOR != null && i < parts.length - 1) {
                    result.append(SEPARATOR);
                }
            }
        }
        return result.toString();
    }

    public boolean isEnabled(int position, List<Object> items, Object item) {
        return true;
    }

}
