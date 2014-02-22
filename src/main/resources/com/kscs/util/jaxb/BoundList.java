package com.kscs.util.jaxb;

import java.util.List;

/**
 * Created by klemm0 on 21/02/14.
 */
public interface BoundList<E> extends List<E> {
	void addCollectionChangeListener(final CollectionChangeListener<E> collectionChangeListener);
	void addVetoableCollectionChangeListener(final VetoableCollectionChangeListener<E> vetoableCollectionChangeListener);
}
