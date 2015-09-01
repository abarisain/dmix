/*
 * Copyright (C) 2010-2015 The MPDroid Project
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

package com.namelessdev.mpdroid.cover;

import com.namelessdev.mpdroid.cover.retriever.ICoverRetriever;
import com.namelessdev.mpdroid.helpers.AlbumInfo;

import android.graphics.Bitmap;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

class CoverInfo extends AlbumInfo {

    static final int MAX_SIZE = 0;

    static final int STATE_CACHE_FETCH = 1;

    static final int STATE_CREATE_BITMAP = 3;

    static final int STATE_FOUND = 4;

    static final int STATE_NEW = 0;

    static final int STATE_NOT_FOUND = 5;

    static final int STATE_WEB_FETCH = 2;

    private Bitmap[] mBitmap = new Bitmap[0];

    private int mCachedCoverMaxSize = MAX_SIZE;

    private byte[] mCoverBytes = new byte[0];

    private int mCoverMaxSize = MAX_SIZE;

    private ICoverRetriever mCoverRetriever;

    private CoverDownloadListener mListener;

    private boolean mPriority;

    private boolean mRequestGivenUp = false;

    private int mState;

    CoverInfo(final AlbumInfo albumInfo) {
        super(albumInfo);
    }

    CoverInfo(final CoverInfo coverInfo) {
        super(coverInfo);
        mState = coverInfo.mState;
        mBitmap = coverInfo.mBitmap;
        mCoverBytes = coverInfo.mCoverBytes;
        mPriority = coverInfo.mPriority;
        mCoverMaxSize = coverInfo.mCoverMaxSize;
        mCachedCoverMaxSize = coverInfo.mCachedCoverMaxSize;
        mCoverRetriever = coverInfo.mCoverRetriever;
        mListener = coverInfo.mListener;
        mRequestGivenUp = coverInfo.mRequestGivenUp;
    }

    public Bitmap[] getBitmap() {
        return mBitmap;
    }

    int getCachedCoverMaxSize() {
        return mCachedCoverMaxSize;
    }

    byte[] getCoverBytes() {
        return mCoverBytes;
    }

    int getCoverMaxSize() {
        return mCoverMaxSize;
    }

    ICoverRetriever getCoverRetriever() {
        return mCoverRetriever;
    }

    CoverDownloadListener getListener() {
        return mListener;
    }

    @STATE
    int getState() {
        return mState;
    }

    boolean isPriority() {
        return mPriority;
    }

    boolean isRequestGivenUp() {
        return mRequestGivenUp;
    }

    void setBitmap(final Bitmap... bitmap) {
        mBitmap = bitmap;
    }

    void setCachedCoverMaxSize(final int cachedCoverMaxSize) {
        mCachedCoverMaxSize = cachedCoverMaxSize;
    }

    void setCoverBytes(final byte[] coverBytes) {
        mCoverBytes = coverBytes;
    }

    void setCoverMaxSize(final int coverMaxSize) {
        mCoverMaxSize = coverMaxSize;
    }

    void setCoverRetriever(final ICoverRetriever coverRetriever) {
        mCoverRetriever = coverRetriever;
    }

    void setListener(final CoverDownloadListener listener) {
        mListener = listener;
    }

    void setPriority(final boolean priority) {
        mPriority = priority;
    }

    void setRequestGivenUp(final boolean requestGivenUp) {
        mRequestGivenUp = requestGivenUp;
    }

    void setState(@STATE final int state) {
        mState = state;
    }

    @Override
    public String toString() {
        return super.toString() + '\n' +
                "CoverInfo{" +
                "mCoverMaxSize=" + mCoverMaxSize +
                ", mCachedCoverMaxSize=" + mCachedCoverMaxSize +
                ", mBitmap=" + Arrays.toString(mBitmap) +
                ", mCoverBytes=" + Arrays.toString(mCoverBytes) +
                ", mCoverRetriever=" + mCoverRetriever +
                ", mListener=" + mListener +
                ", mPriority=" + mPriority +
                ", mRequestGivenUp=" + mRequestGivenUp +
                ", mState=" + mState +
                '}';
    }

    /**
     * This is an annotation which tracks the current cover status state.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_NEW, STATE_CACHE_FETCH, STATE_WEB_FETCH, STATE_CREATE_BITMAP, STATE_FOUND,
            STATE_NOT_FOUND})
    @interface STATE {

    }
}
