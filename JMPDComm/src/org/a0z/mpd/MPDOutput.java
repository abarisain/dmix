
package org.a0z.mpd;

import java.util.List;

/*
 * Class representing one configured output
 */
public class MPDOutput {
    private String name;
    private int id;
    private boolean enabled;

    MPDOutput(List<String> response) {
        for (String line : response) {
            if (line.startsWith("outputid:")) {
                this.id = Integer.parseInt(line.substring("outputid: ".length()));
            } else if (line.startsWith("outputname:")) {
                this.name = line.substring("outputname: ".length());
            } else if (line.startsWith("outputenabled:")) {
                this.enabled = line.substring("outputenabled: ".length()).equals("1");
            }
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String toString() {
        return getName();
    }

}
