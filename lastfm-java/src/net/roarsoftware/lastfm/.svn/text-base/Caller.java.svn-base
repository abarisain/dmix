package net.roarsoftware.lastfm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.roarsoftware.lastfm.Result.Status;
import net.roarsoftware.lastfm.cache.Cache;
import net.roarsoftware.lastfm.cache.MemoryCache;
import static net.roarsoftware.util.StringUtilities.encode;
import static net.roarsoftware.util.StringUtilities.map;
import static net.roarsoftware.util.StringUtilities.md5;

/**
 * The <code>Caller</code> class handles the low-level communication between the client and last.fm.<br/>
 * Direct usage of this class should be unnecessary since all method calls are available via the methods in
 * the <code>Artist</code>, <code>Album</code>, <code>User</code>, etc. classes.
 * If specialized calls which are not covered by the Java API are necessary this class may be used directly.<br/>
 * Supports the setting of a custom {@link Proxy} and a custom <code>User-Agent</code> HTTP header.
 *
 * @author Janni Kovacs
 */
public class Caller {

	private static final String PARAM_API_KEY = "api_key";
	private static final String PARAM_METHOD = "method";

	private static final String DEFAULT_API_ROOT = "http://ws.audioscrobbler.com/2.0/";
	private static final Caller instance = new Caller();

	private String apiRootUrl = DEFAULT_API_ROOT;

	private Proxy proxy;
	private String userAgent = "tst";

	private boolean debugMode = false;

	private Cache cache;
	private Result lastResult;

	private Caller() {
		cache = new MemoryCache();
	}

	/**
	 * Returns the single instance of the <code>Caller</code> class.
	 *
	 * @return a <code>Caller</code>
	 */
	public static Caller getInstance() {
		return instance;
	}

	/**
	 * Set api root url.
	 *
	 * @param apiRootUrl new api root url
	 */
	public void setApiRootUrl(String apiRootUrl) {
		this.apiRootUrl = apiRootUrl;
	}

	/**
	 * Sets a {@link Proxy} instance this Caller will use for all upcoming HTTP requests. May be <code>null</code>.
	 *
	 * @param proxy A <code>Proxy</code> or <code>null</code>.
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Sets a User Agent this Caller will use for all upcoming HTTP requests. For testing purposes use "tst".
	 * If you distribute your application use an identifiable User-Agent.
	 *
	 * @param userAgent a User-Agent string
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Sets the <code>debugMode</code> property. If <code>debugMode</code> is <code>true</code> all call() methods
	 * will print debug information and error messages on failure to stdout and stderr respectively.<br/>
	 * Default is <code>false</code>. Set this to <code>true</code> while in development and for troubleshooting.
	 *
	 * @param debugMode <code>true</code> to enable debug mode
	 */
	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	/**
	 * Returns the current {@link Cache}.
	 *
	 * @return the Cache
	 */
	public Cache getCache() {
		return cache;
	}

	/**
	 * Sets the active {@link Cache}. May be <code>null</code> to disable caching.
	 *
	 * @param cache the new Cache or <code>null</code>
	 */
	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public Result call(String method, String apiKey, String... params) throws CallException {
		return call(method, apiKey, map(params));
	}

	public Result call(String method, String apiKey, Map<String, String> params) throws CallException {
		return call(method, apiKey, params, null);
	}

	public Result call(String method, Session session, String... params) {
		return call(method, session.getApiKey(), map(params), session);
	}

	public Result call(String method, Session session, Map<String, String> params) {
		return call(method, session.getApiKey(), params, session);
	}

	/**
	 * Performs the web-service call. If the <code>session</code> parameter is <code>non-null</code> then an
	 * authenticated call is made. If it's <code>null</code> then an unauthenticated call is made.<br/>
	 * The <code>apiKey</code> parameter is always required, even when a valid session is passed to this method.
	 *
	 * @param method The method to call
	 * @param apiKey A Last.fm API key
	 * @param params Parameters
	 * @param session A Session instance or <code>null</code>
	 * @return the result of the operation
	 */
	private Result call(String method, String apiKey, Map<String, String> params, Session session) {
		params = new HashMap<String, String>(params); // create new Map in case params is an immutable Map
		InputStream inputStream = null;
		// try to load from cache
		String cacheEntryName = Cache.createCacheEntryName(method, params);
		if (session == null && cache != null) {
			if (cache.contains(cacheEntryName) && !cache.isExpired(cacheEntryName)) {
				inputStream = cache.load(cacheEntryName);
			}
		}
		if (inputStream == null) {
			params.put(PARAM_API_KEY, apiKey);
			if (session != null) {
				params.put("sk", session.getKey());
				String sig = Authenticator.createSignature(method, params, session.getSecret());
				params.put("api_sig", sig);
			}
			try {
				HttpURLConnection urlConnection = openConnection(apiRootUrl);
				urlConnection.setRequestMethod("POST");
				urlConnection.setDoOutput(true);
				OutputStream outputStream = urlConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
				String post = buildParameterQueue(method, params);
				if (debugMode) {
					System.out.println("body: " + post);
				}
				writer.write(post);
				writer.close();
				int responseCode = urlConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
					inputStream = urlConnection.getErrorStream();
				} else if (responseCode != HttpURLConnection.HTTP_OK) {
					this.lastResult = Result.createHttpErrorResult(responseCode, urlConnection.getResponseMessage());
					return lastResult;
				} else {
					inputStream = urlConnection.getInputStream();
					if (cache != null) {
						long expires = urlConnection.getHeaderFieldDate("Expires", -1);
						if (expires == -1) {
							long expirationTime = cache.getExpirationPolicy().getExpirationTime(method, params);
							if (expirationTime > 0) {
								if (expirationTime == Long.MAX_VALUE) {
									expires = Long.MAX_VALUE;
								} else {
									expires = System.currentTimeMillis() + expirationTime;
								}
							}
						}
						if (expires != -1) {
							cache.store(cacheEntryName, inputStream, expires); // if data wasn't cached store new result
							inputStream = cache.load(cacheEntryName);
							if (inputStream == null)
								throw new CallException("caching failed.");
						}
					}
				}
			} catch (IOException e) {
				throw new CallException(e);
			}
		}
		try {
			Document document = newDocumentBuilder()
					.parse(new InputSource(new InputStreamReader(inputStream, "UTF-8")));
			Element root = document.getDocumentElement(); // lfm element
			String statusString = root.getAttribute("status");
			Status status = "ok".equals(statusString) ? Status.OK : Status.FAILED;
			if (status == Status.FAILED) {
				if (cache != null)
					cache.remove(cacheEntryName); // if request was failed remove from cache
				Element error = (Element) root.getElementsByTagName("error").item(0);
				int errorCode = Integer.parseInt(error.getAttribute("code"));
				String message = error.getNodeValue();
				if (debugMode)
					System.err.printf("Failed. Code: %d, Error: %s%n", errorCode, message);
				this.lastResult = Result.createRestErrorResult(errorCode, message);
			} else {
				this.lastResult = Result.createOkResult(document);
			}
			return this.lastResult;
		} catch (IOException e) {
			throw new CallException(e);
		} catch (SAXException e) {
			throw new CallException(e);
		}
	}

	/**
	 * Returns the {@link Result} of the last operation, or <code>null</code> if no call operation has been
	 * performed yet.
	 *
	 * @return the last Result object
	 */
	public Result getLastResult() {
		return lastResult;
	}

	private DocumentBuilder newDocumentBuilder() {
		try {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			return builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// better never happens
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new {@link HttpURLConnection}, sets the proxy, if available, and sets the User-Agent property.
	 *
	 * @param url URL to connect to
	 * @return a new connection.
	 * @throws IOException if an I/O exception occurs.
	 */
	public HttpURLConnection openConnection(String url) throws IOException {
		if (isDebugMode())
			System.out.println("open: " + url);
		URL u = new URL(url);
		HttpURLConnection urlConnection;
		if (proxy != null)
			urlConnection = (HttpURLConnection) u.openConnection(proxy);
		else
			urlConnection = (HttpURLConnection) u.openConnection();
		urlConnection.setRequestProperty("User-Agent", userAgent);
		return urlConnection;
	}

	private String buildParameterQueue(String method, Map<String, String> params, String... strings) {
		StringBuilder builder = new StringBuilder(100);
		builder.append("method=");
		builder.append(method);
		builder.append('&');
		for (Iterator<Entry<String, String>> it = params.entrySet().iterator(); it.hasNext();) {
			Entry<String, String> entry = it.next();
			builder.append(entry.getKey());
			builder.append('=');
			builder.append(encode(entry.getValue()));
			if (it.hasNext() || strings.length > 0)
				builder.append('&');
		}
		int count = 0;
		for (String string : strings) {
			builder.append(count % 2 == 0 ? string : encode(string));
			count++;
			if (count != strings.length) {
				if (count % 2 == 0) {
					builder.append('&');
				} else {
					builder.append('=');
				}
			}
		}
		return builder.toString();
	}

	private String createSignature(Map<String, String> params, String secret) {
		Set<String> sorted = new TreeSet<String>(params.keySet());
		StringBuilder builder = new StringBuilder(50);
		for (String s : sorted) {
			builder.append(s);
			builder.append(encode(params.get(s)));
		}
		builder.append(secret);
		return md5(builder.toString());
	}

	public Proxy getProxy() {
		return proxy;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public boolean isDebugMode() {
		return debugMode;
	}
}
