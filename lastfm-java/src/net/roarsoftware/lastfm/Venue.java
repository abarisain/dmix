package net.roarsoftware.lastfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.roarsoftware.xml.DomElement;

/**
 * Venue information bean.
 *
 * @author Janni Kovacs
 */
public class Venue {

	private String name;
	private String url;
	private String city, country, street, postal;

	private float latitude, longitude;
	private String timezone;
	private String id;

	private Venue() {
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	public float getLatitude() {
		return latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public String getName() {
		return name;
	}

	public String getPostal() {
		return postal;
	}

	public String getStreet() {
		return street;
	}

	public String getTimezone() {
		return timezone;
	}

	/**
	 * Search for a venue by venue name.
	 *
	 * @param venue The venue name you would like to search for
	 * @param apiKey A Last.fm API key
	 * @return a list of venues
	 */
	public static Collection<Venue> search(String venue, String apiKey) {
		return search(venue, null, apiKey);
	}

	/**
	 * Search for a venue by venue name.
	 *
	 * @param venue The venue name you would like to search for
	 * @param country Filter your results by country. Expressed as an ISO 3166-2 code
	 * @param apiKey A Last.fm API key
	 * @return a list of venues
	 */
	public static Collection<Venue> search(String venue, String country, String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("venue", venue);
		if (country != null)
			params.put("country", country);
		Result result = Caller.getInstance().call("venue.search", apiKey, params);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement child = result.getContentElement().getChild("venuematches");
		Collection<DomElement> children = child.getChildren("venue");
		List<Venue> venues = new ArrayList<Venue>(children.size());
		for (DomElement element : children) {
			venues.add(venueFromElement(element));
		}
		return venues;
	}

	/**
	 * Get a list of upcoming events at this venue.
	 *
	 * @param venueId The venue id to fetch the events for
	 * @param apiKey A Last.fm API key
	 * @return a list of events
	 * @see #getPastEvents
	 */
	public static Collection<Event> getEvents(String venueId, String apiKey) {
		Result result = Caller.getInstance().call("venue.getEvents", apiKey, "venue", venueId);
		if (!result.isSuccessful())
			return Collections.emptyList();
		Collection<DomElement> children = result.getContentElement().getChildren("event");
		List<Event> events = new ArrayList<Event>(children.size());
		for (DomElement child : children) {
			events.add(Event.eventFromElement(child));
		}
		return events;
	}

	/**
	 * Get a paginated list of all the events held at this venue in the past.
	 *
	 * @param venueId The id for the venue you would like to fetch event listings for
	 * @param apiKey A Last.fm API key
	 * @return a paginated list of events
	 */
	public static PaginatedResult<Event> getPastEvents(String venueId, String apiKey) {
		return getPastEvents(venueId, 1, apiKey);
	}

	/**
	 * Get a paginated list of all the events held at this venue in the past.
	 *
	 * @param venueId The id for the venue you would like to fetch event listings for
	 * @param page The page of results to return
	 * @param apiKey A Last.fm API key
	 * @return a paginated list of events
	 */
	public static PaginatedResult<Event> getPastEvents(String venueId, int page, String apiKey) {
		Result result = Caller.getInstance()
				.call("venue.getPastEvents", apiKey, "venue", venueId, "page", String.valueOf(page));
		if (!result.isSuccessful())
			return new PaginatedResult<Event>(0, 0, Collections.<Event>emptyList());
		DomElement element = result.getContentElement();
		Collection<DomElement> children = element.getChildren("event");
		List<Event> events = new ArrayList<Event>(children.size());
		for (DomElement child : children) {
			events.add(Event.eventFromElement(child));
		}
		int currentPage = Integer.parseInt(element.getAttribute("page"));
		int totalPages = Integer.parseInt(element.getAttribute("totalPages"));
		return new PaginatedResult<Event>(currentPage, totalPages, events);
	}

	static Venue venueFromElement(DomElement e) {
		Venue venue = new Venue();
		venue.id = e.getChildText("id");
		venue.name = e.getChildText("name");
		venue.url = e.getChildText("url");
		DomElement l = e.getChild("location");
		venue.city = l.getChildText("city");
		venue.country = l.getChildText("country");
		venue.street = l.getChildText("street");
		venue.postal = l.getChildText("postalcode");
		venue.timezone = l.getChildText("timezone");
		DomElement p = l.getChild("geo:point");
		if (p.getChildText("geo:lat").length() != 0) { // some venues don't have geo information applied
			venue.latitude = Float.parseFloat(p.getChildText("geo:lat"));
			venue.longitude = Float.parseFloat(p.getChildText("geo:long"));
		}
		return venue;
	}
}
