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
import com.kscs.util.plugins.xjc.codemodel.NestedThisRef;
import com.kscs.util.plugins.xjc.outline.DefinedInterfaceOutline;
import com.kscs.util.plugins.xjc.outline.DefinedPropertyOutline;
import com.kscs.util.plugins.xjc.outline.DefinedTypeOutline;
import com.kscs.util.plugins.xjc.outline.TypeOutline;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

/**
 * @author Mirko Klemm 2015-03-12
 */
public class ModifierGenerator {
	public static final String MODIFIER_CACHE_FIELD_NAME = "__cachedModifier__";
	public static final String SETTER_PREFIX = "set";
	public static final String GETTER_PREFIX = "get";
	private final DefinedTypeOutline classOutline;
	private final JDefinedClass modifierClass;
	private final boolean implement;

	public static void generateClass(final PluginContext pluginContext, final DefinedTypeOutline classOutline, final String modifierClassName, final String modifierInterfaceName, final Collection<TypeOutline> suerInterfaces, final String modifierMethodName) throws JClassAlreadyExistsException {
		new ModifierGenerator(pluginContext, classOutline, modifierClassName, modifierInterfaceName, suerInterfaces, modifierMethodName, true).generatePropertyAccessors();
	}

	public static void generateClass(final PluginContext pluginContext, final DefinedTypeOutline classOutline, final String modifierClassName, final String modifierMethodName) throws JClassAlreadyExistsException {
		new ModifierGenerator(pluginContext, classOutline, modifierClassName, null, null, modifierMethodName, true).generatePropertyAccessors();
	}

	public static void generateInterface(final PluginContext pluginContext, final DefinedTypeOutline classOutline, final String modifierInterfaceName, final Collection<TypeOutline> interfaces, final String modifierMethodName) throws JClassAlreadyExistsException {
		new ModifierGenerator(pluginContext, classOutline, modifierInterfaceName, modifierInterfaceName, interfaces, modifierMethodName, false).generatePropertyAccessors();
	}

	private ModifierGenerator(final PluginContext pluginContext, final DefinedTypeOutline classOutline, final String modifierClassName, final String modifierInterfaceName, final Collection<TypeOutline> interfaces, final String modifierMethodName, final boolean implement) throws JClassAlreadyExistsException {
		this.classOutline = classOutline;
		final JDefinedClass definedClass = classOutline.getImplClass();
		this.implement = implement;
		this.modifierClass = definedClass._class(JMod.PUBLIC, modifierClassName, classOutline.getImplClass().getClassType());
		if(interfaces != null) {
			for (final TypeOutline interfaceOutline : interfaces) {
				this.modifierClass._implements( pluginContext.ref(interfaceOutline.getImplClass(), modifierInterfaceName, true));
			}
		}
		final JFieldRef cachedModifierField;
		if(!"java.lang.Object".equals(definedClass._extends().fullName())) {
			this.modifierClass._extends(pluginContext.ref(definedClass._extends(), modifierClassName, false));
			cachedModifierField = JExpr.refthis(ModifierGenerator.MODIFIER_CACHE_FIELD_NAME);
		} else {
			if(implement) {
				cachedModifierField = JExpr._this().ref(definedClass.field(JMod.PROTECTED | JMod.TRANSIENT, this.modifierClass, ModifierGenerator.MODIFIER_CACHE_FIELD_NAME));
			} else {
				cachedModifierField = null;
			}
		}

		final JDefinedClass typeDefinition = classOutline.isInterface() && ((DefinedInterfaceOutline)classOutline).getSupportInterface() != null ? ((DefinedInterfaceOutline)classOutline).getSupportInterface() : definedClass;
		final JMethod modifierMethod = typeDefinition.method(JMod.PUBLIC, this.modifierClass, modifierMethodName);
		if(this.implement) {
			final JConditional ifCacheNull = modifierMethod.body()._if(JExpr._null().eq(cachedModifierField));
			ifCacheNull._then().assign(cachedModifierField, JExpr._new(this.modifierClass));
			modifierMethod.body()._return(JExpr.cast(this.modifierClass, cachedModifierField));
		}

	}

	private void generatePropertyAccessors() {
		for(final DefinedPropertyOutline fieldOutline:this.classOutline.getDeclaredFields()) {
			generatePropertyAccessor(fieldOutline);
		}
	}

	private void generatePropertyAccessor(final DefinedPropertyOutline fieldOutline) {
		if(fieldOutline.isCollection() && !fieldOutline.isArray()) {
			generateCollectionAccessor(fieldOutline);
		} else {
			generateSingularPropertyAccessor(fieldOutline);
		}
	}

	private void generateSingularPropertyAccessor(final DefinedPropertyOutline fieldOutline) {
		final JFieldVar fieldVar = fieldOutline.getFieldVar();
		if(fieldVar != null) {
			final JMethod modifier = this.modifierClass.method(JMod.PUBLIC, this.modifierClass.owner().VOID, ModifierGenerator.SETTER_PREFIX + fieldOutline.getBaseName());
			final JVar parameter = modifier.param(JMod.FINAL, fieldVar.type(), fieldOutline.getFieldName());
			if(this.implement) {
				modifier.body().add(new NestedThisRef(this.classOutline.getImplClass()).invoke(modifier.name()).arg(parameter));
			}
		}
	}

	private void generateCollectionAccessor(final DefinedPropertyOutline fieldOutline) {
		final JFieldVar fieldVar = fieldOutline.getFieldVar();
		if(fieldVar != null) {
			final JMethod modifier = this.modifierClass.method(JMod.PUBLIC, fieldVar.type(), ModifierGenerator.GETTER_PREFIX + fieldOutline.getBaseName());
			if(this.implement) {
				final JFieldRef fieldRef = new NestedThisRef(this.classOutline.getImplClass()).ref(fieldVar);
				final JConditional ifNull = modifier.body()._if(fieldRef.eq(JExpr._null()));
				ifNull._then().assign(fieldRef, JExpr._new(this.classOutline.getImplClass().owner().ref(ArrayList.class).narrow(fieldOutline.getElementType())));
				modifier.body()._return(fieldRef);
			}
		}
	}

}
