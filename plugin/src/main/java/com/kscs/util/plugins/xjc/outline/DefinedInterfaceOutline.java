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

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import com.sun.codemodel.JDefinedClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.xml.xsom.XSDeclaration;

/**
 * @author mirko 2014-05-29
 */
public class DefinedInterfaceOutline implements InterfaceOutline, DefinedTypeOutline {
	private final List<TypeOutline> superInterfaces = new ArrayList<>();
	private final JDefinedClass implClass;
	private final ClassOutline classOutline;
	private final List<DefinedPropertyOutline> declaredFields = new ArrayList<>();
	private final XSDeclaration schemaComponent;
	private final JDefinedClass supportInterface;
	private final QName name;

	public DefinedInterfaceOutline(final XSDeclaration schemaComponent, final JDefinedClass implClass, final ClassOutline classOutline, final JDefinedClass supportInterface) {
		this.schemaComponent = schemaComponent;
		this.implClass = implClass;
		this.classOutline = classOutline;
		this.name = new QName(schemaComponent.getTargetNamespace(), schemaComponent.getName());
		this.supportInterface = supportInterface;
	}

	@Override
	public List<DefinedPropertyOutline> getDeclaredFields() {
		return this.declaredFields;
	}

	public void addField(final FieldOutline field) {
		this.declaredFields.add(new DefinedPropertyOutline(field));
	}

	@Override
	public TypeOutline getSuperClass() {
		return null;
	}

	@Override
	public JDefinedClass getImplClass() {
		return this.implClass;
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	public XSDeclaration getSchemaComponent() {
		return this.schemaComponent;
	}

	public QName getName() {
		return this.name;
	}

	public void addSuperInterface(final TypeOutline superInterface) {
		this.superInterfaces.add(superInterface);
	}

	public List<TypeOutline> getSuperInterfaces() {
		return this.superInterfaces;
	}

	public ClassOutline getClassOutline() {
		return this.classOutline;
	}

	@Override
	public JDefinedClass getSupportInterface() {
		return this.supportInterface;
	}
}
