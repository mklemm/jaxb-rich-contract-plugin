/*
 * MIT License
 *
 * Copyright (c) 2014 Klemm Software Consulting, Mirko Klemm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.kscs.util.jaxb;

import javax.xml.namespace.QName;

/**
 * Represents a property of a JAXB-generated class.
 * @param <I> The type declaring the property
 * @param <P> The type of the property
 */
public abstract class PropertyInfo<I, P> {
	public final String propertyName;
	public final Class<P> declaredType;
	public final Class<I> declaringClass;
	public final boolean collection;
	public final P defaultValue;
	public final QName schemaType;
	public final boolean attribute;
	public final QName schemaName;

	protected PropertyInfo(final String propertyName, final Class<I> declaringClass, final Class<P> declaredType, final boolean collection, final P defaultValue, final QName schemaName, final QName schemaType, final boolean attribute) {
		this.propertyName = propertyName;
		this.declaredType = declaredType;
		this.declaringClass = declaringClass;
		this.collection = collection;
		this.defaultValue = defaultValue;
		this.schemaType = schemaType;
		this.attribute = attribute;
		this.schemaName = schemaName;
	}

	public abstract Object get(final I instance);

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof PropertyInfo)) return false;
		final PropertyInfo<?, ?> that = (PropertyInfo<?, ?>)o;
		if (!this.propertyName.equals(that.propertyName)) return false;
		return declaringClass.equals(that.declaringClass);
	}

	@Override
	public int hashCode() {
		int result = this.propertyName.hashCode();
		result = 31 * result + this.declaringClass.hashCode();
		return result;
	}
}
