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
 * Interface to be implemented by a property visitor
 */
public interface PropertyVisitor {
	/**
	 * Called upon visiting a top-level object
	 * @param value The Object to be visited
	 */
	void visit(final Object value);

	/**
	 * Called upon visitng a collection element
	 * @param property The property being visited
	 * @return true if visiting shall continue, false if visiting should be finished
	 */
	boolean visit(final ItemProperty<?, ?> property);

	/**
	 * Called upon visitng a single-value property
	 * @param property The property being visited
	 * @return true if visiting shall continue, false if visiting should be finished
	 */
	boolean visit(final SingleProperty<? ,?> property);

	/**
	 * Called upon visiting a collection property
	 * @param property The property being visited
	 * @return true if visiting shall continue, false if visiting should be finished
	 */
	boolean visit(final CollectionProperty<?, ?> property);


	/**
	 * Called upon visiting a collection property, where the collection items are wrapped in a
	 * {@link jakarta.xml.bind.JAXBElement} instance.
	 * @param property The property being visited
	 * @return true if visiting shall continue, false if visiting should be finished
	 */
	boolean visit(final IndirectCollectionProperty<? ,?> property);

	/**
	 * Called upon visiting a collection property, where the collection items are wrapped in a
	 * {@link jakarta.xml.bind.JAXBElement} instance and are of a java primitive type
	 * @param property The property being visited
	 * @return true if visiting shall continue, false if visiting should be finished
	 */
	boolean visit(final IndirectPrimitiveCollectionProperty<? ,?> property);
}
