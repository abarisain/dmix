package com.namelessdev.mpdroid.fragments;

import org.a0z.mpd.Item;
import org.a0z.mpd.exception.MPDServerException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.library.ILibraryFragmentActivity;
import com.namelessdev.mpdroid.library.PlaylistEditActivity;
import com.namelessdev.mpdroid.tools.Tools;

public class PlaylistsFragment extends BrowseFragment {
	public static final int EDIT   = 101;
	public static final int DELETE = 102;

	public PlaylistsFragment() {
		super(R.string.addPlaylist, R.string.playlistAdded, null);
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingPlaylists;
	}
	
	@Override
	public void onItemClick(AdapterView adapterView, View v, int position, long id) {
		((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new StoredPlaylistFragment().init(items.get(position).getName()),
				"stored_playlist");
	}

	@Override
	protected void asyncUpdate() {
		try {
			items = app.oMPDAsyncHelper.oMPD.getPlaylists();
		} catch (MPDServerException e) {
		}
	}

	@Override
	protected void add(Item item, boolean replace, boolean play) {
		try {
			app.oMPDAsyncHelper.oMPD.add(item.getName(), replace, play);
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (MPDServerException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void add(Item item, String playlist) {
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		android.view.MenuItem editItem = menu.add(ContextMenu.NONE, EDIT, 0, R.string.editPlaylist);
		editItem.setOnMenuItemClickListener(this);
		android.view.MenuItem addAndReplaceItem = menu.add(ContextMenu.NONE, DELETE, 0, R.string.deletePlaylist);
		addAndReplaceItem.setOnMenuItemClickListener(this);
	}
	
	@Override
	public boolean onMenuItemClick(android.view.MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case EDIT:
			Intent intent = new Intent(getActivity(), PlaylistEditActivity.class);
			intent.putExtra("playlist", items.get((int) info.id).getName());
			startActivity(intent);
			return true;

		case DELETE:
			String playlist = items.get((int) info.id).getName();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getResources().getString(R.string.deletePlaylist));
			builder.setMessage(String.format(getResources().getString(R.string.deletePlaylistPrompt), playlist));

			DialogClickListener oDialogClickListener = new DialogClickListener((int) info.id);
			builder.setNegativeButton(getResources().getString(android.R.string.no), oDialogClickListener);
			builder.setPositiveButton(getResources().getString(R.string.deletePlaylist), oDialogClickListener);
			try {
				builder.show();
			} catch (BadTokenException e) {
				// Can't display it. Don't care.
			}
			break;
		default:
			super.onMenuItemClick(item);
			break;
		}
		return false;
	}
	
	class DialogClickListener implements OnClickListener {
		private final int itemIndex;
		DialogClickListener(int itemIndex) {
			this.itemIndex=itemIndex;
		}
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case AlertDialog.BUTTON_NEGATIVE:
				break;
			case AlertDialog.BUTTON_POSITIVE:
				String playlist=items.get(itemIndex).getName();
				try {
					app.oMPDAsyncHelper.oMPD.getPlaylist().removePlaylist(playlist);
					Tools.notifyUser(String.format(getResources().getString(R.string.playlistDeleted), playlist), getActivity());
					items.remove(itemIndex);
				} catch (MPDServerException e) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle(getResources().getString(R.string.deletePlaylist));
					builder.setMessage(String.format(getResources().getString(R.string.failedToDelete), playlist));
					builder.setPositiveButton(getResources().getString(android.R.string.cancel), null);

					try {
						builder.show();
					} catch (BadTokenException ex) {
						// Can't display it. Don't care.
					}
				}
				updateFromItems();
				break;

			}
		}
	}
}
