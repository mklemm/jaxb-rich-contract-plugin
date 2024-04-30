package com.kscs.util.jaxb;

import java.util.List;

import jakarta.xml.bind.JAXBElement;

/**
 * Represents the instance of a {@link IndirectPrimitiveCollectionPropertyInfo}, i.e. represents the
 * property meta information along with its value, and enables to get an d set the value
 */
public class IndirectPrimitiveCollectionProperty<I,P> extends Property<I,P> {
	public IndirectPrimitiveCollectionProperty(final PropertyInfo<I, P> info, final I owner) {
			super(info, owner);
		}

		@Override
		public IndirectPrimitiveCollectionPropertyInfo<I, P> getInfo() {
			return (IndirectPrimitiveCollectionPropertyInfo<I, P>)super.getInfo();
		}

		@Override
		public List<JAXBElement<P>> get() {
			return getInfo().get(getOwner());
		}

		public void set(final List<JAXBElement<P>> values) {
			getInfo().set(getOwner(), values);
		}
}
