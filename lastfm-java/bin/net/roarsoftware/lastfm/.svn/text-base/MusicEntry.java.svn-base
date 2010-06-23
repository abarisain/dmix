package net.roarsoftware.lastfm;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import net.roarsoftware.xml.DomElement;

/**
 * <code>MusicEntry</code> is the abstract superclass for {@link Track} {@link Artist} and {@link Album}.
 * It encapsulates data and provides methods used in all subclasses, for example: name, playcount,
 * images and more.
 *
 * @author Janni Kovacs
 */
public abstract class MusicEntry extends ImageHolder {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ",
			Locale.ENGLISH);

	protected String name;
	protected String url;
	protected String mbid;
	protected int playcount;
	protected int listeners;
	protected boolean streamable;

	protected Collection<String> tags = new ArrayList<String>();
	private Date wikiLastChanged;
	private String wikiSummary;
	private String wikiText;

	protected MusicEntry(String name, String url) {
		this(name, url, null, -1, -1, false);
	}

	protected MusicEntry(String name, String url, String mbid, int playcount, int listeners, boolean streamable) {
		this.name = name;
		this.url = url;
		this.mbid = mbid;
		this.playcount = playcount;
		this.listeners = listeners;
		this.streamable = streamable;
	}

	public int getListeners() {
		return listeners;
	}

	public String getMbid() {
		return mbid;
	}

	public String getName() {
		return name;
	}

	public int getPlaycount() {
		return playcount;
	}

	public boolean isStreamable() {
		return streamable;
	}

	public String getUrl() {
		return url;
	}

	public Collection<String> getTags() {
		return tags;
	}

	/**
	 * Loads all generic information from an XML <code>DomElement</code> into the given <code>MusicEntry</code>
	 * instance, i.e. the following tags:<br/>
	 * <ul>
	 * <li>playcount/plays</li>
	 * <li>listeners</li>
	 * <li>streamable</li>
	 * <li>name</li>
	 * <li>url</li>
	 * <li>mbid</li>
	 * <li>image</li>
	 * <li>tags</li>
	 * </ul>
	 *
	 * @param entry An entry
	 * @param element XML source element
	 */
	protected static void loadStandardInfo(MusicEntry entry, DomElement element) {
		// playcount & listeners
		DomElement statsChild = element.getChild("stats");
		String playcountString;
		String listenersString;
		if (statsChild != null) {
			playcountString = statsChild.getChildText("playcount");
			listenersString = statsChild.getChildText("listeners");
		} else {
			playcountString = element.getChildText("playcount");
			listenersString = element.getChildText("listeners");
		}
		int playcount = playcountString == null || playcountString.length() == 0 ? -1 : Integer
				.parseInt(playcountString);
		int listeners = listenersString == null || listenersString.length() == 0 ? -1 : Integer
				.parseInt(listenersString);
		// streamable
		String s = element.getChildText("streamable");
		boolean streamable = s != null && s.length() != 0 && Integer.parseInt(s) == 1;
		// copy
		entry.name = element.getChildText("name");
		entry.url = element.getChildText("url");
		entry.mbid = element.getChildText("mbid");
		entry.playcount = playcount;
		entry.listeners = listeners;
		entry.streamable = streamable;
		// tags
		DomElement tags = element.getChild("tags");
		if (tags == null)
			tags = element.getChild("toptags");
		if (tags != null) {
			for (DomElement tage : tags.getChildren("tag")) {
				entry.tags.add(tage.getChildText("name"));
			}
		}
		// wiki
		DomElement wiki = element.getChild("bio");
		if (wiki == null)
			wiki = element.getChild("wiki");
		if (wiki != null) {
			String publishedText = wiki.getChildText("published");
			try {
				entry.wikiLastChanged = DATE_FORMAT.parse(publishedText);
			} catch (ParseException e) {
				// try parsing it with current locale
				try {
					DateFormat clFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ", Locale.getDefault());
					entry.wikiLastChanged = clFormat.parse(publishedText);
				} catch (ParseException e2) {
					// cannot parse date, wrong locale. wait for last.fm to fix.
				}
			}
			entry.wikiSummary = wiki.getChildText("summary");
			entry.wikiText = wiki.getChildText("content");
		}
		// images
		ImageHolder.loadImages(entry, element);
	}

	public Date getWikiLastChanged() {
		return wikiLastChanged;
	}

	public String getWikiSummary() {
		return wikiSummary;
	}

	public String getWikiText() {
		return wikiText;
	}
}
