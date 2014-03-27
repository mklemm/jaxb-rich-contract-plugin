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

import java.util.*;

/**
 * Represents a property path for use in the clone() method
 */
public class PropertyPath {
	private Map<String, PropertyPath> children;
	private final PropertyPath parent;
	private final String propertyName;
	private final boolean including;
	private PropertyPath root;

	public static final class Builder {
		private final Map<String, Builder> children = new LinkedHashMap<String, Builder>();
		private final Builder parent;
		private final String propertyName;
		private final boolean including;

		public Builder(final boolean including) {
			this(null,null,including);
		}

		private Builder(final Builder parent, final String propertyName, final boolean including) {
			this.parent = parent;
			this.propertyName = propertyName;
			this.including = including;
		}

		public Builder include(final String propertyName) {
			Builder child = this.children.get(propertyName);
			if (child == null) {
				child = new Builder(this, propertyName, true);
				this.children.put(propertyName, child);
			}
			return child;
		}

		public Builder exclude(final String propertyName) {
			Builder child = this.children.get(propertyName);
			if (child == null) {
				child = new Builder(this, propertyName, false);
				this.children.put(propertyName, child);
			}
			return child;
		}

		public PropertyPath build() {
			if(this.parent != null) {
				return this.parent.build();
			} else {
				final PropertyPath product = new PropertyPath(this.propertyName, this.including);
				product.root = product;
				product.children = buildChildren(product);
				return product;
			}
		}

		private Map<String,PropertyPath> buildChildren(final PropertyPath parent) {
			final Map<String,PropertyPath> childProducts = new LinkedHashMap<String, PropertyPath>(this.children.size());
			for(final String propertyName : this.children.keySet()) {
				final Builder childBuilder = this.children.get(propertyName);
				final PropertyPath child = new PropertyPath(parent, childBuilder.propertyName, childBuilder.including);
				child.root = parent.root;
				child.children = childBuilder.buildChildren(child);
				childProducts.put(child.propertyName, child);
			}
			return Collections.unmodifiableMap(childProducts);
		}


		public Builder parent() {
			return this.parent;
		}

		public Builder root() {
			if(this.parent != null) {
				return this.parent.root();
			} else {
				return this;
			}
		}
	}

	public static Builder includeAll() {
		return new Builder(true);
	}

	public static Builder excludeAll() {
		return new Builder(false);
	}

	private PropertyPath(final String propertyName, final boolean including) {
		this(null, propertyName, including);
	}

	private PropertyPath(final PropertyPath parent, final String propertyName, final boolean including) {
		this.parent = parent;
		this.propertyName = propertyName;
		this.including = including;
	}

	public PropertyPath get(final String propertyName) {
		if (this.children == null || this.children.isEmpty()) {
			return this;
		} else {
			final PropertyPath path = this.children.get(propertyName);
			return path == null ? new PropertyPath(this, "*", this.including) : path;
		}
	}

	public boolean includes() {
		return this.including;
	}

	public boolean excludes() {
		return !this.including;
	}

	public String propertyName() {
		return this.propertyName;
	}

	public PropertyPath parent() {
		return this.parent;
	}

	public PropertyPath root() {
		return this.root;
	}
}
