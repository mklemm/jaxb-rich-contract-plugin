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

import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.base.PluginUtil;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import static com.kscs.util.plugins.xjc.base.PluginUtil.nullSafe;

/**
 * XJC Plugin to generate Object.clone() implementation method
 */
public class DeepClonePlugin extends AbstractPlugin {
	@Opt
	private boolean cloneThrows = true;

	public boolean isCloneThrows() {
		return this.cloneThrows;
	}

	@Override
	public String getOptionName() {
		return "Xclone";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final PluginContext pluginContext = PluginContext.get(outline, opt, errorHandler);


		for (final ClassOutline classOutline : outline.getClasses()) {
			classOutline.implClass._implements(Cloneable.class);
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			generateCloneMethod(pluginContext, classOutline);
		}
		return true;

	}

	private void generateCloneMethod(final PluginContext pluginContext, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;
		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, pluginContext.cloneMethodName);
		cloneMethod.annotate(Override.class);
		final JBlock body = cloneMethod.body();
		final JVar newObjectVar;
		newObjectVar = body.decl(JMod.FINAL, definedClass, PluginContext.NEW_OBJECT_VAR_NAME, null);
		final JBlock superMaybeTryBlock = pluginContext.catchCloneNotSupported(body, definedClass._extends());
		superMaybeTryBlock.assign(newObjectVar, JExpr.cast(definedClass, JExpr._super().invoke(pluginContext.cloneMethodName)));
		boolean cloneNotSupportedExceptionPossible = false;
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = JExpr._this().ref(field);
					if (pluginContext.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						if (pluginContext.cloneableInterface.isAssignableFrom(elementType)) {
							final JBlock maybeTryBlock = this.cloneThrows ? body : pluginContext.catchCloneNotSupported(body, elementType);
							cloneNotSupportedExceptionPossible |= pluginContext.mustCatch(elementType);
							final JForEach forLoop = pluginContext.loop(maybeTryBlock, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), pluginContext.castOnDemand(elementType, forLoop.var().invoke(pluginContext.cloneMethodName))));
						} else {
							body.assign(newField, nullSafe(fieldRef, pluginContext.newArrayList(elementType).arg(fieldRef)));
						}
						pluginContext.generateImmutableFieldInit(body, newObjectVar, field);
					} else if (pluginContext.cloneableInterface.isAssignableFrom(fieldType)) {
						final JBlock maybeTryBlock = this.cloneThrows ? body : pluginContext.catchCloneNotSupported(body, fieldType);
						cloneNotSupportedExceptionPossible |= pluginContext.mustCatch(fieldType);
						maybeTryBlock.assign(newField, nullSafe(fieldRef, pluginContext.castOnDemand(fieldType, JExpr._this().ref(field).invoke(pluginContext.cloneMethodName))));
					} else {
						// body.assign(newField, JExpr._this().ref(field));
					}
				}
			}
		}
		body._return(newObjectVar);
		if (this.cloneThrows && cloneNotSupportedExceptionPossible) {
			cloneMethod._throws(CloneNotSupportedException.class);
		}
	}
}
