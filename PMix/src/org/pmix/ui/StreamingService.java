package org.pmix.ui;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class StreamingService extends Service {

	private Timer timer = new Timer();

	public void onCreate() {
		super.onCreate();
		startservice();
	}
	private void startservice() {
		timer.scheduleAtFixedRate( new TimerTask() {
		public void run() {
		//Do whatever you want to do every ÒINTERVALÓ
		}
		}, 0, 2);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}