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

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.plugins.xjc.base.PluginUtil;
import com.sun.codemodel.JAssignmentTarget;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

import static com.kscs.util.plugins.xjc.base.PluginUtil.nullSafe;
/**
 * @author Mirko Klemm 2015-03-17
 */
public class DeepCopyGenerator {
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(DeepCopyGenerator.class.getName());
	public static final String OTHER_PARAM_NAME = "_other";
	private final PluginContext pluginContext;
	private final ClassOutline classOutline;

	public DeepCopyGenerator(final PluginContext pluginContext, final ClassOutline classOutline) {
		this.pluginContext = pluginContext;
		this.classOutline = classOutline;
	}

	public void generateFieldCopyExpression(final CopyGenerator cloneGenerator, final JBlock body, final JExpression targetObject, final JFieldVar field, final JAssignmentTarget targetField, final JExpression sourceField, final FieldOutline fieldOutline) {
		final PropertyTreeVarGenerator treeVarGenerator = cloneGenerator.createPropertyTreeVarGenerator(body, field.name());
		final JBlock currentBlock = treeVarGenerator.generateEnclosingBlock(body);
		if (field.type().isReference()) {
			final JClass fieldType = (JClass) field.type();
			if (this.pluginContext.collectionClass.isAssignableFrom(fieldType)) {
				final JClass elementType = fieldType.getTypeParameters().get(0);
				if (this.pluginContext.partialCopyableInterface.isAssignableFrom(elementType)) {
					final JForEach forLoop = this.pluginContext.loop(currentBlock, sourceField, elementType, targetField, elementType, fieldOutline);
					forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), this.pluginContext.castOnDemand(elementType, treeVarGenerator.generatePartialArgs(forLoop.var().invoke(this.pluginContext.copyMethodName)))));
				} else if (this.pluginContext.copyableInterface.isAssignableFrom(elementType)) {
					final JForEach forLoop = this.pluginContext.loop(currentBlock, sourceField, elementType, targetField, elementType, fieldOutline);
					forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), this.pluginContext.castOnDemand(elementType, forLoop.var().invoke(this.pluginContext.copyMethodName))));
				} else if (this.pluginContext.cloneableInterface.isAssignableFrom(elementType)) {
					final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(currentBlock, elementType);
					final JForEach forLoop = this.pluginContext.loop(maybeTryBlock, sourceField, elementType, targetField, elementType, fieldOutline);
					forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), this.pluginContext.castOnDemand(elementType, forLoop.var().invoke(this.pluginContext.cloneMethodName))));
				} else {
					currentBlock.assign(targetField, nullSafe(sourceField, this.pluginContext.newArrayList(PluginContext.extractMutableListClass(fieldOutline), elementType).arg(sourceField)));
				}

				this.pluginContext.generateImmutableFieldInit(body, targetObject, field);

			} else if (this.pluginContext.partialCopyableInterface.isAssignableFrom(fieldType)) {
				currentBlock.assign(targetField, nullSafe(sourceField, this.pluginContext.castOnDemand(fieldType, treeVarGenerator.generatePartialArgs(sourceField.invoke(this.pluginContext.copyMethodName)))));
			} else if (this.pluginContext.copyableInterface.isAssignableFrom(fieldType)) {
				currentBlock.assign(targetField, nullSafe(sourceField, this.pluginContext.castOnDemand(fieldType, sourceField.invoke(this.pluginContext.copyMethodName))));
			} else if (this.pluginContext.cloneableInterface.isAssignableFrom(fieldType)) {
				final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(currentBlock, fieldType);
				maybeTryBlock.assign(targetField, nullSafe(sourceField, this.pluginContext.castOnDemand(fieldType, sourceField.invoke(this.pluginContext.cloneMethodName))));
			} else {
				currentBlock.assign(targetField, sourceField);
			}
		} else {
			currentBlock.assign(targetField, sourceField);
		}
	}

	JMethod generateCreateCopyMethod(final boolean partial) {
		final JDefinedClass definedClass = this.classOutline.implClass;

		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, this.pluginContext.copyMethodName);
		final CopyGenerator cloneGenerator = this.pluginContext.createCopyGenerator(cloneMethod, partial);
		cloneMethod.annotate(Override.class);

		final JBlock body = cloneMethod.body();

		final JVar newObjectVar;
		final boolean superPartialCopyable = this.pluginContext.partialCopyableInterface.isAssignableFrom(definedClass._extends());
		final boolean superCopyable = this.pluginContext.copyableInterface.isAssignableFrom(definedClass._extends());
		if (superPartialCopyable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, this.pluginContext.newObjectVarName, JExpr.cast(definedClass, cloneGenerator.generatePartialArgs(this.pluginContext.invoke(JExpr._super(), this.pluginContext.copyMethodName))));
		} else if(superCopyable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, this.pluginContext.newObjectVarName, JExpr.cast(definedClass, JExpr._super().invoke(this.pluginContext.copyMethodName)));
		} else {
			newObjectVar = body.decl(JMod.FINAL, definedClass, this.pluginContext.newObjectVarName, null);
			final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(body, definedClass._extends());
			maybeTryBlock.assign(newObjectVar, JExpr.cast(definedClass, JExpr._super().invoke(this.pluginContext.cloneMethodName)));
		}
		generateFieldCopyExpressions(cloneGenerator, body, newObjectVar, JExpr._this());
		body._return(newObjectVar);
		return cloneMethod;
	}


	private void generateFieldCopyExpressions(final CopyGenerator cloneGenerator, final JBlock body, final JExpression targetObject, final JExpression sourceObject) {
		for (final FieldOutline fieldOutline : this.classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					generateFieldCopyExpression(cloneGenerator, body, targetObject, field, targetObject.ref(field.name()), sourceObject.ref(field.name()), fieldOutline);
				}
			}
		}
	}


	JMethod generateConveniencePartialCopyMethod(final JMethod cloneMethod, final String methodName, final JExpression secondParam) {
		final JDefinedClass definedClass = this.classOutline.implClass;
		final JMethod cloneExceptMethod = definedClass.method(JMod.PUBLIC, definedClass, methodName);
		final JVar propertyTreeParam = cloneExceptMethod.param(JMod.FINAL, PropertyTree.class, PartialCopyGenerator.PROPERTY_TREE_PARAM_NAME);
		cloneExceptMethod.body()._return(JExpr.invoke(cloneMethod).arg(propertyTreeParam).arg(secondParam));
		cloneExceptMethod.annotate(Override.class);
		return cloneExceptMethod;
	}

	void generateDefaultConstructor() {
		final JMethod defaultConstructor = this.classOutline.implClass.constructor( this.classOutline.implClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		defaultConstructor.body().directStatement("// " + getMessage("defaultConstructor.bodyComment"));
		defaultConstructor.javadoc().append(getMessage("defaultConstructor.javadoc.desc"));
	}


	void generateCopyConstructor(final boolean partial) {
		final JDefinedClass definedClass = this.classOutline.implClass;
		final JMethod constructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar otherParam = constructor.param(JMod.FINAL, this.classOutline.implClass, DeepCopyGenerator.OTHER_PARAM_NAME);
		final CopyGenerator cloneGenerator = this.pluginContext.createCopyGenerator(constructor, partial);

		final JDocComment docComment = constructor.javadoc();
		docComment.append(getMessage("copyConstructor.javadoc.desc", definedClass.name()));
		docComment.addParam(otherParam).append(getMessage("copyConstructor.javadoc.param.other", definedClass.name()));
		if(partial) {
			docComment.addParam(cloneGenerator.getPropertyTreeParam()).append(getMessage("copyConstructor.javadoc.param.propertyPath", definedClass.name()));
			docComment.addParam(cloneGenerator.getPropertyTreeUseParam()).append(getMessage("copyConstructor.javadoc.param.propertyPathUse", definedClass.name()));
		}

		if (this.classOutline.getSuperClass() != null) {
			constructor.body().add(cloneGenerator.generatePartialArgs(this.pluginContext._super().arg(otherParam)));
		}

		final JBlock body = constructor.body();
		generateFieldCopyExpressions(cloneGenerator, body, JExpr._this(), otherParam);
	}

	private String getMessage(final String resourceKey, final Object... args) {
		return MessageFormat.format(DeepCopyGenerator.RESOURCE_BUNDLE.getString(resourceKey), args);
	}
}
