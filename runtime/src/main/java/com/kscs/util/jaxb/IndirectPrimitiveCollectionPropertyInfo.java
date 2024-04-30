package com.kscs.util.jaxb;

import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;

/**
 * Represents a multi-value property of a JAXB-generated java class where the individual values are wrapped in
 * a {@link JAXBElement} instance, and the individual values are of a java primitive type.
 */
public abstract class IndirectPrimitiveCollectionPropertyInfo<I,P> extends PropertyInfo<I,P> {
	protected IndirectPrimitiveCollectionPropertyInfo(final String propertyName, final Class<I> declaringClass, final Class<P> declaredType, final boolean collection, final P defaultValue, final QName schemaName, final QName schemaType, final boolean attribute) {
			super(propertyName, declaringClass, declaredType, collection, defaultValue, schemaName, schemaType, attribute);
		}

		@Override
		public abstract List<JAXBElement<P>> get(final I i) ;

		public abstract void set(final I instance, final List<JAXBElement<P>> values);
}
