package com.kscs.util.jaxb;

import java.util.Collection;

/**
 * Created by klemm0 on 21/02/14.
 */
public class CollectionChangeEvent<E> {
	private final Collection<E> source;
	private final String methodName;
	private final CollectionChangeEventType eventType;
	private final Collection<E> oldItems;
	private final Collection<? extends E> newItems;
	private final int index;

	public CollectionChangeEvent(Collection<E> source, String methodName, CollectionChangeEventType eventType, Collection<E> oldItems, Collection<? extends E> newItems, int index) {
		this.source = source;
		this.methodName = methodName;
		this.eventType = eventType;
		this.oldItems = oldItems;
		this.newItems = newItems;
		this.index = index;
	}

	public Collection<E> getSource() {
		return source;
	}

	public String getMethodName() {
		return methodName;
	}

	public CollectionChangeEventType getEventType() {
		return eventType;
	}

	public Collection<E> getOldItems() {
		return oldItems;
	}

	public Collection<? extends E> getNewItems() {
		return newItems;
	}

	public int getIndex() {
		return index;
	}
}
