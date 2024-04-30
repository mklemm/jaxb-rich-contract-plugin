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

import java.util.List;

/**
 * Represents the instance of a {@link CollectionPropertyInfo}, i.e. represents the
 * property meta information along with its value, and enables to get and set the value
 */
public class CollectionProperty<I,P> extends Property<I,P> {
	public CollectionProperty(final CollectionPropertyInfo<I, P> info, final I owner) {
		super(info, owner);
	}

	@Override
	public CollectionPropertyInfo<I, P> getInfo() {
		return (CollectionPropertyInfo<I, P>)super.getInfo();
	}

	@Override
	public List<P> get() {
		return getInfo().get(getOwner());
	}

	public void set(final List<P> values) {
		getInfo().set(getOwner(), values);
	}
}
