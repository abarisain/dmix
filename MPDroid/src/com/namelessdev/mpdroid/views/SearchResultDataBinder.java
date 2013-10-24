package com.namelessdev.mpdroid.views;

import java.util.List;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Music;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.SeparatedListDataBinder;

public class SearchResultDataBinder implements SeparatedListDataBinder {

    public static final String SEPARATOR = " - ";

    public void onDataBind(Context context, View targetView, List<Object> items, Object item, int position) {
		final TextView text1 = (TextView) targetView.findViewById(R.id.line1);
		final TextView text2 = (TextView) targetView.findViewById(R.id.line2);
		String formattedResult1 = "";
		String formattedResult2 = null;

        if (item instanceof Music) {
            Music music;
            music = (Music) item;
			formattedResult1 = music.getTitle();
			formattedResult2 = join(music.getAlbum(), music.getArtist());
        } else if (item instanceof Artist) {
			formattedResult1 = (((Artist) item).getName());
        } else if (item instanceof Album) {
            Album album;
            album = (Album) item;
			formattedResult1 = album.getName();
			formattedResult2 = album.getArtist();
        }
		text1.setText(formattedResult1);
		text2.setVisibility(formattedResult2 != null ? View.VISIBLE : View.GONE);
		text2.setText(formattedResult2);
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
