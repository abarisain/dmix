package org.pmix.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.a0z.mpd.Directory;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.Music;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class FSActivity extends BrowseActivity {
	private List<String> items = new ArrayList<String>();
	private Directory currentDirectory = null;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.files);

		items.clear();
		try {
			// Get file system, try to use supplied sub directory if available
			MPDApplication app = (MPDApplication)getApplication();
			if (this.getIntent().getStringExtra("directory") != null) {
				currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory().makeDirectory((String) this.getIntent().getStringExtra("directory"));
			} else {
				currentDirectory = app.oMPDAsyncHelper.oMPD.getRootDirectory();
			}
			currentDirectory.refreshData();

			Collection<Directory> directories = currentDirectory.getDirectories();
			for (Directory child : directories) {
				items.add(child.getName());
			}

			Collection<Music> musics = currentDirectory.getFiles();
			for (Music music : musics) {
				items.add(music.getTitle());
			}

			// Put the list on the screen...
			ListViewButtonAdapter<String> fsentries = new ListViewButtonAdapter<String>(this, android.R.layout.simple_list_item_1, items);
			PlusListener AddListener = new PlusListener() {
				@Override
				public void OnAdd(CharSequence sSelected, int iPosition)
					{
						try {
							MPDApplication app = (MPDApplication)getApplication();
							Directory ToAdd = currentDirectory.getDirectory(sSelected.toString());
							if(ToAdd != null) {
								// Valid directory
								app.oMPDAsyncHelper.oMPD.getPlaylist().add(ToAdd);
								MainMenuActivity.notifyUser(getResources().getString(R.string.addedDirectoryToPlaylist), FSActivity.this);
							} else {
								Music music = currentDirectory.getFileByTitle(sSelected.toString());
								if(music != null) {
									app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
									MainMenuActivity.notifyUser(getResources().getString(R.string.songAdded, sSelected), FSActivity.this);
								}
							}
						} catch (MPDServerException e) {
							e.printStackTrace();
						}
					}
				};
			fsentries.SetPlusListener(AddListener);
			setListAdapter(fsentries);
		} catch (MPDServerException e) {
			e.printStackTrace();
			this.setTitle(e.getMessage());
		}

	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		// click on a file
		if (position > currentDirectory.getDirectories().size() - 1 || currentDirectory.getDirectories().size() == 0) {

			Music music = (Music) currentDirectory.getFiles().toArray()[position - currentDirectory.getDirectories().size()];

			try {
				MPDApplication app = (MPDApplication)getApplication();

				int songId = -1;
				app.oMPDAsyncHelper.oMPD.getPlaylist().add(music);
				if (songId > -1) {
					app.oMPDAsyncHelper.oMPD.skipTo(songId);
				}
				
			} catch (MPDServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// click on a directory
			// open the same sub activity, it would be better to reuse the
			// same instance

			Intent intent = new Intent(this, FSActivity.class);
			String dir;

			dir = ((Directory) currentDirectory.getDirectories().toArray()[position]).getFullpath();
			if(dir != null)
			{
				intent.putExtra("directory", dir);
				startActivityForResult(intent, -1);
			}
		}

	}

}
