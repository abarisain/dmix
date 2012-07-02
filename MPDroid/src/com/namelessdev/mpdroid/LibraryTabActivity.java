package com.namelessdev.mpdroid;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

public class LibraryTabActivity extends TabActivity implements OnTabChangeListener {
	private com.namelessdev.mpdroid.ActionBar compatActionBar;
	private CharSequence[] tabLabels;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_tabs);
		tabLabels = new CharSequence[] { this.getString(R.string.artists), this.getString(R.string.albums), this.getString(R.string.songs),
				this.getString(R.string.files) };

		Resources res = getResources();
		TabHost tabHost = getTabHost();
		tabHost.setOnTabChangedListener(this);
		tabHost.addTab(tabHost.newTabSpec("tab_artist").setIndicator(tabLabels[0], res.getDrawable(R.drawable.ic_tab_artists))
				.setContent(new Intent(LibraryTabActivity.this, ArtistsActivity.class)));
		tabHost.addTab(tabHost.newTabSpec("tab_album").setIndicator(tabLabels[1], res.getDrawable(R.drawable.ic_tab_albums))
				.setContent(new Intent(LibraryTabActivity.this, AlbumsActivity.class)));
		tabHost.addTab(tabHost.newTabSpec("tab_songs").setIndicator(tabLabels[2], res.getDrawable(R.drawable.ic_tab_songs))
				.setContent(new Intent(LibraryTabActivity.this, SongSearchMessage.class)));
		tabHost.addTab(tabHost.newTabSpec("tab_files").setIndicator(tabLabels[3], res.getDrawable(R.drawable.ic_tab_playlists))
				.setContent(new Intent(LibraryTabActivity.this, FSActivity.class)));
		((MPDApplication) getApplication()).setActivity(this);

		final View tmpView = findViewById(R.id.compatActionbar);
		if (tmpView != null) {
			compatActionBar = (com.namelessdev.mpdroid.ActionBar) tmpView;
			//Force it even on tablets
			compatActionBar.setVisibility(View.VISIBLE);
			compatActionBar.setTitle(R.string.libraryTabActivity);
			compatActionBar.setBackActionEnabled(true);
			compatActionBar.showBottomSeparator(true);
			compatActionBar.setSearchButtonParams(true, new OnClickListener() {
				@Override
				public void onClick(View v) {
					final Activity activity = getLocalActivityManager().getActivity(getTabHost().getCurrentTabTag()); 
					if(activity != null) {
						activity.onSearchRequested();
					}
				}
			});
			compatActionBar.setTitle(tabLabels[tabHost.getCurrentTab()].toString());
			compatActionBar.setTitleSelected(false);
			compatActionBar.setTitleBackgroundDrawable(R.drawable.actionbar_button);
			compatActionBar.setTitleRightDrawable(R.drawable.ic_action_menu_indicator);
			compatActionBar.setTitleClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(LibraryTabActivity.this);
					builder.setItems(tabLabels, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							getTabHost().setCurrentTab(item);
						}
					});
					builder.create().show();
				}
			});
			tabHost.getTabWidget().setVisibility(View.GONE);
		}

	}

	@TargetApi(11)
	@Override
	public void onStart() {
		super.onStart();
		try {
			ActionBar actionBar = this.getActionBar();
			if (actionBar != null) {
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.hide();
			}
		} catch (NoClassDefFoundError e) {
			// Older android
		} catch (NullPointerException e) {

		} catch (NoSuchMethodError e) {

		}
	}

	@Override
	protected void onDestroy() {
		((MPDApplication) getApplication()).setActivity(this);
		super.onDestroy();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}

	@Override
	public void onTabChanged(String tabId) {
		final View tmpView = findViewById(R.id.compatActionbar);
		if (tmpView != null) {
			// We are on a phone
			compatActionBar = (com.namelessdev.mpdroid.ActionBar) tmpView;
			compatActionBar.setTitle(tabLabels[getTabHost().getCurrentTab()].toString());
		}
	}
}
