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

import javax.xml.bind.JAXBElement;
import com.kscs.util.jaxb.PropertyInfo;
import com.kscs.util.plugins.xjc.common.AbstractPlugin;
import com.kscs.util.plugins.xjc.common.Opt;
import com.kscs.util.plugins.xjc.common.PropertyOutline;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CDefaultValue;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Generates a helper class to access meta information
 * about a JAXB-generated object.
 * Currently only the generation of property name constants
 * is implemented.
 * @author Mirko Klemm 2015-02-13
 */
public class MetaPlugin extends AbstractPlugin {
	@Opt
	private boolean generateTools = true;
	@Opt
	private boolean full = false;
	@Opt
	private boolean camelCase = false;
	@Opt
	private String metaClassName = "PropInfo";

	@Override
	public String getOptionName() {
		return "Xmeta";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);
		if(this.full && this.generateTools) {
			apiConstructs.writeSourceFile(PropertyInfo.class);
		}
		for(final ClassOutline classOutline:outline.getClasses()) {
			generateMetaClass(classOutline, errorHandler);
		}
		return true;
	}

	private void generateMetaClass(final ClassOutline classOutline, final ErrorHandler errorHandler) throws SAXException {
		try {
			final JDefinedClass metaClass = classOutline.implClass._class(JMod.PUBLIC | JMod.STATIC, this.metaClassName);
			for(final FieldOutline fieldOutline:classOutline.getDeclaredFields()) {
				if(this.full) {
					generateFullMetaField(metaClass, fieldOutline);
				} else {
					generateNameOnlyMetaField(metaClass, fieldOutline);
				}
			}
		} catch (final JClassAlreadyExistsException e) {
			errorHandler.error(new SAXParseException(getMessage("error.metaClassExists", classOutline.implClass.name(), this.metaClassName), classOutline.target.getLocator()));
		}
	}

	private void generateNameOnlyMetaField(final JDefinedClass metaClass, final FieldOutline fieldOutline) {
		final String propertyName = fieldOutline.getPropertyInfo().getName(false);
		final String metaFieldName = this.camelCase ? propertyName : fieldOutline.parent().parent().getModel().getNameConverter().toConstantName(propertyName);
		metaClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL | JMod.TRANSIENT, String.class, metaFieldName, JExpr.lit(propertyName));
	}
	private void generateFullMetaField(final JDefinedClass metaClass, final FieldOutline fieldOutline) {
		final PropertyOutline propertyOutline = new DefinedPropertyOutline(fieldOutline);
		final String propertyName = propertyOutline.getFieldName();
		final Outline outline = fieldOutline.parent().parent();
		final String metaFieldName = this.camelCase ? propertyName : outline.getModel().getNameConverter().toConstantName(propertyName);
		JType propertyType = propertyOutline.getElementType();
		if(outline.getCodeModel().ref(JAXBElement.class).fullName().equals(propertyType.erasure().fullName())) {
			propertyType = ((JClass)propertyType).getTypeParameters().get(0);
		}

		final JClass metaFieldType = outline.getCodeModel().ref(PropertyInfo.class).narrow(fieldOutline.parent().implClass, propertyType.boxify());

		final CDefaultValue defaultValue = fieldOutline.getPropertyInfo().defaultValue;
		metaClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL | JMod.TRANSIENT, metaFieldType, metaFieldName, JExpr._new(metaFieldType)
				.arg(propertyName)
				.arg(fieldOutline.parent().implClass.dotclass())
				.arg(dotClass(propertyType))
				.arg(JExpr.lit(propertyOutline.isCollection()))
				.arg(defaultValue == null ? JExpr._null() : defaultValue.compute(outline)));
	}

	private JExpression dotClass(final JType cl) {
		if(cl instanceof JClass) {
			return ((JClass)cl).dotclass();
		} else {
			return new JExpressionImpl() {
				public void generate(JFormatter f) {
					f.g(cl).p(".class");
				}
			};
		}
	}



}
