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

import com.kscs.util.jaxb.Copyable;
import com.kscs.util.jaxb.PartialCopyable;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.kscs.util.jaxb.Selector;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.base.PluginUtil;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import static com.kscs.util.plugins.xjc.base.PluginUtil.nullSafe;

/**
 * XJC Plugin to generate copy and partial copy methods
 */
public class DeepCopyPlugin extends AbstractPlugin {
	@Opt("partial") protected boolean generatePartialCloneMethod = true;
	@Opt protected boolean generateTools = true;
	@Opt("constructor") protected boolean generateConstructor = true;
	@Opt protected boolean narrow = false;
	@Opt
	protected String selectorClassName = "Selector";
	@Opt
	protected final String rootSelectorClassName = "Select";

	@Override
	public String getOptionName() {
		return "Xcopy";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);

		if(this.generateTools) {
			apiConstructs.writeSourceFile(Copyable.class);
		}

		if (this.generatePartialCloneMethod) {
			if (this.generateTools) {
				apiConstructs.writeSourceFile(PropertyTreeUse.class);
				apiConstructs.writeSourceFile(PartialCopyable.class);
				apiConstructs.writeSourceFile(PropertyTree.class);
				apiConstructs.writeSourceFile(Selector.class);
			}
			final SelectorGenerator selectorGenerator = new SelectorGenerator(apiConstructs, Selector.class, this.selectorClassName, this.rootSelectorClassName, null, null, apiConstructs.cloneGraphClass);
			selectorGenerator.generateMetaFields();
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			classOutline.implClass._implements(Copyable.class);
			if(this.generatePartialCloneMethod) {
				classOutline.implClass._implements(PartialCopyable.class);
			}
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			generateCreateCopyMethod(apiConstructs, classOutline);
			if (this.generatePartialCloneMethod) {
				generatePartialCopyMethod(apiConstructs, classOutline);
			}
			if (this.generateConstructor) {
				generateDefaultConstructor(classOutline.implClass);
				generateCopyConstructor(apiConstructs, classOutline);
				if (this.generatePartialCloneMethod) {
					generatePartialCopyConstructor(apiConstructs, classOutline);
				}
			}
		}
		return true;

	}

	private void generateCreateCopyMethod(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;

		final JMethod copyMethod = definedClass.method(JMod.PUBLIC, definedClass, apiConstructs.copyMethodName);
		copyMethod.annotate(Override.class);

		final JBlock body = copyMethod.body();

		final boolean superCopyable = apiConstructs.codeModel.ref(Copyable.class).isAssignableFrom(definedClass._extends());
		final JVar newObjectVar;
		if(superCopyable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, apiConstructs.newObjectVarName, JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.copyMethodName)));
		} else {
			newObjectVar = body.decl(JMod.FINAL, definedClass, apiConstructs.newObjectVarName, null);
			final JBlock maybeTryBlock = apiConstructs.catchCloneNotSupported(body, definedClass._extends());
			maybeTryBlock.assign(newObjectVar, JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.cloneMethodName)));
		}

		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = JExpr._this().ref(field);
					if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						if (apiConstructs.copyableInterface.isAssignableFrom(elementType)) {
							final JForEach forLoop = apiConstructs.loop(body, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.copyMethodName))));
						} else if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
							final JBlock mayBeTryBlock = apiConstructs.catchCloneNotSupported(body, elementType);
							final JForEach forLoop = apiConstructs.loop(mayBeTryBlock, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.cloneMethodName))));
						} else {
							body.assign(newField, nullSafe(fieldRef, apiConstructs.newArrayList(elementType).arg(fieldRef)));
						}

						final ImmutablePlugin immutablePlugin = apiConstructs.findPlugin(ImmutablePlugin.class);
						if (immutablePlugin != null) {
							immutablePlugin.immutableInit(apiConstructs, body, newObjectVar, field);
						}
					}
					if (apiConstructs.copyableInterface.isAssignableFrom(fieldType)) {
						body.assign(newField, nullSafe(fieldRef, apiConstructs.castOnDemand(fieldType, JExpr._this().ref(field).invoke(apiConstructs.copyMethodName))));
					} else if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
						final JBlock maybeTryBlock = apiConstructs.catchCloneNotSupported(body, fieldType);
						maybeTryBlock.assign(newField, nullSafe(fieldRef, apiConstructs.castOnDemand(fieldType, JExpr._this().ref(field).invoke(apiConstructs.cloneMethodName))));
					} else {
						body.assign(newField, JExpr._this().ref(field));
					}
				}
			}
		}
		body._return(newObjectVar);
	}


	private void generatePartialCopyMethod(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;

		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, apiConstructs.copyMethodName);
		final PartialCopyGenerator cloneGenerator = new PartialCopyGenerator(apiConstructs, cloneMethod);
		cloneMethod.annotate(Override.class);

		final JMethod cloneExceptMethod = generateConvenienceCloneMethod(definedClass, cloneMethod, apiConstructs.copyExceptMethodName, apiConstructs.excludeConst);

		final JMethod cloneOnlyMethod = generateConvenienceCloneMethod(definedClass, cloneMethod, apiConstructs.copyOnlyMethodName, apiConstructs.includeConst);

		final JBlock body = cloneMethod.body();

		final JVar newObjectVar;
		final boolean superPartialCopyable = apiConstructs.partialCopyableInterface.isAssignableFrom(definedClass._extends());
		final boolean superCopyable = apiConstructs.copyableInterface.isAssignableFrom(definedClass._extends());
		final boolean superCloneable = apiConstructs.cloneableInterface.isAssignableFrom(definedClass._extends());
		if (superPartialCopyable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, apiConstructs.newObjectVarName, JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.copyMethodName).arg(cloneGenerator.getPropertyTreeParam()).arg(cloneGenerator.getIncludeParam())));
		} else if(superCopyable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, apiConstructs.newObjectVarName, JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.copyMethodName)));
		} else if(superCloneable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, apiConstructs.newObjectVarName, null);
			final JBlock maybeTryBlock = apiConstructs.catchCloneNotSupported(body, definedClass._extends());
			maybeTryBlock.assign(newObjectVar, JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.cloneMethodName)));
		} else {
			newObjectVar = body.decl(JMod.FINAL, definedClass, apiConstructs.newObjectVarName, null);
			final JTryBlock newObjectTry = body._try();
			final JBlock tryBody = newObjectTry.body();
			tryBody.assign(newObjectVar, JExpr.invoke("getClass").invoke("newInstance"));
			final JCatchBlock newObjectCatch = newObjectTry._catch(apiConstructs.codeModel.ref(Exception.class));
			final JVar exceptionVar = newObjectCatch.param("x");
			newObjectCatch.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}

		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					cloneGenerator.generateFieldAssignment(body, field, newObjectVar, JExpr._this());
				}
			}
		}
		body._return(newObjectVar);

	}

	private JMethod generateConvenienceCloneMethod(final JDefinedClass definedClass, final JMethod cloneMethod, final String methodName, final JExpression secondParam) {
		final JMethod cloneExceptMethod = definedClass.method(JMod.PUBLIC, definedClass, methodName);
		final JVar propertyTreeParam = cloneExceptMethod.param(JMod.FINAL, PropertyTree.class, "propertyTree");
		cloneExceptMethod.body()._return(JExpr.invoke(cloneMethod).arg(propertyTreeParam).arg(secondParam));
		cloneExceptMethod.annotate(Override.class);
		return cloneExceptMethod;
	}

	private void generateDefaultConstructor(final JDefinedClass definedClass) {
		final JMethod defaultConstructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		defaultConstructor.body().directStatement("// " + getMessage("defaultConstructor.bodyComment"));
		defaultConstructor.javadoc().append(getMessage("defaultConstructor.javadoc.desc"));
	}

	private void generateCopyConstructor(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final ImmutablePlugin immutablePlugin = apiConstructs.findPlugin(ImmutablePlugin.class);
		final JDefinedClass definedClass = classOutline.implClass;
		generateCopyConstructor(apiConstructs, classOutline, definedClass, immutablePlugin);
	}

	private void generatePartialCopyConstructor(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final ImmutablePlugin immutablePlugin = apiConstructs.findPlugin(ImmutablePlugin.class);
		final JDefinedClass definedClass = classOutline.implClass;
		generatePartialCopyConstructor(apiConstructs, classOutline, definedClass, immutablePlugin);
	}

	void generateCopyConstructor(final ApiConstructs apiConstructs, final ClassOutline classOutline, final JDefinedClass definedClass, final ImmutablePlugin immutablePlugin) {
		final JMethod constructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar otherParam = constructor.param(JMod.FINAL, classOutline.implClass, "other");
		final JDocComment docComment = constructor.javadoc();
		docComment.append(getMessage("copyConstructor.javadoc.desc", definedClass.name()));
		docComment.addParam(otherParam).append(getMessage("copyConstructor.javadoc.param.other", definedClass.name()));

		if (classOutline.getSuperClass() != null) {
			constructor.body().invoke("super").arg(otherParam);
		}
		final JBlock body = constructor.body();
		final JExpression newObjectVar = JExpr._this();
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = otherParam.ref(field);
					if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						if (this.narrow && apiConstructs.canInstantiate(elementType)) {
							final JForEach forLoop = apiConstructs.loop(body, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), JExpr._new(elementType).arg(forLoop.var())));
						} else if(apiConstructs.copyableInterface.isAssignableFrom(elementType)) {
							final JForEach forLoop = apiConstructs.loop(body, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.copyMethodName))));
						} else if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
							final JBlock mayBeTryBlock = apiConstructs.catchCloneNotSupported(body, elementType);
							final JForEach forLoop = apiConstructs.loop(mayBeTryBlock, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.cloneMethodName))));
						} else {
							body.assign(newField, nullSafe(fieldRef, apiConstructs.newArrayList(elementType).arg(fieldRef)));
						}

						if (immutablePlugin != null) {
							immutablePlugin.immutableInit(apiConstructs, body, JExpr._this(), field);
						}

					} else if (this.narrow && apiConstructs.canInstantiate(fieldType)) {
						body.assign(newField, nullSafe(fieldRef, JExpr._new(fieldType).arg(fieldRef)));
					} else if (apiConstructs.copyableInterface.isAssignableFrom(fieldType)) {
						body.assign(newField, nullSafe(fieldRef, apiConstructs.castOnDemand(fieldType, fieldRef.invoke(apiConstructs.copyMethodName))));
					} else if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
						final JBlock maybeTryBlock = apiConstructs.catchCloneNotSupported(body, fieldType);
						maybeTryBlock.assign(newField, nullSafe(fieldRef, apiConstructs.castOnDemand(fieldType, fieldRef.invoke(apiConstructs.cloneMethodName))));
					} else {
						body.assign(newField, fieldRef);
					}
				}
			}
		}

	}

	void generatePartialCopyConstructor(final ApiConstructs apiConstructs, final ClassOutline classOutline, final JDefinedClass definedClass, final ImmutablePlugin immutablePlugin) {
		final JMethod constructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar otherParam = constructor.param(JMod.FINAL, classOutline.implClass, "other");
		final PartialCopyGenerator cloneGenerator = new PartialCopyGenerator(apiConstructs, constructor);

		final JDocComment docComment = constructor.javadoc();
		docComment.append(getMessage("copyConstructor.javadoc.desc", definedClass.name()));
		docComment.addParam(otherParam).append(getMessage("copyConstructor.javadoc.param.other", definedClass.name()));
		docComment.addParam(cloneGenerator.getPropertyTreeParam()).append(getMessage("copyConstructor.javadoc.param.propertyPath", definedClass.name()));

		if (classOutline.getSuperClass() != null) {
			constructor.body().invoke("super").arg(otherParam).arg(cloneGenerator.getPropertyTreeParam()).arg(cloneGenerator.getIncludeParam());
		}

		final JBlock body = constructor.body();
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					cloneGenerator.generateFieldAssignment(body, field, JExpr._this(), otherParam);
				}
			}
		}
	}

}
