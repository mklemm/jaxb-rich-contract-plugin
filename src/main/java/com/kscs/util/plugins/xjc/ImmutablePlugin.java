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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.base.PluginUtil;
import com.kscs.util.plugins.xjc.outline.PropertyOutline;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;

/**
 * XJC Plugin to make generated classes immutable
 */
public class ImmutablePlugin extends AbstractPlugin {
	@Opt
	private boolean fake = false;
	@Opt
	protected String overrideCollectionClass = null;
	@Opt
	private String constructorAccess = "public";

	@Override
	public String getOptionName() {
		return "Ximmutable";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final PluginContext pluginContext = PluginContext.get(outline, opt, errorHandler);
		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
				final JFieldVar declaredField;
				if (fieldOutline.getPropertyInfo().isCollection() && !(declaredField = PluginUtil.getDeclaredField(fieldOutline)).type().isArray()) {
					final JClass elementType = ((JClass) declaredField.type()).getTypeParameters().get(0);
					final JMethod oldGetter = definedClass.getMethod("get" + fieldOutline.getPropertyInfo().getName(true), new JType[0]);
					final JType getterType = this.overrideCollectionClass != null ? pluginContext.codeModel.ref(this.overrideCollectionClass).narrow(elementType) : oldGetter.type();
					if (fake) {
						oldGetter.type(getterType);
					} else {
						final JFieldVar immutableField = definedClass.field(JMod.PROTECTED | JMod.TRANSIENT, getterType, getImmutableFieldName(declaredField), JExpr._null());
						definedClass.methods().remove(oldGetter);
						final JMethod newGetter = definedClass.method(JMod.PUBLIC, getterType, oldGetter.name());
						final JConditional ifFieldNull = newGetter.body()._if(JExpr._this().ref(declaredField).eq(JExpr._null()));
						ifFieldNull._then().assign(JExpr._this().ref(declaredField), JExpr._new(PluginContext.extractMutableListClass(fieldOutline).narrow(elementType)));

						final JConditional ifImmutableFieldNull = newGetter.body()._if(JExpr._this().ref(immutableField).eq(JExpr._null()));
						immutableInit(pluginContext, ifImmutableFieldNull._then(), JExpr._this(), declaredField);

						newGetter.body()._return(JExpr._this().ref(immutableField));
					}
				} else {
					if (!fake) {
						final String setterName = "set" + fieldOutline.getPropertyInfo().getName(true);
						final JMethod setterMethod = definedClass.getMethod(setterName, new JType[]{fieldOutline.getRawType()});
						if (setterMethod != null) {
							setterMethod.mods().setProtected();
						}
					}
				}
				if (!fake && !"public".equalsIgnoreCase(this.constructorAccess)) {
					final Iterator<JMethod> constructors = definedClass.constructors();
					if (!constructors.hasNext()) {
						// generate protected/private no-arg constructor
						final JMethod constructor = definedClass.constructor("private".equalsIgnoreCase(this.constructorAccess) ? JMod.PRIVATE : JMod.PROTECTED);
						constructor.javadoc().append(getMessage("comment.constructor"));
						constructor.body().directStatement("// " + getMessage("comment.constructor"));
					}
					final List<JMethod> constructorsToChange = new ArrayList<>();
					while (constructors.hasNext()) {
						final JMethod constructor = constructors.next();
						if (constructor.params().isEmpty() && (constructor.mods().getValue() & JMod.PUBLIC) == JMod.PUBLIC) {
							constructorsToChange.add(constructor);
						}
					}

					// use separate loop to avoid concurrentmodificationexception
					for(final JMethod constructor:constructorsToChange) {
						if ("private".equalsIgnoreCase(this.constructorAccess)) {
							constructor.mods().setPrivate();
						} else {
							constructor.mods().setProtected();
						}
					}
				}
			}
		}
		return true;
	}

	String getImmutableFieldName(final PropertyOutline fieldVar) {
		return fieldVar.getFieldName() + "_RO";
	}

	String getImmutableFieldName(final JFieldVar fieldVar) {
		return fieldVar.name() + "_RO";
	}

	public void immutableInit(final PluginContext pluginContext, final JBlock body, final JExpression instanceRef, final PropertyOutline collectionField) {
		if(!this.fake) {
			body.assign(instanceRef.ref(getImmutableFieldName(collectionField)), PluginUtil.nullSafe(collectionField, generateImmutableListInstantiation(pluginContext, instanceRef.ref(collectionField.getFieldName()), collectionField.getElementType())));
		}
	}

	public void immutableInit(final PluginContext pluginContext, final JBlock body, final JExpression instanceRef, final JFieldVar declaredField) {
		if(!this.fake) {
			body.assign(instanceRef.ref(getImmutableFieldName(declaredField)), PluginUtil.nullSafe(declaredField, generateImmutableListInstantiation(pluginContext, instanceRef.ref(declaredField), ((JClass)declaredField.type()).getTypeParameters().get(0))));
		}
	}

	private JInvocation generateImmutableListInstantiation(final PluginContext pluginContext, final JFieldRef fieldRef, final JType elementType) {
		if (this.overrideCollectionClass == null) {
			return pluginContext.unmodifiableList(fieldRef);
		} else {
			final JClass overrideCollection = pluginContext.codeModel.ref(this.overrideCollectionClass);
			if (overrideCollection.isAssignableFrom(pluginContext.codeModel.ref(Collection.class))) {
				return pluginContext.unmodifiableList(fieldRef);
			} else {
				return JExpr._new(overrideCollection.narrow(elementType)).arg(fieldRef);
			}
		}
	}
}
