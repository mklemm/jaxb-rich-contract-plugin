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
 * @author Mirko Klemm 2015-03-15
 */
public interface UnmodifiableList<E> extends UnmodifiableCollection<E> {
	/**
	 * Returns the element at the specified position in this list.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this list
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *                                   (<code>index &lt; 0 || index &gt;= size()</code>)
	 */
	E get(int index);

	// Search Operations

	/**
	 * Returns the index of the first occurrence of the specified element
	 * in this list, or -1 if this list does not contain the element.
	 * More formally, returns the lowest index <code>i</code> such that
	 * <code>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</code>,
	 * or -1 if there is no such index.
	 *
	 * @param o element to search for
	 * @return the index of the first occurrence of the specified element in
	 * this list, or -1 if this list does not contain the element
	 * @throws ClassCastException   if the type of the specified element
	 *                              is incompatible with this list
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified element is null and this
	 *                              list does not permit null elements
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 */
	int indexOf(Object o);

	/**
	 * Returns the index of the last occurrence of the specified element
	 * in this list, or -1 if this list does not contain the element.
	 * More formally, returns the highest index <code>i</code> such that
	 * <code>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</code>,
	 * or -1 if there is no such index.
	 *
	 * @param o element to search for
	 * @return the index of the last occurrence of the specified element in
	 * this list, or -1 if this list does not contain the element
	 * @throws ClassCastException   if the type of the specified element
	 *                              is incompatible with this list
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified element is null and this
	 *                              list does not permit null elements
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 */
	int lastIndexOf(Object o);


	// View

	/**
	 * Returns a view of the portion of this list between the specified
	 * <code>fromIndex</code>, inclusive, and <code>toIndex</code>, exclusive.  (If
	 * <code>fromIndex</code> and <code>toIndex</code> are equal, the returned list is
	 * empty.)  The returned list is backed by this list, so non-structural
	 * changes in the returned list are reflected in this list, and vice-versa.
	 * The returned list supports all of the optional list operations supported
	 * by this list.
	 * This method eliminates the need for explicit range operations (of
	 * the sort that commonly exist for arrays).  Any operation that expects
	 * a list can be used as a range operation by passing a subList view
	 * instead of a whole list.  For example, the following idiom
	 * removes a range of elements from a list:
	 * <pre>
	 *      list.subList(from, to).clear();
	 * </pre>
	 * Similar idioms may be constructed for <code>indexOf</code> and
	 * <code>lastIndexOf</code>, and all of the algorithms in the
	 * <code>Collections</code> class can be applied to a subList.
	 *
	 * The semantics of the list returned by this method become undefined if
	 * the backing list (i.e., this list) is <i>structurally modified</i> in
	 * any way other than via the returned list.  (Structural modifications are
	 * those that change the size of this list, or otherwise perturb it in such
	 * a fashion that iterations in progress may yield incorrect results.)
	 *
	 * @param fromIndex low endpoint (inclusive) of the subList
	 * @param toIndex   high endpoint (exclusive) of the subList
	 * @return a view of the specified range within this list
	 * @throws IndexOutOfBoundsException for an illegal endpoint index value
	 *                                   (<code>fromIndex &lt; 0 || toIndex &gt; size ||
	 *                                   fromIndex &gt; toIndex</code>)
	 */
	UnmodifiableList<E> subList(int fromIndex, int toIndex);

	List<E> toList();
}
