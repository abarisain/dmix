
package org.a0z.mpd;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class Item implements Comparable<Item> {
    public static final Collator defaultCollator = Collator.getInstance(Locale.getDefault());

    /*
     * Merge item lists, for example received by albumartist and artist
     * requests. Sorted lists required!
     */
    public static <T extends Item> List<T> merged(List<T> aa_items,
            List<T> a_items) {
        int j_start = aa_items.size() - 1;
        for (int i = a_items.size() - 1; i >= 0; i--) { // artists
            for (int j = j_start; j >= 0; j--) { // album artists
                if (aa_items.get(j).nameEquals(a_items.get(i))) {
                    j_start = j;
                    a_items.remove(i);
                    break;
                }
            }
        }
        a_items.addAll(aa_items);
        Collections.sort(a_items);
        return a_items;
    }

    @Override
    public int compareTo(Item o) {
        // sort "" behind everything else
        if ("".equals(sortText())) {
            if ("".equals(o.sortText())) {
                return 0;
            }
            return 1;
        }
        if ("".equals(o.sortText())) {
            return -1;
        }
        return defaultCollator.compare(sortText(), o.sortText());
        // return sort().compareToIgnoreCase(o.sort());
    }

    abstract public String getName();

    public String info() {
        return toString();
    }

    public boolean isUnknown() {
        return getName().length() == 0;
    }

    public String mainText() {
        return getName();
    }

    public boolean nameEquals(Item o) {
        return getName().equals(o.getName());
    }

    public String sortText() {
        return getName().toLowerCase(Locale.getDefault());
    }

    public String subText() {
        return null;
    }

    @Override
    public String toString() {
        return mainText();
    }

}
