package org.a0z.mpd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author Felipe Gustavo de Almeida, Stefan Agner
 * 
 */
public class MusicList {

	private HashMap<Integer, Music> map;

	private List<Music> list;

	/**
	 * Constructor.
	 */
	public MusicList() {
		map = new HashMap<Integer, Music>();
		list = Collections.synchronizedList(new ArrayList<Music>());
	}

	/**
	 * Constructs a new <code>MusicList</code> containing all music from <code>list</code>.
	 * 
	 * @param list
	 *           a <code>MusicList</code>
	 */
	public MusicList(MusicList list) {
		this();
		this.addAll(list.getMusic());
	}

	/**
	 * Retrieves a List containing all musics from this <code>MusicList</code>.
	 * 
	 * @return Retrieves a List containing all musics from this <code>MusicList</code>.
	 */
	public List<Music> getMusic() {
		return list;
	}

	/**
	 * Adds all Musics from <code>playlist</code> to this <code>MusicList</code>.
	 * 
	 * @param playlist
	 *           <code>Collection</code> of <code>Music</code> to be added to this <code>MusicList</code>.
	 * @throws ClassCastException
	 *            when <code>playlist</code> contains elements not asignable to <code>Music</code>.
	 */
	public void addAll(List<Music> playlist) throws ClassCastException {
		list.addAll(playlist);
	}
	
	/**
	 * Adds music to <code>MusicList</code>.
	 * 
	 * @param music
	 *           music to be added.
	 */
	public void add(Music music) {
		if (getById(music.getSongId()) != null)
			throw new IllegalArgumentException("Music is already on list");
		
		// add it to the map and at the right position to the list
		map.put(new Integer(music.getSongId()), music);
		while (list.size() < (music.getPos() + 1))
			list.add(null);
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
	 * 
	 * @param songId
	 *           songId from the music to be retrieved.
	 * @return a Music with given songId or <code>null</code> if it is not present on this <code>MusicList</code>.
	 */
	public Music getById(int songId) {
		return map.get(new Integer(songId));
	}

	/**
	 * Retrieves a music by its position on playlist.
	 * 
	 * @param index
	 *           position of the music to be retrieved.
	 * @return a Music with given position or <code>null</code> if it is not present on this <code>MusicList</code>.
	 */
	public Music getByIndex(int index) {
		if (index < 0 || list.size() <= index)
			return null;
		
		return list.get(index);
	}

	/**
	 * Remove music with given <code>songId</code> from this <code>MusicList</code>, if it is present.
	 * 
	 * @param songId
	 *           songId of the <code>Music</code> to be removed from this <code>MusicList</code>.
	 */
	public void removeById(int songId) {
		Music music = getById(songId);
		
		if (music != null) {
			map.remove(new Integer(songId));
			list.remove(music);
		}
	}
	
	/**
	 * Removes music at <code>position</code> from this <code>MusicList</code>, if it is present.
	 * 
	 * @param index
	 *           position of the <code>Music</code> to be removed from this <code>MusicList</code>.
	 */
	public void removeByIndex(int index) {
		Music music = getByIndex(index);
		
		if (music != null) {
			list.remove(index);
			map.remove(new Integer(music.getSongId()));
		}
	}



	/**
	 * Retrieves this <code>MusicList</code> size.
	 * 
	 * @return <code>MusicList</code> size.
	 */
	public int size() {
		return list.size();
	}

	/**
	 * Retrieves a <code>List</code> with selected slice from this <code>MusicList</code>.
	 * 
	 * @param fromIndex
	 *           first index (included).
	 * @param toIndex
	 *           last index (not included).
	 * @return a <code>List</code> with selected slice from this <code>MusicList</code>.
	 * @see List#subList(int, int)
	 */
	public List<Music> subList(int fromIndex, int toIndex) {
		return this.list.subList(fromIndex, toIndex);
	}
}