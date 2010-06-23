package net.roarsoftware.lastfm;

import java.util.*;

import net.roarsoftware.xml.DomElement;

/**
 * Bean for Chart information. Contains a start date, an end date and a list of entries.
 *
 * @author Janni Kovacs
 */
public class Chart<T extends MusicEntry> {

	private Date from, to;
	private Collection<T> entries;

	public Chart(Date from, Date to, Collection<T> entries) {
		this.from = from;
		this.to = to;
		this.entries = entries;
	}

	public Collection<T> getEntries() {
		return entries;
	}

	public Date getFrom() {
		return from;
	}

	public Date getTo() {
		return to;
	}

	/**
	 * This is an internal method to retrieve Chart data.
	 *
	 * @param method The method to call, must be one of the getWeeklyXXXChart methods
	 * @param sourceType The name of the parameter to get the charts for, either "user", "tag" or "group"
	 * @param source The username, tag or group to get charts from
	 * @param target The expected chart type, either "album", "artist" or "track"
	 * @param from Start date or <code>null</code>
	 * @param to End date or <code>null</code>
	 * @param limit The number of chart items to return or -1
	 * @param apiKey A Last.fm API key.
	 * @return a Chart
	 */
	@SuppressWarnings("unchecked")
	static <T extends MusicEntry> Chart<T> getChart(String method, String sourceType, String source,
													String target, String from, String to, int limit,
													String apiKey) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(sourceType, source);
		if (from != null && to != null) {
			params.put("from", from);
			params.put("to", to);
		}
		if (limit != -1) {
			params.put("limit", String.valueOf(limit));
		}
		Result result = Caller.getInstance().call(method, apiKey, params);
		if (!result.isSuccessful())
			return null;
		DomElement element = result.getContentElement();
		Collection<DomElement> children = element.getChildren(target);
		Collection collection = new ArrayList(children.size());
		boolean targetArtist = "artist".equals(target);
		boolean targetTrack = "track".equals(target);
		boolean targetAlbum = "album".equals(target);
		for (DomElement domElement : children) {
			if (targetArtist)
				collection.add(Artist.artistFromElement(domElement));
			if (targetTrack)
				collection.add(Track.trackFromElement(domElement));
			if (targetAlbum)
				collection.add(Album.albumFromElement(domElement));
		}
		long fromTime = 1000 * Long.parseLong(element.getAttribute("from"));
		long toTime = 1000 * Long.parseLong(element.getAttribute("to"));
		return new Chart<T>(new Date(fromTime), new Date(toTime), collection);
	}

	/**
	 * This is an internal method to get a list of available charts.
	 *
	 * @param sourceType The name of the parameter to get the charts for, either "user", "tag" or "group"
	 * @param source The username, tag or group to get charts from
	 * @param apiKey A Last.fm API key.
	 * @return a list of available charts as a Map
	 */
	static LinkedHashMap<String, String> getWeeklyChartList(String sourceType, String source, String apiKey) {
		Result result = Caller.getInstance().call(sourceType + ".getWeeklyChartList", apiKey, sourceType, source);
		if (!result.isSuccessful())
			return new LinkedHashMap<String, String>(0);
		DomElement element = result.getContentElement();
		LinkedHashMap<String, String> list = new LinkedHashMap<String, String>();
		for (DomElement domElement : element.getChildren("chart")) {
			list.put(domElement.getAttribute("from"), domElement.getAttribute("to"));
		}
		return list;
	}

	/**
	 * This is an internal method to get a list of available charts.
	 *
	 * @param sourceType The name of the parameter to get the charts for, either "user", "tag" or "group"
	 * @param source The username, tag or group to get charts from
	 * @param apiKey A Last.fm API key.
	 * @return a list of available charts as a Collection of Charts
	 */
	@SuppressWarnings("unchecked")
	static Collection<Chart> getWeeklyChartListAsCharts(String sourceType, String source, String apiKey) {
		Result result = Caller.getInstance().call(sourceType + ".getWeeklyChartList", apiKey, sourceType, source);
		if (!result.isSuccessful())
			return Collections.emptyList();
		DomElement element = result.getContentElement();
		List<Chart> list = new ArrayList<Chart>();
		for (DomElement domElement : element.getChildren("chart")) {
			long fromTime = 1000 * Long.parseLong(domElement.getAttribute("from"));
			long toTime = 1000 * Long.parseLong(domElement.getAttribute("to"));
			list.add(new Chart(new Date(fromTime), new Date(toTime), null));
		}
		return list;
	}
}
