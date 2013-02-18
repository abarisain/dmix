package com.namelessdev.mpdroid.library;

import java.util.ArrayList;

public interface ILibraryTabActivity {
	public ArrayList<String> getTabList();

	public void pageChanged(int position);
}
