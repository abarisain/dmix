package examples;

import java.text.DateFormat;

import de.umass.lastfm.Event;
import de.umass.lastfm.Geo;
import de.umass.lastfm.PaginatedResult;

/**
 * @author Janni Kovacs
 */
public class EventExample {

	public static void main(String[] args) {
		String key = "b25b959554ed76058ac220b7b2e0a026"; //this is the key used in the last.fm API examples online.
		PaginatedResult<Event> events = Geo.getEvents("Berlin", null, key);
		System.out.println("Events in Berlin, Germany:");
		for (Event event : events.getPageResults()) {
			System.out.printf("%s: %s - %s%n", event.getHeadliner(), event.getVenue().getName(),
					DateFormat.getDateInstance().format(event.getStartDate()));
		}
	}
}
