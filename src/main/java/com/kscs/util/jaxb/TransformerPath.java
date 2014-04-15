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
import java.util.HashMap;
import java.util.Map;

/**
 * @author mirko 2014-04-07
 */
public class TransformerPath {
	private final Map<String, TransformerPath> children;
	private final String propertyName;
	private final PropertyTransformer<?,?> transfomer;

	public TransformerPath(final String propertyName, final PropertyTransformer<?,?> transformer, final Map<String, TransformerPath> children) {
		this.propertyName = propertyName;
		this.children = Collections.unmodifiableMap(children);
		this.transfomer = transformer;
	}

	private TransformerPath( final String propertyName, final PropertyTransformer<?,?> transformer) {
		this(propertyName, transformer, new HashMap<String, TransformerPath>());
	}

	public TransformerPath get(final String propertyName) {
		return this.children.get(propertyName);
	}

	public PropertyTransformer<?,?> transformer() {
		return this.transfomer;
	}

	public String propertyName() {
		return this.propertyName;
	}
}
