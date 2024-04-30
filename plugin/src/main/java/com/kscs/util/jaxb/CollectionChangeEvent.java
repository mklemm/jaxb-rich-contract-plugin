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

import java.util.Collection;

/**
 * @author klemm0 2014-03-27
 *
 */
public class CollectionChangeEvent<E> {
	private final Collection<E> source;
	private final String methodName;
	private final CollectionChangeEventType eventType;
	private final Collection<E> oldItems;
	private final Collection<? extends E> newItems;
	private final int index;

	public CollectionChangeEvent(final Collection<E> source, final String methodName, final CollectionChangeEventType eventType, final Collection<E> oldItems, final Collection<? extends E> newItems, final int index) {
		this.source = source;
		this.methodName = methodName;
		this.eventType = eventType;
		this.oldItems = oldItems;
		this.newItems = newItems;
		this.index = index;
	}

	public Collection<E> getSource() {
		return this.source;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public CollectionChangeEventType getEventType() {
		return this.eventType;
	}

	public Collection<E> getOldItems() {
		return this.oldItems;
	}

	public Collection<? extends E> getNewItems() {
		return this.newItems;
	}

	public int getIndex() {
		return this.index;
	}
}
