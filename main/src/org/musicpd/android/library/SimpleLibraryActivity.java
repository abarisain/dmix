package org.musicpd.android.library;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;

import com.actionbarsherlock.view.MenuItem;
import org.musicpd.android.MPDApplication;
import org.musicpd.android.MPDActivities.MPDFragmentActivity;
import org.musicpd.android.R;
import org.musicpd.android.R.string;
import org.musicpd.android.fragments.AlbumsFragment;
import org.musicpd.android.fragments.BrowseFragment;
import org.musicpd.android.fragments.FSFragment;
import org.musicpd.android.fragments.NowPlayingFragment;
import org.musicpd.android.fragments.SongsFragment;
import org.musicpd.android.fragments.StreamsFragment;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.YouTube;

public class SimpleLibraryActivity extends MPDFragmentActivity implements ILibraryFragmentActivity {

	public final String EXTRA_ALBUM = "album";
	public final String EXTRA_ARTIST = "artist";
	public final String EXTRA_STREAM = "streams";
	public final String EXTRA_FOLDER = "folder";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.library_tabs);
		Object targetElement = null;
		Fragment rootFragment = null;
		Intent intent = getIntent();
		if (Intent.ACTION_SEND.equals(intent.getAction()) || intent.getBooleanExtra("streams", false)) {
			final Activity activity = this;
			final StreamsFragment sf = new StreamsFragment();
			rootFragment = sf;
			new AsyncTask<Intent, Integer, scala.Tuple2<String,String>>() {
				protected scala.Tuple2<String,String> doInBackground(Intent... intents) {
					try {
						Bundle extras = intents[0].getExtras();
						String url = extras.getString(Intent.EXTRA_TEXT);
						return YouTube.resolve(url);
					} catch(Exception e) {
						Log.e(e);
						return null;
					}
				}
				protected void onPostExecute(scala.Tuple2<String,String> stream) {
					try {
						if (stream != null)
							sf.addStream(stream._1, stream._2, -1, activity);
					} catch(Exception e) {
						Log.e(e);
					}
				}
			}.execute(intent);
		} else {
			targetElement = intent.getParcelableExtra(EXTRA_ALBUM);
			if (targetElement == null)
				targetElement = intent.getParcelableExtra(EXTRA_ARTIST);
			if (targetElement == null)
				targetElement = intent.getStringExtra(EXTRA_FOLDER);
			if (targetElement instanceof Artist) {
				rootFragment = new AlbumsFragment().init((Artist) targetElement);
			} else if (targetElement instanceof Album) {
				rootFragment = new SongsFragment().init((Artist) getIntent().getParcelableExtra(EXTRA_ARTIST), (Album) targetElement);
			} else {
				rootFragment = new FSFragment().init(
					targetElement instanceof String
						? (String) targetElement
						: ""
				);
			}
		}
		if (rootFragment != null) {
			if (rootFragment instanceof BrowseFragment)
				setTitle(((BrowseFragment) rootFragment).getTitle());
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.replace(R.id.root_frame, rootFragment);
			ft.commit();
		} else {
			throw new RuntimeException("Error : SimpleLibraryActivity root fragment is null");
		}
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		final MPDApplication app = (MPDApplication) getApplicationContext();
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						app.oMPDAsyncHelper.oMPD.next();
					} catch (MPDServerException e) {
						Log.w(e);
					}
				}
			}).start();
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						app.oMPDAsyncHelper.oMPD.previous();
					} catch (MPDServerException e) {
						Log.w(e);
					}
				}
			}).start();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			// For onKeyLongPress to work
			event.startTracking();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		final MPDApplication app = (MPDApplication) getApplicationContext();
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (event.isTracking() && !event.isCanceled() && !app.getApplicationState().streamingMode) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							app.oMPDAsyncHelper.oMPD.adjustVolume(
									event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ?
											NowPlayingFragment.VOLUME_STEP
											: -NowPlayingFragment.VOLUME_STEP);
						} catch (MPDServerException e) {
							Log.w(e);
						}
					}
				}).start();
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void pushLibraryFragment(Fragment fragment, String label) {
		String title = "";
		if (fragment instanceof BrowseFragment) {
			title = ((BrowseFragment) fragment).getTitle();
		} else {
			title = fragment.toString();
		}
		setTitle(title);
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.replace(R.id.root_frame, fragment);
		ft.addToBackStack(label);
		ft.setBreadCrumbTitle(title);
		ft.commit();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		final FragmentManager supportFM = getSupportFragmentManager();
		final int fmStackCount = supportFM.getBackStackEntryCount();
		if (fmStackCount > 0) {
			setTitle(supportFM.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
		}
		return false;
	}
}
