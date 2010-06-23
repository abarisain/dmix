package org.a0z.mpd.event;

/**
 * Represents a database update task status change event.
 *
 * @author Darren Greaves
 * @version $Id: MPDUpdateStateChangedEvent.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public class MPDUpdateStateChangedEvent {
    private boolean updating;
    /**
     * Constructs a new <code>MPDUpdateStateChangedEvent</code>.
     * @param updating true when updating, false when not updating.
     */
    public MPDUpdateStateChangedEvent(boolean updating) {
        this.updating = updating;
    }

    /**
     * Retrieves new connection state.
     * @return true when updating, false when not updating.
     */
    public boolean isUpdating() {
        return updating;
    }
}
