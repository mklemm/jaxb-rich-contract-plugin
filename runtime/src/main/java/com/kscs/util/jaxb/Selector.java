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

import java.util.Collections;
import java.util.Map;

/**
 * Helper class acting as base class for all selectors and
 * concrete implementation of leaf selectors.
 * @author mirko 2014-04-04
 */
public class Selector<TRoot extends Selector<TRoot, ?>, TParent> {
	public final TRoot _root;
	public final TParent _parent;
	protected final String _propertyName;
	protected final boolean _include;

	@SuppressWarnings("unchecked")
	public Selector(final TRoot root, final TParent parent, final String propertyName, final boolean include) {
			this._root = root == null ? (TRoot) this : root;
			this._parent = parent;
			this._propertyName = propertyName;
			this._include = include;
	}

	public Selector(final TRoot root, final TParent parent, final String propertyName) {
		this(root, parent, propertyName, true);
	}

	/**
	 * This is only used by builders and other implementational details
	 * @return A map representing the child nodes of this selector
	 */
	public Map<String, PropertyTree> buildChildren() {
		return Collections.emptyMap();
	}

	/**
	 * Builds a property tree specified by this selector
	 * @return A property tree specified by this selector
	 */
	public PropertyTree build() {
		return this._root.init();
	}

	/**
	 * This is only used by builders and other implementational details
	 * @return A property tree specified by this selector
	 */
	public PropertyTree init() {
		return new PropertyTree(this._propertyName, buildChildren());
	}
}
