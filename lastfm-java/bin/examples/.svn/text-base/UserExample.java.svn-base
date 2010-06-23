package examples;

import java.text.DateFormat;
import java.util.Collection;

import net.roarsoftware.lastfm.Artist;
import net.roarsoftware.lastfm.Chart;
import net.roarsoftware.lastfm.User;

/**
 * @author Janni Kovacs
 */
public class UserExample {

	public static void main(String[] args) {
		String key = "b25b959554ed76058ac220b7b2e0a026"; //this is the key used in the last.fm API examples online.
		String user = "JRoar";
		Chart<Artist> chart = User.getWeeklyArtistChart(user, 10, key);
		DateFormat format = DateFormat.getDateInstance();
		String from = format.format(chart.getFrom());
		String to = format.format(chart.getTo());
		System.out.printf("Charts for %s for the week from %s to %s:%n", user, from, to);
		Collection<Artist> artists = chart.getEntries();
		for (Artist artist : artists) {
			System.out.println(artist.getName());
		}
	}
}
