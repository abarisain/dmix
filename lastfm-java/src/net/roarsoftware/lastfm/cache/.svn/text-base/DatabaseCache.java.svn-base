package net.roarsoftware.lastfm.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Generic class for caching into a database. It's constructor takes a {@link Connection} instance, which must
 * be opened and closed by the client. SQL code used in this class should work with all common databases
 * (which support varchar, timestamp and longvarchar datatypes).<br/>
 * For more specialized versions of this class for different databases one may extend this class and
 * override methods as needed.
 *
 * @author Janni Kovacs
 */
public class DatabaseCache extends Cache {

	protected static final String TABLE_NAME = "LASTFM_CACHE";

	protected Connection conn;

	public DatabaseCache(Connection connection) throws SQLException {
		this.conn = connection;
		ResultSet tables = conn.getMetaData().getTables(null, null, TABLE_NAME, null);
		if (!tables.next()) {
			createTable();
		}
	}

	protected void createTable() throws SQLException {
		PreparedStatement stmt = conn
				.prepareStatement(
						"CREATE TABLE " + TABLE_NAME + " (key varchar, expiration_date timestamp, response longvarchar);");
		stmt.execute();
		stmt.close();
	}

	public boolean contains(String cacheEntryName) {
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT key FROM " + TABLE_NAME + " WHERE key = ?;");
			stmt.setString(1, cacheEntryName);
			ResultSet result = stmt.executeQuery();
			boolean b = result.next();
			stmt.close();
			return b;
		} catch (SQLException e) {
			return false;
		}
	}

	public InputStream load(String cacheEntryName) {
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT response FROM " + TABLE_NAME + " WHERE key = ?;");
			stmt.setString(1, cacheEntryName);
			ResultSet result = stmt.executeQuery();
			if (result.next()) {
				String s = result.getString("response");
				stmt.close();
				return new ByteArrayInputStream(s.getBytes("UTF-8"));
			}
			stmt.close();
		} catch (SQLException e) {
			// ignore
		} catch (UnsupportedEncodingException e) {
			// won't happen
		}
		return null;
	}

	public void remove(String cacheEntryName) {
		try {
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE key = ?;");
			stmt.setString(1, cacheEntryName);
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			// ignore
		}
	}

	public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
		try {
			InputStreamReader reader = new InputStreamReader(inputStream);
			StringBuilder sb = new StringBuilder(inputStream.available());
			char[] buf = new char[2048];
			int read;
			while ((read = reader.read(buf, 0, buf.length)) != -1) {
				sb.append(buf, 0, read);
			}
			PreparedStatement stmt = conn
					.prepareStatement(
							"INSERT INTO " + TABLE_NAME + " (key, expiration_date, response) VALUES(?, ?, ?);");
			stmt.setString(1, cacheEntryName);
			stmt.setTimestamp(2, new Timestamp(expirationDate));
			stmt.setString(3, sb.toString());
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			// ignore
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			// won't happen
		} catch (IOException e) {
			e.printStackTrace();
			// better won't happen
		}
	}

	public boolean isExpired(String cacheEntryName) {
		try {
			PreparedStatement stmt = conn
					.prepareStatement("SELECT expiration_date FROM " + TABLE_NAME + " WHERE key = ?;");
			stmt.setString(1, cacheEntryName);
			ResultSet result = stmt.executeQuery();
			if (result.next()) {
				Timestamp timestamp = result.getTimestamp("expiration_date");
				long expirationDate = timestamp.getTime();
				stmt.close();
				return expirationDate < System.currentTimeMillis();
			}
		} catch (SQLException e) {
			// ignore
		}
		return false;
	}

	public void clear() {
		try {
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + ";");
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			// ignore
		}
	}
}
