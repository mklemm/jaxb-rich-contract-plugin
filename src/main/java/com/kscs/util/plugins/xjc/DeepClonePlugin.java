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
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import static com.kscs.util.plugins.xjc.PluginUtil.nullSafe;

/**
 * XJC Plugin to generate Object.clone() implementation method
 */
public class DeepClonePlugin extends AbstractPlugin {
	public DeepClonePlugin() {
		super("clone");
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);


		for (final ClassOutline classOutline : outline.getClasses()) {
			classOutline.implClass._implements(Cloneable.class);
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			generateCloneMethod(apiConstructs, classOutline);
		}
		return true;

	}

	private void generateCloneMethod(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;

		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, apiConstructs.cloneMethod);
		cloneMethod.annotate(Override.class);

		final JBlock body = cloneMethod.body();
		final JVar newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.cloneMethod)));
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = JExpr._this().ref(field);
					if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
							final JForEach forLoop = apiConstructs.loop(body, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.cloneMethod))));
						} else {
							body.assign(newField, nullSafe(fieldRef, apiConstructs.newArrayList(elementType).arg(fieldRef)));
						}

						final ImmutablePlugin immutablePlugin = apiConstructs.findPlugin(ImmutablePlugin.class);
						if (immutablePlugin != null) {
							immutablePlugin.immutableInit(apiConstructs, body, newObjectVar, field);
						}
					}
					if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
						body.assign(newField, nullSafe(fieldRef, apiConstructs.castOnDemand(fieldType, JExpr._this().ref(field).invoke(apiConstructs.cloneMethod))));
					} else {
						// body.assign(newField, JExpr._this().ref(field));
					}
				}
			}
		}
		body._return(newObjectVar);

		cloneMethod._throws(CloneNotSupportedException.class);
	}

}
