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
 * Contract for objects that can be copied partially,
 * i.e. by explicitly excluding or including specified
 * branches of the object tree.
 */
public interface PartialCopyable<T extends PartialCopyable<T>> {

	/**
	 * Clones this instances partially, the parts
	 * will be defined by <tt>propertyTree</tt>
	 *
	 * @param propertyTree Defines which parts of the object tree will be cloned or excluded
	 * @param propertyTreeUse Defines how the clone graph will be used: To include or to exclude properties.
	 * @return A copy of the original object.
	 */
	T createCopy(final PropertyTree propertyTree, final PropertyTreeUse propertyTreeUse);

	/**
	 * Clones this instances partially, the parts
	 * to be EXCLUDED will be defined by <tt>propertyTree</tt>
	 *
	 * @param propertyTree Defines which parts of the object tree will be excluded
	 * @return A copy of the original object.
	 */
	T copyExcept(final PropertyTree propertyTree);

	/**
	 * Clones this instances partially, the parts
	 * to be INCLUDED will be defined by <tt>propertyTree</tt>,
	 * all other parts will be excluded.
	 *
	 * @param propertyTree Defines which parts of the object tree will be included in the clone
	 * @return A copy of the original object.
	 */
	T copyOnly(final PropertyTree propertyTree);
}