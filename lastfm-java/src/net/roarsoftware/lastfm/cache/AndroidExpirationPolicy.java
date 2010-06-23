package net.roarsoftware.lastfm.cache;

import java.util.Map;

public class AndroidExpirationPolicy implements ExpirationPolicy {

	@Override
	public long getExpirationTime(String method, Map<String, String> params) {
		// TODO Auto-generated method stub
		return 1000 * 60 * 60; // Cache all things 1 hour...
	}

}
