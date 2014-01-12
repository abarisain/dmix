
package com.namelessdev.mpdroid;

import android.content.SearchRecentSuggestionsProvider;

public class SearchRecentProvider extends SearchRecentSuggestionsProvider {

    public static final String AUTHORITY = "com.namelessdev.mpdroid.recent_searches_authority";
    public static final int MODE = DATABASE_MODE_QUERIES;

    public SearchRecentProvider() {
        super();
        setupSuggestions(AUTHORITY, MODE);
    }
}
