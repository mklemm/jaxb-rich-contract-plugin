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

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JType;

import jakarta.xml.bind.JAXBElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Mirko Klemm 2015-01-28
 */
public class ReferencedPropertyOutline implements PropertyOutline {
	private final Field field;
	private final JCodeModel codeModel;

	private JType rawType = null;
	private boolean collection = false;

	public ReferencedPropertyOutline(final JCodeModel codeModel, final Field field) {
		this.codeModel = codeModel;
		this.field = field;
	}

	@Override
	public String getBaseName() {
		return this.field.getName().substring(0,1).toUpperCase() + this.field.getName().substring(1);
	}

	@Override
	public String getFieldName() {
		return this.field.getName();
	}

	@Override
	public JType getElementType() {
		initRawType();
		return this.rawType;
	}

	@Override
	public JType getRawType() {
		if(isCollection() && !this.field.getType().isArray()) {
			return this.codeModel.ref(this.field.getType()).narrow(getElementType());
		} else {
			return this.codeModel._ref(this.field.getType());
		}
	}

	private void initRawType() {
		if(this.rawType == null) {
			final Class<?> type;
			final Class<?> declaredType = this.field.getType();
			if (declaredType.isArray()) {
				type = declaredType.getComponentType();
				this.collection = true;
			} else if (Collection.class.isAssignableFrom(declaredType)) {
				type = (Class<?>) ((ParameterizedType) this.field.getGenericType()).getActualTypeArguments()[0];
				this.collection = true;
			} else {
				type = declaredType;
			}
			this.rawType = this.codeModel._ref(type);
		}
	}

	@Override
	public JFieldVar getFieldVar() {
		return null;
	}

	public boolean hasGetter() {
		Method method;
		try {
			method = this.field.getDeclaringClass().getMethod("get" + getBaseName());
		} catch (final NoSuchMethodException e) {
			try {
				method = this.field.getDeclaringClass().getMethod("is" + getBaseName());
			} catch (final NoSuchMethodException nsme) {
				method = null;
			}
		}
		return method != null;
	}

	@Override
	public boolean isCollection() {
		initRawType();
		return this.collection;
	}

	@Override
	public boolean isIndirect() {
		return getElementType().erasure().fullName().equals(this.codeModel.ref(JAXBElement.class).fullName());
	}

	@Override
	public List<TagRef> getChoiceProperties() {
		return Collections.emptyList();
	}
}
