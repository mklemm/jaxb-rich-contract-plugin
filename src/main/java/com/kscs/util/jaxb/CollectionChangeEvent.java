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

	public CollectionChangeEvent(final Collection<E> source, final String methodName, final CollectionChangeEventType eventType, final Collection<E> oldItems, final Collection<? extends E> newItems, final int index) {
		this.source = source;
		this.methodName = methodName;
		this.eventType = eventType;
		this.oldItems = oldItems;
		this.newItems = newItems;
		this.index = index;
	}

	public Collection<E> getSource() {
		return this.source;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public CollectionChangeEventType getEventType() {
		return this.eventType;
	}

	public Collection<E> getOldItems() {
		return this.oldItems;
	}

	public Collection<? extends E> getNewItems() {
		return this.newItems;
	}

	public int getIndex() {
		return this.index;
	}
}
