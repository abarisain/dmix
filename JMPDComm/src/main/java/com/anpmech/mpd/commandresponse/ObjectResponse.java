/*
 * Copyright (C) 2004 Felipe Gustavo de Almeida
 * Copyright (C) 2010-2016 The MPDroid Project
 *
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice,this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.anpmech.mpd.commandresponse;

import com.anpmech.mpd.ResponseObject;
import com.anpmech.mpd.connection.CommandResult;
import com.anpmech.mpd.item.Directory;
import com.anpmech.mpd.item.Music;
import com.anpmech.mpd.item.PlaylistFile;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * This class is used to create an Object from a {@link CommandResult}.
 *
 * <p>This class is immutable, thus, thread-safe. All optional operations, as defined by the
 * {@link Collection} interface will throw an {@link UnsupportedOperationException}.</p>
 *
 * @param <T> The type of object to create from this {@link CommandResult}.
 */
public abstract class ObjectResponse<T> implements Collection<T> {

    /**
     * This is a list of all tokens which begin a new block.
     */
    protected static final String[] ENTRY_BLOCK_TOKENS = {Directory.RESPONSE_DIRECTORY,
            Music.RESPONSE_FILE, PlaylistFile.RESPONSE_PLAYLIST_FILE};

    /**
     * This is the error message thrown by an exception if mutation is attempted by this class.
     */
    private static final String MUTATION_ERROR = "Illegal argument caused by mutation attempt";

    /**
     * This is the result to create this Object from.
     */
    protected final String mResult;

    /**
     * This constructor builds this class from an empty MPD protocol result.
     */
    protected ObjectResponse() {
        this("");
    }

    /**
     * This constructor builds this class from the MPD protocol result.
     *
     * @param response The MPD protocol command response.
     */
    protected ObjectResponse(final String response) {
        super();

        mResult = response;
    }

    /**
     * This class is used to subclass a CommandResult.
     *
     * @param result The result to subclass.
     */
    protected ObjectResponse(final CommandResult result) {
        this(result.getResult());
    }

    /**
     * This constructor is used to create a subclass objects from another compatible
     * {@code ObjectResponse}.
     *
     * @param response The ObjectResponse containing a MPD response.
     */
    protected ObjectResponse(final ObjectResponse<?> response) {
        this(response.mResult);
    }

    /**
     * This constructor is used to iterate over responses in a {@link ResponseObject}.
     *
     * @param response The ResponseObject to iterate over.
     */
    protected ObjectResponse(final ResponseObject response) {
        this(response.getResponse());
    }

    /**
     * This method is unsupported by this immutable object, and will throw an exception.
     *
     * @param object ignored
     */
    @Override
    public boolean add(final T object) {
        throw new UnsupportedOperationException(MUTATION_ERROR);
    }

    /**
     * This method is unsupported by this immutable object, and will throw an exception.
     *
     * @param collection ignored
     */
    @Override
    public boolean addAll(@NotNull final Collection<? extends T> collection) {
        throw new UnsupportedOperationException(MUTATION_ERROR);
    }

    /**
     * This method is unsupported by this immutable object, and will throw an exception.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException(MUTATION_ERROR);
    }

    /**
     * Tests whether this {@code Collection} contains the specified object. Returns {@code true}
     * if and only if at least one element {@code elem} in this {@code Collection} meets following
     * requirement: {@code (object==null ? elem==null : object.equals(elem))}.
     *
     * @param object the object to search for.
     * @return {@code true} if object is an element of this {@code Collection},
     * {@code false} otherwise.
     * @throws ClassCastException   if the object to look for isn't of the correct type.
     * @throws NullPointerException if the object to look for is {@code null} and this
     *                              {@code Collection} doesn't support {@code null} elements.
     */
    @Override
    public boolean contains(final Object object) {
        final Iterator<T> iterator = iterator();
        boolean contains = false;

        while (iterator.hasNext() && !contains) {
            // This must compare the object because it must be able to throw an NPE if it's null.
            if (object.equals(iterator.next())) {
                contains = true;
            }
        }

        return contains;
    }

    /**
     * Tests whether this {@code Collection} contains all objects contained in the specified
     * {@code Collection}. If an element {@code elem} is contained several times in the specified
     * {@code Collection}, the method returns {@code true} even if {@code elem} is contained only
     * once in this {@code Collection}.
     *
     * @param collection the collection of objects.
     * @return {@code true} if all objects in the specified {@code Collection} are elements of this
     * {@code Collection}, {@code false} otherwise.
     * @throws ClassCastException   if one or more elements of {@code collection} isn't of the
     *                              correct type.
     * @throws NullPointerException if {@code collection} contains at least one {@code null}
     *                              element and this {@code Collection} doesn't support {@code
     *                              null} elements.
     * @throws NullPointerException if {@code collection} is {@code null}.
     */
    @Override
    public boolean containsAll(@NotNull final Collection<?> collection) {
        final Iterator<?> iterator = collection.iterator();
        boolean contains = true;

        while (iterator.hasNext() && contains) {
            final Object element = iterator.next();

            if (!contains(element)) {
                contains = false;
            }
        }

        return contains;
    }

    /**
     * Compares this instance with the specified object and indicates if they are equal. In order
     * to be equal, {@code o} must represent the same object as this instance using a
     * class-specific comparison. The general contract is that this comparison should be reflexive,
     * symmetric, and transitive. Also, no object reference other than null is equal to null.
     *
     * <p>The default implementation returns {@code true} only if {@code this == o}. See
     * <a href="{@docRoot}reference/java/lang/Object.html#writing_equals">Writing a correct
     * {@code equals} method</a> if you intend implementing your own {@code equals} method.</p>
     *
     * <p>The general contract for the {@code equals} and {@link #hashCode()} methods is that if
     * {@code equals} returns {@code true} for any two objects, then {@code hashCode()} must
     * return the same value for these objects. This means that subclasses of {@code Object}
     * usually override either both methods or neither of them.</p>
     *
     * @param o the object to compare this instance with.
     * @return {@code true} if the specified object is equal to this {@code Object}; {@code false}
     * otherwise.
     * @see #hashCode
     */
    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;
        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            final ObjectResponse<?> entry = (ObjectResponse<?>) o;
            //noinspection ConstantConditions
            if (!entry.mResult.equals(mResult)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual.booleanValue();
    }

    /**
     * Returns the element at the specified location in this {@code List}.
     *
     * @param location the index of the element to return.
     * @return the element at the specified location.
     * @throws IndexOutOfBoundsException if {@code location < 0 || location >= size()}
     */
    public T get(final int location) {
        final Iterator<T> iterator = iterator();
        int i = 0;

        while (iterator.hasNext() && i != location) {
            iterator.next();
            i++;
        }

        if (!iterator.hasNext()) {
            throw new IndexOutOfBoundsException("Location " + location + " does not exist in " +
            "this collection.");
        }

        if (i != location) {
            throw new IndexOutOfBoundsException("Location: " + location + " is less than the size"
                    + " of this collection: " + i);
        }

        return iterator.next();
    }

    /**
     * Returns an integer hash code for this object. By contract, any two objects for which
     * {@link #equals} returns {@code true} must return the same hash code value. This means that
     * subclasses of {@code Object} usually override both methods or neither method.
     *
     * <p>Note that hash values must not change over time unless information used in equals
     * comparisons also changes.</p>
     *
     * <p>See <a href="{@docRoot}reference/java/lang/Object.html#writing_hashCode">Writing a
     * correct {@code hashCode} method</a> if you intend implementing your own {@code hashCode}
     * method.</p>
     *
     * @return this object's hash code.
     * @see #equals
     */
    public int hashCode() {
        return super.hashCode() + getClass().hashCode() + mResult.hashCode();
    }

    /**
     * Returns if this {@code Collection} contains no elements.
     *
     * @return {@code true} if this {@code Collection} has no elements, {@code false}
     * otherwise.
     * @see #size
     */
    @Override
    public boolean isEmpty() {
        /**
         * This method is, typically, a call to see if size() != 0, but, this implementation
         * doesn't require iterating over the entire response to return for this method.
         */
        return !iterator().hasNext();
    }

    /**
     * Returns an {@link Iterator} for the elements in this object.
     *
     * @return An {@code Iterator} instance.
     */
    @Override
    @NotNull
    public Iterator<T> iterator() {
        return listIterator();
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @return A iterator to return the response, line by line.
     */
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    /**
     * This method returns a iterator, starting at the beginning of the response.
     *
     * @param position The position to begin the iterator at, typically beginning or end.
     * @return A iterator to return the response.
     */
    protected abstract ListIterator<T> listIterator(final int position);

    /**
     * This method is unsupported by this immutable object, and will throw an exception.
     *
     * @param object ignored
     * @throws UnsupportedOperationException if removing from this {@code Collection} is not
     *                                       supported.
     */
    @Override
    public boolean remove(final Object object) {
        throw new UnsupportedOperationException(MUTATION_ERROR);
    }

    /**
     * This method is unsupported by this immutable object, and will throw an exception.
     *
     * @param collection ignored
     * @throws UnsupportedOperationException if removing from this {@code Collection} is not
     *                                       supported.
     */
    @Override
    public boolean removeAll(@NotNull final Collection<?> collection) {
        throw new UnsupportedOperationException(MUTATION_ERROR);
    }

    /**
     * This method is unsupported by this immutable object, and will throw an exception.
     *
     * @param collection ignored
     * @throws UnsupportedOperationException if removing from this {@code Collection} is not
     *                                       supported.
     */
    @Override
    public boolean retainAll(@NotNull final Collection<?> collection) {
        throw new UnsupportedOperationException(MUTATION_ERROR);
    }

    /**
     * This method returns a iterator, starting at the end of the response.
     *
     * @return A list iterator positioned to begin at the end of the response.
     */
    public ListIterator<T> reverseListIterator() {
        return listIterator(mResult.length());
    }

    /**
     * Returns a new array containing all elements contained in this {@code Collection}.
     *
     * If the implementation has ordered elements it will return the element
     * array in the same order as an iterator would return them.
     *
     * The array returned does not reflect any changes of the {@code Collection}. A new
     * array is created even if the underlying data structure is already an
     * array.
     *
     * @return an array of the elements from this {@code Collection}.
     */
    @Override
    @NotNull
    public Object[] toArray() {
        final Iterator<T> iterator = iterator();
        final int size = size();
        final Object[] result = new Object[size];

        for (int i = 0; i < size; ++i) {
            result[i] = iterator.next();
        }

        return result;
    }

    /**
     * Returns an array containing all elements contained in this {@code Collection}. If the
     * specified array is large enough to hold the elements, the specified array is used, otherwise
     * an array of the same type is created. If the specified array is used and is larger than this
     * {@code Collection}, the array element following the {@code Collection} elements is set to
     * null.
     *
     * If the implementation has ordered elements it will return the element array in the same
     * order as an iterator would return them.
     *
     * {@code toArray(new Object[0])} behaves exactly the same way as {@code toArray()} does.
     *
     * @param array the array.
     * @return an array of the elements from this {@code Collection}.
     * @throws ArrayStoreException if the type of an element in this {@code Collection} cannot be
     *                             stored in the type of the specified array.
     */
    @Override
    @NotNull
    public <S> S[] toArray(@NotNull final S[] array) {
        final Object[] thisArray = toArray();
        final S[] processArray;

        if (array.length < thisArray.length) {
            final Class<?> componentType = array.getClass().getComponentType();

            //noinspection unchecked
            processArray = (S[]) Array.newInstance(componentType, thisArray.length);
        } else {
            processArray = array;
        }

        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(thisArray, 0, processArray, 0, thisArray.length);
        if (array.length > thisArray.length) {
            array[thisArray.length] = null;
        }

        return array;
    }

    @Override
    public String toString() {
        return "ObjectResponse{" +
                "mResult='" + mResult + '\'' +
                '}';
    }
}
