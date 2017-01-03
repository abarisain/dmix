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

package com.namelessdev.mpdroid.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.item.Genre;
import com.anpmech.mpd.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GenresGroup extends Item<GenresGroup> {

    /**
     * This field is used to instantiate this class from a {@link Parcel}.
     */
    public static final Creator<GenresGroup> CREATOR = new GenresGroupParcelCreator();

    /**
     * This is a convenience string to use as a Intent extra tag.
     */
    public static final String EXTRA = "Genres";

    private final String name;

    private Set<Genre> genres = new HashSet<>();

    public GenresGroup(final String name) {
        assert name != null;
        this.name = name;
    }

    public void addGenre(final Genre genre) {
        genres.add(genre);
    }

    public Set<Genre> getGenres() {
        return Collections.unmodifiableSet(genres);
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(name);
        dest.writeTypedList(new ArrayList<>(genres));
    }

    @Override
    public boolean equals(final Object o) {
        return o != null && (o == this || o.getClass() == this.getClass() && name.equals(((GenresGroup) o).name));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return !name.isEmpty() ? name : getGenres().iterator().next().toString();
    }

    /**
     * This class is used to instantiate a Genre Object from a {@code Parcel}.
     */
    private static final class GenresGroupParcelCreator implements Parcelable.Creator<GenresGroup> {

        @Override
        public GenresGroup createFromParcel(final Parcel source) {
            final GenresGroup genresGroup = new GenresGroup(source.readString());
            source.readParcelable(ResponseObject.LOADER);
            final List<Genre> genres = new ArrayList<>();
            source.readTypedList(genres, Genre.CREATOR);
            genresGroup.genres = new HashSet<>(genres);
            return genresGroup;
        }

        @Override
        public GenresGroup[] newArray(final int size) {
            return new GenresGroup[size];
        }
    }

}