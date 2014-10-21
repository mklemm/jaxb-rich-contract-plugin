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
 * Base class for property transformer nodes.
 * @author mirko 2014-04-15
 */
public class Transformer<TRoot extends Transformer<TRoot, ?>, TParent> {
	public final TRoot _root;
	public final TParent _parent;
	protected final String _propertyName;
	protected final PropertyTransformer<?,?> _propertyTransformer;

	@SuppressWarnings("unchecked")
	public Transformer(final TRoot root, final TParent parent, final String propertyName, final PropertyTransformer<?,?> propertyTransformer) {
		this._root = root == null ? (TRoot) this : root;
		this._parent = parent;
		this._propertyName = propertyName;
		this._propertyTransformer = propertyTransformer;
	}

	/**
	 * This is only used by builders and other implementational details
	 */
	public Map<String, TransformerPath> buildChildren() {
		return Collections.emptyMap();
	}

	public TransformerPath build() {
		return this._root.init();
	}

	/**
	 * This is only used by builders and other implementational details
	 */
	public TransformerPath init() {
		return new TransformerPath(this._propertyName, this._propertyTransformer, buildChildren());
	}
}
