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

import com.kscs.util.plugins.xjc.common.PropertyOutline;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * @author Mirko Klemm 2015-01-28
 */
public class DefinedPropertyOutline implements PropertyOutline {
	private final FieldOutline fieldOutline;

	public DefinedPropertyOutline(final FieldOutline fieldOutline) {
		this.fieldOutline = fieldOutline;
	}

	@Override
	public String getBaseName() {
		return this.fieldOutline.getPropertyInfo().getName(true);
	}

	@Override
	public String getFieldName() {
		return this.fieldOutline.getPropertyInfo().getName(false);
	}

	@Override
	public JType getRawType() {
		return this.fieldOutline.getRawType();
	}

	@Override
	public JType getElementType() {
		if(isCollection() && ! getRawType().isArray()) {
			return ((JClass) getRawType()).getTypeParameters().get(0);
		} else {
			return getRawType();
		}
	}

	@Override
	public JFieldVar getFieldVar() {
		return this.fieldOutline.parent().implClass.fields().get(this.fieldOutline.getPropertyInfo().getName(false));
	}

	public boolean hasGetter() {
		for(final JMethod method : this.fieldOutline.parent().implClass.methods()) {
			if((method.name().equals("get"+ this.fieldOutline.getPropertyInfo().getName(true))
					|| method.name().equals("is"+ this.fieldOutline.getPropertyInfo().getName(true))) && method.params().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCollection() {
		return this.fieldOutline.getPropertyInfo().isCollection();
	}

}
