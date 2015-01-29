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
package com.kscs.util.plugins.xjc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * @author mirko 2014-05-29
 */
public class DefinedClassOutline implements TypeOutline {
	private final ApiConstructs apiConstructs;
	private final ClassOutline classOutline;
	private final List<PropertyOutline> declaredFields;
	private TypeOutline cachedSuperClass = null;

	public DefinedClassOutline(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		this.apiConstructs = apiConstructs;
		this.classOutline = classOutline;
		final List<PropertyOutline> properties = new ArrayList<PropertyOutline>(classOutline.getDeclaredFields().length);
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			properties.add(new DefinedPropertyOutline(fieldOutline));
		}
		this.declaredFields = Collections.unmodifiableList(properties);
	}

	@Override
	public List<PropertyOutline> getDeclaredFields() {
		return this.declaredFields;
	}

	@Override
	public TypeOutline getSuperClass() {
		if (this.cachedSuperClass == null) {
			if (this.classOutline.getSuperClass() != null) {
				this.cachedSuperClass = new DefinedClassOutline(this.apiConstructs, this.classOutline.getSuperClass());
			} else {
				try {
					final Class<?> ungeneratedSuperClass = Class.forName(this.classOutline.implClass._extends().fullName());
					if (Object.class.equals(ungeneratedSuperClass)) {
						return null;
					} else {
						final JClass jClass = this.apiConstructs.getBuilderClass(ungeneratedSuperClass);
						if (jClass != null) {
							return new ReferencedClassOutline(this.apiConstructs.codeModel, ungeneratedSuperClass);
						} else {
							return null;
						}
					}
				} catch (final Exception e) {
					throw new RuntimeException("Cannot find superclass of " + this.classOutline.target.getName() + ": " + this.classOutline.target.getLocator());
				}

			}
		}
		return this.cachedSuperClass;
	}

	@Override
	public JDefinedClass getImplClass() {
		return this.classOutline.implClass;
	}

	public ClassOutline getClassOutline() {
		return this.classOutline;
	}
}
