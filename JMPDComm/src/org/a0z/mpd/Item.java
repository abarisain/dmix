package org.a0z.mpd;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class Item implements Comparable<Item> {
    public static final Collator defaultCollator = Collator.getInstance(Locale.getDefault());

	public String mainText() {
		return getName();
	}
	public String subText() {
		return null;
	}
	public String sort() {
            return getName().toLowerCase(Locale.getDefault());
	}
	abstract public String getName();

	@Override
        public int compareTo(Item o) {
            // sort "" behind everything else
            if ("".equals(sort())) {
                if ("".equals(o.sort())) {
                    return 0;
                }
                return 1;
            }
            if ("".equals(o.sort())) {
                return -1;
            }
            return defaultCollator.compare(sort(), o.sort());
            //return sort().compareToIgnoreCase(o.sort());
	}

	@Override
        public String toString() {
		return mainText();
	}

	public String info() {
            return toString();
        }

        public boolean nameEquals(Item o) {
            return getName().equals(o.getName());
        }

        public boolean isUnknown() {
            return getName().length() == 0;
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
                if (aa_items.get(j).nameEquals(a_items.get(i))) {
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
