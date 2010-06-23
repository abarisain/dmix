package net.roarsoftware.lastfm;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.roarsoftware.xml.DomElement;

/**
 * Provides access to the Last.fm radio streaming service.<br/>
 * Note that you have to be a subscriber (or have a special API key) to use this API.
 * Official documentation can be found here <a href="http://www.last.fm/api/radio">http://www.last.fm/api/radio</a>
 *
 * @author Janni Kovacs
 */
public class Radio {

	private String type;
	private String stationName;
	private String stationUrl;
	private boolean supportsDiscovery;

	private Session session;
	private int expiry = -1;

	private Radio(Session session) {
		this.session = session;
	}

	public String getType() {
		return type;
	}

	public String getStationName() {
		return stationName;
	}

	public String getStationUrl() {
		return stationUrl;
	}

	public boolean supportsDiscovery() {
		return supportsDiscovery;
	}

	/**
	 * Returns the playlist expiration value for the last playlist fetchet, or -1 if no playlist has been fetched yet.
	 *
	 * @return playlist expiration in seconds
	 */
	public int playlistExpiresIn() {
		return expiry;
	}

	/**
	 * Tune in to a Last.fm radio station.
	 *
	 * @param station An instance of {@link RadioStation}
	 * @param session A Session instance
	 * @return a Radio instance
	 */
	public static Radio tune(RadioStation station, Session session) {
		return tune(station, Locale.getDefault(), session);
	}

	/**
	 * Tune in to a Last.fm radio station.
	 *
	 * @param station An instance of {@link RadioStation}
	 * @param locale The language you want the radio's name in
	 * @param session A Session instance
	 * @return a Radio instance
	 */
	public static Radio tune(RadioStation station, Locale locale, Session session) {
		return tune(station.getUrl(), locale, session);
	}

	/**
	 * Tune in to a Last.fm radio station.
	 *
	 * @param station A lastfm radio URL
	 * @param locale The language you want the radio's name in
	 * @param session A Session instance
	 * @return a Radio instance
	 */
	public static Radio tune(String station, Locale locale, Session session) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("station", station);
		if (locale != null && locale.getLanguage().length() != 0) {
			params.put("lang", locale.getLanguage());
		}
		Result result = Caller.getInstance().call("radio.tune", session, params);
		if (!result.isSuccessful())
			return null;
		DomElement root = result.getContentElement();
		Radio radio = new Radio(session);
		radio.type = root.getChildText("type");
		radio.stationName = root.getChildText("name");
		radio.stationUrl = root.getChildText("url");
		radio.supportsDiscovery = "1".equals(root.getChildText("type"));
		return radio;
	}

	/**
	 * Fetches a new radio playlist.
	 *
	 * @return a new Playlist
	 */
	public Playlist getPlaylist() {
		return getPlaylist(false, true);
	}

	/**
	 * Fetches a new radio playlist.
	 *
	 * @param discovery Whether to request last.fm content with discovery mode switched on
	 * @param rtp Whether the user is scrobbling or not during this radio session (helps content generation)
	 * @return a new Playlist
	 */
	public Playlist getPlaylist(boolean discovery, boolean rtp) {
		Result result = Caller.getInstance()
				.call("radio.getPlaylist", session, "discovery", String.valueOf(discovery), "rtp", String.valueOf(rtp));
//		Result result = Caller.getInstance().call("radio.getPlaylist", session);
		if (!result.isSuccessful())
			return null;
		DomElement root = result.getContentElement();
		for (DomElement e : root.getChildren("link")) {
			if ("http://www.last.fm/expiry".equals(e.getAttribute("rel"))) {
				this.expiry = Integer.parseInt(e.getText());
				break;
			}
		}
		return Playlist.playlistFromElement(root);
	}

	public static class RadioStation {
		private String url;

		private RadioStation(String s) {
			this.url = s;
		}

		public String getUrl() {
			return url;
		}

		public static RadioStation similarArtists(String artist) {
			return new RadioStation("lastfm://artist/" + artist + "/similarartists");
		}

		public static RadioStation artistFans(String artist) {
			return new RadioStation("lastfm://artist/" + artist + "/fans");
		}

		public static RadioStation library(String user) {
			return new RadioStation("lastfm://user/" + user + "/library");
		}

		public static RadioStation neighbours(String user) {
			return new RadioStation("lastfm://user/" + user + "/neighbours");
		}

		public static RadioStation lovedTracks(String user) {
			return new RadioStation("lastfm://user/" + user + "/loved");
		}

		public static RadioStation recommended(String user) {
			return new RadioStation("lastfm://user/" + user + "/recommended");
		}

		public static RadioStation tagged(String tag) {
			return new RadioStation("lastfm://globaltags/" + tag);
		}

		public static RadioStation playlist(String playlistId) {
			return new RadioStation("lastfm://playlist/" + playlistId);
		}

		public static RadioStation personalTag(String user, String tag) {
			return new RadioStation("lastfm://usertags/" + user + "/" + tag);
		}
	}
}
