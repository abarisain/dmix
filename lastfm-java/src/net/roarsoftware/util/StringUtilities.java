package net.roarsoftware.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utilitiy class with methods to calculate an md5 hash and to encode URLs.
 *
 * @author Janni Kovacs
 */
public class StringUtilities {

	private static MessageDigest digest;
	private static Pattern MBID_PATTERN = Pattern
			.compile("^[0-9a-f]{8}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{12}$",
					Pattern.CASE_INSENSITIVE);
	private static final Pattern MD5_PATTERN = Pattern.compile("[a-zA-Z0-9]{32}");

	static {
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// better never happens
		}
	}

	public static String md5(String s) {
		try {
			byte[] bytes = digest.digest(s.getBytes("UTF-8"));
			StringBuilder b = new StringBuilder(32);
			for (byte aByte : bytes) {
				String hex = Integer.toHexString((int) aByte & 0xFF);
				if (hex.length() == 1)
					b.append('0');
				b.append(hex);
			}
			return b.toString();
		} catch (UnsupportedEncodingException e) {
			// utf-8 always available
		}
		return null;
	}

	public static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// utf-8 always available
		}
		return null;
	}

	public static String decode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// utf-8 always available
		}
		return null;
	}

	public static boolean isMbid(String artistOrMbid) {
		// example: bfcc6d75-a6a5-4bc6-8282-47aec8531818
		return artistOrMbid.length() == 36 && MBID_PATTERN.matcher(artistOrMbid).matches();
	}

	/**
	 * Creates a Map out of an array with Strings.
	 *
	 * @param strings input strings, key-value alternating
	 * @return a parameter map
	 */
	public static Map<String, String> map(String... strings) {
		if (strings.length % 2 != 0)
			throw new IllegalArgumentException("strings.length % 2 != 0");
		Map<String, String> mp = new HashMap<String, String>();
		for (int i = 0; i < strings.length; i += 2) {
			mp.put(strings[i], strings[i + 1]);
		}
		return mp;
	}

	/**
	 * Strips all characters from a String, that might be invalid to be used in file names.
	 * By default <tt>: / \ < > | ? " *</tt> are all replaced by <tt>-</tt>.
	 * Note that this is no guarantee that the returned name will be definately valid.
	 *
	 * @param s the String to clean up
	 * @return the cleaned up String
	 */
	public static String cleanUp(String s) {
		return s.replaceAll("[*:/\\\\?|<>\"]", "-");
	}

	/**
	 * Tests if the given string might already be a 32-char md5 string.
	 *
	 * @param s String to test
	 * @return <code>true</code> if the given String might be a md5 string
	 */
	public static boolean isMD5(String s) {
		return s.length() == 32 && MD5_PATTERN.matcher(s).matches();
	}
}
