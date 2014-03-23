package com.kscs.util.jaxb;

import java.beans.PropertyVetoException;
import java.util.*;

/**
 * Created by klemm0 on 21/02/14.
 */
public class BoundListProxy<E> implements BoundList<E> {
	private final List<E> list;
	private final List<VetoableCollectionChangeListener<E>> vetoableCollectionChangeListeners = new ArrayList<VetoableCollectionChangeListener<E>>();
	private final List<CollectionChangeListener<E>> collectionChangeListeners = new ArrayList<CollectionChangeListener<E>>();

	public BoundListProxy(final List<E> list) {
		this.list = list;
	}

	@Override
	public int size() {
		return this.list.size();
	}

	@Override
	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	@Override
	public boolean contains(final Object o) {
		return this.list.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return this.list.iterator();
	}

	@Override
	public Object[] toArray() {
		return this.list.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return this.list.toArray(a);
	}

	public boolean add(final E e) {
		final CollectionChangeEvent<E> event = checkCollectionChange("add", CollectionChangeEventType.ADD, new ArrayList<E>(this.list), Arrays.asList(e), this.list.size());
		final boolean retVal = this.list.add(e);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public boolean remove(final Object o) {
		final CollectionChangeEvent<E> event = checkCollectionChange("remove", CollectionChangeEventType.REMOVE, new ArrayList<E>(this.list), Arrays.asList((E)o), -1);
		final boolean retVal = this.list.remove(o);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return this.list.containsAll(c);
	}

	public boolean addAll(final Collection<? extends E> c) {
		final CollectionChangeEvent<E> event = checkCollectionChange("addAll", CollectionChangeEventType.ADD_ALL, new ArrayList<E>(this.list), c, this.list.size());
		final boolean retVal = this.list.addAll(c);
		fireCollectionChange(event);
		return retVal;
	}

	public boolean addAll(final int index, final Collection<? extends E> c) {
		final CollectionChangeEvent<E> event = checkCollectionChange("addAll", CollectionChangeEventType.ADD_ALL_AT, new ArrayList<E>(this.list), c, this.list.size());
		final boolean retVal = this.list.addAll(index, c);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		final CollectionChangeEvent<E> event = checkCollectionChange("removeAll", CollectionChangeEventType.REMOVE_ALL, new ArrayList<E>(this.list), (Collection<E>)c, -1);
		final boolean retVal = this.list.removeAll(c);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		final CollectionChangeEvent<E> event = checkCollectionChange("retainAll", CollectionChangeEventType.RETAIN_ALL, new ArrayList<E>(this.list), (Collection<E>)c, -1);
		final boolean retVal = this.list.retainAll(c);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public void clear() {
		final CollectionChangeEvent<E> event = checkCollectionChange("clear", CollectionChangeEventType.RETAIN_ALL, new ArrayList<E>(this.list), Collections.<E>emptyList(), -1);
		this.list.clear();
		fireCollectionChange(event);
	}

	@Override
	public boolean equals(final Object o) {
		return this.list.equals(o);
	}

	@Override
	public int hashCode() {
		return this.list.hashCode();
	}

	@Override
	public E get(final int index) {
		return this.list.get(index);
	}

	public E set(final int index, final E element) {
		final CollectionChangeEvent<E> event = checkCollectionChange("set", CollectionChangeEventType.SET_AT, new ArrayList<E>(this.list), Arrays.asList(element), index);
		final E retVal = this.list.set(index, element);
		fireCollectionChange(event);
		return retVal;
	}

	public void add(final int index, final E element) {
		final CollectionChangeEvent<E> event = checkCollectionChange("add", CollectionChangeEventType.ADD_AT, new ArrayList<E>(this.list), Arrays.asList(element), index);
		this.list.add(index, element);
		fireCollectionChange(event);
	}

	@Override
	public E remove(final int index) {
		final CollectionChangeEvent<E> event = checkCollectionChange("remove", CollectionChangeEventType.REMOVE_AT, new ArrayList<E>(this.list), Collections.<E>emptyList(), index);
		final E retVal = this.list.remove(index);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public int indexOf(final Object o) {
		return this.list.indexOf(o);
	}

	@Override
	public int lastIndexOf(final Object o) {
		return this.list.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return this.list.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(final int index) {
		return this.list.listIterator(index);
	}

	@Override
	public List<E> subList(final int fromIndex, final int toIndex) {
		return this.list.subList(fromIndex, toIndex);
	}

	protected CollectionChangeEvent<E> checkCollectionChange(final String methodName, final CollectionChangeEventType eventType, final Collection<E> oldItems, final Collection<? extends E> newItems, final int index) {
		try {
			final CollectionChangeEvent<E> event = new CollectionChangeEvent<E>(
					this,
					methodName,
					eventType,
					oldItems,
					newItems,
					index
			);

			for(final VetoableCollectionChangeListener<E> listener : this.vetoableCollectionChangeListeners) {
				listener.vetoableCollectionChange(event);
			}
			return event;
		} catch(final PropertyVetoException pvx) {
			throw new RuntimeException(pvx);
		}
	}

	protected void fireCollectionChange(final CollectionChangeEvent<E> event) {
		for(final CollectionChangeListener<E> listener : this.collectionChangeListeners) {
			listener.collectionChange(event);
		}
	}

	@Override
	public void addCollectionChangeListener(final CollectionChangeListener<E> collectionChangeListener) {
		this.collectionChangeListeners.add(collectionChangeListener);
	}

	@Override
	public void addVetoableCollectionChangeListener(final VetoableCollectionChangeListener<E> vetoableCollectionChangeListener) {
		this.vetoableCollectionChangeListeners.add(vetoableCollectionChangeListener);
	}
}
