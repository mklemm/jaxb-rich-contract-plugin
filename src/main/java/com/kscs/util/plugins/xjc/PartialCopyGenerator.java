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

import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JVar;

import static com.kscs.util.plugins.xjc.base.PluginUtil.nullSafe;

/**
 * @author mirko 2014-06-04
 */
class PartialCopyGenerator {
	private final PluginContext pluginContext;
	private final JVar propertyTreeUseParam;
	private final JVar propertyTreeParam;
	private final JMethod copyMethod;

	public PartialCopyGenerator(final PluginContext pluginContext, final JMethod copyMethod, final String propertyTreeParamName, final String propertyTreeUseParamName) {
		this.pluginContext = pluginContext;
		this.copyMethod = copyMethod;
		this.propertyTreeParam = copyMethod.param(JMod.FINAL, PropertyTree.class, propertyTreeParamName);
		this.propertyTreeUseParam = copyMethod.param(JMod.FINAL, PropertyTreeUse.class, propertyTreeUseParamName);
	}

	public PluginContext getPluginContext() {
		return this.pluginContext;
	}

	public JVar getPropertyTreeUseParam() {
		return this.propertyTreeUseParam;
	}

	public JVar getPropertyTreeParam() {
		return this.propertyTreeParam;
	}

	public JMethod getCopyMethod() {
		return this.copyMethod;
	}


	public void generateFieldAssignment(final JBlock body, final JFieldVar field, final JExpression targetInstanceVar, final JExpression sourceInstanceVar) {
		final JFieldRef newField = targetInstanceVar.ref(field);
		final JFieldRef fieldRef = sourceInstanceVar.ref(field);

		final JVar fieldPathVar = generatePropertyTreeVarDeclaration(body, field);
		final JExpression includeCondition = getIncludeCondition(fieldPathVar);
		final JConditional ifHasClonePath = body._if(includeCondition);
		final JBlock currentBlock = ifHasClonePath._then();
		if (field.type().isReference()) {
			final JClass fieldType = (JClass) field.type();
			if (this.pluginContext.collectionClass.isAssignableFrom(fieldType)) {
				final JClass elementType = fieldType.getTypeParameters().get(0);
				if (this.pluginContext.partialCopyableInterface.isAssignableFrom(elementType)) {
					final JForEach forLoop = this.pluginContext.loop(currentBlock, fieldRef, elementType, newField, elementType);
					forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.pluginContext.castOnDemand(elementType, forLoop.var().invoke(this.pluginContext.copyMethodName).arg(fieldPathVar).arg(this.propertyTreeUseParam))));
				} else if (this.pluginContext.copyableInterface.isAssignableFrom(elementType)) {
					final JForEach forLoop = this.pluginContext.loop(currentBlock, fieldRef, elementType, newField, elementType);
					forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.pluginContext.castOnDemand(elementType, forLoop.var().invoke(this.pluginContext.copyMethodName))));
				} else if (this.pluginContext.cloneableInterface.isAssignableFrom(elementType)) {
					final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(currentBlock, elementType);
					final JForEach forLoop = this.pluginContext.loop(maybeTryBlock, fieldRef, elementType, newField, elementType);
					forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.pluginContext.castOnDemand(elementType, forLoop.var().invoke(this.pluginContext.cloneMethodName))));
				} else {
					currentBlock.assign(newField, nullSafe(fieldRef, this.pluginContext.newArrayList(elementType).arg(fieldRef)));
				}

				this.pluginContext.generateImmutableFieldInit(body, targetInstanceVar, field);

			} else if (this.pluginContext.partialCopyableInterface.isAssignableFrom(fieldType)) {
				currentBlock.assign(newField, nullSafe(fieldRef, this.pluginContext.castOnDemand(fieldType, fieldRef.invoke(this.pluginContext.copyMethodName).arg(fieldPathVar).arg(this.propertyTreeUseParam))));
			} else if (this.pluginContext.copyableInterface.isAssignableFrom(fieldType)) {
				currentBlock.assign(newField, nullSafe(fieldRef, this.pluginContext.castOnDemand(fieldType, fieldRef.invoke(this.pluginContext.copyMethodName))));
			} else if (this.pluginContext.cloneableInterface.isAssignableFrom(fieldType)) {
				final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(currentBlock, fieldType);
				maybeTryBlock.assign(newField, nullSafe(fieldRef, this.pluginContext.castOnDemand(fieldType, fieldRef.invoke(this.pluginContext.cloneMethodName))));
			} else {
				currentBlock.assign(newField, fieldRef);
			}
		} else {
			currentBlock.assign(newField, fieldRef);
		}
	}


	JExpression getIncludeCondition(final JVar fieldPathVar) {
		return JOp.cond(
				this.propertyTreeUseParam.eq(this.pluginContext.includeConst),
				fieldPathVar.ne(JExpr._null()),
				fieldPathVar.eq(JExpr._null()).cor(fieldPathVar.invoke("isLeaf").not())
		);
	}



	JVar generatePropertyTreeVarDeclaration(final JBlock body, final JFieldVar field) {
		return body.decl(JMod.FINAL, this.pluginContext.codeModel._ref(PropertyTree.class), field.name() + "PropertyTree", JOp.cond(this.propertyTreeParam.eq(JExpr._null()), JExpr._null(), this.propertyTreeParam.invoke("get").arg(JExpr.lit(field.name()))));
	}
}
