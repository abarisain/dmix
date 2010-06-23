package net.roarsoftware.lastfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.roarsoftware.xml.DomElement;

/**
 * Provides the binding for the "tasteometer.compare" method.
 *
 * @author Janni Kovacs
 */
public class Tasteometer {

	private Tasteometer() {
	}

	/**
	 * Get a Tasteometer score from two inputs, along with a list of shared artists.
	 *
	 * @param type1 Type of the first input
	 * @param value1 First input value
	 * @param type2 Type of the second input
	 * @param value2 Second input value
	 * @param apiKey The Last.fm API key
	 * @return result of Tasteometer comparison
	 */
	public static ComparisonResult compare(InputType type1, String value1, InputType type2, String value2,
										   String apiKey) {
		Result result = Caller.getInstance()
				.call("tasteometer.compare", apiKey, "type1", type1.name().toLowerCase(), "type2",
						type2.name().toLowerCase(), "value1", value1, "value2", value2);
		if (!result.isSuccessful())
			return null;
		DomElement element = result.getContentElement();
		DomElement re = element.getChild("result");
		float score = Float.parseFloat(re.getChildText("score"));
		List<Artist> artists = new ArrayList<Artist>();
		for (DomElement domElement : re.getChild("artists").getChildren("artist")) {
			artists.add(Artist.artistFromElement(domElement));
		}
		return new ComparisonResult(score, artists);
	}

	/**
	 * Contains the result of a tasteometer comparison, i.e. the score (0.0-1.0) and a list of
	 * shared artists.
	 */
	public static class ComparisonResult {

		private float score;
		private Collection<Artist> matches;

		ComparisonResult(float score, Collection<Artist> matches) {
			this.score = score;
			this.matches = matches;
		}

		/**
		 * Returns a list of artist matches, i.e. artists both partys listen to.
		 *
		 * @return artist matches
		 */
		public Collection<Artist> getMatches() {
			return matches;
		}

		/**
		 * Returns the compatability score between 0.0 (no compatability) and 1.0 (highest compatability).
		 *
		 * @return the score
		 */
		public float getScore() {
			return score;
		}
	}

	/**
	 * Specifies the type of the input for the {@linkplain Tasteometer#compare Tasteometer.compare} operation.
	 */
	public enum InputType {
		USER,
		ARTISTS,
		MYSPACE
	}
}
