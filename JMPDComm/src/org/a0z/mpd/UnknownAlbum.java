package org.a0z.mpd;

import android.os.Parcel;
import android.os.Parcelable;

public class UnknownAlbum extends Album {

	public UnknownAlbum() {
		super("Unknown Album");
	}

	protected UnknownAlbum(Parcel in) {
		super(in);
	}

	@Override
	public String subText() {
		return "";
	}

	public static final Parcelable.Creator CREATOR =
    	new Parcelable.Creator() {
            public UnknownAlbum createFromParcel(Parcel in) {
                return new UnknownAlbum(in);
            }
 
            public UnknownAlbum[] newArray(int size) {
                return new UnknownAlbum[size];
            }
        };

}
