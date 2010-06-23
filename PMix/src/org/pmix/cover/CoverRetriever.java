package org.pmix.cover;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class CoverRetriever {

	private final static String URL = "http://ecs.amazonaws.%s/onca/xml?Service=AWSECommerceService&Operation=ItemSearch&SearchIndex=Music&ResponseGroup=Images,EditorialReview&SubscriptionId=%s&Artist=%s&%s=%s";

	private final static String AMAZON_KEY = "14TC04B24356BPHXW1R2";

	private static DocumentBuilder builder;

	static {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			builder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private static String getURL(String artist, String album) throws UnsupportedEncodingException {
		return String.format(URL, new Object[] { "fr", AMAZON_KEY, urlEncode(artist), "Keywords", urlEncode(album)});
	}

	private static String urlEncode(String a) throws UnsupportedEncodingException {
		a = a.replace('\u0232', 'e').replace('\u0232', 'e').replace('\u0234', 'e').replace('\u0235', 'e');
		a = a.replace('\u0224', 'a').replace('\u0239', 'i').replace('\u0238', 'i');

		return URLEncoder.encode(a, "utf-8");
	}

	public static String getCoverUrl(String artist, String album) throws Exception {
		String url = getURL(artist, album);
		URL u = new URL(url);

		URLConnection connection = u.openConnection();

		Document document = builder.parse(connection.getInputStream());

		String firstImage = null;
		/*
		 * if java xpath were available : XPath xpath =
		 * XPathFactory.newInstance().newXPath(); String expression =
		 * "/ItemSearchResponse/Items/Item/MediumImage/URL"; firstImage =
		 * (String) xpath.evaluate(expression, document, XPathConstants.STRING);
		 */

		NodeList list = document.getElementsByTagName("URL");

		if (list != null & list.getLength() > 0) {
			if (list.getLength() > 1) {
				firstImage = list.item(1).getFirstChild().getNodeValue();
			}

			else
				firstImage = list.item(0).getFirstChild().getNodeValue();
		}

		return firstImage;
	}

	/**
	 * @param args
	 * @throws UnsupportedEncodingException
	 */
	public static void main(String[] args) throws Exception {

		String artist = "The Hoosiers";
		String album = "a trick to life";

		CoverRetriever coverRetriever = new CoverRetriever();

		String url = coverRetriever.getCoverUrl(artist, album);

		System.out.println(url);

	}
}
