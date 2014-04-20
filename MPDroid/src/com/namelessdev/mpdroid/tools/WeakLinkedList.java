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
    /**
     * Iterator implementation that can deal with weak nodes.
     */
    private class DurableListIterator implements ListIterator<T> {
        private WeakListNode nextNode;
        private WeakListNode prevNode;
        private long expectedModCount;
        private int index;
        private byte lastDirection;

        public DurableListIterator(final int initialIndex) {
            synchronized (LOCK) {
                this.expectedModCount = WeakLinkedList.this.modcount;
                this.lastDirection = 0;

                // Make worst case for initialization O(N/2)
                if (initialIndex <= (size / 2)) {
                    this.prevNode = null;
                    this.nextNode = WeakLinkedList.this.head;
                    this.index = 0;

                    // go head -> tail to find the initial index
                    while (this.nextIndex() < initialIndex) {
                        this.next();
                    }
                }
                else {
                    this.prevNode = WeakLinkedList.this.tail;
                    this.nextNode = null;
                    this.index = WeakLinkedList.this.size;

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
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                final WeakListNode newNode = new WeakListNode(o);

                // Add first node
                if (WeakLinkedList.this.size == 0) {
                    WeakLinkedList.this.head = newNode;
                    WeakLinkedList.this.tail = newNode;
                }
                // Add to head
                else if (this.index == 0) {
                    newNode.setNext(WeakLinkedList.this.head);
                    WeakLinkedList.this.head.setPrev(newNode);
                    WeakLinkedList.this.head = newNode;
                }
                // Add to tail
                else if (this.index == WeakLinkedList.this.size) {
                    newNode.setPrev(WeakLinkedList.this.tail);
                    WeakLinkedList.this.tail.setNext(newNode);
                    WeakLinkedList.this.tail = newNode;
                }
                // Add otherwise
                else {
                    newNode.setPrev(this.prevNode);
                    newNode.setNext(this.nextNode);
                    newNode.getPrev().setNext(newNode);
                    newNode.getNext().setPrev(newNode);
                }

                // The new node is always set as the previous node
                this.prevNode = newNode;

                // Update all the counters
                WeakLinkedList.this.size++;
                WeakLinkedList.this.modcount++;
                this.index++;
                this.expectedModCount++;
                this.lastDirection = 0;
            }
        }

        /**
         * Checks to see if the list has been modified by means other than this
         * Iterator
         */
        private void checkConcurrentModification() {
            if (this.expectedModCount != WeakLinkedList.this.modcount)
                throw new ConcurrentModificationException(
                        "The WeakLinkedList was modified outside of this Iterator");
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();
                return this.nextNode != null;
            }
        }

        /**
         * @see java.util.ListIterator#hasPrevious()
         */
        public boolean hasPrevious() {
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();
                return this.prevNode != null;
            }
        }

        /**
         * @see java.util.Iterator#next()
         */
        public T next() {
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                if (this.nextNode == null)
                    throw new NoSuchElementException("No elements remain to iterate through");

                // Move the node refs up one
                this.prevNode = this.nextNode;
                this.nextNode = this.nextNode.getNext();

                // Update the list index
                this.index++;

                // Mark the iterator as clean so add/remove/set operations will
                // work
                this.lastDirection = 1;

                // Return the appropriate value
                return this.prevNode.get();
            }
        }

        /**
         * @see java.util.ListIterator#nextIndex()
         */
        public int nextIndex() {
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();
                return this.index;
            }
        }

        /**
         * @see java.util.ListIterator#previous()
         */
        public T previous() {
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                if (this.prevNode == null)
                    throw new NoSuchElementException(
                            "No elements previous element to iterate through");

                // Move the node refs down one
                this.nextNode = this.prevNode;
                this.prevNode = this.prevNode.getPrev();

                // Update the list index
                this.index--;

                // Mark the iterator as clean so add/remove/set operations will
                // work
                this.lastDirection = -1;

                // Return the appropriate value
                return this.nextNode.get();
            }
        }

        /**
         * @see java.util.ListIterator#previousIndex()
         */
        public int previousIndex() {
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();
                return this.index - 1;
            }
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                if (this.lastDirection == 0)
                    throw new IllegalStateException("next or previous must be called first");

                if (this.lastDirection == 1) {
                    if (this.prevNode == null)
                        throw new IllegalStateException("No element to remove");

                    // Use the remove node method from the List to ensure clean
                    // up
                    WeakLinkedList.this.removeNode(this.prevNode);

                    // Update the prevNode reference
                    this.prevNode = this.prevNode.getPrev();

                    // Update position
                    this.index--;
                }
                else if (this.lastDirection == -1) {
                    if (this.nextNode == null)
                        throw new IllegalStateException("No element to remove");

                    // Use the remove node method from the List to ensure clean
                    // up
                    WeakLinkedList.this.removeNode(this.nextNode);

                    // Update the nextNode reference
                    this.nextNode = this.nextNode.getNext();
                }

                // Update the counters
                this.expectedModCount++;
                WeakLinkedList.this.modcount++;
                this.lastDirection = 0;
            }
        }

        /**
         * @see java.util.ListIterator#set(java.lang.Object)
         */
        public void set(T o) {
            synchronized (LOCK) {
                this.checkConcurrentModification();
                this.updateRefs();

                if (this.prevNode == null)
                    throw new IllegalStateException("No element to set");
                if (this.lastDirection == 0)
                    throw new IllegalStateException("next or previous must be called first");

                final WeakListNode deadNode = this.prevNode;
                final WeakListNode newNode = new WeakListNode(o);

                // If the replaced node was the head of the list
                if (deadNode == WeakLinkedList.this.head) {
                    WeakLinkedList.this.head = newNode;
                }
                // Otherwise replace refs with node before the one being set
                else {
                    newNode.setPrev(deadNode.getPrev());
                    newNode.getPrev().setNext(newNode);
                }

                // If the replaced node was the tail of the list
                if (deadNode == WeakLinkedList.this.tail) {
                    WeakLinkedList.this.tail = newNode;
                }
                // Otherwise replace refs with node after the one being set
                else {
                    newNode.setNext(deadNode.getNext());
                    newNode.getNext().setPrev(newNode);
                }

                // Update the ListIterator reference
                this.prevNode = newNode;

                // Clean up the dead node(WeakLinkedList.this.removeNode is not
                // used as it does not work with inserting nodes)
                deadNode.setRemoved();

                // Update counters
                this.expectedModCount++;
                WeakLinkedList.this.modcount++;
                this.lastDirection = 0;
            }
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            final StringBuffer buff = new StringBuffer();

            buff.append("[index='").append(this.index).append("'");

            buff.append(", prev=");
            if (this.prevNode == null) {
                buff.append("null");
            }
            else {
                buff.append("'").append(this.prevNode).append("'");
            }

            buff.append(", next=");
            if (this.nextNode == null) {
                buff.append("null");
            }
            else {
                buff.append("'").append(this.nextNode).append("'");
            }

            buff.append("]");

            return buff.toString();
        }

        /**
         * Inspects the previous and next nodes to see if either have been
         * removed from the list because of a removed reference
         */
        private void updateRefs() {
            synchronized (LOCK) {
                WeakLinkedList.this.cleanPhantomReferences();

                // Update nextNode refs
                while (this.nextNode != null
                        && (this.nextNode.isRemoved() || this.nextNode.isEnqueued())) {
                    this.nextNode = this.nextNode.getNext();
                }

                // Update prevNode refs
                while (this.prevNode != null
                        && (this.prevNode.isRemoved() || this.prevNode.isEnqueued())) {
                    this.prevNode = this.prevNode.getPrev();
                }

                // Update index
                this.index = 0;
                WeakListNode currNode = this.prevNode;
                while (currNode != null) {
                    currNode = currNode.getPrev();
                    this.index++;
                }

                // Ensure the iterator is still valid
                if (this.nextNode != null && this.nextNode.getPrev() != this.prevNode)
                    throw new IllegalStateException("nextNode.prev != prevNode");
                if (this.prevNode != null && this.prevNode.getNext() != this.nextNode)
                    throw new IllegalStateException("prevNode.next != nextNode");
            }
        }
    }

    /**
     * Represents a node in the list
     */
    private class WeakListNode extends WeakReference<T> {
        private boolean removed = false;
        private WeakListNode prev;
        private WeakListNode next;

        public WeakListNode(T value) {
            super(value, WeakLinkedList.this.queue);
        }

        /**
         * @return Returns the next.
         */
        public WeakListNode getNext() {
            return this.next;
        }

        /**
         * @return Returns the prev.
         */
        public WeakListNode getPrev() {
            return this.prev;
        }

        /**
         * @return true if this node has been removed from a list.
         */
        public boolean isRemoved() {
            return this.removed;
        }

        /**
         * @param next The next to set.
         */
        public void setNext(WeakListNode next) {
            this.next = next;
        }

        /**
         * @param prev The prev to set.
         */
        public void setPrev(WeakListNode prev) {
            this.prev = prev;
        }

        /**
         * Marks this node as being removed from a list.
         */
        public void setRemoved() {
            this.removed = true;
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            final StringBuffer buff = new StringBuffer();

            buff.append("[prev=");

            if (this.prev == null) {
                buff.append("null");
            }
            else {
                buff.append("'").append(this.prev.get()).append("'");
            }

            buff.append(", value='");
            buff.append(this.get());
            buff.append("', next=");

            if (this.next == null) {
                buff.append("null");
            }
            else {
                buff.append("'").append(this.next.get()).append("'");
            }

            buff.append("]");

            return buff.toString();
        }
    }

    private final Object LOCK = new Object();
    private final ReferenceQueue<T> queue = new ReferenceQueue<T>();
    private int size = 0;
    private long modcount = 0;
    private WeakListNode head = null;

    private WeakListNode tail = null;

    private String listName = null;

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
        synchronized (LOCK) {
            final ListIterator<T> itr = this.listIterator(index);
            itr.add(element);
        }
    }

    /**
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(T o) {
        synchronized (LOCK) {
            this.cleanPhantomReferences();
            this.add(this.size, o);
            return true;
        }
    }

    /**
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends T> c) {
        synchronized (LOCK) {
            this.cleanPhantomReferences();
            return this.addAll(this.size, c);
        }
    }

    /**
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection<? extends T> c) {
        if (c.size() <= 0) {
            return false;
        }

        synchronized (LOCK) {
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
        synchronized (LOCK) {
            WeakListNode deadNode;
            while ((deadNode = (WeakListNode) queue.poll()) != null) {
                // Ensure the node hasn't already been removed
                if (!deadNode.isRemoved()) {
                    if (listName != null) {
                        Log.e("WeakLinkedList",
                                "Error : "
                                        + listName
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
        synchronized (LOCK) {
            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext();) {
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
        synchronized (LOCK) {
            boolean foundAll = true;

            for (final Iterator<?> elementItr = c.iterator(); elementItr.hasNext() && foundAll;) {
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
        }
        else if (!(obj instanceof List)) {
            return false;
        }
        else {
            final List<?> other = (List<?>) obj;

            if (this.size() != other.size()) {
                return false;
            }
            else {
                synchronized (LOCK) {
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
        synchronized (LOCK) {
            final ListIterator<T> itr = this.listIterator(index);
            try {
                return (itr.next());
            } catch (NoSuchElementException exc) {
                throw (new IndexOutOfBoundsException("Index: " + index));
            }
        }
    }

    public String getName() {
        return listName;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int hashCode = 1;

        synchronized (LOCK) {
            for (final Iterator<?> itr = this.iterator(); itr.hasNext();) {
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
        synchronized (LOCK) {
            int index = 0;
            for (final ListIterator<T> itr = this.listIterator(); itr.hasNext();) {
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
        synchronized (LOCK) {
            this.cleanPhantomReferences();
            return this.size == 0;
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
        synchronized (LOCK) {
            this.cleanPhantomReferences();

            int index = this.size - 1;
            for (final ListIterator<T> itr = this.listIterator(this.size); itr.hasPrevious();) {
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
        synchronized (LOCK) {
            this.cleanPhantomReferences();

            if (index < 0)
                throw new IndexOutOfBoundsException("index must be >= 0");
            else if (index > this.size)
                throw new IndexOutOfBoundsException("index must be <= size()");

            return new DurableListIterator(index);
        }
    }

    /**
     * @see java.util.List#remove(int)
     */
    public T remove(int index) {
        synchronized (LOCK) {
            this.cleanPhantomReferences();

            final ListIterator<T> itr = this.listIterator(index);
            final T value;
            try {
                value = itr.next();
            } catch (NoSuchElementException exc) {
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
        synchronized (LOCK) {
            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext();) {
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
        synchronized (LOCK) {
            boolean changed = false;

            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext();) {
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
        synchronized (LOCK) {
            if (deadNode.isRemoved())
                throw new IllegalArgumentException("node has already been removed");

            final WeakListNode deadPrev = deadNode.getPrev();
            final WeakListNode deadNext = deadNode.getNext();

            // Removing the only node in the list
            if (deadPrev == null && deadNext == null) {
                this.head = null;
                this.tail = null;
            }
            // Removing the first node in the list
            else if (deadPrev == null) {
                this.head = deadNext;
                deadNext.setPrev(null);
            }
            // Removing the last node in the list
            else if (deadNext == null) {
                this.tail = deadPrev;
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
            this.size--;

            // Ensure the list is still valid
            if (this.size < 0)
                throw new IllegalStateException("size is less than zero - '" + this.size + "'");
            if (this.size == 0 && this.head != null)
                throw new IllegalStateException("size is zero but head is not null");
            if (this.size == 0 && this.tail != null)
                throw new IllegalStateException("size is zero but tail is not null");
            if (this.size > 0 && this.head == null)
                throw new IllegalStateException("size is greater than zero but head is null");
            if (this.size > 0 && this.tail == null)
                throw new IllegalStateException("size is greater than zero but tail is null");
        }
    }

    /**
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> c) {
        synchronized (LOCK) {
            boolean changed = false;

            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext();) {
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
        synchronized (LOCK) {
            final ListIterator<T> itr = this.listIterator(index);
            try {
                final T oldVal = itr.next();
                itr.set(element);
                return oldVal;
            } catch (NoSuchElementException exc) {
                throw (new IndexOutOfBoundsException("Index: " + index));
            }
        }
    }

    public void setName(String name) {
        listName = name;
    }

    /**
     * @see java.util.List#size()
     */
    public int size() {
        synchronized (LOCK) {
            this.cleanPhantomReferences();
            return this.size;
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
        synchronized (LOCK) {
            this.cleanPhantomReferences();
            return this.toArray(new Object[this.size]);
        }
    }

    /**
     * @see java.util.List#toArray(java.lang.Object[])
     */
    @SuppressWarnings("unchecked")
    public Object[] toArray(Object[] a) {
        synchronized (LOCK) {
            this.cleanPhantomReferences();

            if (a.length < this.size) {
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), this.size);
            }

            int index = 0;
            for (final ListIterator<?> itr = this.listIterator(); itr.hasNext();) {
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
        synchronized (LOCK) {
            for (final Iterator<?> itr = this.iterator(); itr.hasNext();) {
                buff.append(itr.next());

                if (itr.hasNext()) {
                    buff.append(", ");
                }
            }
        }
        buff.append("]");

        return buff.toString();
    }
}
