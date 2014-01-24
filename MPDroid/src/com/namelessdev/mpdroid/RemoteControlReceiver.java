/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

/**
 * RemoteControlReceiver receives media player button stuff. Most of the code is
 * taken from google's music app.
 * 
 * @author Arnaud Barisain Monrose (Dream_Team)
 * @version $Id: $
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            Intent i = new Intent(context, StreamingService.class);
            i.setAction(StreamingService.CMD_REMOTE);
            i.putExtra(StreamingService.CMD_COMMAND, StreamingService.CMD_STOP);
            context.startService(i);
        } else if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }
            int keycode = event.getKeyCode();
            int eventAction = event.getAction();
            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = StreamingService.CMD_STOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = StreamingService.CMD_PLAYPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = StreamingService.CMD_NEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = StreamingService.CMD_PREV;
                    break;
            }
            if (command != null) {
                if (eventAction == KeyEvent.ACTION_DOWN) {
                    Intent i = new Intent(context, StreamingService.class);
                    i.setAction(StreamingService.CMD_REMOTE);
                    i.putExtra(StreamingService.CMD_COMMAND, command);
                    context.startService(i);

                    // Start the remote control service too (TODO: shouldn't
                    // start if currently streaming)
                    i = new Intent(context, NotificationService.class);
                    i.setAction(command);
                    context.startService(i);
                }
            }
        }
    }
}
