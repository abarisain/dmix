package com.namelessdev.mpdroid.fragments;

import java.util.List;

import org.a0z.mpd.Item;
import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;

public abstract class BrowseFragment extends SherlockListFragment implements OnMenuItemClickListener, AsyncExecListener {

	protected int iJobID = -1;

	public static final int MAIN = 0;
	public static final int PLAYLIST = 3;

	public static final int ADD = 0;
	public static final int ADDNREPLACE = 1;
	public static final int ADDNPLAY = 2;

	protected List<? extends Item> items = null;
	
	protected View loadingView;
	protected TextView loadingTextView;
	protected View noResultView;
	protected ListView list;

	String context;
	int irAdd, irAdded;

	public BrowseFragment(int rAdd, int rAdded, String pContext) {
		super();
		irAdd = rAdd;
		irAdded = rAdded;

		context = pContext;

		setHasOptionsMenu(false);
	}

	@TargetApi(11)
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		try {
			Activity activity = this.getActivity();
			ActionBar actionBar = activity.getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		} catch (NoClassDefFoundError e) {
			// Older android
		} catch (NullPointerException e) {

		} catch (NoSuchMethodError e) {

		}
	}

	@Override
	public void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getActivity().getApplicationContext();
		app.setActivity(getActivity());
	}

	@Override
	public void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getActivity().getApplicationContext();
		app.unsetActivity(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.browse, container, false);
		list = (ListView) view.findViewById(android.R.id.list);
		loadingView = view.findViewById(R.id.loadingLayout);
		loadingTextView = (TextView) view.findViewById(R.id.loadingText);
		noResultView = view.findViewById(R.id.noResultLayout);
		loadingView.setVisibility(View.VISIBLE);
		loadingTextView.setText(getLoadingText());
		return view;
	}
	
	/*
	 * Override this to display a custom loading text
	 */
	public int getLoadingText() {
		return R.string.loading;
	}
	
	public void setActivityTitle(String title, int drawableID) {
		getActivity().setTitle(title);
	}

	public void UpdateList() {
		noResultView.setVisibility(View.GONE);
		loadingView.setVisibility(View.VISIBLE);
		MPDApplication app = (MPDApplication) getActivity().getApplication();

		// Loading Artists asynchronous...
		app.oMPDAsyncHelper.addAsyncExecListener(this);
		iJobID = app.oMPDAsyncHelper.execAsync(new Runnable() {
			@Override
			public void run() {
				asyncUpdate();
			}
		});
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle(items.get((int) info.id).toString());
		android.view.MenuItem addItem = menu.add(ContextMenu.NONE, ADD, 0, getResources().getString(irAdd));
		addItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, ADDNREPLACE, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndPlayItem = menu.add(ContextMenu.NONE, ADDNPLAY, 0, R.string.addAndPlay);
		addAndPlayItem.setOnMenuItemClickListener(this);
	}

	protected abstract void Add(Item item);

	@Override
	public boolean onMenuItemClick(android.view.MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final MPDApplication app = (MPDApplication) getActivity().getApplication();
		switch (item.getItemId()) {
		case ADDNREPLACE:
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						String status = app.oMPDAsyncHelper.oMPD.getStatus().getState();
						app.oMPDAsyncHelper.oMPD.stop();
						app.oMPDAsyncHelper.oMPD.getPlaylist().clear();

						Add(items.get((int) info.id));
						if (status.equals(MPDStatus.MPD_STATE_PLAYING)) {
							app.oMPDAsyncHelper.oMPD.play();
						}
					} catch (MPDServerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});
			break;
		case ADD:
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					Add(items.get((int) info.id));
				}
			});

			break;

		case ADDNPLAY:
			app.oMPDAsyncHelper.execAsync(new Runnable() {
				@Override
				public void run() {
					try {
						MPDPlaylist pl = app.oMPDAsyncHelper.oMPD.getPlaylist();
						int oldsize = pl.size();
						Add(items.get((int) info.id));
						int id = pl.getByIndex(oldsize).getSongId();
						app.oMPDAsyncHelper.oMPD.skipToId(id);
						app.oMPDAsyncHelper.oMPD.play();
					} catch (MPDServerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			break;
		}
		return false;
	}

	protected void asyncUpdate() {

	}

	/**
	 * Update the view from the items list if items is set.
	 */
	public void updateFromItems() {
		if (items != null) {
			setListAdapter(getCustomListAdapter());
			try {
				getListView().setEmptyView(noResultView);
				loadingView.setVisibility(View.GONE);
			} catch (Exception e) {}
		}
	}

	protected ListAdapter getCustomListAdapter() {
		return new ArrayIndexerAdapter(getActivity(), R.layout.simple_list_item_1, items);
	}
	
	@Override
	public void asyncExecSucceeded(int jobID) {
		if (iJobID == jobID) {
			updateFromItems();
		}

	}

	public void scrollToTop() {
		try {
			getListView().setSelection(-1);
		} catch (Exception e) {
			// What if the list is empty or some other bug ? I don't want any crashes because of that
		}
	}

}