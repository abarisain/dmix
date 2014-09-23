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

    private final ReferenceQueue<T> mQueue = new ReferenceQueue<T>();

    private WeakListNode mHead = null;

    private String mListName = null;

    private long mModCount = 0;

    private int mSize = 0;

    private WeakListNode mTail = null;

    public WeakLinkedList() {
    }

    public WeakLinkedList(Collection<? extends T> c) {
        this();
        this.addAll(c);
    }

    public WeakLinkedList(String name) {
        super();
        setName(name);
    }

    /**
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(final int index, final T element) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = this.listIterator(index);
            itr.add(element);
        }
    }

    /**
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(T o) {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();
            this.add(this.mSize, o);
            return true;
        }
    }

    /**
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends T> c) {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();
            return this.addAll(this.mSize, c);
        }
    }

    /**
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection<? extends T> c) {
        if (c.size() <= 0) {
            return false;
        }

        synchronized (mLOCK) {
            this.cleanPhantomReferences();
            for (T element : c) {
                this.add(index++, element);
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
                    this.removeNode(deadNode);
                }
            }
        }
    }

    /**
     * @see java.util.List#clear()
     */
    public void clear() {
        synchronized (mLOCK) {
            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext(); ) {
                itr.next();
                itr.remove();
            }
        }
    }

    /**
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return this.indexOf(o) != -1;
    }

    /**
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> c) {
        synchronized (mLOCK) {
            boolean foundAll = true;

            for (final Iterator<?> elementItr = c.iterator(); elementItr.hasNext() && foundAll; ) {
                foundAll = this.contains(elementItr.next());
            }

            return foundAll;
        }
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof List)) {
            return false;
        } else {
            final List<?> other = (List<?>) obj;

            if (this.size() != other.size()) {
                return false;
            } else {
                synchronized (mLOCK) {
                    final Iterator<?> itr1 = this.iterator();
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

    /**
     * @see java.util.List#get(int)
     */
    public T get(int index) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = this.listIterator(index);
            try {
                return (itr.next());
            } catch (final NoSuchElementException ignored) {
                throw (new IndexOutOfBoundsException("Index: " + index));
            }
        }
    }

    public String getName() {
        return mListName;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int hashCode = 1;

        synchronized (mLOCK) {
            for (final Iterator<?> itr = this.iterator(); itr.hasNext(); ) {
                final Object obj = itr.next();
                hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
            }
        }

        return hashCode;
    }

    /**
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        synchronized (mLOCK) {
            int index = 0;
            for (final ListIterator<T> itr = this.listIterator(); itr.hasNext(); ) {
                final T value = itr.next();
                if (o == value || (o != null && o.equals(value))) {
                    return index;
                }

                index++;
            }

            return -1;
        }
    }

    /**
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty() {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();
            return this.mSize == 0;
        }
    }

    /**
     * Returns an Iterator that gracefully handles expired elements. The
     * Iterator cannot ensure that after calling hasNext() successfully a call
     * to next() will not throw a NoSuchElementException due to element
     * expiration due to weak references. <br>
     * The remove method has been implemented
     *
     * @see java.util.List#iterator()
     */
    public Iterator<T> iterator() {
        return this.listIterator();
    }

    /**
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o) {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();

            int index = this.mSize - 1;
            for (final ListIterator<T> itr = this.listIterator(this.mSize); itr.hasPrevious(); ) {
                final Object value = itr.previous();
                if (o == value || (o != null && o.equals(value))) {
                    return index;
                }

                index--;
            }

            return -1;
        }
    }

    /**
     * @see java.util.List#listIterator()
     */
    public ListIterator<T> listIterator() {
        return this.listIterator(0);
    }

    /**
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<T> listIterator(int index) {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();

            if (index < 0) {
                throw new IndexOutOfBoundsException("index must be >= 0");
            } else if (index > this.mSize) {
                throw new IndexOutOfBoundsException("index must be <= size()");
            }

            return new DurableListIterator(index);
        }
    }

    /**
     * @see java.util.List#remove(int)
     */
    public T remove(int index) {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();

            final ListIterator<T> itr = this.listIterator(index);
            final T value;
            try {
                value = itr.next();
            } catch (final NoSuchElementException ignored) {
                throw (new IndexOutOfBoundsException("Index: " + index));
            }

            itr.remove();
            return value;
        }
    }

    /**
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        synchronized (mLOCK) {
            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                if (o == value || (o != null && o.equals(value))) {
                    itr.remove();
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> c) {
        synchronized (mLOCK) {
            boolean changed = false;

            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext(); ) {
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
    private void removeNode(WeakListNode deadNode) {
        synchronized (mLOCK) {
            if (deadNode.isRemoved()) {
                throw new IllegalArgumentException("node has already been removed");
            }

            final WeakListNode deadPrev = deadNode.getPrev();
            final WeakListNode deadNext = deadNode.getNext();

            // Removing the only node in the list
            if (deadPrev == null && deadNext == null) {
                this.mHead = null;
                this.mTail = null;
            }
            // Removing the first node in the list
            else if (deadPrev == null) {
                this.mHead = deadNext;
                deadNext.setPrev(null);
            }
            // Removing the last node in the list
            else if (deadNext == null) {
                this.mTail = deadPrev;
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
            this.mSize--;

            // Ensure the list is still valid
            if (this.mSize < 0) {
                throw new IllegalStateException("size is less than zero - '" + this.mSize + "'");
            }
            if (this.mSize == 0 && this.mHead != null) {
                throw new IllegalStateException("size is zero but head is not null");
            }
            if (this.mSize == 0 && this.mTail != null) {
                throw new IllegalStateException("size is zero but tail is not null");
            }
            if (this.mSize > 0 && this.mHead == null) {
                throw new IllegalStateException("size is greater than zero but head is null");
            }
            if (this.mSize > 0 && this.mTail == null) {
                throw new IllegalStateException("size is greater than zero but tail is null");
            }
        }
    }

    /**
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> c) {
        synchronized (mLOCK) {
            boolean changed = false;

            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext(); ) {
                final Object value = itr.next();
                if (!c.contains(value)) {
                    itr.remove();
                    changed = true;
                }
            }

            return changed;
        }
    }

    /**
     * @see java.util.List#set(int, java.lang.Object)
     */
    public T set(int index, T element) {
        synchronized (mLOCK) {
            final ListIterator<T> itr = this.listIterator(index);
            try {
                final T oldVal = itr.next();
                itr.set(element);
                return oldVal;
            } catch (final NoSuchElementException ignored) {
                throw (new IndexOutOfBoundsException("Index: " + index));
            }
        }
    }

    public void setName(String name) {
        mListName = name;
    }

    /**
     * @see java.util.List#size()
     */
    public int size() {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();
            return this.mSize;
        }
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    public List<T> subList(int fromIndex, int toIndex) {
        // TODO
        throw new UnsupportedOperationException("subList is not yet supported");
    }

    /**
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();
            return this.toArray(new Object[this.mSize]);
        }
    }

    /**
     * @see java.util.List#toArray(java.lang.Object[])
     */
    @SuppressWarnings("unchecked")
    public Object[] toArray(Object[] a) {
        synchronized (mLOCK) {
            this.cleanPhantomReferences();

            if (a.length < this.mSize) {
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), this.mSize);
            }

            int index = 0;
            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext(); ) {
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

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuffer buff = new StringBuffer();

        buff.append("[");
        synchronized (mLOCK) {
            for (final Iterator<?> itr = this.iterator(); itr.hasNext(); ) {
                buff.append(itr.next());

                if (itr.hasNext()) {
                    buff.append(", ");
                }
            }
        }
        buff.append("]");

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
            synchronized (mLOCK) {
                this.mExpectedModCount = WeakLinkedList.this.mModCount;
                this.mLastDirection = 0;

                // Make worst case for initialization O(N/2)
                if (initialIndex <= (mSize / 2)) {
                    this.mPrevNode = null;
                    this.mNextNode = WeakLinkedList.this.mHead;
                    this.mIndex = 0;

                    // go head -> tail to find the initial index
                    while (this.nextIndex() < initialIndex) {
                        this.next();
                    }
                } else {
                    this.mPrevNode = WeakLinkedList.this.mTail;
                    this.mNextNode = null;
                    this.mIndex = WeakLinkedList.this.mSize;

                    // go tail -> head to find the initial index
                    while (this.nextIndex() > initialIndex) {
                        this.previous();
                    }
                }
            }
        }

        /**
         * @see java.util.ListIterator#add(java.lang.Object)
         */
        public void add(T o) {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                final WeakListNode newNode = new WeakListNode(o);

                // Add first node
                if (WeakLinkedList.this.mSize == 0) {
                    WeakLinkedList.this.mHead = newNode;
                    WeakLinkedList.this.mTail = newNode;
                }
                // Add to head
                else if (this.mIndex == 0) {
                    newNode.setNext(WeakLinkedList.this.mHead);
                    WeakLinkedList.this.mHead.setPrev(newNode);
                    WeakLinkedList.this.mHead = newNode;
                }
                // Add to tail
                else if (this.mIndex == WeakLinkedList.this.mSize) {
                    newNode.setPrev(WeakLinkedList.this.mTail);
                    WeakLinkedList.this.mTail.setNext(newNode);
                    WeakLinkedList.this.mTail = newNode;
                }
                // Add otherwise
                else {
                    newNode.setPrev(this.mPrevNode);
                    newNode.setNext(this.mNextNode);
                    newNode.getPrev().setNext(newNode);
                    newNode.getNext().setPrev(newNode);
                }

                // The new node is always set as the previous node
                this.mPrevNode = newNode;

                // Update all the counters
                WeakLinkedList.this.mSize++;
                WeakLinkedList.this.mModCount++;
                this.mIndex++;
                this.mExpectedModCount++;
                this.mLastDirection = 0;
            }
        }

        /**
         * Checks to see if the list has been modified by means other than this
         * Iterator
         */
        private void checkConcurrentModification() {
            if (this.mExpectedModCount != WeakLinkedList.this.mModCount) {
                throw new ConcurrentModificationException(
                        "The WeakLinkedList was modified outside of this Iterator");
            }
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();
                return this.mNextNode != null;
            }
        }

        /**
         * @see java.util.ListIterator#hasPrevious()
         */
        public boolean hasPrevious() {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();
                return this.mPrevNode != null;
            }
        }

        /**
         * @see java.util.Iterator#next()
         */
        public T next() {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                if (this.mNextNode == null) {
                    throw new NoSuchElementException("No elements remain to iterate through");
                }

                // Move the node refs up one
                this.mPrevNode = this.mNextNode;
                this.mNextNode = this.mNextNode.getNext();

                // Update the list index
                this.mIndex++;

                // Mark the iterator as clean so add/remove/set operations will
                // work
                this.mLastDirection = 1;

                // Return the appropriate value
                return this.mPrevNode.get();
            }
        }

        /**
         * @see java.util.ListIterator#nextIndex()
         */
        public int nextIndex() {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();
                return this.mIndex;
            }
        }

        /**
         * @see java.util.ListIterator#previous()
         */
        public T previous() {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                if (this.mPrevNode == null) {
                    throw new NoSuchElementException(
                            "No elements previous element to iterate through");
                }

                // Move the node refs down one
                this.mNextNode = this.mPrevNode;
                this.mPrevNode = this.mPrevNode.getPrev();

                // Update the list index
                this.mIndex--;

                // Mark the iterator as clean so add/remove/set operations will
                // work
                this.mLastDirection = -1;

                // Return the appropriate value
                return this.mNextNode.get();
            }
        }

        /**
         * @see java.util.ListIterator#previousIndex()
         */
        public int previousIndex() {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();
                return this.mIndex - 1;
            }
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                if (this.mLastDirection == 0) {
                    throw new IllegalStateException("next or previous must be called first");
                }

                if (this.mLastDirection == 1) {
                    if (this.mPrevNode == null) {
                        throw new IllegalStateException("No element to remove");
                    }

                    // Use the remove node method from the List to ensure clean
                    // up
                    WeakLinkedList.this.removeNode(this.mPrevNode);

                    // Update the prevNode reference
                    this.mPrevNode = this.mPrevNode.getPrev();

                    // Update position
                    this.mIndex--;
                } else if (this.mLastDirection == -1) {
                    if (this.mNextNode == null) {
                        throw new IllegalStateException("No element to remove");
                    }

                    // Use the remove node method from the List to ensure clean
                    // up
                    WeakLinkedList.this.removeNode(this.mNextNode);

                    // Update the nextNode reference
                    this.mNextNode = this.mNextNode.getNext();
                }

                // Update the counters
                this.mExpectedModCount++;
                WeakLinkedList.this.mModCount++;
                this.mLastDirection = 0;
            }
        }

        /**
         * @see java.util.ListIterator#set(java.lang.Object)
         */
        public void set(T o) {
            synchronized (mLOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                if (this.mPrevNode == null) {
                    throw new IllegalStateException("No element to set");
                }
                if (this.mLastDirection == 0) {
                    throw new IllegalStateException("next or previous must be called first");
                }

                final WeakListNode deadNode = this.mPrevNode;
                final WeakListNode newNode = new WeakListNode(o);

                // If the replaced node was the head of the list
                if (deadNode == WeakLinkedList.this.mHead) {
                    WeakLinkedList.this.mHead = newNode;
                }
                // Otherwise replace refs with node before the one being set
                else {
                    newNode.setPrev(deadNode.getPrev());
                    newNode.getPrev().setNext(newNode);
                }

                // If the replaced node was the tail of the list
                if (deadNode == WeakLinkedList.this.mTail) {
                    WeakLinkedList.this.mTail = newNode;
                }
                // Otherwise replace refs with node after the one being set
                else {
                    newNode.setNext(deadNode.getNext());
                    newNode.getNext().setPrev(newNode);
                }

                // Update the ListIterator reference
                this.mPrevNode = newNode;

                // Clean up the dead node(WeakLinkedList.this.removeNode is not
                // used as it does not work with inserting nodes)
                deadNode.setRemoved();

                // Update counters
                this.mExpectedModCount++;
                WeakLinkedList.this.mModCount++;
                this.mLastDirection = 0;
            }
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            final StringBuffer buff = new StringBuffer();

            buff.append("[index='").append(this.mIndex).append("'");

            buff.append(", prev=");
            if (this.mPrevNode == null) {
                buff.append("null");
            } else {
                buff.append("'").append(this.mPrevNode).append("'");
            }

            buff.append(", next=");
            if (this.mNextNode == null) {
                buff.append("null");
            } else {
                buff.append("'").append(this.mNextNode).append("'");
            }

            buff.append("]");

            return buff.toString();
        }

        /**
         * Inspects the previous and next nodes to see if either have been
         * removed from the list because of a removed reference
         */
        private void updateRefs() {
            synchronized (mLOCK) {
                WeakLinkedList.this.cleanPhantomReferences();

                // Update nextNode refs
                while (this.mNextNode != null
                        && (this.mNextNode.isRemoved() || this.mNextNode.isEnqueued())) {
                    this.mNextNode = this.mNextNode.getNext();
                }

                // Update prevNode refs
                while (this.mPrevNode != null
                        && (this.mPrevNode.isRemoved() || this.mPrevNode.isEnqueued())) {
                    this.mPrevNode = this.mPrevNode.getPrev();
                }

                // Update index
                this.mIndex = 0;
                WeakListNode currNode = this.mPrevNode;
                while (currNode != null) {
                    currNode = currNode.getPrev();
                    this.mIndex++;
                }

                // Ensure the iterator is still valid
                if (this.mNextNode != null && this.mNextNode.getPrev() != this.mPrevNode) {
                    throw new IllegalStateException("nextNode.prev != prevNode");
                }
                if (this.mPrevNode != null && this.mPrevNode.getNext() != this.mNextNode) {
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

        public WeakListNode(T value) {
            super(value, WeakLinkedList.this.mQueue);
        }

        /**
         * @return Returns the next.
         */
        public WeakListNode getNext() {
            return this.mNext;
        }

        /**
         * @return Returns the prev.
         */
        public WeakListNode getPrev() {
            return this.mPrev;
        }

        /**
         * @return true if this node has been removed from a list.
         */
        public boolean isRemoved() {
            return this.mRemoved;
        }

        /**
         * @param next The next to set.
         */
        public void setNext(WeakListNode next) {
            this.mNext = next;
        }

        /**
         * @param prev The prev to set.
         */
        public void setPrev(WeakListNode prev) {
            this.mPrev = prev;
        }

        /**
         * Marks this node as being removed from a list.
         */
        public void setRemoved() {
            this.mRemoved = true;
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            final StringBuffer buff = new StringBuffer();

            buff.append("[prev=");

            if (this.mPrev == null) {
                buff.append("null");
            } else {
                buff.append("'").append(this.mPrev.get()).append("'");
            }

            buff.append(", value='");
            buff.append(this.get());
            buff.append("', next=");

            if (this.mNext == null) {
                buff.append("null");
            } else {
                buff.append("'").append(this.mNext.get()).append("'");
            }

            buff.append("]");

            return buff.toString();
        }
    }
}
