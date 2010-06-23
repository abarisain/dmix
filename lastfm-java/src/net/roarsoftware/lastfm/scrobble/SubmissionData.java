package net.roarsoftware.lastfm.scrobble;

import static net.roarsoftware.util.StringUtilities.decode;
import static net.roarsoftware.util.StringUtilities.encode;

/**
 * Bean that contains track information.
 *
 * @author Janni Kovacs
 */
public class SubmissionData {

	private String artist;
	private String track;
	private String album;
	private long startTime;
	private Source source;
	private Rating rating;
	private String recommendationKey;
	private int length;
	private int tracknumber;

	public SubmissionData(String artist, String track, String album, int length, int tracknumber, Source source,
						  long startTime) {
		this(artist, track, album, length, tracknumber, source, null, startTime);
	}

	public SubmissionData(String artist, String track, String album, int length, int tracknumber, Source source,
						  Rating rating, long startTime) {
		this.artist = artist;
		this.track = track;
		this.album = album;
		this.length = length;
		this.tracknumber = tracknumber;
		this.source = source;
		this.rating = rating;
		this.startTime = startTime;
	}

	public SubmissionData(String artist, String track, String album, int length, int tracknumber, Source source,
						  Rating rating, long startTime, String recommendationKey) {
		this(artist, track, album, length, tracknumber, source, rating, startTime);
		this.recommendationKey = recommendationKey;
	}

	/**
	 * Creates a new SubmissionData object based on a String returned by {@link #toString()}.
	 *
	 * @param s A String
	 */
	public SubmissionData(String s) {
		String[] parts = s.split("&", 9);
		artist = decode(parts[0]);
		track = decode(parts[1]);
		startTime = parts[2].length() == 0 ? 0 : Long.valueOf(parts[2]);
		source = Source.valueOf(parts[3]);
		recommendationKey = parts[4].length() == 0 ? null : parts[4];
		rating = parts[5].length() == 0 ? null : Rating.valueOf(parts[5]);
		length = parts[6].length() == 0 ? -1 : Integer.valueOf(parts[6]);
		album = parts[7].length() == 0 ? null : decode(parts[7]);
		tracknumber = parts[8].length() == 0 ? -1 : Integer.valueOf(parts[8]);
	}

	/**
	 * Returns a String representation of this submission with the fields separated by &.
	 * Order of the fields is:<br/>
	 * <tt>artist&track&startTime&Source&RecommendationKey&Rating&length&album&tracknumber</tt><br/>
	 * Note that:
	 * - Values may possibly be <code>null</code> or empty
	 * - enum values such as Rating and Source are <code>null</code> or their constant name is used (i.e. "LOVE")
	 * - all string values (artist, track, album) are utf8-url-encoded
	 *
	 * @return a String
	 */
	public String toString() {
		String b = encode(album != null ? album : "");
		String artist = encode(this.artist);
		String track = encode(this.track);
		String l = length == -1 ? "" : String.valueOf(length);
		String n = tracknumber == -1 ? "" : String.valueOf(tracknumber);

		String r = "";
		if (rating != null)
			r = rating.name();
		String rec = "";
		if (recommendationKey != null && source == Source.LAST_FM && recommendationKey.length() == 5)
			rec = recommendationKey;

		return String.format("%s&%s&%s&%s&%s&%s&%s&%s&%s", artist, track, startTime, source.name(), rec, r, l, b, n);
	}

	String toString(String sessionId, int index) {
		String b = encode(album != null ? album : "");
		String artist = encode(this.artist);
		String track = encode(this.track);
		String l = length == -1 ? "" : String.valueOf(length);
		String n = tracknumber == -1 ? "" : String.valueOf(tracknumber);

		String r = "";
		if (rating != null)
			r = rating.getCode();
		String rec = "";
		if (recommendationKey != null && source == Source.LAST_FM && recommendationKey.length() == 5)
			rec = recommendationKey;

		return String
				.format("s=%s&a[%10$d]=%s&t[%10$d]=%s&i[%10$d]=%s&o[%10$d]=%s&r[%10$d]=%s&l[%10$d]=%s&b[%10$d]=%s&n[%10$d]=%s&m[%10$d]=",
						sessionId, artist, track, startTime, source.getCode() + rec, r, l, b, n, index);
	}

}
