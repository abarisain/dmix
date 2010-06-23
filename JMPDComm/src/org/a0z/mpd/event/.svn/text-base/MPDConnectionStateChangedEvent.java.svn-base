package org.a0z.mpd.event;

/**
 * Represents a connection change event.
 *
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDConnectionStateChangedEvent.java 2614 2004-11-11 18:46:31Z galmeida $
 */
public class MPDConnectionStateChangedEvent {
    private boolean connected;
    private boolean connectionLost;
    /**
     * Constructs a new <code>MPDConnectionStateChangedEvent</code>.
     * @param connected new connection state: true, conneted; false, disconnected.
     * @param connectionLost true when connection was lost, false otherwise.
     */
    public MPDConnectionStateChangedEvent(boolean connected, boolean connectionLost) {
        this.connected = connected;
        this.connectionLost = connectionLost;
    }

    /**
     * Retrieves new connection state.
     * @return true when connected, false when disconnected.
     */
    public boolean isConnected() {
        return connected;
    }
    /**
     * Returns true when connection was lost, false otherwise.
     * @return true when connection was lost, false otherwise.
     */
    public boolean isConnectionLost() {
        return connectionLost;
    }
}
