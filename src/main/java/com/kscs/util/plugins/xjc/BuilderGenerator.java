package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

import java.util.Map;

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

/**
 * Utility class for base functionality
 * of the fluent-builder plugin.
 */
public abstract class BuilderGenerator {
	protected final ApiConstructs apiConstructs;

	protected final JDefinedClass definedClass;
	protected final JDefinedClass builderClass;
	protected final ClassOutline classOutline;
	protected final boolean hasImmutablePlugin;
	protected final Map<String,BuilderOutline> builderOutlines;

	protected BuilderGenerator(final ApiConstructs apiConstructs, final Map<String,BuilderOutline> builderOutlines, final BuilderOutline builderOutline) {
		this.apiConstructs = apiConstructs;
		this.builderOutlines = builderOutlines;
		this.classOutline = builderOutline.getClassOutline();
		this.definedClass = this.classOutline.implClass;
		this.hasImmutablePlugin = apiConstructs.hasPlugin(ImmutablePlugin.class);
		this.builderClass = builderOutline.getDefinedBuilderClass();
	}

	public void buildProperties() {
		final ClassOutline superClass = this.classOutline.getSuperClass();

		final JMethod initMethod = this.builderClass.method(JMod.PROTECTED, this.definedClass, ApiConstructs.INIT_METHOD_NAME);
		final JTypeVar typeVar = initMethod.generify("P", this.definedClass);
		initMethod.type(typeVar);
		final JVar productParam = initMethod.param(JMod.FINAL, typeVar, ApiConstructs.PRODUCT_INSTANCE_NAME);
		final JBlock initBody = initMethod.body();

		for (final FieldOutline fieldOutline : this.classOutline.getDeclaredFields()) {
			generateBuilderMember(fieldOutline, initBody, productParam);
		}

		if (superClass != null) {
			generateExtendsClause(getBuilderDeclaration(superClass.implClass));
			initBody._return(JExpr._super().invoke(initMethod).arg(productParam));
			generateBuilderMemeberOverrides(superClass);
		} else {
			initBody._return(productParam);
		}

		if (!this.definedClass.isAbstract()) {
			generateBuildMethod(initMethod);
			generateBuilderMethod();
		}

	}

	private void generateBuilderMemeberOverrides(final ClassOutline superClass) {
		final JDefinedClass definedSuperClass = superClass.implClass;
		for (final FieldOutline superFieldOutline : superClass.getDeclaredFields()) {
			final JFieldVar declaredSuperField = definedSuperClass.fields().get(superFieldOutline.getPropertyInfo().getName(false));
			final String superPropertyName = superFieldOutline.getPropertyInfo().getName(true);
			generateBuilderMemberOverride(superFieldOutline, declaredSuperField, superPropertyName);

		}

		if (superClass.getSuperClass() != null) {
			generateBuilderMemeberOverrides(superClass.getSuperClass());
		}
	}

	protected BuilderOutline getBuilderDeclaration(final JType type) {
		return this.builderOutlines.get(type.fullName());
	}

	protected abstract void generateBuilderMember(final FieldOutline fieldOutline, final JBlock initBody, final JVar productParam);

	protected abstract void generateBuilderMemberOverride(final FieldOutline superFieldOutline, final JFieldVar declaredSuperField, final String superPropertyName);

	protected abstract JDefinedClass generateExtendsClause(final BuilderOutline superBuilder);

	protected abstract JMethod generateBuildMethod(final JMethod initMethod);

	protected abstract JMethod generateBuilderMethod();
}
