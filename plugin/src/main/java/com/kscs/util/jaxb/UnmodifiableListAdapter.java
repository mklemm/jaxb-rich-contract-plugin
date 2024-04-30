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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mirko Klemm 2015-03-15
 */
public class UnmodifiableListAdapter<E> implements UnmodifiableList<E> {
	private final List<E> delegateList;

	public UnmodifiableListAdapter(final List<E> delegateList) {
		this.delegateList = Collections.unmodifiableList(delegateList);
	}

	@Override
	public E get(final int index) {
		return this.delegateList.get(index);
	}

	@Override
	public int indexOf(final Object o) {
		return this.delegateList.indexOf(o);
	}

	@Override
	public int lastIndexOf(final Object o) {
		return this.delegateList.lastIndexOf(o);
	}

	@Override
	public UnmodifiableList<E> subList(final int fromIndex, final int toIndex) {
		return new UnmodifiableListAdapter<>(this.delegateList.subList(fromIndex, toIndex));
	}

	@Override
	public int size() {
		return this.delegateList.size();
	}

	@Override
	public boolean isEmpty() {
		return this.delegateList.isEmpty();
	}

	@Override
	public boolean contains(final Object o) {
		return this.delegateList.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return this.delegateList.iterator();
	}

	@Override
	public Object[] toArray() {
		return this.delegateList.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return this.delegateList.toArray(a);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return this.delegateList.containsAll(c);
	}

	public List<E> toList() {
		return this.delegateList;
	}
}
