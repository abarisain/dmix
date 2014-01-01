package org.a0z.mpd;
import java.util.Collections;
import java.util.List;

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

	public String info() {
            return toString();
        }



    /*
     * Merge item lists, for example received by albumartist and artist requests.
     *
     * Sorted lists required!
     */
    public static <T extends Item> List<T> merged(List<T> aa_items,
                                                  List<T> a_items) {
        int j_start = aa_items.size()-1;
        for (int i = a_items.size()-1; i >= 0; i--) {  // artists
            String a_name = a_items.get(i).getName();
            for (int j = j_start; j >= 0; j--) {  // album artists
                if (aa_items.get(j).getName().equals(a_name)) {
                    j_start = j;
                    a_items.remove(i);
                    break;
                }
            }
        }
        List<T> result = a_items;
        result.addAll(aa_items);
        Collections.sort(result);
        return result;
    }





}
