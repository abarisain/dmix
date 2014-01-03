package com.namelessdev.mpdroid.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.a0z.mpd.Music;

public class MusicParcelable extends Music implements Parcelable {
//    private boolean mIsNull;

    /**
     * Public constructor, used to encapsulate a {@link org.a0z.mpd.Music} into this {@link android.os.Parcelable}
     *
     * @param music The target {@link org.a0z.mpd.Music} object
     */
    public MusicParcelable(Music music) {
        super(music.getAlbum(), music.getArtist(), music.getAlbumArtist(), music.getFullpath(), music.getDisc(),
                music.getDate(), music.getTime(), music.getParentDirectory(), music.getTitle(), music.getTotalTracks(),
                music.getTrack(), music.getSongId(), music.getPos(), music.getName());
//        mIsNull = music == null;
//        if (!mIsNull) {
//            setAlbum(music.getAlbum());
//            setAlbumArtist(music.getAlbumArtist());
//            setArtist(music.getArtist());
//            setDate(music.getDate());
//            setDisc(music.getDisc());
//            setParent(music.getParentDirectory());
//            setSongId(music.getSongId());
//            setTime(music.getTime());
//            setTitle(music.getTitle());
//            setTotalTracks(music.getTotalTracks());
//            setTrack(music.getTrack());
//        }
    }

    /**
     * Protected constructor, used by the Android framework when reconstructing the object from a {@link android.os.Parcel}<br />
     * This constructor will instantiate the object through the default {@link org.a0z.mpd.Music#Music(String, String, String, String, int, long, long, org.a0z.mpd.Directory, String, int, int, int, int, String)} constructor.
     *
     * @param in The {@link android.os.Parcel} that contains our object
     */
    protected MusicParcelable(Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readString(), in.readInt(), in.readLong(), in.readLong(), null, in.readString(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
//        if (mIsNull) {
//            dest.writeInt(0);
//        } else {
//            dest.writeInt(1);
            dest.writeString(getAlbum());
            dest.writeString(getArtist());
            dest.writeString(getAlbumArtist());
            dest.writeString(getFullpath());
            dest.writeInt(getDisc());
            dest.writeLong(getDate());
            dest.writeLong(getTime());
            //dest.writeString(getParent()); // TODO: is it used?
            dest.writeString(getTitle());
            dest.writeInt(getTotalTracks());
            dest.writeInt(getTrack());
            dest.writeInt(getSongId());
            dest.writeInt(getPos());
            dest.writeString(getName());
//        }
    }

    public static final Parcelable.Creator<MusicParcelable> CREATOR = new Parcelable.Creator<MusicParcelable>() {
        public MusicParcelable createFromParcel(Parcel in) {
//            if (in.readInt() > 0) {
                return new MusicParcelable(in);
//            }
//            return null;
        }

        public MusicParcelable[] newArray(int size) {
            return new MusicParcelable[size];
        }
    };
}
