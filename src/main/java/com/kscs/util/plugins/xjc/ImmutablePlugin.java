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

import java.util.Iterator;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.base.PluginUtil;
import com.kscs.util.plugins.xjc.outline.DefinedClassOutline;
import com.kscs.util.plugins.xjc.outline.PropertyOutline;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * XJC Plugin to make generated classes immutable
 */
public class ImmutablePlugin extends AbstractPlugin {
	@Opt
	private String constructorAccess = "public";

	@Opt
	protected boolean generateModifier = true;

	@Opt
	protected String modifierClassName = "Modifier";

	@Opt
	protected String modifierMethodName = "modifier";

	@Opt
	protected boolean fake = false;

	@Opt
	protected boolean collectionsAsIterable = false;

	@Override
	public String getOptionName() {
		return "Ximmutable";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);
		for (final ClassOutline classOutline : outline.getClasses()) {
				final JDefinedClass definedClass = classOutline.implClass;
				for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
					final JFieldVar declaredField;
					if (fieldOutline.getPropertyInfo().isCollection() && !((declaredField = PluginUtil.getDeclaredField(fieldOutline)).type().isArray())) {
						final JClass elementType = ((JClass) declaredField.type()).getTypeParameters().get(0);
						final JMethod oldGetter = definedClass.getMethod("get" + fieldOutline.getPropertyInfo().getName(true), new JType[0]);
						final JType getterType = this.collectionsAsIterable ? apiConstructs.iterableClass.narrow(elementType) : oldGetter.type();
						if(fake) {
							oldGetter.type(getterType);
						} else {
							final JFieldVar immutableField = definedClass.field(JMod.PROTECTED | JMod.TRANSIENT, declaredField.type(), getImmutableFieldName(declaredField), JExpr._null());
							definedClass.methods().remove(oldGetter);
							final JMethod newGetter = definedClass.method(JMod.PUBLIC, getterType, oldGetter.name());
							final JConditional ifFieldNull = newGetter.body()._if(JExpr._this().ref(declaredField).eq(JExpr._null()));
							ifFieldNull._then().assign(JExpr._this().ref(declaredField), JExpr._new(apiConstructs.arrayListClass.narrow(elementType)));

							final JConditional ifImmutableFieldNull = newGetter.body()._if(JExpr._this().ref(immutableField).eq(JExpr._null()));
							immutableInit(apiConstructs, ifImmutableFieldNull._then(), JExpr._this(), declaredField);

							newGetter.body()._return(JExpr._this().ref(immutableField));
						}
					} else {
						if(!fake) {
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
						while (constructors.hasNext()) {
							final JMethod constructor = constructors.next();
							if (constructor.params().isEmpty() && (constructor.mods().getValue() & JMod.PUBLIC) == JMod.PUBLIC) {
								if ("private".equals(this.constructorAccess.toLowerCase())) {
									constructor.mods().setPrivate();
								} else {
									constructor.mods().setProtected();
								}
							}
						}
					}
			}
			if(this.generateModifier) {
				try {
					final GroupInterfacePlugin groupInterfacePlugin = apiConstructs.findPlugin(GroupInterfacePlugin.class);
					if(groupInterfacePlugin != null) {
						ModifierGenerator.generateClass(apiConstructs, new DefinedClassOutline(apiConstructs, classOutline), this.modifierClassName, this.modifierClassName, groupInterfacePlugin.getGroupInterfacesForClass(apiConstructs, classOutline.implClass.fullName()), this.modifierMethodName);
					} else {
						ModifierGenerator.generateClass(apiConstructs, new DefinedClassOutline(apiConstructs, classOutline), this.modifierClassName, this.modifierMethodName);
					}
				} catch (final JClassAlreadyExistsException e) {
					errorHandler.error(new SAXParseException(e.getMessage(), classOutline.target.getLocator()));
				}
			}
		}
		return true;
	}

	String getImmutableFieldName(final FieldOutline fieldVar) {
		return fieldVar.getPropertyInfo().getName(false) + "_RO";
	}
	String getImmutableFieldName(final PropertyOutline fieldVar) {
		return fieldVar.getFieldName() + "_RO";
	}
	String getImmutableFieldName(final JFieldVar fieldVar) {
		return fieldVar.name() + "_RO";
	}

	public void immutableInit(final ApiConstructs apiConstructs, final JBlock body, final JExpression instanceRef, final FieldOutline collectionField) {
		body.assign(instanceRef.ref(getImmutableFieldName(collectionField)), PluginUtil.nullSafe(collectionField, apiConstructs.unmodifiableList(instanceRef.ref(collectionField.getPropertyInfo().getName(false)))));
	}

	public void immutableInit(final ApiConstructs apiConstructs, final JBlock body, final JExpression instanceRef, final PropertyOutline collectionField) {
		body.assign(instanceRef.ref(getImmutableFieldName(collectionField)), PluginUtil.nullSafe(collectionField, apiConstructs.unmodifiableList(instanceRef.ref(collectionField.getFieldName()))));
	}

	public void immutableInit(final ApiConstructs apiConstructs, final JBlock body, final JExpression instanceRef, final JFieldVar declaredField) {
		body.assign(instanceRef.ref(getImmutableFieldName(declaredField)), PluginUtil.nullSafe(declaredField, apiConstructs.unmodifiableList(instanceRef.ref(declaredField))));
	}

}
