package org.a0z.mpd;

import android.os.Parcel;
import android.os.Parcelable;

public class UnknownAlbum extends Album {

	public UnknownAlbum() {
		super(MPD.getApplicationContext().getString(R.string.jmpdcomm_unknown_album), MPD.getApplicationContext().getString(R.string.jmpdcomm_unknown_artist));
	}

	protected UnknownAlbum(Parcel in) {
		super(in);
	}

	@Override
	public String subText() {
		return "";
	}

	public static final Parcelable.Creator<UnknownAlbum> CREATOR =
			new Parcelable.Creator<UnknownAlbum>() {
            public UnknownAlbum createFromParcel(Parcel in) {
                return new UnknownAlbum(in);
            }
 
            public UnknownAlbum[] newArray(int size) {
                return new UnknownAlbum[size];
            }
        };

}
