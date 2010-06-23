package net.roarsoftware.lastfm.cache;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import net.roarsoftware.lastfm.scrobble.Scrobbler;
import net.roarsoftware.lastfm.scrobble.SubmissionData;

/**
 * Standard {@link Cache} implementation which is used by default by the {@link net.roarsoftware.lastfm.Caller} class.
 * This implementation caches all responses in the file system. In addition to the raw responses it stores a
 * .meta file which contains the expiration date for the specified request.
 *
 * @author Janni Kovacs
 */
@SuppressWarnings({"ALL"})
public class FileSystemCache extends Cache implements ScrobbleCache {

	private static final String SUBMISSIONS_FILE = "submissions.txt";

	private File cacheDir;

	public FileSystemCache() {
		this(new File(System.getProperty("user.home") + "/.last.fm-cache"));
	}

	public FileSystemCache(File cacheDir) {
		this.cacheDir = cacheDir;
	}

	public boolean contains(String cacheEntryName) {
		return new File(cacheDir, cacheEntryName + ".xml").exists();
	}

	public void remove(String cacheEntryName) {
		new File(cacheDir, cacheEntryName + ".xml").delete();
		new File(cacheDir, cacheEntryName + ".meta").delete();
	}

	public boolean isExpired(String cacheEntryName) {
		File f = new File(cacheDir, cacheEntryName + ".meta");
		if (!f.exists())
			return false;
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(f));
			long expirationDate = Long.valueOf(p.getProperty("expiration-date"));
			return expirationDate < System.currentTimeMillis();
		} catch (IOException e) {
			return false;
		}
	}

	public void clear() {
		for (File file : cacheDir.listFiles()) {
			if (file.isFile()) {
				file.delete();
			}
		}
	}

	public InputStream load(String cacheEntryName) {
		try {
			return new FileInputStream(new File(cacheDir, cacheEntryName + ".xml"));
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
		createCache();
		File f = new File(cacheDir, cacheEntryName + ".xml");
		try {
			BufferedInputStream is = new BufferedInputStream(inputStream);
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f));
			int read;
			byte[] buffer = new byte[4096];
			while ((read = is.read(buffer)) != -1) {
				os.write(buffer, 0, read);
			}
			os.close();
			is.close();
			File fm = new File(cacheDir, cacheEntryName + ".meta");
			Properties p = new Properties();
			p.setProperty("expiration-date", Long.toString(expirationDate));
			p.store(new FileOutputStream(fm), null);
		} catch (IOException e) {
			// we ignore the exception. if something went wrong we just don't cache it.
		}
	}

	private void createCache() {
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
			if (!cacheDir.isDirectory()) {
				this.cacheDir = cacheDir.getParentFile();
			}
		}
	}

	public void cacheScrobble(Collection<SubmissionData> submissions) {
		cacheScrobble(submissions.toArray(new SubmissionData[submissions.size()]));
	}

	public void cacheScrobble(SubmissionData... submissions) {
		createCache();
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(new File(cacheDir, SUBMISSIONS_FILE), true));
			for (SubmissionData submission : submissions) {
				w.append(submission.toString());
				w.newLine();
			}
			w.close();
		} catch (IOException e) {
			// huh ?
			//	e.printStackTrace();
		}
	}

	public boolean isEmpty() {
		File file = new File(cacheDir, SUBMISSIONS_FILE);
		if (!file.exists())
			return true;
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line = r.readLine();
			r.close();
			return line == null || "".equals(line);
		} catch (IOException e) {
			// huh
			//	e.printStackTrace();
		}
		return true;
	}

	public void scrobble(Scrobbler scrobbler) throws IOException {
		File file = new File(cacheDir, SUBMISSIONS_FILE);
		if (file.exists()) {
			BufferedReader r = new BufferedReader(new FileReader(file));
			List<SubmissionData> list = new ArrayList<SubmissionData>(50);
			String line;
			while ((line = r.readLine()) != null) {
				SubmissionData d = new SubmissionData(line);
				list.add(d);
				if (list.size() == 50) {
					scrobbler.submit(list);
					list.clear();
				}
			}
			if (list.size() > 0)
				scrobbler.submit(list);
			r.close();
			FileWriter w = new FileWriter(file);
			w.close();
		}
	}

	public void clearScrobbleCache() {
		File file = new File(cacheDir, SUBMISSIONS_FILE);
		file.delete();
	}
}
