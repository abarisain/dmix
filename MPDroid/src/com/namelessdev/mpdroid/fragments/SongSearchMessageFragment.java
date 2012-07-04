package com.namelessdev.mpdroid.fragments;

import com.actionbarsherlock.app.SherlockFragment;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SongSearchMessageFragment extends SherlockFragment {
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.song_search_message, container, false);
	}
}
