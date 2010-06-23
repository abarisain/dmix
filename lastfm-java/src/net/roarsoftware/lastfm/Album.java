package net.roarsoftware.lastfm;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.roarsoftware.util.StringUtilities;
import net.roarsoftware.xml.DomElement;

/**
 * Bean for Album info.
 *
 * @author Janni Kovacs
 */
public class Album extends MusicEntry {

	private static final DateFormat RELEASE_DATE_FORMAT = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH);
	private String artist;
	private Date releaseDate;
	private String id;

	protected Album(String name, String url, String artist) {
		super(name, url);
		this.artist = artist;
	}

	protected Album(String name, String url, String mbid, int playcount, int listeners, boolean streamable,
					String artist) {
		super(name, url, mbid, playcount, listeners, streamable);
		this.artist = artist;
	}

	public String getArtist() {
		return artist;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public String getId() {
		return id;
	}

	/**
	 * Get the metadata for an album on Last.fm using the album name or a musicbrainz id.
	 * See playlist.fetch on how to get the album playlist.
	 *
	 * @param artist Artist's name
	 * @param albumOrMbid Album name or MBID
	 * @param apiKey The API key
	 * @return Album metadata
	 */
	public static Album getInfo(String artist, String albumOrMbid, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(albumOrMbid)) {
			params.put("mbid", albumOrMbid);
		} else {
			params.put("artist", artist);
			params.put("album", albumOrMbid);
		}
		Result result = Caller.getInstance().call("album.getInfo", apiKey, params);
		DomElement element = result.getContentElement().getChild("album");
		return albumFromElement(element);
	}

	/**
	 * Tag an album using a list of user supplied tags.<br/>
	 *
	 * @param artist The artist name in question
	 * @param album The album name in question
	 * @param tags A comma delimited list of user supplied tags to apply to this album. Accepts a maximum of 10 tags.
	 * @param session The Session instance
	 * @return the Result of the operation
	 * @see Authenticator
	 */
	public static Result addTags(String artist, String album, String tags, Session session) {
		return Caller.getInstance().call("album.addTags", session, "artist", artist, "album", album, "tags", tags);
	}

	/**
	 * Remove a user's tag from an album.
	 *
	 * @param artist The artist name in question
	 * @param album The album name in question
	 * @param tag A single user tag to remove from this album.
	 * @param session The Session instance
	 * @return the Result of the operation
	 * @see Authenticator
	 */
	public static Result removeTag(String artist, String album, String tag, Session session) {
		return Caller.getInstance().call("album.removeTag", session, "artist", artist, "album", album, "tag", tag);
	}

	/**
	 * Get the tags applied by an individual user to an album on Last.fm.
	 *
	 * @param artist The artist name in question
	 * @param album The album name in question
	 * @param session A Session instance
	 * @return a list of tags
	 */
	public static Collection<String> getTags(String artist, String album, Session session) {
		Result result = Caller.getInstance().call("album.getTags", session, "artist", artist, "album", album);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		Collection<String> tags = new ArrayList<String>();
		for (DomElement domElement : element.getChildren("tag")) {
			tags.add(domElement.getChildText("name"));
		}
		return tags;
	}

	/**
	 * Search for an album by name. Returns album matches sorted by relevance.
	 *
	 * @param album The album name in question.
	 * @param apiKey A Last.fm API key.
	 * @return a Collection of matches
	 */
	public static Collection<Album> search(String album, String apiKey) {
		Result result = Caller.getInstance().call("album.search", apiKey, "album", album);
		DomElement matches = result.getContentElement().getChild("albummatches");
		Collection<DomElement> children = matches.getChildren("album");
		Collection<Album> albums = new ArrayList<Album>(children.size());
		for (DomElement element : children) {
			albums.add(albumFromElement(element));
		}
		return albums;
	}

	static Album albumFromElement(DomElement element) {
		return albumFromElement(element, null);
	}

	static Album albumFromElement(DomElement element, String artistName) {
		if (element == null)
			return null;
		Album album = new Album(null, null, artistName);
		MusicEntry.loadStandardInfo(album, element);
		if (element.hasChild("id")) {
			album.id = element.getChildText("id");
		}
		if (element.hasChild("artist")) {
			album.artist = element.getChild("artist").getChildText("name");
			if (album.artist == null)
				album.artist = element.getChildText("artist");
		}
		if (element.hasChild("releasedate")) {
			try {
				album.releaseDate = RELEASE_DATE_FORMAT.parse(element.getChildText("releasedate"));
			} catch (ParseException e) {
				// uh oh
			}
		}
		if (element.hasChild("toptags")) {
			for (DomElement o : element.getChild("toptags").getChildren("tag")) {
				album.tags.add(o.getChildText("name"));
			}
		}
		return album;
	}

}
