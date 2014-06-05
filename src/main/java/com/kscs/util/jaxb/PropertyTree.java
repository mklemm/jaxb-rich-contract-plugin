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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a property path for use in the clone() method
 */
public class PropertyTree {
	private final Map<String, PropertyTree> children;
	private final String propertyName;

	public static final class Builder {
		private final Map<String, Builder> children = new LinkedHashMap<String, Builder>();
		private final Builder parent;
		private final String propertyName;

		public Builder() {
			this(null, null);
		}

		private Builder(final Builder parent, final String propertyName) {
			this.parent = parent;
			this.propertyName = propertyName;
		}

		public Builder with(final String propertyName) {
			Builder child = this.children.get(propertyName);
			if (child == null) {
				child = new Builder(this, propertyName);
				this.children.put(propertyName, child);
			}
			return child;
		}


		public PropertyTree build() {
			if (this.parent != null) {
				return this.parent.build();
			} else {
				return new PropertyTree(this.propertyName, buildChildren());
			}
		}

		private Map<String, PropertyTree> buildChildren() {
			final Map<String, PropertyTree> childProducts = new LinkedHashMap<String, PropertyTree>(this.children.size());
			for (final Builder childBuilder : this.children.values()) {
				final PropertyTree child = new PropertyTree(childBuilder.propertyName, childBuilder.buildChildren());
				childProducts.put(child.propertyName, child);
			}
			return Collections.unmodifiableMap(childProducts);
		}


		public Builder parent() {
			return this.parent;
		}

		public Builder root() {
			if (this.parent != null) {
				return this.parent.root();
			} else {
				return this;
			}
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public PropertyTree(final String propertyName, final Map<String, PropertyTree> children) {
		this.propertyName = propertyName;
		this.children = Collections.unmodifiableMap(children);
	}

	public PropertyTree get(final String propertyName) {
		return isLeaf() ? null : this.children.get(propertyName);
	}

	public boolean isLeaf() {
		return this.children == null || this.children.isEmpty();
	}

	public String propertyName() {
		return this.propertyName;
	}

}
