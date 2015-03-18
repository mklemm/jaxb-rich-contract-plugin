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
package com.kscs.util.plugins.xjc.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.kscs.util.plugins.xjc.PluginContext;
import com.sun.codemodel.JDefinedClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * @author mirko 2014-05-29
 */
public class DefinedClassOutline implements DefinedTypeOutline {
	private final PluginContext pluginContext;
	private final ClassOutline classOutline;
	private final List<DefinedPropertyOutline> declaredFields;
	private final TypeOutline superClass;

	public DefinedClassOutline(final PluginContext pluginContext, final ClassOutline classOutline) {
		this.pluginContext = pluginContext;
		this.classOutline = classOutline;
		final List<DefinedPropertyOutline> properties = new ArrayList<>(classOutline.getDeclaredFields().length);
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			properties.add(new DefinedPropertyOutline(fieldOutline));
		}
		this.declaredFields = Collections.unmodifiableList(properties);
		if (this.classOutline.getSuperClass() != null) {
			this.superClass = new DefinedClassOutline(this.pluginContext, this.classOutline.getSuperClass());
		} else {
			try {
				final Class<?> ungeneratedSuperClass = Class.forName(this.classOutline.implClass._extends().fullName());
				if (Object.class.equals(ungeneratedSuperClass)) {
					this.superClass = null;
				} else {
					this.superClass = new ReferencedClassOutline(this.pluginContext.codeModel, ungeneratedSuperClass);
				}
			} catch (final Exception e) {
				throw new RuntimeException("Cannot find superclass of " + this.classOutline.target.getName() + ": " + this.classOutline.target.getLocator());
			}

		}
	}

	@Override
	public List<DefinedPropertyOutline> getDeclaredFields() {
		return this.declaredFields;
	}

	@Override
	public TypeOutline getSuperClass() {
		return this.superClass;
	}

	@Override
	public JDefinedClass getImplClass() {
		return this.classOutline.implClass;
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public boolean isInterface() {
		return false;
	}

	public ClassOutline getClassOutline() {
		return this.classOutline;
	}

}
