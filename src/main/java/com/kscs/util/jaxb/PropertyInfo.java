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
 * @author mirko 2014-05-28
 */
public class PropertyInfo<TInstance, TProperty> {
	public final String propertyName;
	public final Class<TProperty> declaredType;
	public final Class<TInstance> declaringClass;
	public final boolean collection;
	public final TProperty defaultValue;
	public final QName schemaType;
	public final boolean attribute;
	public final QName schemaName;

	public PropertyInfo(final String propertyName, final Class<TInstance> declaringClass, final Class<TProperty> declaredType, final boolean collection, final TProperty defaultValue, final QName schemaName, final QName schemaType, final boolean attribute) {
		this.propertyName = propertyName;
		this.declaredType = declaredType;
		this.declaringClass = declaringClass;
		this.collection = collection;
		this.defaultValue = defaultValue;
		this.schemaType = schemaType;
		this.attribute = attribute;
		this.schemaName = schemaName;
	}

}
