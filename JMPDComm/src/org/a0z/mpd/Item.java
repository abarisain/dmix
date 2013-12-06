package org.a0z.mpd;
import android.util.Log;

public abstract class Item implements Comparable<Item> {
	public String mainText() {
		return getName();
	}
	public String subText() {
		return null;
	}
	public String sort() {
		return mainText();
	}
	abstract public String getName();

	@Override
    public int compareTo(Item o) {
            return getName().compareToIgnoreCase(o.getName());
	}

    public boolean isSameOnList(Item o) {
        if (null == o) {
            return false;
        }
        return getName().equals(o.getName());
    }

	@Override
	public	String toString() {
		return mainText();
	}
}
