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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import com.kscs.util.plugins.xjc.base.PropertyOutline;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;

/**
 * @author Mirko Klemm 2015-01-28
 */
public class ReferencedClassOutline implements TypeOutline {
	private final Class<?> referencedClass;
	private final JCodeModel codeModel;
	private final List<PropertyOutline> declaredFields;

	private ReferencedClassOutline superClassOutline = null;

	public ReferencedClassOutline(final JCodeModel codeModel, final Class<?> referencedClass) {
		this.codeModel = codeModel;
		this.referencedClass = referencedClass;
		this.declaredFields = new ArrayList<>(referencedClass.getDeclaredFields().length);
		for(final Field field : referencedClass.getDeclaredFields()) {
			this.declaredFields.add(new ReferencedPropertyOutline(codeModel, field));
		}
	}

	@Override
	public List<PropertyOutline> getDeclaredFields() {
		return this.declaredFields;
	}

	@Override
	public TypeOutline getSuperClass() {
		if(this.superClassOutline == null && this.referencedClass.getSuperclass() != null) {
			this.superClassOutline = new ReferencedClassOutline(this.codeModel, this.referencedClass.getSuperclass());
		}
		return this.superClassOutline;
	}

	@Override
	public JClass getImplClass() {
		return this.codeModel.ref(this.referencedClass);
	}

	@Override
	public boolean isLocal() {
		return false;
	}
}
