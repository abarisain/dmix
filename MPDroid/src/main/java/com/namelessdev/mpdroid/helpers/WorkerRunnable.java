/*
 * Copyright (C) 2010-2016 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.helpers;

import android.os.Handler;
import android.os.Message;

/**
 * A simple Runnable used to callback to a helper thread after finishing a Runnable.
 *
 * This class is immutable, thus thread safe.
 */
final class WorkerRunnable implements Runnable {

    /**
     * The message to be sent upon completion.
     */
    private final Message mCompletionMessage;

    /**
     * The final destination callback.
     *
     * <p>If null, there will be no indication of completion.</p>
     */
    private final MPDAsyncHelper.AsyncExecListener mListener;

    /**
     * The Runnable to call on the worker thread.
     */
    private final Runnable mRunnable;

    /**
     * The source assigned token to give back to the source in the callable.
     *
     * <p>If null, there will be no indication of completion.</p>
     */
    private final CharSequence mToken;

    /**
     * Sole constructor.
     *
     * @param handler  The callback handler.
     * @param token    The source assigned token key for the runnable value.
     * @param runnable The runnable to run in the background.
     * @param listener The source and destination listener.
     */
    WorkerRunnable(final Handler handler, final CharSequence token, final Runnable runnable,
            final MPDAsyncHelper.AsyncExecListener listener) {
        super();

        mToken = token;
        mRunnable = runnable;
        mListener = listener;

        if (mListener == null || mToken == null) {
            mCompletionMessage = null;
        } else {
            mCompletionMessage =
                    handler.obtainMessage(MPDAsyncWorker.EVENT_EXEC_ASYNC_FINISHED, this);
        }
    }

    /**
     * The destination callback.
     *
     * @return The destination callback.
     */
    MPDAsyncHelper.AsyncExecListener getListener() {
        return mListener;
    }

    /**
     * The token to send to the destination callback.
     *
     * @return The token to send to the destination callback.
     */
    CharSequence getToken() {
        return mToken;
    }

    /**
     * The runnable to execute the runnable then send the callback.
     */
    @Override
    public void run() {
        mRunnable.run();
        if (mCompletionMessage != null) {
            mCompletionMessage.sendToTarget();
        }
    }

    @Override
    public String toString() {
        return "WorkerRunnable{" +
                "mCompletionMessage=" + mCompletionMessage +
                ", mListener=" + mListener +
                ", mRunnable=" + mRunnable +
                ", mToken=" + mToken +
                '}';
    }
}
