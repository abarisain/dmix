/*
 * Copyright (C) 2010-2016 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.fragments;

import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import com.anpmech.mpd.exception.MPDException;
import com.anpmech.mpd.item.Album;
import com.namelessdev.mpdroid.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends AlbumsGridFragment {

    @Override
    protected List<Album> loadAlbums(boolean sortByYear) throws IOException, MPDException {
        return new ArrayList<>(mApp.getFavorites().getAlbums());
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuItem removeFromFavoritesItem = menu.add(POPUP_REMOVE_FROM_FAVORITES,
                POPUP_REMOVE_FROM_FAVORITES, 0, R.string.removeFromFavorites);
        removeFromFavoritesItem.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        boolean result = false;

        switch (item.getGroupId()) {
            case POPUP_REMOVE_FROM_FAVORITES:
                removeFromFavorites(item);
                break;
            default:
                result = super.onMenuItemClick(item);
                break;
        }

        return result;
    }

}
