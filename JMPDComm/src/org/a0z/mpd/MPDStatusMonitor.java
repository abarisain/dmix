/*
 * Created on 12/02/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.a0z.mpd;

import java.util.LinkedList;

import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDConnectionException;
import org.a0z.mpd.exception.MPDServerException;

/**
 * Monitors MPD Server and sends events on status changes.
 * 
 * @version $Id: MPDStatusMonitor.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public class MPDStatusMonitor extends Thread {
	private int delay;
	private MPD mpd;
	private boolean giveup;

	private LinkedList<StatusChangeListener> statusChangedListeners;
	private LinkedList<TrackPositionListener> trackPositionChangedListeners;

	/**
	 * Constructs a MPDStatusMonitor.
	 * 
	 * @param mpd
	 *           MPD server to monitor.
	 * @param delay
	 *           status query interval.
	 */
	public MPDStatusMonitor(MPD mpd, int delay) {
		super("MPDStatusMonitor");
		
		this.mpd = mpd;
		this.delay = delay;
		this.giveup = false;
		this.statusChangedListeners = new LinkedList<StatusChangeListener>();
		this.trackPositionChangedListeners = new LinkedList<TrackPositionListener>();
	}

	/**
	 * Main thread method
	 */
	public void run() {
		// initialize value cache
		int oldSong = -1;
		int oldPlaylistVersion = -1;
		long oldElapsedTime = -1;
		String oldState = "";
		int oldVolume = -1;
		boolean oldUpdating = false;
		boolean oldRepeat = false;
		boolean oldRandom = false;
		boolean oldConnectionState = false;
		boolean connectionLost = false;

		while (!giveup) {
			Boolean connectionState = Boolean.valueOf(mpd.isConnected());

			if (connectionLost || oldConnectionState != connectionState) {
				for (StatusChangeListener listener : statusChangedListeners) {
					listener.connectionStateChanged(connectionState.booleanValue(), connectionLost);
				}
				connectionLost = false;
				oldConnectionState = connectionState;
			}

			if (connectionState == Boolean.TRUE) {
				// playlist
				try {
					MPDStatus status = mpd.getStatus();

					// playlist
					if (oldPlaylistVersion != status.getPlaylistVersion() && status.getPlaylistVersion() != -1) {
						// Lets update our own copy
						for (StatusChangeListener listener : statusChangedListeners)
							listener.playlistChanged(status, oldPlaylistVersion);
						oldPlaylistVersion = status.getPlaylistVersion();
					}

					// song
					if (oldSong != status.getSongPos()) {
						for (StatusChangeListener listener : statusChangedListeners)
							listener.trackChanged(status, oldSong);
						oldSong = status.getSongPos();
					}

					// time
					if (oldElapsedTime != status.getElapsedTime()) {
						for (TrackPositionListener listener : trackPositionChangedListeners)
							listener.trackPositionChanged(status);
						oldElapsedTime = status.getElapsedTime();
					}

					// state
					if (oldState != status.getState()) {
						for (StatusChangeListener listener : statusChangedListeners)
							listener.stateChanged(status, oldState);
						oldState = status.getState();
					}

					// volume
					if (oldVolume != status.getVolume()) {
						for (StatusChangeListener listener : statusChangedListeners)
							listener.volumeChanged(status, oldVolume);
						oldVolume = status.getVolume();
					}

					// repeat
					if (oldRepeat != status.isRepeat()) {
						for (StatusChangeListener listener : statusChangedListeners)
							listener.repeatChanged(status.isRepeat());
						oldRepeat = status.isRepeat();
					}

					// volume
					if (oldRandom != status.isRandom()) {
						for (StatusChangeListener listener : statusChangedListeners)
							listener.randomChanged(status.isRandom());
						oldRandom = status.isRandom();
					}

					// update database
					if (oldUpdating != status.isUpdating()) {
						for (StatusChangeListener listener : statusChangedListeners)
							listener.libraryStateChanged(status.isUpdating());
						oldUpdating = status.isUpdating();
					}
				} catch (MPDConnectionException e) {
					// connection lost
					connectionState = Boolean.FALSE;
					connectionLost = true;
				} catch (MPDServerException e) {
					e.printStackTrace();
				}
			}
			try {
				synchronized (this) {
					this.wait(this.delay);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * Adds a <code>StatusChangeListener</code>.
	 * 
	 * @param listener
	 *           a <code>StatusChangeListener</code>.
	 */
	public void addStatusChangeListener(StatusChangeListener listener) {
		statusChangedListeners.add(listener);
	}

	/**
	 * Adds a <code>TrackPositionListener</code>.
	 * 
	 * @param listener
	 *           a <code>TrackPositionListener</code>.
	 */
	public void addTrackPositionListener(TrackPositionListener listener) {
		trackPositionChangedListeners.add(listener);
	}

	/**
	 * Gracefully terminate tread.
	 */
	public void giveup() {
		this.giveup = true;
	}

	public boolean isGivingUp() {
		return this.giveup;
	}
}
