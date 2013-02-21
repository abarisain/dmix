package com.namelessdev.mpdroid.library;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.fragments.AlbumsFragment;
import com.namelessdev.mpdroid.fragments.BrowseFragment;
import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.namelessdev.mpdroid.fragments.SongsFragment;

public class SimpleLibraryActivity extends SherlockFragmentActivity implements ILibraryFragmentActivity {

	public final String EXTRA_ALBUM = "album";
	public final String EXTRA_ARTIST = "artist";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.simple_frame);
		Parcelable targetElement = null;
		Fragment rootFragment = null;
		targetElement = getIntent().getParcelableExtra(EXTRA_ALBUM);
		if (targetElement == null)
			targetElement = getIntent().getParcelableExtra(EXTRA_ARTIST);
		if (targetElement == null) {
			throw new RuntimeException("Error : cannot start SimpleLibraryActivity without an extra");
		} else {
			if (targetElement instanceof Artist) {
				rootFragment = new AlbumsFragment().init((Artist) targetElement);
			} else if (targetElement instanceof Album) {
				rootFragment = new SongsFragment().init(null, (Album) targetElement);
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
						e.printStackTrace();
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
						e.printStackTrace();
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
							e.printStackTrace();
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
}
