package com.namelessdev.mpdroid.widgets;

import android.app.IntentService;
import android.content.Intent;
import com.namelessdev.mpdroid.MPDApplication;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

public class WidgetHelperService extends IntentService {
    static final String TAG = "MPDroidWidgetHelperService";

    public static final String CMD_PLAYPAUSE = "PLAYPAUSE";
    public static final String CMD_PREV = "PREV";
    public static final String CMD_NEXT = "NEXT";
    public static final String CMD_UPDATE_WIDGET = "UPDATE_WIDGET";
    public static final String CMD_STOP = "STOP";


    private boolean playing = false;

    public WidgetHelperService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // get MPD connection
        MPDApplication app = (MPDApplication) getApplication();
        app.setActivity(this);

        // prepare values for runnable
        final MPD mpd = app.oMPDAsyncHelper.oMPD;
        final String action = intent.getAction();

        // schedule real work
        app.oMPDAsyncHelper.execAsync(new Runnable() {
            @Override
            public void run() {
                processIntent(action, mpd);
            }
        });

        // clean up
        app.unsetActivity(this);
    }

    void processIntent(String action, MPD mpd) {
        try {
            if (action.equals(CMD_PREV)) {
                mpd.previous();
            } else if (action.equals(CMD_PLAYPAUSE)) {
                if (mpd.getStatus().getState().equals(MPDStatus.MPD_STATE_PLAYING))
                    mpd.pause();
                else
                    mpd.play();

                playing = mpd.getStatus().getState().equals(MPDStatus.MPD_STATE_PLAYING);
                SimpleWidgetProvider.getInstance().notifyChange(this);
            } else if (action.equals(CMD_NEXT)) {
                mpd.next();
            } else if (action.equals(CMD_STOP)) {
                mpd.stop();
            } else if (action.equals(CMD_UPDATE_WIDGET)) {
                playing = mpd.getStatus().getState().equals(MPDStatus.MPD_STATE_PLAYING);
                SimpleWidgetProvider.getInstance().notifyChange(this);
            }
        } catch (MPDServerException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying() {
        return playing;
    }
}
