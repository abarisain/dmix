/*
 * Copyright (c) 2011, the Last.fm Java Project and Committers
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.umass.lastfm;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>ItemFactoryBuilder</code> can be used to obtain {@link ItemFactory ItemFactories} for a specific type.
 *
 * @author Janni Kovacs
 * @see ItemFactory
 */
final class ItemFactoryBuilder {

	private static final ItemFactoryBuilder INSTANCE = new ItemFactoryBuilder();
	@SuppressWarnings("rawtypes")
	private Map<Class, ItemFactory> factories = new HashMap<Class, ItemFactory>();

	private ItemFactoryBuilder() {
		// register default factories
		addItemFactory(Album.class, Album.FACTORY);
		addItemFactory(Track.class, Track.FACTORY);
		addItemFactory(Artist.class, Artist.FACTORY);
		addItemFactory(Tag.class, Tag.FACTORY);
		addItemFactory(Image.class, Image.FACTORY);
		addItemFactory(User.class, User.FACTORY);
		addItemFactory(Event.class, Event.FACTORY);
		addItemFactory(Venue.class, Venue.FACTORY);
		addItemFactory(Shout.class, Shout.FACTORY);
	}

	/**
	 * Retrieve the instance of the <code>ItemFactoryBuilder</code>.
	 *
	 * @return the instance
	 */
	public static ItemFactoryBuilder getFactoryBuilder() {
		return INSTANCE;
	}

	public <T> void addItemFactory(Class<T> itemClass, ItemFactory<T> factory) {
		factories.put(itemClass, factory);
	}

	/**
	 * Retrieves an {@link ItemFactory} for the given type, or <code>null</code> if no such factory was registered.
	 *
	 * @param itemClass the type's Class object
	 * @return the <code>ItemFactory</code> or <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public <T> ItemFactory<T> getItemFactory(Class<T> itemClass) {
		return factories.get(itemClass);
	}
}
