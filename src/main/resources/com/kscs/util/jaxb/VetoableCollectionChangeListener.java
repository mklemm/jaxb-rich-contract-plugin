package com.kscs.util.jaxb;

import java.beans.PropertyVetoException;

/**
 * Created by klemm0 on 21/02/14.
 */
public interface VetoableCollectionChangeListener<E> {
	void vetoableCollectionChange(final CollectionChangeEvent<E> event) throws PropertyVetoException;
}
