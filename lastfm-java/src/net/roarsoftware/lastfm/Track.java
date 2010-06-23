package net.roarsoftware.lastfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roarsoftware.util.StringUtilities;
import net.roarsoftware.xml.DomElement;

/**
 * Bean that contains information related to <code>Track</code>s and provides bindings to methods
 * in the <code>track.</code> namespace.
 *
 * @author Janni Kovacs
 */
public class Track extends MusicEntry {

	public static final String ARTIST_PAGE = "artistpage";
	public static final String ALBUM_PAGE = "albumpage";
	public static final String TRACK_PAGE = "trackpage";

	protected String artist;
	protected String artistMbid;

	protected String album;
	protected String albumMbid;
	protected int position = -1;

	protected boolean fullTrackAvailable;
	protected boolean nowPlaying;

	protected Date playedWhen;
	protected int duration;
	protected String location;

	protected Map<String, String> lastFmExtensionInfos = new HashMap<String, String>();


	protected Track(String name, String url, String artist) {
		super(name, url);
		this.artist = artist;
	}

	protected Track(String name, String url, String mbid, int playcount, int listeners, boolean streamable,
					String artist, String artistMbid, boolean fullTrackAvailable, boolean nowPlaying) {
		super(name, url, mbid, playcount, listeners, streamable);
		this.artist = artist;
		this.artistMbid = artistMbid;
		this.fullTrackAvailable = fullTrackAvailable;
		this.nowPlaying = nowPlaying;
	}

	/**
	 * Returns the duration of the song, if available, in seconds. The duration attribute is only available
	 * for tracks retrieved by {@link Playlist#fetch(String, String) Playlist.fetch} and
	 * {@link Track#getInfo(String, String, String) Track.getInfo}.
	 *
	 * @return duration in seconds
	 */
	public int getDuration() {
		return duration;
	}

	public String getArtist() {
		return artist;
	}

	public String getArtistMbid() {
		return artistMbid;
	}

	public String getAlbum() {
		return album;
	}

	public String getAlbumMbid() {
		return albumMbid;
	}

	public boolean isFullTrackAvailable() {
		return fullTrackAvailable;
	}

	public boolean isNowPlaying() {
		return nowPlaying;
	}

	/**
	 * Returns the location (URL) of this Track. This information is only available with the {@link Radio} services.
	 *
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Returns last.fm specific information about this Track. Only available in Tracks, fetched from
	 * radio-playlists. <tt>key</tt> can be one of the following:
	 * <ul>
	 * <li>artistpage</li>
	 * <li>albumpage</li>
	 * <li>trackpage</li>
	 * <li>buyTrackURL</li>
	 * <li>buyAlbumURL</li>
	 * <li>freeTrackURL</li>
	 * </ul>
	 * Or use the available constants in this class.<br/>
	 * Note that the key string is case sensitive.
	 *
	 * @param key A key
	 * @return associated value
	 * @see #ARTIST_PAGE
	 * @see #ALBUM_PAGE
	 * @see #TRACK_PAGE
	 */
	public String getLastFmInfo(String key) {
		return lastFmExtensionInfos.get(key);
	}

	/**
	 * Returns the time when the track was played, if this data is available (e.g. for recent tracks) or <code>null</code>,
	 * if this data is not available.<br/>
	 *
	 * @return the date when the track was played or <code>null</code>
	 */
	public Date getPlayedWhen() {
		return playedWhen;
	}

	/**
	 * Returns the position of this track in its associated album, or -1 if not available.
	 *
	 * @return the album position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Searches for a track with the given name and returns a list of possible matches.
	 *
	 * @param track Track name
	 * @param apiKey The API key
	 * @return a list of possible matches
	 * @see #search(String, String, int, String)
	 */
	public static Collection<Track> search(String track, String apiKey) {
		return search(null, track, 30, apiKey);
	}

	/**
	 * Searches for a track with the given name and returns a list of possible matches.
	 * Specify an artist name or a limit to narrow down search results.
	 * Pass <code>null</code> for the artist parameter if you want to specify a limit but don't want
	 * to define an artist.
	 *
	 * @param artist Artist's name or <code>null</code>
	 * @param track Track name
	 * @param limit Number of maximum results
	 * @param apiKey The API key
	 * @return a list of possible matches
	 */
	public static Collection<Track> search(String artist, String track, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("track", track);
		params.put("limit", String.valueOf(limit));
		if (artist != null)
			params.put("artist", artist);
		Result result = Caller.getInstance().call("track.search", apiKey, params);
		DomElement element = result.getContentElement();
		DomElement matches = element.getChild("trackmatches");
		List<Track> tracks = new ArrayList<Track>();
		for (DomElement domElement : matches.getChildren("track")) {
			tracks.add(trackFromElement(domElement));
		}
		return tracks;
	}

	/**
	 * Retrieves the top tags for the given track. You either have to specify a track and artist name or
	 * a mbid. If you specify an mbid you may pass <code>null</code> for the first parameter.
	 *
	 * @param artist Artist name or <code>null</code> if an MBID is specified
	 * @param trackOrMbid Track name or MBID
	 * @param apiKey The API key
	 * @return list of tags
	 */
	public static List<Tag> getTopTags(String artist, String trackOrMbid, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(trackOrMbid)) {
			params.put("mbid", trackOrMbid);
		} else {
			params.put("artist", artist);
			params.put("track", trackOrMbid);
		}
		Result result = Caller.getInstance().call("track.getTopTags", apiKey, params);
		DomElement element = result.getContentElement();
		List<Tag> tags = new ArrayList<Tag>();
		for (DomElement domElement : element.getChildren("tag")) {
			tags.add(Tag.tagFromElement(domElement));
		}
		return tags;
	}

	/**
	 * Retrieves the top fans for the given track. You either have to specify a track and artist name or
	 * a mbid. If you specify an mbid you may pass <code>null</code> for the first parameter.
	 *
	 * @param artist Artist name or <code>null</code> if an MBID is specified
	 * @param trackOrMbid Track name or MBID
	 * @param apiKey The API key
	 * @return list of fans
	 */
	public static Collection<User> getTopFans(String artist, String trackOrMbid, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(trackOrMbid)) {
			params.put("mbid", trackOrMbid);
		} else {
			params.put("artist", artist);
			params.put("track", trackOrMbid);
		}
		Result result = Caller.getInstance().call("track.getTopFans", apiKey, params);
		DomElement element = result.getContentElement();
		List<User> users = new ArrayList<User>();
		for (DomElement domElement : element.getChildren("user")) {
			users.add(User.userFromElement(domElement));
		}
		return users;
	}

	/**
	 * Tag an album using a list of user supplied tags.
	 *
	 * @param artist The artist name in question
	 * @param track The track name in question
	 * @param tags A comma delimited list of user supplied tags to apply to this track. Accepts a maximum of 10 tags.
	 * @param session A Session instance.
	 * @return the Result of the operation
	 */
	public static Result addTags(String artist, String track, String tags, Session session) {
		return Caller.getInstance().call("track.addTags", session, "artist", artist, "track", track, "tags", tags);
	}

	/**
	 * Remove a user's tag from a track.
	 *
	 * @param artist The artist name in question
	 * @param track The track name in question
	 * @param tag A single user tag to remove from this track.
	 * @param session A Session instance.
	 * @return the Result of the operation
	 */
	public static Result removeTag(String artist, String track, String tag, Session session) {
		return Caller.getInstance().call("track.removeTag", session, "artist", artist, "track", track, "tag", tag);
	}

	/**
	 * Share a track twith one or more Last.fm users or other friends.
	 *
	 * @param artist An artist name.
	 * @param track A track name.
	 * @param message A message to send with the recommendation or <code>null</code>. If not supplied a default message will be used.
	 * @param recipient A comma delimited list of email addresses or Last.fm usernames. Maximum is 10.
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result share(String artist, String track, String message, String recipient, Session session) {
		Map<String, String> params = StringUtilities.map("artist", artist, "track", track, "recipient", recipient);
		if (message != null)
			params.put("message", message);
		return Caller.getInstance().call("track.share", session, params);
	}

	/**
	 * Love a track for a user profile.
	 * This needs to be supplemented with a scrobbling submission containing the 'love' rating (see the audioscrobbler API).
	 *
	 * @param artist An artist name
	 * @param track A track name
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result love(String artist, String track, Session session) {
		return Caller.getInstance().call("track.love", session, "artist", artist, "track", track);
	}

	/**
	 * Ban a track for a given user profile.
	 * This needs to be supplemented with a scrobbling submission containing the 'ban' rating (see the audioscrobbler API). 	 * @param artist An artist name
	 *
	 * @param artist An artist name
	 * @param track A track name
	 * @param session A Session instance
	 * @return the Result of the operation
	 */
	public static Result ban(String artist, String track, Session session) {
		return Caller.getInstance().call("track.ban", session, "artist", artist, "track", track);
	}

	/**
	 * Get the similar tracks for this track on Last.fm, based on listening data.<br/>
	 * You have to provide either an artist and a track name <i>or</i> an mbid. Pass <code>null</code>
	 * for parameters you don't need.
	 *
	 * @param artist The artist name in question
	 * @param track The track name in question
	 * @param mbid The musicbrainz id for the track
	 * @param apiKey A Last.fm API key.
	 * @return a list of similar <code>Track</code>s
	 */
	public static Collection<Track> getSimilar(String artist, String track, String mbid, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (artist != null && track != null) {
			params.put("artist", artist);
			params.put("track", track);
		}
		if (mbid != null) {
			params.put("mbid", mbid);
		}
		Result result = Caller.getInstance().call("track.getSimilar", apiKey, params);
		List<Track> tracks = new ArrayList<Track>();
		for (DomElement element : result.getContentElement().getChildren("track")) {
			tracks.add(Track.trackFromElement(element));
		}
		return tracks;
	}

	/**
	 * Get the tags applied by an individual user to an track on Last.fm.
	 *
	 * @param artist The artist name in question
	 * @param track The track name in question
	 * @param session A Session instance
	 * @return a list of tags
	 */
	public static Collection<String> getTags(String artist, String track, Session session) {
		Result result = Caller.getInstance().call("track.getTags", session, "artist", artist, "track", track);
		DomElement element = result.getContentElement();
		Collection<String> tags = new ArrayList<String>();
		for (DomElement domElement : element.getChildren("tag")) {
			tags.add(domElement.getChildText("name"));
		}
		return tags;
	}

	/**
	 * Get the metadata for a track on Last.fm using the artist/track name or a musicbrainz id.
	 *
	 * @param artist The artist name in question or <code>null</code> if an mbid is specified
	 * @param trackOrMbid The track name in question or the musicbrainz id for the track
	 * @param apiKey A Last.fm API key.
	 * @return Track information
	 */
	public static Track getInfo(String artist, String trackOrMbid, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		if (StringUtilities.isMbid(trackOrMbid)) {
			params.put("mbid", trackOrMbid);
		} else {
			params.put("artist", artist);
			params.put("track", trackOrMbid);
		}
		Result result = Caller.getInstance().call("track.getInfo", apiKey, params);
		if (!result.isSuccessful())
			return null;
		DomElement content = result.getContentElement();
		DomElement album = content.getChild("album");
		Track track = trackFromElement(content);
		if (album != null) {
			String pos = album.getAttribute("position");
			if ((pos != null) && pos.length() != 0) {
				track.position = Integer.parseInt(pos);
			}
			track.album = album.getChildText("title");
			track.albumMbid = album.getChildText("mbid");
			ImageHolder.loadImages(track, album);
		}
		return track;
	}

	static Track trackFromElement(DomElement element, String artistName) {
		Track track = new Track(null, null, artistName);
		MusicEntry.loadStandardInfo(track, element);
		final String nowPlayingAttr = element.getAttribute("nowplaying");
		if (nowPlayingAttr != null)
			track.nowPlaying = Boolean.valueOf(nowPlayingAttr);
		if (element.hasChild("duration"))
			track.duration = Integer.parseInt(element.getChildText("duration")) / 1000;
		DomElement album = element.getChild("album");
		if (album != null) {
			track.album = album.getText();
			track.albumMbid = album.getAttribute("mbid");
		}
		DomElement artist = element.getChild("artist");
		if (artist.getChild("name") != null) {
			track.artist = artist.getChildText("name");
			track.artistMbid = artist.getChildText("mbid");
		} else {
			if (artistName == null)
				track.artist = artist.getText();
			track.artistMbid = artist.getAttribute("mbid");
		}
		DomElement date = element.getChild("date");
		if (date != null) {
			String uts = date.getAttribute("uts");
			long utsTime = Long.parseLong(uts);
			track.playedWhen = new Date(utsTime * 1000);
		}
		DomElement stream = element.getChild("streamable");
		if (stream != null) {
			String s = stream.getAttribute("fulltrack");
			track.fullTrackAvailable = s != null && Integer.parseInt(s) == 1;
		}
		return track;
	}

	public static Track trackFromElement(DomElement e) {
		return trackFromElement(e, null);
	}
}
