package org.a0z.mpd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Felipe Gustavo de Almeida, Stefan Agner
 * 
 */
public class MusicList {

    private HashMap<Integer, Music> map;

    private ArrayList<Music> list;

    /**
     * Constructor.
     */
    public MusicList() {
        map = new HashMap<Integer, Music>();
        list = new ArrayList<Music>();
    }

    /**
     * Constructs a new <code>MusicList</code> containing all musics from <code>list</code>.
     * @param list a <code>MusicList</code>
     */
    public MusicList(MusicList list) {
        this();
        this.addAll(list.getMusics());
    }

    /**
     * Retrieves a List containing all musics from this <code>MusicList</code>.
     * @return Retrieves a List containing all musics from this <code>MusicList</code>.
     */
    public List<Music> getMusics() {
        return list;
    }

    /**
     * Adds music to <code>MusicList</code>.
     * @param music music to be added.
     */
    public void add(Music music) {
        map.put(new Integer(music.getSongId()), music);
        while (list.size() < (music.getPos() + 1)) {
            list.add(null);
        }
        list.set(music.getPos(), music);

    }

    /**
     * Removes all musics from this <code>MusicList</code>.
     */
    public void clear() {
        list.clear();
        map.clear();
    }

    /**
     * Retrieves a music by its songId.
     * @param songId songId from the music to be retrieved.
     * @return a Music with given songId or <code>null</code> if it is not present on this <code>MusicList</code>.
     */
    public Music getById(int songId) {
        return map.get(new Integer(songId));
    }

    /**
     * Retrieves a music by its positon on playlist.
     * @param index position of the music to be retrieved.
     * @return a Music with given position or <code>null</code> if it is not present on this <code>MusicList</code>.
     */
    public Music getByIndex(int index) {
        return list.get(index);
    }

    /**
     * Adds all Musics from <code>playlist</code> to this <code>MusicList</code>.
     * @param playlist <code>Collection</code> of <code>Music</code> to be added to this <code>MusicList</code>.
     * @throws ClassCastException when <code>playlist</code> contains elements not asignable to <code>Music</code>.
     */
    public void addAll(List<Music> playlist) throws ClassCastException {
    	list.addAll(playlist);
    }

    /**
     * Removes music at <code>position</code> from this <code>MusicList</code>, if it is present.
     * @param position position of the <code>Music</code> to be removed from this <code>MusicList</code>.
     */
    public void removeByPosition(int position) {
        Music music = (Music) list.get(position);
        if (music != null) {
            list.remove(position);
            map.remove(new Integer(music.getSongId()));
        }
    }

    /**
     * Remove music with given <code>songId</code> from this <code>MusicList</code>, if it is present.
     * @param songId songId of the <code>Music</code> to be removed from this <code>MusicList</code>.
     */
    public void removeBySongId(int songId) {
        Music music = (Music) map.get(new Integer(songId));
        if (music != null) {
            map.remove(new Integer(songId));
            for(int i = 0; i<list.size();i++)
            {
            	Music m = (Music)list.get(i);
            	if(m.getSongId() == songId)
            	{
            		list.remove(i);
            		return;
            	}
            }
            // list.remove(music.getPos()); //This can fail if Playlist is out of sync...
        }
    }

    /**
     * Retrieves this <code>MusicList</code> size.
     * @return <code>MusicList</code> size.
     */
    public int size() {
        return list.size();
    }

    /**
     * Retrieves a <code>List</code> with selected slice from this <code>MusicList</code>.
     * @param fromIndex first index (included).
     * @param toIndex last index (not included).
     * @return a <code>List</code> with selected slice from this <code>MusicList</code>.
     * @see List#subList(int, int)
     */
    public List<Music> subList(int fromIndex, int toIndex) {
        return this.list.subList(fromIndex, toIndex);
    }
}