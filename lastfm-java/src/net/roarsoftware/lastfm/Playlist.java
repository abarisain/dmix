package net.roarsoftware.lastfm;

import java.util.ArrayList;
import java.util.Collection;

import net.roarsoftware.xml.DomElement;

/**
 * Bean for music playlists. Contains the {@link #fetch(String, String) fetch} method and various <code>fetchXXX</code>
 * methods to retrieve playlists from the server. Playlists are identified by lastfm:// playlist urls. Valid urls
 * include:
 * <ul>
 * <li><b>Album Playlists:</b> lastfm://playlist/album/{@literal <album_id>}</li>
 * <li><b>User Playlists:</b> lastfm://playlist/{@literal <playlist_id>}</li>
 * <li><b>Tag Playlists:</b> lastfm://playlist/tag/{@literal <tag_name>}/freetracks</li>
 * </ul>
 * See <a href="http://www.last.fm/api/playlists">http://www.last.fm/api/playlists</a> for more information about playlists.
 *
 * @author Janni Kovacs
 */
public class Playlist {

	private int id;
	private String title;
	private String annotation;
	private int size;
	private String creator;

	private Collection<Track> tracks = new ArrayList<Track>();

	private Playlist() {
	}

	public String getCreator() {
		return creator;
	}

	public int getId() {
		return id;
	}

	public int getSize() {
		return size;
	}

	public String getTitle() {
		return title;
	}

	public String getAnnotation() {
		return annotation;
	}

	public Collection<Track> getTracks() {
		return tracks;
	}

	/**
	 * Fetches an album playlist, which contains the tracks of the specified album.
	 *
	 * @param albumId The album id as returned in {@link Album#getInfo(String, String, String) Album.getInfo}.
	 * @param apiKey A Last.fm API key.
	 * @return a playlist
	 */
	public static Playlist fetchAlbumPlaylist(String albumId, String apiKey) {
		return fetch("lastfm://playlist/album/" + albumId, apiKey);
	}

	/**
	 * Fetches a user-created playlist.
	 *
	 * @param playlistId A playlist id.
	 * @param apiKey A Last.fm API key.
	 * @return a playlist
	 */
	public static Playlist fetchUserPlaylist(int playlistId, String apiKey) {
		return fetch("lastfm://playlist/" + playlistId, apiKey);
	}

	/**
	 * Fetches a playlist of freetracks for a given tag name.
	 *
	 * @param tag A tag name.
	 * @param apiKey A Last.fm API key.
	 * @return a playlist
	 */
	public static Playlist fetchTagPlaylist(String tag, String apiKey) {
		return fetch("lastfm://playlist/tag/" + tag + "/freetracks", apiKey);
	}

	/**
	 * Fetches a playlist using a lastfm playlist url. See the class description for a list of valid
	 * playlist urls.
	 *
	 * @param playlistUrl A valid playlist url.
	 * @param apiKey A Last.fm API key.
	 * @return a playlist
	 */
	public static Playlist fetch(String playlistUrl, String apiKey) {
		Result result = Caller.getInstance().call("playlist.fetch", apiKey, "playlistURL", playlistUrl);
		return playlistFromElement(result.getContentElement());
	}

	/**
	 * Add a track to a Last.fm user's playlist.
	 *
	 * @param playlistId The ID of the playlist - this is available in user.getPlaylists
	 * @param artist The artist name that corresponds to the track to be added.
	 * @param track The track name to add to the playlist.
	 * @param session A Session instance.
	 * @return the result of the operation
	 */
	public static Result addTrack(int playlistId, String artist, String track, Session session) {
		return Caller.getInstance()
				.call("playlist.addTrack", session, "playlistID", String.valueOf(playlistId), "artist", artist, "track",
						track);
	}

	/**
	 * Creates a Last.fm playlist.
	 *
	 * @param title A title for the playlist
	 * @param description A description for the playlist
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Playlist create(String title, String description, Session session) {
		Result result = Caller.getInstance()
				.call("Playlist.create", session, "title", title, "description", description);
		if (!result.isSuccessful())
			return null;
		return playlistFromElement(result.getContentElement().getChild("playlist"));
	}

	static Playlist playlistFromElement(DomElement e) {
		if (e == null)
			return null;
		Playlist p = new Playlist();
		if (e.hasChild("id"))
			p.id = Integer.parseInt(e.getChildText("id"));
		p.title = e.getChildText("title");
		if (e.hasChild("size"))
			p.size = Integer.parseInt(e.getChildText("size"));
		p.creator = e.getChildText("creator");
		p.annotation = e.getChildText("annotation");
		DomElement tl = e.getChild("trackList");
		if (tl != null) {
			for (DomElement te : tl.getChildren("track")) {
				Track t = new Track(te.getChildText("title"), te.getChildText("identifier"),
						te.getChildText("creator"));
				t.album = te.getChildText("album");
				t.duration = Integer.parseInt(te.getChildText("duration")) / 1000;
				t.imageUrls.put(ImageSize.LARGE, te.getChildText("image"));
				t.imageUrls.put(ImageSize.ORIGINAL, te.getChildText("image"));
				t.location = te.getChildText("location");
				for (DomElement ext : te.getChildren("extension")) {
					if ("http://www.last.fm".equals(ext.getAttribute("application"))) {
						for (DomElement child : ext.getChildren()) {
							t.lastFmExtensionInfos.put(child.getTagName(), child.getText());
						}
					}
				}
				p.tracks.add(t);
			}
			if (p.size == 0)
				p.size = p.tracks.size();
		}
		return p;
	}
}
