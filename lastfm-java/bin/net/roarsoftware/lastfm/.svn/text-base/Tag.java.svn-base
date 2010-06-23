package net.roarsoftware.lastfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import net.roarsoftware.xml.DomElement;

/**
 * Bean for Tag data and provides methods for global tags.
 *
 * @author Janni Kovacs
 */
public class Tag implements Comparable<Tag> {

	private String name;
	private String url;
	private int count;

	private Tag() {
	}

	Tag(String name) {
		this.name = name;
	}

	public int getCount() {
		return count;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * Returns the sum of all <code>count</code> elements in the results.
	 *
	 * @param tags a list of tags
	 * @return the total count of all tags
	 */
	public static long getTagCountSum(Collection<Tag> tags) {
		long total = 0;
		for (Tag topTag : tags) {
			total += topTag.count;
		}
		return total;
	}

	/**
	 * Filters tags from the given list; retains only those tags with a count
	 * higher than the given percentage of the total sum as from
	 * {@link #getTagCountSum(Collection)}.
	 *
	 * @param tags list of tags
	 * @param percentage cut off percentage
	 * @return the filtered list of tags
	 */
	public static List<Tag> filter(Collection<Tag> tags, double percentage) {
		ArrayList<Tag> tops = new ArrayList<Tag>();
		long total = getTagCountSum(tags);
		double cutOff = total / 100.0 * percentage;
		for (Tag tag : tags) {
			if (tag.count > cutOff) {
				tops.add(tag);
			}
		}
		return tops;
	}

	public static Collection<String> getSimilar(String tag, String apiKey) {
		Result result = Caller.getInstance().call("tag.getSimilar", apiKey, "tag", tag);
		if (!result.isSuccessful())
			return Collections.emptyList();
		List<String> tags = new ArrayList<String>();
		for (DomElement domElement : result.getContentElement().getChildren("tag")) {
			tags.add(domElement.getChildText("name"));
		}
		return tags;
	}

	public static List<Tag> getTopTags(String apiKey) {
		Result result = Caller.getInstance().call("tag.getTopTags", apiKey);
		if (!result.isSuccessful())
			return Collections.emptyList();
		List<Tag> tags = new ArrayList<Tag>();
		for (DomElement domElement : result.getContentElement().getChildren("tag")) {
			tags.add(tagFromElement(domElement));
		}
		return tags;
	}

	public static Collection<Album> getTopAlbums(String tag, String apiKey) {
		Result result = Caller.getInstance().call("tag.getTopAlbums", apiKey, "tag", tag);
		if (!result.isSuccessful())
			return Collections.emptyList();
		List<Album> albums = new ArrayList<Album>();
		for (DomElement domElement : result.getContentElement().getChildren("album")) {
			albums.add(Album.albumFromElement(domElement));
		}
		return albums;
	}

	public static Collection<Track> getTopTracks(String tag, String apiKey) {
		Result result = Caller.getInstance().call("tag.getTopTracks", apiKey, "tag", tag);
		if (!result.isSuccessful())
			return Collections.emptyList();
		List<Track> tracks = new ArrayList<Track>();
		for (DomElement domElement : result.getContentElement().getChildren("track")) {
			tracks.add(Track.trackFromElement(domElement));
		}
		return tracks;
	}

	public static Collection<Artist> getTopArtists(String tag, String apiKey) {
		Result result = Caller.getInstance().call("tag.getTopArtists", apiKey, "tag", tag);
		if (!result.isSuccessful())
			return Collections.emptyList();
		List<Artist> artists = new ArrayList<Artist>();
		for (DomElement domElement : result.getContentElement().getChildren("artist")) {
			artists.add(Artist.artistFromElement(domElement));
		}
		return artists;
	}

	public static Collection<String> search(String tag, String apiKey) {
		return search(tag, 30, apiKey);
	}

	public static Collection<String> search(String tag, int limit, String apiKey) {
		Result result = Caller.getInstance().call("tag.search", apiKey, "tag", tag, "limit", String.valueOf(limit));
		List<String> tags = new ArrayList<String>();
		for (DomElement s : result.getContentElement().getChild("tagmatches").getChildren("tag")) {
			tags.add(s.getChildText("name"));
		}
		return tags;
	}

	public static Chart<Artist> getWeeklyArtistChart(String tag, String apiKey) {
		return getWeeklyArtistChart(tag, null, null, -1, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String tag, int limit, String apiKey) {
		return getWeeklyArtistChart(tag, null, null, limit, apiKey);
	}

	public static Chart<Artist> getWeeklyArtistChart(String tag, String from, String to, int limit, String apiKey) {
		return Chart.getChart("tag.getWeeklyArtistChart", "tag", tag, "artist", from, to, limit, apiKey);
	}

	public static LinkedHashMap<String, String> getWeeklyChartList(String tag, String apiKey) {
		return Chart.getWeeklyChartList("tag", tag, apiKey);
	}

	public static Collection<Chart> getWeeklyChartListAsCharts(String tag, String apiKey) {
		return Chart.getWeeklyChartListAsCharts("tag", tag, apiKey);
	}

	static Tag tagFromElement(DomElement element) {
		Tag t = new Tag(element.getChildText("name"));
		t.url = element.getChildText("url");
		if (element.hasChild("count"))
			t.count = Integer.parseInt(element.getChildText("count"));
		return t;
	}

	public int compareTo(Tag o) {
		// descending order
		return Double.compare(o.getCount(), this.getCount());
	}
}
