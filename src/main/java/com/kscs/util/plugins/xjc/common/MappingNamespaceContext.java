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

package com.kscs.util.plugins.xjc.common;

import javax.xml.namespace.NamespaceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
* @author Mirko Klemm 2015-02-03
*/
public class MappingNamespaceContext implements NamespaceContext {
	private final Map<String, List<String>> namespacesByUri = new HashMap<>();
	private final HashMap<String, String> namespacesByPrefix = new HashMap<>();

	public MappingNamespaceContext add(final String prefix, final String namespaceUri) {
		putMapValue(this.namespacesByUri, namespaceUri, prefix);
		this.namespacesByPrefix.put(prefix, namespaceUri);
		return this;
	}

	@Override
	public String getNamespaceURI(final String prefix) {
		return this.namespacesByPrefix.get(prefix);
	}

	@Override
	public String getPrefix(final String namespaceURI) {
		return getPrefixes(namespaceURI).hasNext() ? (String) getPrefixes(namespaceURI).next() : null;
	}

	@Override
	public Iterator getPrefixes(final String namespaceURI) {
		return getMapValues(this.namespacesByUri, namespaceURI).iterator();
	}

	private static List<String> getMapValues(final Map<String, List<String>> map, final String key) {
		final List<String> val = map.get(key);
		return val == null ? Collections.<String>emptyList() : val;
	}

	private static void putMapValue(final Map<String, List<String>> map, final String key, final String value) {
		List<String> values = map.get(key);
		if (values == null) {
			values = new ArrayList<>();
			map.put(key, values);
		}
		values.add(value);
	}
}
