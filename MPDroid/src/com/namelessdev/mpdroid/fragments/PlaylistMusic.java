package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Directory;
import org.a0z.mpd.Item;
import org.a0z.mpd.Music;

public class PlaylistMusic {

    public PlaylistMusic(Music music) {
        this.music = music;
    }

    private int play;
    private boolean marked;

    public int getPlay() {
        return play;
    }

    public void setPlay(int play) {
        this.play = play;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public Music getMusic() {
        return music;
    }

    private final Music music;

    public String mainText() {
        return music.mainText();
    }

    public String subText() {
        return music.subText();
    }

    public String sort() {
        return music.sort();
    }

    public int compareTo(Item o) {
        return music.compareTo(o);
    }

    public String toString() {
        return music.toString();
    }

    public String getAlbum() {
        return music.getAlbum();
    }

    public String getArtist() {
        return music.getArtist();
    }

    public String getAlbumArtist() {
        return music.getAlbumArtist();
    }

    public String getFilename() {
        return music.getFilename();
    }

    public String getFullpath() {
        return music.getFullpath();
    }

    public boolean isStream() {
        return music.isStream();
    }

    public int getSongId() {
        return music.getSongId();
    }

    public int getPos() {
        return music.getPos();
    }

    public String getPath() {
        return music.getPath();
    }

    public long getTime() {
        return music.getTime();
    }

    public boolean haveTitle() {
        return music.haveTitle();
    }

    public String getTitle() {
        return music.getTitle();
    }

    public String getName() {
        return music.getName();
    }

    public int getTotalTracks() {
        return music.getTotalTracks();
    }

    public int getTrack() {
        return music.getTrack();
    }

    public String getFormatedTime() {
        return music.getFormatedTime();
    }

    public int getDisc() {
        return music.getDisc();
    }

    public long getDate() {
        return music.getDate();
    }

    public String getParent() {
        return music.getParent();
    }

    public void setAlbum(String string) {
        music.setAlbum(string);
    }

    public void setArtist(String string) {
        music.setArtist(string);
    }

    public void setTime(long l) {
        music.setTime(l);
    }

    public void setTitle(String string) {
        music.setTitle(string);
    }

    public void setTotalTracks(int total) {
        music.setTotalTracks(total);
    }

    public void setTrack(int num) {
        music.setTrack(num);
    }

    public void setDisc(int value) {
        music.setDisc(value);
    }

    public void setDate(long value) {
        music.setDate(value);
    }

    public void setParent(Directory directory) {
        music.setParent(directory);
    }

    public void setSongId(int value) {
        music.setSongId(value);
    }

    public int getMedia() {
        return music.getMedia();
    }

    public String getAlbumartist() {
        return music.getAlbumartist();
    }

    public void setAlbumartist(String albumartist) {
        this.music.setAlbumartist(albumartist);
    }


}
