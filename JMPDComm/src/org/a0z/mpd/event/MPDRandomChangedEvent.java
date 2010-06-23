package org.a0z.mpd.event;

/**
 * Represents a random state change event.
 *
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDRandomChangedEvent.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public class MPDRandomChangedEvent {
    private boolean random;
    /**
     * Constructs a new <code>MPDRandomChangedEvent</code>.
     * @param connected new random state: true, on; false, off.
     */
    public MPDRandomChangedEvent(boolean connected) {
        this.random = connected;
    }

    /**
     * Retrieves new random state.
     * @return true when random is on, false when random is off.
     */
    public boolean isRandom() {
        return random;
    }
}
