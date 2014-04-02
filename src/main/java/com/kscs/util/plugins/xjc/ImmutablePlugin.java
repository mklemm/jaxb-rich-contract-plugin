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

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * XJC Plugin to make generated classes immutable
 */
public class ImmutablePlugin extends Plugin {

	@Override
	public String getOptionName() {
		return "Ximmutable";
	}

	@Override
	public String getUsage() {
		return "-Ximmutable: Make generated classes immutable. All property setters will be generated as \"protected\".";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);
		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
				final JFieldVar declaredField;
				if(fieldOutline.getPropertyInfo().isCollection() && !((declaredField = PluginUtil.getDeclaredField(fieldOutline)).type().isArray())) {
					final JClass elementType = ((JClass)declaredField.type()).getTypeParameters().get(0);
					final JMethod oldGetter = definedClass.getMethod("get"+fieldOutline.getPropertyInfo().getName(true), new JType[0]);
					final JFieldVar immutableField = definedClass.field(JMod.PRIVATE | JMod.TRANSIENT, declaredField.type(), getImmutableFieldName(declaredField), JExpr._null());
					definedClass.methods().remove(oldGetter);
					final JMethod newGetter = definedClass.method(JMod.PUBLIC, oldGetter.type(), oldGetter.name());
					final JConditional ifFieldNull = newGetter.body()._if(JExpr._this().ref(declaredField).eq(JExpr._null()));
					ifFieldNull._then().assign(JExpr._this().ref(declaredField), JExpr._new(apiConstructs.arrayListClass.narrow(elementType)));

					final JConditional ifImmutableFieldNull = newGetter.body()._if(JExpr._this().ref(immutableField).eq(JExpr._null()));
					immutableInit(apiConstructs, ifImmutableFieldNull._then(), JExpr._this(), declaredField);

					newGetter.body()._return(JExpr._this().ref(immutableField));
				} else {
					final String setterName = "set" + fieldOutline.getPropertyInfo().getName(true);
					final JMethod setterMethod = definedClass.getMethod(setterName, new JType[]{fieldOutline.getRawType()});
					if (setterMethod != null) {
						setterMethod.mods().setProtected();
					}
				}
			}
		}
		return true;
	}

	public String getImmutableFieldName(final JFieldVar fieldVar) {
		return fieldVar.name() + "_RO";
	}

	public void immutableInit(final ApiConstructs apiConstructs, final JBlock body, final JExpression instanceRef, final JFieldVar collectionField) {
		body.assign(instanceRef.ref(getImmutableFieldName(collectionField)), PluginUtil.nullSafe(collectionField, apiConstructs.unmodifiableList(instanceRef.ref(collectionField))));
	}
}
