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

package com.namelessdev.mpdroid.helpers;

import com.namelessdev.mpdroid.cover.ICoverRetriever;

import android.graphics.Bitmap;

public class CoverInfo extends AlbumInfo {

    public static final int MAX_SIZE = 0;

    private int mCoverMaxSize = MAX_SIZE;

    private int mCachedCoverMaxSize = MAX_SIZE;

    private Bitmap[] mBitmap = new Bitmap[0];

    private byte[] mCoverBytes = new byte[0];

    private ICoverRetriever mCoverRetriever;

    private CoverDownloadListener mListener;

    private boolean mPriority;

    private boolean mRequestGivenUp = false;

    private STATE mState = STATE.NEW;

    public CoverInfo(final AlbumInfo albumInfo) {
        super(albumInfo.getArtist(), albumInfo.getAlbum(),
                albumInfo.getPath(), albumInfo.getFilename());
    }

    public CoverInfo(final CoverInfo coverInfo) {
        super(coverInfo.mArtist, coverInfo.mAlbum, coverInfo.mPath, coverInfo.mFilename);
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

    public int getCachedCoverMaxSize() {
        return mCachedCoverMaxSize;
    }

    public byte[] getCoverBytes() {
        return mCoverBytes;
    }

    public int getCoverMaxSize() {
        return mCoverMaxSize;
    }

    public ICoverRetriever getCoverRetriever() {
        return mCoverRetriever;
    }

    public CoverDownloadListener getListener() {
        return mListener;
    }

    public STATE getState() {
        return mState;
    }

    public boolean isPriority() {
        return mPriority;
    }

    public boolean isRequestGivenUp() {
        return mRequestGivenUp;
    }

    public void setBitmap(final Bitmap[] bitmap) {
        mBitmap = bitmap;
    }

    public void setCachedCoverMaxSize(final int cachedCoverMaxSize) {
        mCachedCoverMaxSize = cachedCoverMaxSize;
    }

    public void setCoverBytes(final byte[] coverBytes) {
        mCoverBytes = coverBytes;
    }

    public void setCoverMaxSize(final int coverMaxSize) {
        mCoverMaxSize = coverMaxSize;
    }

    public void setCoverRetriever(final ICoverRetriever coverRetriever) {
        mCoverRetriever = coverRetriever;
    }

    @Override
    public void setFilename(final String filename) {
        mFilename = filename;
    }

    public void setListener(final CoverDownloadListener listener) {
        mListener = listener;
    }

    @Override
    public void setPath(final String path) {
        mPath = path;
    }

    public void setPriority(final boolean priority) {
        mPriority = priority;
    }

    public void setRequestGivenUp(final boolean requestGivenUp) {
        mRequestGivenUp = requestGivenUp;
    }

    public void setState(final STATE state) {
        mState = state;
    }

    @Override
    public String toString() {
        return "CoverInfo{state=" + mState + ", artist=" + mArtist == null ? ""
                : mArtist + ", album="
                        + mAlbum == null ? "" : mAlbum + " priority=" + mPriority + '}';
    }

    public enum STATE {
        NEW, CACHE_COVER_FETCH, WEB_COVER_FETCH, CREATE_BITMAP, COVER_FOUND, COVER_NOT_FOUND
    }
}
