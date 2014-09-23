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
public class WeakLinkedList<T> implements List<T> {

    private final Object mLOCK = new Object();

    private final ReferenceQueue<T> mQueue = new ReferenceQueue<>();

    private WeakListNode mHead = null;

    private String mListName = null;

    private long mModCount = 0;

    private int mSize = 0;

    private WeakListNode mTail = null;

    public WeakLinkedList() {
        super();
    }

    public WeakLinkedList(final Collection<? extends T> c) {
        this();
        addAll(c);
    }

    public WeakLinkedList(final String name) {
        super();
        setName(name);
    }

    public void add(final int index, final T element) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = listIterator(index);
            itr.add(element);
        }
    }

    public boolean add(final T o) {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            add(mSize, o);
            return true;
        }
    }

    public boolean addAll(final Collection<? extends T> c) {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            return addAll(mSize, c);
        }
    }

    public boolean addAll(int index, final Collection<? extends T> c) {
        if (c.size() <= 0) {
            return false;
        }

        synchronized (mLOCK) {
            cleanPhantomReferences();
            for (final T element : c) {
                add(index++, element);
            }

            return true;
        }
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
                        Log.e("WeakLinkedList",
                                "Error : "
                                        + mListName
                                        + " has leaked. Please be sure to always remove yourself from the listeners.");
                    }
                    removeNode(deadNode);
                }
            }
        }
    }

    public void clear() {
        synchronized (mLOCK) {
            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                itr.next();
                itr.remove();
            }
        }
    }

    public boolean contains(final Object o) {
        return indexOf(o) != -1;
    }

    public boolean containsAll(final Collection<?> c) {
        synchronized (mLOCK) {
            boolean foundAll = true;

            for (final Iterator<?> elementItr = c.iterator(); elementItr.hasNext() && foundAll; ) {
                foundAll = contains(elementItr.next());
            }

            return foundAll;
        }
    }

    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof List)) {
            return false;
        } else {
            final List<?> other = (List<?>) obj;

            if (size() != other.size()) {
                return false;
            } else {
                synchronized (mLOCK) {
                    final Iterator<?> itr1 = iterator();
                    final Iterator<?> itr2 = other.iterator();

                    while (itr1.hasNext() && itr2.hasNext()) {
                        final Object v1 = itr1.next();
                        final Object v2 = itr2.next();

                        if (v1 != v2 && (v1 == null || !v1.equals(v2))) {
                            return false;
                        }
                    }
                }

                return true;
            }
        }
    }

    public T get(final int index) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = listIterator(index);
            try {
                return itr.next();
            } catch (final NoSuchElementException ignored) {
                throw new IndexOutOfBoundsException("Index: " + index);
            }
        }
    }

    public String getName() {
        return mListName;
    }

    public int hashCode() {
        int hashCode = 1;

        synchronized (mLOCK) {
            for (final Iterator<?> itr = iterator(); itr.hasNext(); ) {
                final Object obj = itr.next();
                hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
            }
        }

        return hashCode;
    }

    public int indexOf(final Object o) {
        synchronized (mLOCK) {
            int index = 0;
            for (final ListIterator<T> itr = listIterator(); itr.hasNext(); ) {
                final T value = itr.next();
                if (o == value || (o != null && o.equals(value))) {
                    return index;
                }

                index++;
            }

            return -1;
        }
    }

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
    public Iterator<T> iterator() {
        return listIterator();
    }

    public int lastIndexOf(final Object o) {
        synchronized (mLOCK) {
            cleanPhantomReferences();

            int index = mSize - 1;
            for (final ListIterator<T> itr = listIterator(mSize); itr.hasPrevious(); ) {
                final Object value = itr.previous();
                if (o == value || (o != null && o.equals(value))) {
                    return index;
                }

                index--;
            }

            return -1;
        }
    }

    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    public ListIterator<T> listIterator(final int index) {
        synchronized (mLOCK) {
            cleanPhantomReferences();

            if (index < 0) {
                throw new IndexOutOfBoundsException("index must be >= 0");
            } else if (index > mSize) {
                throw new IndexOutOfBoundsException("index must be <= size()");
            }

            return new DurableListIterator(index);
        }
    }

    public T remove(final int index) {
        synchronized (mLOCK) {
            cleanPhantomReferences();

            final ListIterator<T> itr = listIterator(index);
            final T value;
            try {
                value = itr.next();
            } catch (final NoSuchElementException ignored) {
                throw new IndexOutOfBoundsException("Index: " + index);
            }

            itr.remove();
            return value;
        }
    }

    public boolean remove(final Object o) {
        synchronized (mLOCK) {
            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                if (o == value || (o != null && o.equals(value))) {
                    itr.remove();
                    return true;
                }
            }

            return false;
        }
    }

    public boolean removeAll(final Collection<?> c) {
        synchronized (mLOCK) {
            boolean changed = false;

            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                if (c.contains(value)) {
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

    public boolean retainAll(final Collection<?> c) {
        synchronized (mLOCK) {
            boolean changed = false;

            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                if (!c.contains(value)) {
                    itr.remove();
                    changed = true;
                }
            }

            return changed;
        }
    }

    public T set(final int index, final T element) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = listIterator(index);
            try {
                final T oldVal = itr.next();
                itr.set(element);
                return oldVal;
            } catch (final NoSuchElementException ignored) {
                throw new IndexOutOfBoundsException("Index: " + index);
            }
        }
    }

    public void setName(final String name) {
        mListName = name;
    }

    public int size() {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            return mSize;
        }
    }

    public List<T> subList(final int fromIndex, final int toIndex) {
        // TODO
        throw new UnsupportedOperationException("subList is not yet supported");
    }

    public Object[] toArray() {
        synchronized (mLOCK) {
            cleanPhantomReferences();
            return toArray(new Object[mSize]);
        }
    }

    @SuppressWarnings("unchecked")
    public Object[] toArray(Object[] a) {
        synchronized (mLOCK) {
            cleanPhantomReferences();

            if (a.length < mSize) {
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), mSize);
            }

            int index = 0;
            for (final ListIterator<?> itr = listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                a[index] = value;
                index++;
            }

            if (a.length > index) {
                a[index] = null;
            }

            return a;
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
                mLastDirection = 0;

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

        public void add(final T o) {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();

                final WeakListNode newNode = new WeakListNode(o);

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
                mLastDirection = 0;
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
        public boolean hasNext() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();
                return mNextNode != null;
            }
        }

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
                mLastDirection = 1;

                // Return the appropriate value
                return mPrevNode.get();
            }
        }

        public int nextIndex() {
            synchronized (mLOCK) {
                checkConcurrentModification();
                updateRefs();
                return mIndex;
            }
        }

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
                mLastDirection = -1;

                // Return the appropriate value
                return mNextNode.get();
            }
        }

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
                mLastDirection = 0;
            }
        }

        public void set(final T o) {
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
                final WeakListNode newNode = new WeakListNode(o);

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
                mLastDirection = 0;
            }
        }

        public String toString() {
            final StringBuilder buff = new StringBuilder();

            buff.append("[index='").append(mIndex).append('\'');

            buff.append(", prev=");
            if (mPrevNode == null) {
                buff.append("null");
            } else {
                buff.append('\'').append(mPrevNode).append('\'');
            }

            buff.append(", next=");
            if (mNextNode == null) {
                buff.append("null");
            } else {
                buff.append('\'').append(mNextNode).append('\'');
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

        private boolean mRemoved = false;

        public WeakListNode(final T value) {
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
                buff.append('\'').append(mPrev.get()).append('\'');
            }

            buff.append(", value='");
            /** It actually IS necessary. */
            //noinspection UnnecessaryThis
            buff.append(this.get());
            buff.append("', next=");

            if (mNext == null) {
                buff.append("null");
            } else {
                buff.append('\'').append(mNext.get()).append('\'');
            }

            buff.append(']');

            return buff.toString();
        }
    }
}
