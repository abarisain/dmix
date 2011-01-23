package com.namelessdev.mpdroid;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class WebViewActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WebView webview = new WebView(this);
		this.setContentView(webview);
		final String url = getIntent().getStringExtra("url");
		if (url != null) {
			webview.loadUrl(url);
		} else {
			// Defaut on the what's new page
			webview.loadUrl("http://nlss.fr/mpdroid/new.html");
		}
	}
}
