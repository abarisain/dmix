package org.a0z.mpd;

import android.os.Parcel;

public class UnknownArtist extends Artist {

    public static final UnknownArtist instance = new UnknownArtist();

    private UnknownArtist() {
        super(MPD.getApplicationContext().getString(R.string.jmpdcomm_unknown_artist), 0);
    }

    protected UnknownArtist(Parcel in) {
        super(in);
    }

    @Override
    public String subText() {
        return "";
    }

    public static final Creator<UnknownArtist> CREATOR =
            new Creator<UnknownArtist>() {
                public UnknownArtist createFromParcel(Parcel in) {
                    return new UnknownArtist(in);
                }

                public UnknownArtist[] newArray(int size) {
                    return new UnknownArtist[size];
                }
            };

}
