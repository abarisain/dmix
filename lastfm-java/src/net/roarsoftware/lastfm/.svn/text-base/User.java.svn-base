package net.roarsoftware.lastfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.roarsoftware.xml.DomElement;

/**
 * Contains user information and provides bindings to the methods in the user. namespace.
 *
 * @author Janni Kovacs
 */
public class User extends ImageHolder {

	private String name;
	private String url;

	private String realname;

	private String language;
	private String country;
	private int age = -1;
	private String gender;
	private boolean subscriber;
	private int numPlaylists;
	private int playcount;

	private User(String name, String url) {
		this.name = name;
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public String getRealname() {
		return realname;
	}

	public String getUrl() {
		return url;
	}

	public int getAge() {
		return age;
	}

	public String getCountry() {
		return country;
	}

	public String getGender() {
		return gender;
	}

	public String getLanguage() {
		return language;
	}

	public int getNumPlaylists() {
		return numPlaylists;
	}

	public int getPlaycount() {
		return playcount;
	}

	public boolean isSubscriber() {
		return subscriber;
	}

	public String getImageURL() {
		return getImageURL(ImageSize.MEDIUM);
	}

	public static Collection<User> getFriends(String user, String apiKey) {
		return getFriends(user, false, 100, apiKey);
	}

	public static Collection<User> getFriends(String user, boolean recenttracks, int limit, String apiKey) {
		Result result = Caller.getInstance().call("user.getFriends", apiKey, "user", user, "recenttracks",
				String.valueOf(recenttracks ? 1 : 0), "limit", String.valueOf(limit));
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<User> friends = new ArrayList<User>();
		for (DomElement domElement : element.getChildren("user")) {
			friends.add(userFromElement(domElement));
		}
		return friends;
	}

	public static Collection<User> getNeighbours(String user, String apiKey) {
		return getNeighbours(user, 100, apiKey);
	}

	public static Collection<User> getNeighbours(String user, int limit, String apiKey) {
		Result result = Caller.getInstance()
				.call("user.getNeighbours", apiKey, "user", user, "limit", String.valueOf(limit));
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<User> friends = new ArrayList<User>();
		for (DomElement domElement : element.getChildren("user")) {
			friends.add(userFromElement(domElement));
		}
		return friends;
	}

	public static Collection<Track> getRecentTracks(String user, String apiKey) {
		return getRecentTracks(user, 10, apiKey);
	}

	public static Collection<Track> getRecentTracks(String user, int limit, String apiKey) {
		Result result = Caller.getInstance()
				.call("user.getRecentTracks", apiKey, "user", user, "limit", String.valueOf(limit));
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<Track> tracks = new ArrayList<Track>();
		for (DomElement e : element.getChildren("track")) {
			tracks.add(Track.trackFromElement(e));
		}
		return tracks;
	}

	public static Collection<Album> getTopAlbums(String user, String apiKey) {
		return getTopAlbums(user, Period.OVERALL, apiKey);
	}

	public static Collection<Album> getTopAlbums(String user, Period period, String apiKey) {
		Result result = Caller.getInstance()
				.call("user.getTopAlbums", apiKey, "user", user, "period", period.getString());
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<Album> albums = new ArrayList<Album>();
		for (DomElement domElement : element.getChildren("album")) {
			albums.add(Album.albumFromElement(domElement));
		}
		return albums;
	}

	public static Collection<Artist> getTopArtists(String user, String apiKey) {
		return getTopArtists(user, Period.OVERALL, apiKey);
	}

	public static Collection<Artist> getTopArtists(String user, Period period, String apiKey) {
		Result result = Caller.getInstance()
				.call("user.getTopArtists", apiKey, "user", user, "period", period.getString());
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<Artist> artists = new ArrayList<Artist>();
		for (DomElement domElement : element.getChildren("artist")) {
			artists.add(Artist.artistFromElement(domElement));
		}
		return artists;
	}

	public static Collection<Track> getTopTracks(String user, String apiKey) {
		return getTopTracks(user, Period.OVERALL, apiKey);
	}

	public static Collection<Track> getTopTracks(String user, Period period, String apiKey) {
		Result result = Caller.getInstance()
				.call("user.getTopTracks", apiKey, "user", user, "period", period.getString());
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<Track> tracks = new ArrayList<Track>();
		for (DomElement domElement : element.getChildren("track")) {
			tracks.add(Track.trackFromElement(domElement));
		}
		return tracks;
	}

	public static Collection<String> getTopTags(String user, String apiKey) {
		return getTopTags(user, -1, apiKey);
	}

	public static Collection<String> getTopTags(String user, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		if (limit != -1) {
			params.put("limit", String.valueOf(limit));
		}
		Result result = Caller.getInstance().call("user.getTopTags", apiKey, params);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<String> tags = new ArrayList<String>();
		for (DomElement domElement : element.getChildren("tag")) {
			tags.add(domElement.getChildText("name"));
		}
		return tags;
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, String apiKey) {
		return getWeeklyAlbumChart(user, null, null, -1, apiKey);
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, int limit, String apiKey) {
		return getWeeklyAlbumChart(user, null, null, limit, apiKey);
	}

	public static Chart<Album> getWeeklyAlbumChart(String user, String from, String to, int limit, String apiKey) {
		return Chart.getChart("user.getWeeklyAlbumChart", "user", user, "album", from, to, limit, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, String apiKey) {
		return getWeeklyArtistChart(user, null, null, -1, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, int limit, String apiKey) {
		return getWeeklyArtistChart(user, null, null, limit, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String user, String from, String to, int limit, String apiKey) {
		return Chart.getChart("user.getWeeklyArtistChart", "user", user, "artist", from, to, limit, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, String apiKey) {
		return getWeeklyTrackChart(user, null, null, -1, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, int limit, String apiKey) {
		return getWeeklyTrackChart(user, null, null, limit, apiKey);
	}

	public static Chart<Track> getWeeklyTrackChart(String user, String from, String to, int limit, String apiKey) {
		return Chart.getChart("user.getWeeklyTrackChart", "user", user, "track", from, to, limit, apiKey);
	}

	public static LinkedHashMap<String, String> getWeeklyChartList(String user, String apiKey) {
		return Chart.getWeeklyChartList("user", user, apiKey);
	}

	public static Collection<Chart> getWeeklyChartListAsCharts(String user, String apiKey) {
		return Chart.getWeeklyChartListAsCharts("user", user, apiKey);
	}

	/**
	 * GetS a list of upcoming events that this user is attending.
	 *
	 * @param user The user to fetch the events for.
	 * @param apiKey A Last.fm API key.
	 * @return a list of upcoming events
	 */
	public static Collection<Event> getEvents(String user, String apiKey) {
		Result result = Caller.getInstance().call("user.getEvents", apiKey, "user", user);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<Event> events = new ArrayList<Event>();
		for (DomElement domElement : element.getChildren("event")) {
			events.add(Event.eventFromElement(domElement));
		}
		return events;
	}

	/**
	 * Get the first page of a paginated result of all events a user has attended in the past.
	 *
	 * @param user The username to fetch the events for.
	 * @param apiKey A Last.fm API key.
	 * @return a list of past {@link Event}s
	 */
	public static PaginatedResult<Event> getPastEvents(String user, String apiKey) {
		return getPastEvents(user, 1, 0, apiKey);
	}

	/**
	 * Gets a paginated list of all events a user has attended in the past.
	 *
	 * @param user The username to fetch the events for.
	 * @param page The page number to scan to.
	 * @param limit The number of events to return per page.
	 * @param apiKey A Last.fm API key.
	 * @return a list of past {@link Event}s
	 */
	public static PaginatedResult<Event> getPastEvents(String user, int page, int limit, String apiKey) {
		Result result = Caller.getInstance().call("user.getPastEvents", apiKey, "user", user, "page",
				String.valueOf(page), "limit", String.valueOf(limit));
		if (!result.isSuccessful())
			return new PaginatedResult<Event>(0, 0, Collections.<Event>emptyList());
		DomElement element = result.getContentElement();
		List<Event> events = new ArrayList<Event>();
		for (DomElement domElement : element.getChildren("event")) {
			events.add(Event.eventFromElement(domElement));
		}
		int currentPage = Integer.valueOf(element.getAttribute("page"));
		int totalPages = Integer.valueOf(element.getAttribute("totalPages"));
		return new PaginatedResult<Event>(currentPage, totalPages, events);
	}

	public static PaginatedResult<Event> getRecommendedEvents(Session session) {
		return getRecommendedEvents(0, session);
	}

	public static PaginatedResult<Event> getRecommendedEvents(int page, Session session) {
		return getRecommendedEvents(0, 0, session);
	}

	public static PaginatedResult<Event> getRecommendedEvents(int page, int limit, Session session) {
		Result result = Caller.getInstance().call("user.getRecommendedEvents", session, "page", String.valueOf(page),
				"limit", String.valueOf(limit), "user", session.getUsername());
		if (!result.isSuccessful())
			return new PaginatedResult<Event>(0, 0, Collections.<Event>emptyList());
		DomElement element = result.getContentElement();
		List<Event> events = new ArrayList<Event>();
		for (DomElement domElement : element.getChildren("event")) {
			events.add(Event.eventFromElement(domElement));
		}
		int currentPage = Integer.valueOf(element.getAttribute("page"));
		int totalPages = Integer.valueOf(element.getAttribute("totalPages"));
		return new PaginatedResult<Event>(currentPage, totalPages, events);
	}

	/**
	 * Gets a list of a user's playlists on Last.fm. Note that this method only fetches metadata regarding the user's
	 * playlists. If you want to retrieve the list of tracks in a playlist use
	 * {@link Playlist#fetch(String, String) Playlist.fetch()}.
	 *
	 * @param user The last.fm username to fetch the playlists of.
	 * @param apiKey A Last.fm API key.
	 * @return a list of Playlists
	 */
	public static Collection<Playlist> getPlaylists(String user, String apiKey) {
		Result result = Caller.getInstance().call("user.getPlaylists", apiKey, "user", user);
		if (!result.isSuccessful())
			return Collections.emptyList();
		Collection<Playlist> playlists = new ArrayList<Playlist>();
		for (DomElement element : result.getContentElement().getChildren("playlist")) {
			playlists.add(Playlist.playlistFromElement(element));
		}
		return playlists;
	}

	/**
	 * Retrieves the loved tracks by a user.
	 *
	 * @param user The user name to fetch the loved tracks for.
	 * @param apiKey A Last.fm API key.
	 * @return the loved tracks
	 */
	public static PaginatedResult<Track> getLovedTracks(String user, String apiKey) {
		return getLovedTracks(user, 1, apiKey);
	}

	/**
	 * Retrieves the loved tracks by a user.
	 *
	 * @param user The user name to fetch the loved tracks for.
	 * @param page The page number to scan to
	 * @param apiKey A Last.fm API key.
	 * @return the loved tracks
	 */
	public static PaginatedResult<Track> getLovedTracks(String user, int page, String apiKey) {
		Result result = Caller.getInstance()
				.call("user.getLovedTracks", apiKey, "user", user, "page", String.valueOf(page));
		if (!result.isSuccessful())
			return new PaginatedResult<Track>(0, 0, Collections.<Track>emptyList());
		DomElement element = result.getContentElement();
		Collection<DomElement> children = element.getChildren("track");
		Collection<Track> tracks = new ArrayList<Track>(children.size());
		for (DomElement domElement : children) {
			tracks.add(Track.trackFromElement(domElement));
		}
		int currentPage = Integer.valueOf(element.getAttribute("page"));
		int totalPages = Integer.valueOf(element.getAttribute("totalPages"));
		return new PaginatedResult<Track>(currentPage, totalPages, tracks);
	}

	/**
	 * Retrieves profile information about the current authenticated user.
	 *
	 * @param session A Session instance
	 * @return User info
	 */
	public static User getInfo(Session session) {
		Result result = Caller.getInstance().call("user.getInfo", session);
		if (!result.isSuccessful())
			return null;
		DomElement element = result.getContentElement();
		return userFromElement(element);
	}

	/**
	 * Get Last.fm artist recommendations for a user.
	 *
	 * @param session A Session instance
	 * @return a list of {@link Artist}s
	 */
	public static PaginatedResult<Artist> getRecommendedArtists(Session session) {
		return getRecommendedArtists(1, session);
	}

	/**
	 * Get Last.fm artist recommendations for a user.
	 *
	 * @param page The page to fetch
	 * @param session A Session instance
	 * @return a list of {@link Artist}s
	 */
	public static PaginatedResult<Artist> getRecommendedArtists(int page, Session session) {
		Result result = Caller.getInstance().call("user.getRecommendedArtists", session, "page", String.valueOf(page));
		if (!result.isSuccessful())
			return new PaginatedResult<Artist>(0, 0, Collections.<Artist>emptyList());
		DomElement element = result.getContentElement();
		Collection<DomElement> children = element.getChildren("artist");
		List<Artist> artists = new ArrayList<Artist>(children.size());
		for (DomElement domElement : children) {
			artists.add(Artist.artistFromElement(domElement));
		}
		page = Integer.parseInt(element.getAttribute("page"));
		int total = Integer.parseInt(element.getAttribute("totalPages"));
		return new PaginatedResult<Artist>(page, total, artists);
	}

	/**
	 * Shout on this user's shoutbox
	 *
	 * @param user The name of the user to shout on
	 * @param message The message to post to the shoutbox
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result shout(String user, String message, Session session) {
		return Caller.getInstance().call("user.shout", session, "user", user, "message", message);
	}

	static User userFromElement(DomElement element) {
		User user = new User(element.getChildText("name"), element.getChildText("url"));
		if (element.hasChild("realname"))
			user.realname = element.getChildText("realname");
		ImageHolder.loadImages(user, element);
		user.language = element.getChildText("lang");
		user.country = element.getChildText("country");
		if (element.hasChild("age")) {
			try {
				user.age = Integer.parseInt(element.getChildText("age"));
			} catch (NumberFormatException e) {
				// no age
			}
		}
		user.gender = element.getChildText("gender");
		user.subscriber = "1".equals(element.getChildText("subscriber"));
		if (element.hasChild("playcount")) { // extended user information
			try {
				user.playcount = Integer.parseInt(element.getChildText("playcount"));
			} catch (NumberFormatException e) {
				// no playcount
			}
		}
		if (element.hasChild("playlists")) { // extended user information
			try {
				user.numPlaylists = Integer.parseInt(element.getChildText("playlists"));
			} catch (NumberFormatException e) {
				// no playlists
			}
		}
		return user;
	}
}
