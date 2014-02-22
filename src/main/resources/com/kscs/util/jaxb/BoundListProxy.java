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

	public BoundListProxy(List<E> list) {
		this.list = list;
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	public boolean add(E e) {
		final CollectionChangeEvent<E> event = checkCollectionChange("add", CollectionChangeEventType.ADD, new ArrayList<E>(list), Arrays.asList(e), list.size());
		boolean retVal = list.add(e);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public boolean remove(Object o) {
		final CollectionChangeEvent<E> event = checkCollectionChange("remove", CollectionChangeEventType.REMOVE, new ArrayList<E>(list), Arrays.asList((E)o), -1);
		boolean retVal = list.remove(o);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public boolean addAll(Collection<? extends E> c) {
		final CollectionChangeEvent<E> event = checkCollectionChange("addAll", CollectionChangeEventType.ADD_ALL, new ArrayList<E>(list), c, list.size());
		final boolean retVal = list.addAll(c);
		fireCollectionChange(event);
		return retVal;
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		final CollectionChangeEvent<E> event = checkCollectionChange("addAll", CollectionChangeEventType.ADD_ALL_AT, new ArrayList<E>(list), c, list.size());
		final boolean retVal = list.addAll(index, c);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		final CollectionChangeEvent<E> event = checkCollectionChange("removeAll", CollectionChangeEventType.REMOVE_ALL, new ArrayList<E>(list), (Collection<E>)c, -1);
		final boolean retVal = list.removeAll(c);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		final CollectionChangeEvent<E> event = checkCollectionChange("retainAll", CollectionChangeEventType.RETAIN_ALL, new ArrayList<E>(list), (Collection<E>)c, -1);
		final boolean retVal =  list.retainAll(c);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public void clear() {
		final CollectionChangeEvent<E> event = checkCollectionChange("clear", CollectionChangeEventType.RETAIN_ALL, new ArrayList<E>(list), Collections.<E>emptyList(), -1);
		list.clear();
		fireCollectionChange(event);
	}

	@Override
	public boolean equals(Object o) {
		return list.equals(o);
	}

	@Override
	public int hashCode() {
		return list.hashCode();
	}

	@Override
	public E get(int index) {
		return list.get(index);
	}

	public E set(int index, E element) {
		final CollectionChangeEvent<E> event = checkCollectionChange("set", CollectionChangeEventType.SET_AT, new ArrayList<E>(list), Arrays.asList(element), index);
		final E retVal = list.set(index, element);
		fireCollectionChange(event);
		return retVal;
	}

	public void add(int index, E element) {
		final CollectionChangeEvent<E> event = checkCollectionChange("add", CollectionChangeEventType.ADD_AT, new ArrayList<E>(list), Arrays.asList(element), index);
		list.add(index, element);
		fireCollectionChange(event);
	}

	@Override
	public E remove(int index) {
		final CollectionChangeEvent<E> event = checkCollectionChange("remove", CollectionChangeEventType.REMOVE_AT, new ArrayList<E>(list), Collections.<E>emptyList(), index);
		final E retVal = list.remove(index);
		fireCollectionChange(event);
		return retVal;
	}

	@Override
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return list.listIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
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
		} catch(PropertyVetoException pvx) {
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
