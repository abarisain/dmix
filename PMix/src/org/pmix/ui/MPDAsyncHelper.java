package org.pmix.ui;

import java.util.Collection;
import java.util.LinkedList;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDServerException;
import org.a0z.mpd.MPDStatusMonitor;
import org.a0z.mpd.event.MPDConnectionStateChangedEvent;
import org.a0z.mpd.event.MPDPlaylistChangedEvent;
import org.a0z.mpd.event.MPDRandomChangedEvent;
import org.a0z.mpd.event.MPDRepeatChangedEvent;
import org.a0z.mpd.event.MPDStateChangedEvent;
import org.a0z.mpd.event.MPDTrackChangedEvent;
import org.a0z.mpd.event.MPDTrackPositionChangedEvent;
import org.a0z.mpd.event.MPDUpdateStateChangedEvent;
import org.a0z.mpd.event.MPDVolumeChangedEvent;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

/**
 * This Class implements the whole MPD Communication as a thread. It also "translates" 
 * the monitor event of the JMPDComm Library back to the GUI-Thread, and allows to  
 * execute custom commands asynchronously.
 * @author sag
 *
 */
public class MPDAsyncHelper extends Handler {
	
	// Event-ID's for PMix internal events...
	private static final int EVENT_CONNECT = 0;
	private static final int EVENT_DISCONNECT = 1;
	private static final int EVENT_CONNECTFAILED = 2;
	private static final int EVENT_CONNECTSUCCEEDED = 3;
	private static final int EVENT_STARTMONITOR = 4;
	private static final int EVENT_STOPMONITOR = 5;
	private static final int EVENT_EXECASYNC = 6;
	private static final int EVENT_EXECASYNCFINISHED = 7;
	
	// Event-ID's for JMPDComm events (from the listener)...
	private static final int EVENT_CONNECTIONSTATE = 11;
	private static final int EVENT_PLAYLIST = 12;
	private static final int EVENT_RANDOM = 13;
	private static final int EVENT_REPEAT = 14;
	private static final int EVENT_STATE = 15;
	private static final int EVENT_TRACK = 16;
	private static final int EVENT_UPDATESTATE = 17;
	private static final int EVENT_VOLUME = 18;
	private static final int EVENT_TRACKPOSITION = 19;
	
	
	private MPDAsyncWorker oMPDAsyncWorker;
	private HandlerThread oMPDAsyncWorkerThread;
	private MPDStatusMonitor oMonitor;
	public MPD oMPD;
	private static int iJobID = 0;
	
	// PMix internal ConnectionListener interface 
	public interface ConnectionListener {
		public void connectionFailed(String message);
		public void connectionSucceeded(String message);
	}
	
	// Interface for callback when Asynchronous operations are finished
	public interface AsyncExecListener {
		public void asyncExecSucceeded(int jobID);
	}
	
	// Listener Collections
    private Collection<ConnectionListener> connectionListners;
    private Collection<StatusChangeListener> statusChangedListeners;
    private Collection<TrackPositionListener> trackPositionListeners;
    private Collection<AsyncExecListener> asyncExecListeners;
	
    // Current connection Information
	private MPDConnectionInfo conInfo;
    
	/**
	 * Private constructor for static class
	 */
	public MPDAsyncHelper() {	
 		oMPD = new MPD();
		oMPDAsyncWorkerThread = new HandlerThread("MPDAsyncWorker");
		oMPDAsyncWorkerThread.start();
		oMPDAsyncWorker = new MPDAsyncWorker(oMPDAsyncWorkerThread.getLooper());
		connectionListners = new LinkedList<ConnectionListener>();
        statusChangedListeners = new LinkedList<StatusChangeListener>();
        trackPositionListeners = new LinkedList<TrackPositionListener>();
        asyncExecListeners = new LinkedList<AsyncExecListener>();
        conInfo = new MPDConnectionInfo();
	}

	/**
	 * This method handels Messages, which comes from the AsyncWorker. This Message handler
	 * runns in the UI-Thread, and can therfore send the information back to the listeners
	 * of the matching events... 
	 */
	public void handleMessage(Message msg) {
		 switch (msg.what) 
		 {
		 	case EVENT_CONNECTIONSTATE:
		 		for(StatusChangeListener listener : statusChangedListeners)
		 			listener.connectionStateChanged((MPDConnectionStateChangedEvent)msg.obj);
		 		// Also notify Connection Listener...
		 		if(((MPDConnectionStateChangedEvent)msg.obj).isConnected())
		 			for(ConnectionListener listener : connectionListners)
		 				listener.connectionSucceeded("");
		 		if(((MPDConnectionStateChangedEvent)msg.obj).isConnectionLost())
		 			for(ConnectionListener listener : connectionListners)
		 				listener.connectionFailed("Connection Lost");
		 		break;
		 	case EVENT_PLAYLIST:
		 		for(StatusChangeListener listener : statusChangedListeners)
		 			listener.playlistChanged((MPDPlaylistChangedEvent)msg.obj);
		 		break;
		 	case EVENT_RANDOM:
		 		for(StatusChangeListener listener : statusChangedListeners)
		 			listener.randomChanged((MPDRandomChangedEvent)msg.obj);
		 		break;
		 	case EVENT_REPEAT:
		 		for(StatusChangeListener listener : statusChangedListeners)
		 			listener.repeatChanged((MPDRepeatChangedEvent)msg.obj);
		 		break;
		 	case EVENT_STATE:
		 		for(StatusChangeListener listener : statusChangedListeners)
		 			listener.stateChanged((MPDStateChangedEvent)msg.obj);
		 		break;
		 	case EVENT_TRACK:
		 		for(StatusChangeListener listener : statusChangedListeners)
		 			listener.trackChanged((MPDTrackChangedEvent)msg.obj);
		 		break;
		 	case EVENT_UPDATESTATE:
		 		for(StatusChangeListener listener : statusChangedListeners)
		 			listener.updateStateChanged((MPDUpdateStateChangedEvent)msg.obj);
		 		break;
		 	case EVENT_VOLUME:
		 		for(StatusChangeListener listener : statusChangedListeners)
		 			listener.volumeChanged((MPDVolumeChangedEvent)msg.obj);
		 		break;
		 	case EVENT_TRACKPOSITION:
		 		for(TrackPositionListener listener : trackPositionListeners)
		 			listener.trackPositionChanged((MPDTrackPositionChangedEvent)msg.obj);
		 		break;
		 	case EVENT_CONNECTFAILED:
		 		for(ConnectionListener listener : connectionListners)
		 			listener.connectionFailed((String)msg.obj);
		 		break;
		 	case EVENT_CONNECTSUCCEEDED:
		 		for(ConnectionListener listener : connectionListners)
		 			listener.connectionSucceeded(null);
		 		break;
		 	case EVENT_EXECASYNCFINISHED:
		 		// Asynchronous operation finished, call the listeners and supply the JobID...
		 		for(AsyncExecListener listener : asyncExecListeners)
		 			listener.asyncExecSucceeded(msg.arg1);
		 		break;
		 }
	 }

	/**
	 * Sets the new connection information
	 * @param sServer
	 * @param iPort
	 * @param sPassword
	 */
	public void setConnectionInfo(String sServer, int iPort, String sPassword)
	{
		conInfo.sServer = sServer;
		conInfo.iPort = iPort;
		conInfo.sPassword = sPassword;
	}
	public void doConnect()
	{
		oMPDAsyncWorker.obtainMessage(EVENT_CONNECT, conInfo).sendToTarget();
	}
	public void startMonitor()
	{
		oMPDAsyncWorker.obtainMessage(EVENT_STARTMONITOR).sendToTarget();
	}
	public void stopMonitor()
	{
		oMPDAsyncWorker.obtainMessage(EVENT_STOPMONITOR).sendToTarget();
	}
	public boolean isMonitorAlive()
	{
		if(oMonitor == null)
			return false;
		else
			return oMonitor.isAlive() & !oMonitor.isGivingUp();
	}
	
	public void disconnect()
	{
		oMPDAsyncWorker.obtainMessage(EVENT_DISCONNECT).sendToTarget();
	}
	
	/**
	 * Executes a Runnable Asynchronous. Meant to use for individual long during operations
	 * on JMPDComm. Use this method only, when the code to execute is only used once in the 
	 * project. If its use more than once, implement indiviual events and listener in this 
	 * class. 
	 * @param run Runnable to execute async
	 * @return JobID, which is brougth back with the AsyncExecListener interface...
	 */
	public int execAsync(Runnable run) {
		int actjobid = iJobID++;
		oMPDAsyncWorker.obtainMessage(EVENT_EXECASYNC, actjobid, 0, run).sendToTarget();
		return actjobid;
	}
	
	
	public void addStatusChangeListener(StatusChangeListener listener)
	{
		statusChangedListeners.add(listener);
	}
	public void addTrackPositionListener(TrackPositionListener listener)
	{
		trackPositionListeners.add(listener);
	}
	public void addConnectionListener(ConnectionListener listener)
	{
		connectionListners.add(listener);
	}
	public void addAsyncExecListener(AsyncExecListener listener)
	{
		asyncExecListeners.add(listener);
	}
	public void removeAsyncExecListener(AsyncExecListener listener)
	{
		asyncExecListeners.remove(listener);
	}
	
	/**
	 * Asynchronous worker thread-class for long during operations on JMPDComm
	 * @author sag
	 *
	 */
	public class MPDAsyncWorker extends Handler implements StatusChangeListener, TrackPositionListener {
		public MPDAsyncWorker(Looper looper)
		{
			super(looper);
		}

		 public void handleMessage(Message msg) {
			 switch (msg.what) {
			 	case EVENT_CONNECT:
				 	try {
				 		MPDConnectionInfo conInfo = (MPDConnectionInfo)msg.obj;
				 		oMPD.connect(conInfo.sServer, conInfo.iPort);
				 		if(!conInfo.sPassword.equals(""))
				 			oMPD.password(conInfo.sPassword);
				 		MPDAsyncHelper.this.obtainMessage(EVENT_CONNECTSUCCEEDED).sendToTarget();
					} catch (MPDServerException e) {
						MPDAsyncHelper.this.obtainMessage(EVENT_CONNECTFAILED, e.getMessage()).sendToTarget();
					}
					break;
			 	case EVENT_STARTMONITOR:
			 		oMonitor = new MPDStatusMonitor(oMPD, 500);
			 		oMonitor.addStatusChangeListener(this);
			 		oMonitor.addTrackPositionListener(this);
			 		oMonitor.start();
					break;
			 	case EVENT_STOPMONITOR:
			 		oMonitor.giveup();
			 		break;
			 	case EVENT_DISCONNECT:
			 		try {
						oMPD.disconnect();
					} catch (MPDServerException e) {
					} catch (NullPointerException ex) {
					}
			 		break;
			 	case EVENT_EXECASYNC:
			 		Runnable run = (Runnable)msg.obj;
			 		run.run();
					MPDAsyncHelper.this.obtainMessage(EVENT_EXECASYNCFINISHED, msg.arg1, 0).sendToTarget();
				default:
					break;
			 }
		 }

		// Send all events as Messages back to the GUI-Thread
		public void connectionStateChanged(MPDConnectionStateChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_CONNECTIONSTATE, event).sendToTarget();
		}

		public void playlistChanged(MPDPlaylistChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_PLAYLIST, event).sendToTarget();
		}

		public void randomChanged(MPDRandomChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_RANDOM, event).sendToTarget();
		}

		public void repeatChanged(MPDRepeatChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_REPEAT, event).sendToTarget();
			
		}

		public void stateChanged(MPDStateChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_STATE, event).sendToTarget();
			
		}

		public void trackChanged(MPDTrackChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_TRACK, event).sendToTarget();
			
		}

		public void updateStateChanged(MPDUpdateStateChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_UPDATESTATE, event).sendToTarget();
			
		}

		public void volumeChanged(MPDVolumeChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_VOLUME, event).sendToTarget();
			
		}

		public void trackPositionChanged(MPDTrackPositionChangedEvent event) {
			MPDAsyncHelper.this.obtainMessage(EVENT_TRACKPOSITION, event).sendToTarget();
			
		}
	}
	
	private class MPDConnectionInfo {
		public String sServer;
		public int iPort;
		public String sPassword;
	}
	
}
