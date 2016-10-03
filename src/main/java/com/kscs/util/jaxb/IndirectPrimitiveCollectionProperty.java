package com.kscs.util.jaxb;

import java.util.List;

import javax.xml.bind.JAXBElement;

/**
 * @author Mirko Klemm 2016-10-03
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
