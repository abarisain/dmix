package com.namelessdev.mpdroid;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.namelessdev.mpdroid.fragments.NowPlayingFragment;
import com.namelessdev.mpdroid.tools.Tools;

/**
 * MainMenuActivity launches the NowPlayingFragment and the current playlist
 * 
 * @author Arnaud Barisain Monrose
 * @version $Id: $
 */
public class MainMenuActivity extends FragmentActivity {
	NowPlayingFragment nowPlaying;
	
	@Override
	protected void onCreate(Bundle arg0) {
		// If put after super.onCreate on android versions < 3.0 it will not work
		setTheme(Tools.isHoneycombOrBetter() ? R.style.AppTheme_Dark : android.R.style.Theme_Wallpaper_NoTitleBar);

		super.onCreate(arg0);
		setContentView(R.layout.main_activity);
	}

	@Override
	public void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
		
		//nowPlaying = (NowPlayingFragment) getSupportFragmentManager().findFragmentById(R.id.nowPlayingFragment);
		//return nowPlaying.onKeyDown(keyCode, event) ? true :  super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
		
		//nowPlaying = (NowPlayingFragment) getSupportFragmentManager().findFragmentById(R.id.nowPlayingFragment);
		//return nowPlaying.onTouchEvent(event) ? true : super.onTouchEvent(event);
	}
}