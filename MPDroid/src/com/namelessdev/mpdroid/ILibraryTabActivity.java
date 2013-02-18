package com.namelessdev.mpdroid;

import java.util.ArrayList;

public interface ILibraryTabActivity {
	public ArrayList<String> getTabList();

	public void pageChanged(int position);
}
