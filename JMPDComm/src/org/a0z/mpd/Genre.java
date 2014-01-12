package org.a0z.mpd;

import android.os.Parcel;
import android.os.Parcelable;

public class Genre extends Item implements Parcelable {

	private final String name;
	private final String sort;

	// private final boolean isVa;

	public Genre(String name) {
		this.name = name;
		sort = null;
	}

	protected Genre(Parcel in) {
		this.name = in.readString();
		this.sort = in.readString();
	}

	public String getName() {
		return name;
	}

    /*
     * text for display
     */
    @Override
    public String mainText() {
        return (name.equals("") ?
                MPD.getApplicationContext().getString(R.string.jmpdcomm_unknown_genre) :
                name);
    }

	public String sort() {
		return null == sort ? name : sort;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof Genre) && ((Genre) o).name.equals(name);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.name);
		dest.writeString(this.sort);
	}

	public static final Parcelable.Creator<Genre> CREATOR = new Parcelable.Creator<Genre>() {
		public Genre createFromParcel(Parcel in) {
			return new Genre(in);
		}

		public Genre[] newArray(int size) {
			return new Genre[size];
		}
	};
}
