package com.kscs.util.jaxb;

/**
 * Created by klemm0 on 12/03/14.
 */
public interface PathCloneable<T extends PathCloneable<T>> {
	T clone(final PropertyPath path) throws CloneNotSupportedException;
}
