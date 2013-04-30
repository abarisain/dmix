package com.namelessdev.mpdroid.fragments;

import java.util.List;

import org.a0z.mpd.Item;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AbsListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.ArrayIndexerAdapter;
import com.namelessdev.mpdroid.helpers.MPDAsyncHelper.AsyncExecListener;

public abstract class BrowseFragment extends SherlockFragment implements OnMenuItemClickListener, AsyncExecListener, OnItemClickListener {

	protected int iJobID = -1;

	public static final int MAIN = 0;
	public static final int PLAYLIST = 3;

	public static final int ADD = 0;
	public static final int ADDNREPLACE = 1;
	public static final int ADDNREPLACEPLAY = 4;
	public static final int ADDNPLAY = 2;
	public static final int ADD_TO_PLAYLIST = 3;

	protected List<? extends Item> items = null;
	
	protected MPDApplication app = null;
	protected View loadingView;
	protected TextView loadingTextView;
	protected View noResultView;
	protected AbsListView list;
	private boolean firstUpdateDone = false;

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
		app = (MPDApplication) getActivity().getApplicationContext();
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
		app.setActivity(getActivity());
		if(!firstUpdateDone) {
			firstUpdateDone = true;
			UpdateList();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		app.unsetActivity(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.browse, container, false);
		list = (ListView) view.findViewById(R.id.list);
		registerForContextMenu(list);
		list.setOnItemClickListener(this);
		loadingView = view.findViewById(R.id.loadingLayout);
		loadingTextView = (TextView) view.findViewById(R.id.loadingText);
		noResultView = view.findViewById(R.id.noResultLayout);
		loadingTextView.setText(getLoadingText());
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(items != null) {
			list.setAdapter(getCustomListAdapter());
		}
	}

	@Override
	public void onDestroyView() {
		// help out the GC; imitated from ListFragment source
		loadingView = null;
		loadingTextView = null;
		noResultView = null;
		super.onDestroyView();
	}

	/*
	 * Override this to display a custom activity title
	 */
	public String getTitle() {
		return "";
	}

	/*
	 * Override this to display a custom loading text
	 */
	public int getLoadingText() {
		return R.string.loading;
	}
	
	public void setActivityTitle(String title) {
		getActivity().setTitle(title);
	}

	public void UpdateList() {
		list.setAdapter(null);
		noResultView.setVisibility(View.GONE);
		loadingView.setVisibility(View.VISIBLE);

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
		android.view.MenuItem addItem = menu.add(ADD, ADD, 0, getResources().getString(irAdd));
		addItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndReplaceItem = menu.add(ADDNREPLACE, ADDNREPLACE, 0, R.string.addAndReplace);
		addAndReplaceItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndReplacePlayItem = menu.add(ADDNREPLACEPLAY, ADDNREPLACEPLAY, 0, R.string.addAndReplacePlay);
		addAndReplacePlayItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndPlayItem = menu.add(ADDNPLAY, ADDNPLAY, 0, R.string.addAndPlay);
		addAndPlayItem.setOnMenuItemClickListener(this);
		
		if (R.string.addPlaylist!=irAdd && R.string.addStream!=irAdd) {
			int id=0;
			SubMenu playlistMenu=menu.addSubMenu(R.string.addToPlaylist);
			android.view.MenuItem item=playlistMenu.add(ADD_TO_PLAYLIST, id++, (int)info.id, R.string.newPlaylist);
			item.setOnMenuItemClickListener(this);
			
			try {
				List<Item> playlists=((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD.getPlaylists();
				
				if (null!=playlists) {
					for (Item pl : playlists) {
						item = playlistMenu.add(ADD_TO_PLAYLIST, id++, (int) info.id, pl.getName());
						item.setOnMenuItemClickListener(this);
					}
				}
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	protected abstract void add(Item item, boolean replace, boolean play);

	protected abstract void add(Item item, String playlist);

	@Override
	public boolean onMenuItemClick(final android.view.MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getGroupId()) {
			case ADDNREPLACEPLAY:
			case ADDNREPLACE:
			case ADD:
			case ADDNPLAY:
				app.oMPDAsyncHelper.execAsync(new Runnable() {
					@Override
					public void run() {
						boolean replace = false;
						boolean play = false;
						switch (item.getGroupId()) {
							case ADDNREPLACEPLAY:
								replace = true;
								play = true;
								break;
							case ADDNREPLACE:
								replace = true;
								break;
							case ADDNPLAY:
								play = true;
								break;
						}
						add(items.get((int) info.id), replace, play);
					}
				});
				break;
			case ADD_TO_PLAYLIST: {
				final EditText input = new EditText(getActivity());
				final int id = (int) item.getOrder();
				if (item.getItemId() == 0) {
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.playlistName)
							.setMessage(R.string.newPlaylistPrompt)
							.setView(input)
							.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									final String name = input.getText().toString().trim();
									if (null != name && name.length() > 0) {
										app.oMPDAsyncHelper.execAsync(new Runnable() {
											@Override
											public void run() {
												add(items.get(id), name);
											}
										});
									}
								}
							}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// Do nothing.
								}
							}).show();
				} else {
					add(items.get(id), item.getTitle().toString());
				}
				break;
			}
			default:
				final String name = item.getTitle().toString();
				final int id = (int) item.getOrder();
				app.oMPDAsyncHelper.execAsync(new Runnable() {
					@Override
					public void run() {
						add(items.get(id), name);
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
		if (getView() == null) {
			// The view has been destroyed, bail.
			return;
		}
		if (items != null) {
			list.setAdapter(getCustomListAdapter());
			try {
				if (forceEmptyView() || ((list instanceof ListView) && ((ListView) list).getHeaderViewsCount() == 0))
					list.setEmptyView(noResultView);
				loadingView.setVisibility(View.GONE);
			} catch (Exception e) {}
		}
	}

	protected ListAdapter getCustomListAdapter() {
		return new ArrayIndexerAdapter(getActivity(), R.layout.simple_list_item_1, items);
	}
	
	//Override if you want setEmptyView to be called on the list even if you have a header
	protected boolean forceEmptyView() {
		return false;
	}
	
	@Override
	public void asyncExecSucceeded(int jobID) {
		if (iJobID == jobID) {
			updateFromItems();
		}

	}

	public void scrollToTop() {
		try {
			list.setSelection(-1);
		} catch (Exception e) {
			// What if the list is empty or some other bug ? I don't want any crashes because of that
		}
	}

}
