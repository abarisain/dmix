package examples;

import java.util.Collection;

import net.roarsoftware.lastfm.Artist;
import net.roarsoftware.lastfm.Track;

/**
 * @author Janni Kovacs
 */
public class ArtistExample {

	public static void main(String[] args) {
		String key = "b25b959554ed76058ac220b7b2e0a026"; //this is the key used in the last.fm API examples online.
		Collection<Track> topTracks = Artist.getTopTracks("Depeche Mode", key);
		System.out.println("Top Tracks for Depeche Mode:");
		for (Track track : topTracks) {
			System.out.printf("%s (%d plays)%n", track.getName(), track.getPlaycount());
		}
	}
}
