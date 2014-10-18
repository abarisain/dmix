/*
 * Copyright (C) 2005 The JA-SIG Collaborative
 * Copyright (C) 2010-2014 The MPDroid Project
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

package com.namelessdev.mpdroid.tools;

import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A List implementation that uses WeakReferences and is backed by a custom
 * linked list implementation. This list tries to gracefully handle elements
 * dropping out of the list due to being unreachable. <br>
 * <br>
 * An Iterator or ListIterator on this list will not fail if an element
 * disappears due to being unreachable. Also while iterating no reachable element
 * will ever be skipped.
 *
 * @author Eric Dalquist <a
 *         href="mailto:edalquist@unicon.net">edalquist@unicon.net</a>
 * @version $Revision$
 */

/** Let's assume the author was smart enough to check object equality. */
@SuppressWarnings("ObjectEquality")
public class WeakLinkedList<T> implements List<T> {

    private static final char QUOTATION = '\'';

    private static final String TAG = "WeakLinkedList";

    private final Object mLOCK = new Object();

    private final ReferenceQueue<T> mQueue = new ReferenceQueue<>();

    private WeakListNode mHead;

    private String mListName;

    private long mModCount;

    private int mSize;

    private WeakListNode mTail;

    public WeakLinkedList() {
        super();
    }

    public WeakLinkedList(final Collection<? extends T> collection) {
        this();
        addAll(collection);
    }

    public WeakLinkedList(final String name) {
        super();
        mListName = name;
    }

    @Override
    public void add(final int location, final T object) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = listIterator(location);
            itr.add(object);
        }
    }

    @Override
    public boolean add(final T object) {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            add(mSize, object);
            return true;
        }
    }

    @Override
    public final boolean addAll(final Collection<? extends T> collection) {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            return addAll(mSize, collection);
        }
    }

    @Override
    public boolean addAll(int location, final Collection<? extends T> collection) {
        boolean result = false;

        if (!collection.isEmpty()) {
            synchronized (mLOCK) {
                cleanPhantomReferences();
                for (final T element : collection) {
                    add(location++, element);
                }

                result = true;
            }
        }

        return result;
    }

    /**
     * Checks the ReferenceQueue for nodes whose values are no long valid and
     * cleanly removes them from the list
     */
    @SuppressWarnings("unchecked")
    private void cleanPhantomReferences() {
        synchronized (mLOCK) {
            WeakListNode deadNode;
            while ((deadNode = (WeakListNode) mQueue.poll()) != null) {
                // Ensure the node hasn't already been removed
                if (!deadNode.isRemoved()) {
                    if (mListName != null) {
                        Log.e(TAG, "Error : " + mListName + " has leaked. Please be sure to always"
                                + " remove yourself from the listeners.");
                    }
                    removeNode(deadNode);
                }
            }
        }
    }

    @Override
    public void clear() {
        synchronized (mLOCK) {
            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                itr.next();
                itr.remove();
            }
        }
    }

    @Override
    public boolean contains(final Object object) {
        return indexOf(object) != -1;
    }

    @Override
    public boolean containsAll(@NonNull final Collection<?> collection) {
        synchronized (mLOCK) {
            boolean foundAll = true;

            for (final Iterator<?> elementItr = collection.iterator();
                    elementItr.hasNext() && foundAll; ) {
                foundAll = contains(elementItr.next());
            }

            return foundAll;
        }
    }

    public boolean equals(final Object o) {
        Boolean result = null;

        if (this == o) {
            result = Boolean.TRUE;
        } else if (!(o instanceof List)) {
            result = Boolean.FALSE;
        } else {
            final Collection<?> other = (Collection<?>) o;

            if (size() == other.size()) {
                synchronized (mLOCK) {
                    final Iterator<?> itr1 = iterator();
                    final Iterator<?> itr2 = other.iterator();

                    while (itr1.hasNext() && itr2.hasNext()) {
                        final Object v1 = itr1.next();
                        final Object v2 = itr2.next();

                        if (v1 != v2 && (v1 == null || !v1.equals(v2))) {
                            result = Boolean.FALSE;
                            break;
                        }
                    }
                }

                if (result == null) {
                    result = Boolean.TRUE;
                }
            } else {
                result = Boolean.FALSE;
            }
        }

        return result;
    }

    @Override
    public T get(final int location) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = listIterator(location);
            try {
                return itr.next();
            } catch (final NoSuchElementException ignored) {
                throw new IndexOutOfBoundsException("Index: " + location);
            }
        }
    }

    public String getName() {
        return mListName;
    }

    public int hashCode() {
        int hashCode = 1;

        synchronized (mLOCK) {
            for (final T obj : this) {
                int thisHash = 0;

                if (obj != null) {
                    thisHash = obj.hashCode();
                }

                hashCode = 31 * hashCode + thisHash;
            }
        }

        return hashCode;
    }

    @Override
    public int indexOf(final Object object) {
        Integer indexOf = null;

        synchronized (mLOCK) {
            int index = 0;
            for (final T value : this) {
                if (object == value || (object != null && object.equals(value))) {
                    indexOf = Integer.valueOf(index);
                    break;
                }

                index++;
            }

            if (indexOf == null) {
                indexOf = Integer.valueOf(-1);
            }
        }

        return indexOf.intValue();
    }

    @Override
    public boolean isEmpty() {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            return mSize == 0;
        }
    }

    /**
     * Returns an Iterator that gracefully handles expired elements. The
     * Iterator cannot ensure that after calling hasNext() successfully a call
     * to next() will not throw a NoSuchElementException due to element
     * expiration due to weak references. <br>
     * The remove method has been implemented
     */
    @NonNull
    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    public int lastIndexOf(final Object object) {
        Integer lastIndexOf = null;

        synchronized (mLOCK) {
            cleanPhantomReferences();

            int index = mSize - 1;
            for (final ListIterator<T> itr = listIterator(mSize); itr.hasPrevious(); ) {
                final Object value = itr.previous();
                if (object == value || (object != null && object.equals(value))) {
                    lastIndexOf = Integer.valueOf(index);
                    break;
                }

                index--;
            }

            if (lastIndexOf == null) {
                lastIndexOf = Integer.valueOf(-1);
            }
        }

        return lastIndexOf.intValue();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator(final int location) {
        synchronized (mLOCK) {
            cleanPhantomReferences();

            if (location < 0) {
                throw new IndexOutOfBoundsException("index must be >= 0");
            }

            if (location > mSize) {
                throw new IndexOutOfBoundsException("index must be <= size()");
            }

            return new DurableListIterator(location);
        }
    }

    @Override
    public T remove(final int location) {
        synchronized (mLOCK) {
            cleanPhantomReferences();

            final ListIterator<T> itr = listIterator(location);
            final T value;
            try {
                value = itr.next();
            } catch (final NoSuchElementException ignored) {
                throw new IndexOutOfBoundsException("Index: " + location);
            }

            itr.remove();
            return value;
        }
    }

    @Override
    public boolean remove(final Object object) {
        boolean removed = false;

        synchronized (mLOCK) {
            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                if (object == value || (object != null && object.equals(value))) {
                    itr.remove();
                    removed = true;
                    break;
                }
            }
        }

        return removed;
    }

    @Override
    public boolean removeAll(@NonNull final Collection<?> collection) {
        synchronized (mLOCK) {
            boolean changed = false;

            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                if (collection.contains(value)) {
                    itr.remove();
                    changed = true;
                }
            }

            return changed;
        }
    }

    /**
     * Removes a node from the list
     *
     * @param deadNode The node which gets removed by this method.
     */
    private void removeNode(final WeakListNode deadNode) {
        synchronized (mLOCK) {
            if (deadNode.isRemoved()) {
                throw new IllegalArgumentException("node has already been removed");
            }

            final WeakListNode deadPrev = deadNode.getPrev();
            final WeakListNode deadNext = deadNode.getNext();

            // Removing the only node in the list
            if (deadPrev == null && deadNext == null) {
                mHead = null;
                mTail = null;
            }
            // Removing the first node in the list
            else if (deadPrev == null) {
                mHead = deadNext;
                deadNext.setPrev(null);
            }
            // Removing the last node in the list
            else if (deadNext == null) {
                mTail = deadPrev;
                deadPrev.setNext(null);
            }
            // Removing any other node
            else {
                deadPrev.setNext(deadNext);
                deadNext.setPrev(deadPrev);
            }

            // Flag the removed node as removed
            deadNode.setRemoved();

            // Update the lists size
            mSize--;

            // Ensure the list is still valid
            if (mSize < 0) {
                throw new IllegalStateException("size is less than zero - '" + mSize + '\'');
            }
            if (mSize == 0 && mHead != null) {
                throw new IllegalStateException("size is zero but head is not null");
            }
            if (mSize == 0 && mTail != null) {
                throw new IllegalStateException("size is zero but tail is not null");
            }
            if (mSize > 0 && mHead == null) {
                throw new IllegalStateException("size is greater than zero but head is null");
            }
            if (mSize > 0 && mTail == null) {
                throw new IllegalStateException("size is greater than zero but tail is null");
            }
        }
    }

    @Override
    public boolean retainAll(@NonNull final Collection<?> collection) {
        synchronized (mLOCK) {
            boolean changed = false;

            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                if (!collection.contains(value)) {
                    itr.remove();
                    changed = true;
                }
            }

            return changed;
        }
    }

    @Override
    public T set(final int location, final T object) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = listIterator(location);
            try {
                final T oldVal = itr.next();
                itr.set(object);
                return oldVal;
            } catch (final NoSuchElementException ignored) {
                throw new IndexOutOfBoundsException("Index: " + location);
            }
        }
    }

    public void setName(final String name) {
        mListName = name;
    }

    @Override
    public int size() {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            return mSize;
        }
    }

    @NonNull
    @Override
    public List<T> subList(final int start, final int end) {
        throw new UnsupportedOperationException("subList is not yet supported");
    }

    @NonNull
    @Override
    public Object[] toArray() {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            return toArray(new Object[mSize]);
        }
    }

    @NonNull
    @Override
    public Object[] toArray(@NonNull Object[] array) {
        synchronized (mLOCK) {
            cleanPhantomReferences();

            if (array.length < mSize) {
                array = (Object[]) Array.newInstance(array.getClass().getComponentType(), mSize);
            }

            int index = 0;
            for (final T value : this) {
                array[index] = value;
                index++;
            }

            if (array.length > index) {
                array[index] = null;
            }

            return array;
        }
    }

    public String toString() {
        final StringBuilder buff = new StringBuilder();

        buff.append('[');
        synchronized (mLOCK) {
            for (final Iterator<?> itr = iterator(); itr.hasNext(); ) {
                buff.append(itr.next());

                if (itr.hasNext()) {
                    buff.append(", ");
                }
            }
        }
        buff.append(']');

        return buff.toString();
    }

    /**
     * Iterator implementation that can deal with weak nodes.
     */
    @SuppressWarnings("ObjectEquality")
    private class DurableListIterator implements ListIterator<T> {

        private long mExpectedModCount;

        private int mIndex;

        private byte mLastDirection;

        private WeakListNode mNextNode;

        private WeakListNode mPrevNode;

        private DurableListIterator(final int initialIndex) {
            super();
            synchronized (mLOCK) {
                mExpectedModCount = mModCount;
                mLastDirection = (byte) 0;

                // Make worst case for initialization O(N/2)
                if (initialIndex <= mSize / 2) {
                    mPrevNode = null;
                    mNextNode = mHead;
                    mIndex = 0;

                    // go head -> tail to find the initial index
                    while (nextIndex() < initialIndex) {
                        next();
                    }
                } else {
                    mPrevNode = mTail;
                    mNextNode = null;
                    mIndex = mSize;

                    // go tail -> head to find the initial index
                    while (nextIndex() > initialIndex) {
                        previous();
                    }
                }
            }
        }

        @Override
        public void add(final T object) {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();

                final WeakListNode newNode = new WeakListNode(object);

                // Add first node
                if (mSize == 0) {
                    mHead = newNode;
                    mTail = newNode;
                }
                // Add to head
                else if (mIndex == 0) {
                    newNode.setNext(mHead);
                    mHead.setPrev(newNode);
                    mHead = newNode;
                }
                // Add to tail
                else if (mIndex == mSize) {
                    newNode.setPrev(mTail);
                    mTail.setNext(newNode);
                    mTail = newNode;
                }
                // Add otherwise
                else {
                    newNode.setPrev(mPrevNode);
                    newNode.setNext(mNextNode);
                    newNode.getPrev().setNext(newNode);
                    newNode.getNext().setPrev(newNode);
                }

                // The new node is always set as the previous node
                mPrevNode = newNode;

                // Update all the counters
                mSize++;
                mModCount++;
                mIndex++;
                mExpectedModCount++;
                mLastDirection = (byte) 0;
            }
        }

        /**
         * Checks to see if the list has been modified by means other than this
         * Iterator
         */
        private void checkConcurrentModification() {
            if (mExpectedModCount != mModCount) {
                throw new ConcurrentModificationException(
                        "The WeakLinkedList was modified outside of this Iterator");
            }
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();
                return mNextNode != null;
            }
        }

        @Override
        public boolean hasPrevious() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();
                return mPrevNode != null;
            }
        }

        /**
         * @see java.util.Iterator#next()
         */
        @Override
        public T next() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();

                if (mNextNode == null) {
                    throw new NoSuchElementException("No elements remain to iterate through");
                }

                // Move the node refs up one
                mPrevNode = mNextNode;
                mNextNode = mNextNode.getNext();

                // Update the list index
                mIndex++;

                // Mark the iterator as clean so add/remove/set operations will
                // work
                mLastDirection = (byte) 1;

                // Return the appropriate value
                return mPrevNode.get();
            }
        }

        @Override
        public int nextIndex() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();
                return mIndex;
            }
        }

        @Override
        public T previous() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();

                if (mPrevNode == null) {
                    throw new NoSuchElementException(
                            "No elements previous element to iterate through");
                }

                // Move the node refs down one
                mNextNode = mPrevNode;
                mPrevNode = mPrevNode.getPrev();

                // Update the list index
                mIndex--;

                // Mark the iterator as clean so add/remove/set operations will
                // work
                mLastDirection = (byte) -1;

                // Return the appropriate value
                return mNextNode.get();
            }
        }

        @Override
        public int previousIndex() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();
                return mIndex - 1;
            }
        }

        /**
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();

                if (mLastDirection == 0) {
                    throw new IllegalStateException("next or previous must be called first");
                }

                if (mLastDirection == 1) {
                    if (mPrevNode == null) {
                        throw new IllegalStateException("No element to remove");
                    }

                    // Use the remove node method from the List to ensure clean
                    // up
                    removeNode(mPrevNode);

                    // Update the prevNode reference
                    mPrevNode = mPrevNode.getPrev();

                    // Update position
                    mIndex--;
                } else if (mLastDirection == -1) {
                    if (mNextNode == null) {
                        throw new IllegalStateException("No element to remove");
                    }

                    // Use the remove node method from the List to ensure clean
                    // up
                    removeNode(mNextNode);

                    // Update the nextNode reference
                    mNextNode = mNextNode.getNext();
                }

                // Update the counters
                mExpectedModCount++;
                mModCount++;
                mLastDirection = (byte) 0;
            }
        }

        @Override
        public void set(final T object) {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();

                if (mPrevNode == null) {
                    throw new IllegalStateException("No element to set");
                }
                if (mLastDirection == 0) {
                    throw new IllegalStateException("next or previous must be called first");
                }

                final WeakListNode deadNode = mPrevNode;
                final WeakListNode newNode = new WeakListNode(object);

                // If the replaced node was the head of the list
                if (deadNode == mHead) {
                    mHead = newNode;
                }
                // Otherwise replace refs with node before the one being set
                else {
                    newNode.setPrev(deadNode.getPrev());
                    newNode.getPrev().setNext(newNode);
                }

                // If the replaced node was the tail of the list
                if (deadNode == mTail) {
                    mTail = newNode;
                }
                // Otherwise replace refs with node after the one being set
                else {
                    newNode.setNext(deadNode.getNext());
                    newNode.getNext().setPrev(newNode);
                }

                // Update the ListIterator reference
                mPrevNode = newNode;

                // Clean up the dead node(WeakLinkedList.this.removeNode is not
                // used as it does not work with inserting nodes)
                deadNode.setRemoved();

                // Update counters
                mExpectedModCount++;
                mModCount++;
                mLastDirection = (byte) 0;
            }
        }

        public String toString() {
            final StringBuilder buff = new StringBuilder();

            buff.append("[index='").append(mIndex).append(QUOTATION);

            buff.append(", prev=");
            if (mPrevNode == null) {
                buff.append("null");
            } else {
                buff.append(QUOTATION).append(mPrevNode).append(QUOTATION);
            }

            buff.append(", next=");
            if (mNextNode == null) {
                buff.append("null");
            } else {
                buff.append(QUOTATION).append(mNextNode).append(QUOTATION);
            }

            buff.append(']');

            return buff.toString();
        }

        /**
         * Inspects the previous and next nodes to see if either have been
         * removed from the list because of a removed reference
         */
        private void updateRefs() {
            synchronized (mLOCK) {
                cleanPhantomReferences();

                // Update nextNode refs
                while (mNextNode != null
                        && (mNextNode.isRemoved() || mNextNode.isEnqueued())) {
                    mNextNode = mNextNode.getNext();
                }

                // Update prevNode refs
                while (mPrevNode != null
                        && (mPrevNode.isRemoved() || mPrevNode.isEnqueued())) {
                    mPrevNode = mPrevNode.getPrev();
                }

                // Update index
                mIndex = 0;
                WeakListNode currNode = mPrevNode;
                while (currNode != null) {
                    currNode = currNode.getPrev();
                    mIndex++;
                }

                // Ensure the iterator is still valid
                if (mNextNode != null && mNextNode.getPrev() != mPrevNode) {
                    throw new IllegalStateException("nextNode.prev != prevNode");
                }
                if (mPrevNode != null && mPrevNode.getNext() != mNextNode) {
                    throw new IllegalStateException("prevNode.next != nextNode");
                }
            }
        }
    }

    /**
     * Represents a node in the list
     */
    private class WeakListNode extends WeakReference<T> {

        private WeakListNode mNext;

        private WeakListNode mPrev;

        private boolean mRemoved;

        WeakListNode(final T value) {
            super(value, mQueue);
        }

        /**
         * @return Returns the next.
         */
        public WeakListNode getNext() {
            return mNext;
        }

        /**
         * @return Returns the prev.
         */
        public WeakListNode getPrev() {
            return mPrev;
        }

        /**
         * @return true if this node has been removed from a list.
         */
        public boolean isRemoved() {
            return mRemoved;
        }

        /**
         * @param next The next to set.
         */
        public void setNext(final WeakListNode next) {
            mNext = next;
        }

        /**
         * @param prev The prev to set.
         */
        public void setPrev(final WeakListNode prev) {
            mPrev = prev;
        }

        /**
         * Marks this node as being removed from a list.
         */
        public void setRemoved() {
            mRemoved = true;
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            final StringBuilder buff = new StringBuilder();

            buff.append("[prev=");

            if (mPrev == null) {
                buff.append("null");
            } else {
                buff.append(QUOTATION).append(mPrev.get()).append(QUOTATION);
            }

            buff.append(", value='");
            /** It actually IS necessary. */
            //noinspection UnnecessaryThis
            buff.append(this.get());
            buff.append("', next=");

            if (mNext == null) {
                buff.append("null");
            } else {
                buff.append(QUOTATION).append(mNext.get()).append(QUOTATION);
            }

            buff.append(']');

            return buff.toString();
        }
    }
}
