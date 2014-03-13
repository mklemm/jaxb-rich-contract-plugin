package com.kscs.util.jaxb;

import java.util.List;

/**
 * Created by jaxb2-rich-contract-plugin.
 * Defines the contract for a List implementaion
 * that supports change and vetoable change events.
 */
public interface BoundList<E> extends List<E> {
	void addCollectionChangeListener(final CollectionChangeListener<E> collectionChangeListener);
	void addVetoableCollectionChangeListener(final VetoableCollectionChangeListener<E> vetoableCollectionChangeListener);
}
