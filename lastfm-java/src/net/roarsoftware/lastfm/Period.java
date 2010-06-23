package net.roarsoftware.lastfm;

/**
 * @author Janni Kovacs
 */
public enum Period {

	OVERALL("overall"),
	THREE_MONTHS("3month"),
	SIX_MONTHS("6month"),
	TWELVE_MONTHS("12month");

	private String string;

	Period(String string) {
		this.string = string;
	}

	public String getString() {
		return string;
	}
}
