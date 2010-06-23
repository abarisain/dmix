package net.roarsoftware.lastfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roarsoftware.xml.DomElement;

/**
 * Contains bindings for all methods in the "library" namespace.
 *
 * @author Martin Chorley
 * @author Janni Kovacs
 */
public class Library {

	private Library() {
	}

	/**
	 * Retrieves a paginated list of all the artists in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the artists
	 */
	public static PaginatedResult<Artist> getArtists(String user, String apiKey) {
		return getArtists(user, 1, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the artists in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the artists
	 */
	public static PaginatedResult<Artist> getArtists(String user, int page, String apiKey) {
		return getArtists(user, page, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the artists in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param limit Limit the amount of artists returned (maximum/default is 50).
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the artists
	 */
	public static PaginatedResult<Artist> getArtists(String user, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		params.put("page", String.valueOf(page));
		params.put("limit", String.valueOf(limit));
		Result result = Caller.getInstance().call("library.getArtists", apiKey, params);
		if (!result.isSuccessful())
			return new PaginatedResult<Artist>(0, 0, Collections.<Artist>emptyList());
		DomElement element = result.getContentElement();
		List<Artist> artists = new ArrayList<Artist>();
		for (DomElement domElement : element.getChildren("artist")) {
			artists.add(Artist.artistFromElement(domElement));
		}
		int currentPage = Integer.valueOf(element.getAttribute("page"));
		int totalPages = Integer.valueOf(element.getAttribute("totalPages"));
		return new PaginatedResult<Artist>(currentPage, totalPages, artists);
	}

	/**
	 * Retrieves all artists in a user's library. Pay attention if you use this method as it may produce
	 * a lot of network traffic and therefore may consume a long time.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return all artists in a user's library
	 */
	public static Collection<Artist> getAllArtists(String user, String apiKey) {
		Collection<Artist> artists = null;
		int page = 1, total;
		do {
			PaginatedResult<Artist> result = getArtists(user, page, apiKey);
			total = result.getTotalPages();
			Collection<Artist> pageResults = result.getPageResults();
			if (artists == null) {
				// artists is initialized here to initialize it with the right size and avoid array copying later on
				artists = new ArrayList<Artist>(total * pageResults.size());
			}
			for (Artist artist : pageResults) {
				artists.add(artist);
			}
			page++;
		} while (page <= total);
		return artists;
	}


	/**
	 * Retrieves a paginated list of all the albums in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the albums
	 */
	public static PaginatedResult<Album> getAlbums(String user, String apiKey) {
		return getAlbums(user, 1, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the albums in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the albums
	 */
	public static PaginatedResult<Album> getAlbums(String user, int page, String apiKey) {
		return getAlbums(user, page, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the albums in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param limit Limit the amount of albumss returned (maximum/default is 50).
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the albums
	 */
	public static PaginatedResult<Album> getAlbums(String user, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		params.put("page", String.valueOf(page));
		params.put("limit", String.valueOf(limit));
		Result result = Caller.getInstance().call("library.getAlbums", apiKey, params);
		if (!result.isSuccessful())
			return new PaginatedResult<Album>(0, 0, Collections.<Album>emptyList());
		DomElement element = result.getContentElement();
		List<Album> artists = new ArrayList<Album>();
		for (DomElement domElement : element.getChildren("album")) {
			artists.add(Album.albumFromElement(domElement));
		}
		int currentPage = Integer.valueOf(element.getAttribute("page"));
		int totalPages = Integer.valueOf(element.getAttribute("totalPages"));
		return new PaginatedResult<Album>(currentPage, totalPages, artists);
	}

	/**
	 * Retrieves all albums in a user's library. Pay attention if you use this method as it may produce
	 * a lot of network traffic and therefore may consume a long time.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return all albums in a user's library
	 */
	public static Collection<Album> getAllAlbums(String user, String apiKey) {
		Collection<Album> albums = null;
		int page = 1, total;
		do {
			PaginatedResult<Album> result = getAlbums(user, page, apiKey);
			total = result.getTotalPages();
			Collection<Album> pageResults = result.getPageResults();
			if (albums == null) {
				// albums is initialized here to initialize it with the right size and avoid array copying later on
				albums = new ArrayList<Album>(total * pageResults.size());
			}
			for (Album album : pageResults) {
				albums.add(album);
			}
			page++;
		} while (page <= total);
		return albums;
	}


	/**
	 * Retrieves a paginated list of all the tracks in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the tracks
	 */
	public static PaginatedResult<Track> getTracks(String user, String apiKey) {
		return getTracks(user, 1, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the tracks in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the tracks
	 */
	public static PaginatedResult<Track> getTracks(String user, int page, String apiKey) {
		return getTracks(user, page, 0, apiKey);
	}

	/**
	 * Retrieves a paginated list of all the tracks in a user's library.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param page The page number you wish to scan to.
	 * @param limit Limit the amount of albumss returned (maximum/default is 50).
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} of the tracks
	 */
	public static PaginatedResult<Track> getTracks(String user, int page, int limit, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", user);
		params.put("page", String.valueOf(page));
		params.put("limit", String.valueOf(limit));
		Result result = Caller.getInstance().call("library.getTracks", apiKey, params);
		if (!result.isSuccessful())
			return new PaginatedResult<Track>(0, 0, Collections.<Track>emptyList());
		DomElement element = result.getContentElement();
		List<Track> tracks = new ArrayList<Track>();
		for (DomElement domElement : element.getChildren("track")) {
			tracks.add(Track.trackFromElement(domElement));
		}
		int currentPage = Integer.valueOf(element.getAttribute("page"));
		int totalPages = Integer.valueOf(element.getAttribute("totalPages"));
		return new PaginatedResult<Track>(currentPage, totalPages, tracks);
	}

	/**
	 * Retrieves all tracks in a user's library. Pay attention if you use this method as it may produce
	 * a lot of network traffic and therefore may consume a long time.
	 *
	 * @param user The user whose library you want to fetch.
	 * @param apiKey A Last.fm API key.
	 * @return all tracks in a user's library
	 */
	public static Collection<Track> getAllTracks(String user, String apiKey) {
		Collection<Track> tracks = null;
		int page = 1, total;
		do {
			PaginatedResult<Track> result = getTracks(user, page, apiKey);
			total = result.getTotalPages();
			Collection<Track> pageResults = result.getPageResults();
			if (tracks == null) {
				// tracks is initialized here to initialize it with the right size and avoid array copying later on
				tracks = new ArrayList<Track>(total * pageResults.size());
			}
			for (Track track : pageResults) {
				tracks.add(track);
			}
			page++;
		} while (page <= total);
		return tracks;
	}

	/**
	 * Add an artist to a user's Last.fm library
	 *
	 * @param artist The artist name you wish to add
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result addArtist(String artist, Session session) {
		return Caller.getInstance().call("library.addArtist", session, "artist", artist);
	}

	/**
	 * Add an album to a user's Last.fm library
	 *
	 * @param artist The artist that composed the track
	 * @param album The album name you wish to add
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result addAlbum(String artist, String album, Session session) {
		return Caller.getInstance().call("library.addAlbum", session, "artist", artist, "album", album);
	}

	/**
	 * Add a track to a user's Last.fm library
	 *
	 * @param artist The artist that composed the track
	 * @param track The track name you wish to add
	 * @param session A Session instance
	 * @return the result of the operation
	 */
	public static Result addTrack(String artist, String track, Session session) {
		return Caller.getInstance().call("library.addTrack", session, "artist", artist, "track", track);
	}
}
