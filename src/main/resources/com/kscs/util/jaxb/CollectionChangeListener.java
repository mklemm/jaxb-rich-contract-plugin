package com.kscs.util.jaxb;

/**
 * Created by klemm0 on 21/02/14.
 */
public interface CollectionChangeListener<E> {
	void collectionChange(final CollectionChangeEvent<E> event);
}
