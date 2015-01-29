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

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import com.sun.codemodel.JDefinedClass;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.xml.xsom.XSDeclaration;

/**
 * @author mirko 2014-05-29
 */
public class InterfaceOutline<T extends XSDeclaration> implements TypeOutline {
	private InterfaceOutline<T> superInterface = null;
	private final JDefinedClass implClass;
	private List<PropertyOutline> declaredFields = null;
	private final T schemaComponent;
	private final QName name;

	public InterfaceOutline(final T schemaComponent, final JDefinedClass implClass) {
		this.schemaComponent = schemaComponent;
		this.implClass = implClass;
		this.name = new QName(schemaComponent.getTargetNamespace(), schemaComponent.getName());
	}

	@Override
	public List<PropertyOutline> getDeclaredFields() {
		return this.declaredFields;
	}

	public void addField(final FieldOutline field) {
		if(this.declaredFields == null) {
			this.declaredFields = new ArrayList<>();
		}
		this.declaredFields.add(new DefinedPropertyOutline(field));
	}

	@Override
	public InterfaceOutline getSuperClass() {
		return this.superInterface;
	}

	@Override
	public JDefinedClass getImplClass() {
		return this.implClass;
	}

	public T getSchemaComponent() {
		return this.schemaComponent;
	}

	public QName getName() {
		return this.name;
	}

	void setSuperInterface(final InterfaceOutline superInterface) {
		this.superInterface = superInterface;
	}


}
