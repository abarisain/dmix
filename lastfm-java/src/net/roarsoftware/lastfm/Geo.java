package net.roarsoftware.lastfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roarsoftware.xml.DomElement;

/**
 * Provides nothing more than a namespace for the API methods starting with geo.
 *
 * @author Janni Kovacs
 */
public class Geo {

	private Geo() {
	}

	/**
	 * Get all events in a specific location by country or city name.<br/>
	 * This method returns <em>all</em> events by subsequently calling {@link #getEvents(String, String, int, String)}
	 * and concatenating the single results into one list.<br/>
	 * Pay attention if you use this method as it may produce a lot of network traffic and therefore
	 * may consume a long time.
	 *
	 * @param location Specifies a location to retrieve events for
	 * @param distance Find events within a specified distance
	 * @param apiKey A Last.fm API key.
	 * @return a list containing all events
	 */
	public static Collection<Event> getAllEvents(String location, String distance, String apiKey) {
		Collection<Event> events = null;
		int page = 1, total;
		do {
			PaginatedResult<Event> result = getEvents(location, distance, page, apiKey);
			total = result.getTotalPages();
			Collection<Event> pageResults = result.getPageResults();
			if (events == null) {
				// events is initialized here to initialize it with the right size and avoid array copying later on
				events = new ArrayList<Event>(total * pageResults.size());
			}
			for (Event artist : pageResults) {
				events.add(artist);
			}
			page++;
		} while (page <= total);
		return events;
	}

	/**
	 * Get all events in a specific location by country or city name.<br/>
	 * This method only returns the first page of a possibly paginated result. To retrieve all pages
	 * get the total number of pages via {@link net.roarsoftware.lastfm.PaginatedResult#getTotalPages()} and
	 * subsequently call {@link #getEvents(String, String, int, String)} with the successive page numbers.
	 *
	 * @param location Specifies a location to retrieve events for
	 * @param distance Find events within a specified distance
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} containing a list of events
	 */
	public static PaginatedResult<Event> getEvents(String location, String distance, String apiKey) {
		return getEvents(location, distance, 1, apiKey);
	}

	/**
	 * Get all events in a specific location by country or city name.<br/>
	 * This method only returns the specified page of a paginated result.
	 *
	 * @param location Specifies a location to retrieve events for
	 * @param distance Find events within a specified distance
	 * @param page A page number for pagination
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} containing a list of events
	 */
	public static PaginatedResult<Event> getEvents(String location, String distance, int page, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("page", String.valueOf(page));
		if (location != null)
			params.put("location", location);
		if (distance != null)
			params.put("distance", distance);
		Result result = Caller.getInstance().call("geo.getEvents", apiKey, params);
		if (!result.isSuccessful())
			return new PaginatedResult<Event>(0, 0, Collections.<Event>emptyList());
		DomElement element = result.getContentElement();
		List<Event> events = new ArrayList<Event>();
		for (DomElement domElement : element.getChildren("event")) {
			events.add(Event.eventFromElement(domElement));
		}
		int currentPage = Integer.valueOf(element.getAttribute("page"));
		int totalPages = Integer.valueOf(element.getAttribute("totalpages"));
		return new PaginatedResult<Event>(page, totalPages, events);
	}

	/**
	 * Get all events in a specific location by country or city name.<br/>
	 * This method only returns the specified page of a paginated result.
	 *
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @param page A page number for pagination
	 * @param apiKey A Last.fm API key.
	 * @return a {@link PaginatedResult} containing a list of events
	 */
	public static PaginatedResult<Event> getEvents(double latitude, double longitude, int page, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("page", String.valueOf(page));
		params.put("lat", String.valueOf(latitude));
		params.put("long", String.valueOf(longitude));
		Result result = Caller.getInstance().call("geo.getEvents", apiKey, params);
		if (!result.isSuccessful())
			return new PaginatedResult<Event>(0, 0, Collections.<Event>emptyList());
		DomElement element = result.getContentElement();
		List<Event> events = new ArrayList<Event>();
		for (DomElement domElement : element.getChildren("event")) {
			events.add(Event.eventFromElement(domElement));
		}
		int currentPage = Integer.valueOf(element.getAttribute("page"));
		int totalPages = Integer.valueOf(element.getAttribute("totalpages"));
		return new PaginatedResult<Event>(page, totalPages, events);
	}

	/**
	 * Get the most popular artists on Last.fm by country
	 *
	 * @param country A country name, as defined by the ISO 3166-1 country names standard
	 * @param apiKey A Last.fm API key.
	 * @return list of Artists
	 */
	public static Collection<Artist> getTopArtists(String country, String apiKey) {
		Result result = Caller.getInstance().call("geo.getTopArtists", apiKey, "country", country);
		if (!result.isSuccessful())
			return Collections.emptyList();
		List<Artist> artists = new ArrayList<Artist>();
		for (DomElement domElement : result.getContentElement().getChildren("artist")) {
			artists.add(Artist.artistFromElement(domElement));
		}
		return artists;
	}

	/**
	 * Get the most popular tracks on Last.fm by country
	 *
	 * @param country A country name, as defined by the ISO 3166-1 country names standard
	 * @param apiKey A Last.fm API key.
	 * @return a list of Tracks
	 */
	public static Collection<Track> getTopTracks(String country, String apiKey) {
		Result result = Caller.getInstance().call("geo.getTopTracks", apiKey, "country", country);
		if (!result.isSuccessful())
			return Collections.emptyList();
		List<Track> tracks = new ArrayList<Track>();
		for (DomElement domElement : result.getContentElement().getChildren("track")) {
			tracks.add(Track.trackFromElement(domElement));
		}
		return tracks;
	}

}