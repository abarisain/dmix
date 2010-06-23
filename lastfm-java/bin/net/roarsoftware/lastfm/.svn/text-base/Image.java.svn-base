package net.roarsoftware.lastfm;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.roarsoftware.xml.DomElement;

/**
 * An <code>Image</code> contains metadata and URLs for an artist's image. Metadata contains title, votes, format and other.
 * Images are available in various sizes, see {@link ImageSize} for all sizes.
 *
 * @author Janni Kovacs
 * @see ImageSize
 */
public class Image extends ImageHolder {

	private static final DateFormat DATE_ADDED_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss",
			Locale.ENGLISH);

	private String title;
	private String url;
	private Date dateAdded;
	private String format;
	private String owner;

	private int thumbsUp, thumbsDown;

	private Image() {
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public Date getDateAdded() {
		return dateAdded;
	}

	public String getFormat() {
		return format;
	}

	public String getOwner() {
		return owner;
	}

	public int getThumbsUp() {
		return thumbsUp;
	}

	public int getThumbsDown() {
		return thumbsDown;
	}

	static Image imageFromElement(DomElement e) {
		Image i = new Image();
		i.title = e.getChildText("title");
		i.url = e.getChildText("url");
		i.format = e.getChildText("format");
		try {
			i.dateAdded = DATE_ADDED_FORMAT.parse(e.getChildText("dateadded"));
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		DomElement owner = e.getChild("owner");
		if (owner != null)
			i.owner = owner.getChildText("name");
		DomElement votes = e.getChild("votes");
		if (votes != null) {
			i.thumbsUp = Integer.parseInt(votes.getChildText("thumbsup"));
			i.thumbsDown = Integer.parseInt(votes.getChildText("thumbsdown"));
		}
		DomElement sizes = e.getChild("sizes");
		for (DomElement image : sizes.getChildren("size")) {
			// code copied from ImageHolder.loadImages
			String attribute = image.getAttribute("name");
			ImageSize size;
			if (attribute == null)
				size = ImageSize.MEDIUM; // workaround for image responses without size attr.
			else
				size = ImageSize.valueOf(attribute.toUpperCase(Locale.ENGLISH));
			i.imageUrls.put(size, image.getText());
		}
		return i;
	}
}
