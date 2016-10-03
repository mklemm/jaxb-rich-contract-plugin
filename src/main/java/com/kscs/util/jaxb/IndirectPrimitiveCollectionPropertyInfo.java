package com.kscs.util.jaxb;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * @author Mirko Klemm 2016-10-03
 */
public abstract class IndirectPrimitiveCollectionPropertyInfo<I,P> extends PropertyInfo<I,P> {
	protected IndirectPrimitiveCollectionPropertyInfo(final String propertyName, final Class<I> declaringClass, final Class<P> declaredType, final boolean collection, final P defaultValue, final QName schemaName, final QName schemaType, final boolean attribute) {
			super(propertyName, declaringClass, declaredType, collection, defaultValue, schemaName, schemaType, attribute);
		}

		@Override
		public abstract List<JAXBElement<P>> get(final I i) ;

		public abstract void set(final I instance, final List<JAXBElement<P>> values);
}
