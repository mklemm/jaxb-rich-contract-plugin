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

/**
 * @author mirko 2014-05-28
 */
public class PropertyInfo<TInstance, TProperty> {
	public static final int UNBOUNDED = Integer.MAX_VALUE;
	private final String propertyName;
	private final Class<TProperty> declaredType;
	private final Class<TInstance> declaringClass;
	private final int minOccurs;
	private final int maxOccurs;
	private final boolean writable;

	public PropertyInfo(final String propertyName, final Class<TProperty> declaredType, final Class<TInstance> declaringClass, final int minOccurs, final int maxOccurs, final boolean writable) {
		this.propertyName = propertyName;
		this.declaredType = declaredType;
		this.declaringClass = declaringClass;
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
		this.writable = writable;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public Class<?> getDeclaredType() {
		return this.declaredType;
	}

	public Class<?> getDeclaringClass() {
		return this.declaringClass;
	}

	public int getMinOccurs() {
		return this.minOccurs;
	}

	public int getMaxOccurs() {
		return this.maxOccurs;
	}

	public boolean isWritable() {
		return this.writable;
	}

}
