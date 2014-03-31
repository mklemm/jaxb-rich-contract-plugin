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

import com.kscs.util.jaxb.PathCloneable;
import com.kscs.util.jaxb.PropertyPath;
import com.sun.codemodel.*;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * XJC Plugin to generate clone and partial clone methods
 */
public class DeepClonePlugin extends Plugin {
	private boolean throwCloneNotSupported = false;
	private boolean generatePartialCloneMethod = true;
	private boolean generateTools = true;
	private boolean generateConstructor = true;
	private boolean narrow = true;

	private final ResourceBundle resources;

	public DeepClonePlugin() {
		this.resources = ResourceBundle.getBundle(DeepClonePlugin.class.getName());
	}

	private String getMessage(final String key, final Object... params) {
		return MessageFormat.format(this.resources.getString(key), params);
	}


	public boolean isThrowCloneNotSupported() {
		return this.throwCloneNotSupported;
	}

	@Override
	public String getOptionName() {
		return "Xclone";
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		PluginUtil.Arg<Boolean> arg = PluginUtil.parseBooleanArgument("clone-throws", this.throwCloneNotSupported, opt, args, i);
		this.throwCloneNotSupported = arg.getValue();
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("partial-clone", this.generatePartialCloneMethod, opt, args, i);
			this.generatePartialCloneMethod = arg.getValue();
		}
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("generate-tools", this.generateTools, opt, args, i);
			this.generateTools = arg.getValue();
		}
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("copy-constructor", this.generateConstructor, opt, args, i);
			this.generateConstructor = arg.getValue();
		}
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("narrow", this.narrow, opt, args, i);
			this.narrow = arg.getValue();
		}
		return arg.getArgsParsed();
	}

	@Override
	public String getUsage() {
		return " -Xclone: "+getMessage("clonePlugin.usage")
				+ "\n" + getMessage("clonePlugin.usage.options")
				+ "\n\t-clone-throws={y|n}:\n\t\t" + getMessage("clonePlugin.usage.cloneThrows")
				+ "\n\n\t-partial-clone={y|n}:\n\t\t" + getMessage("clonePlugin.usage.partialClone")
				+ "\n\n\t-copy-constructor={y|n}:\n\t\t" + getMessage("clonePlugin.usage.copyConstructor")
				+ "\n\n\t-generate-tools={y|n}:\n\t\t" + getMessage("clonePl")
				+ "\n\n\t-narrow:\n\t\t" + getMessage("clonePlugin.usage.narrow");
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final ApiConstructs apiConstructs = new ApiConstructs(outline,  opt);

		if (this.generatePartialCloneMethod && this.generateTools) {
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, PathCloneable.class.getName());
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, PropertyPath.class.getName());
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			classOutline.implClass._implements(Cloneable.class);
			if (this.generatePartialCloneMethod) {
				classOutline.implClass._implements(PathCloneable.class);
			}
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			generateCloneMethod(apiConstructs, classOutline);
			if (this.generatePartialCloneMethod) {
				generatePartialCloneMethod(apiConstructs, classOutline);
			}
			if(this.generateConstructor) {
				generateDefaultConstructor(classOutline.implClass);
				generateCopyConstructor(apiConstructs, classOutline);
				if(this.generatePartialCloneMethod) {
					generatePartialCopyConstructor(apiConstructs, classOutline);
				}
			}
		}
		return true;

	}

	private void generateCloneMethod(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;

		final boolean mustCatch = mustCatch(apiConstructs, classOutline);

		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, "clone");
		cloneMethod.annotate(Override.class);

		final JBlock outer;
		final JBlock body;
		final JTryBlock tryBlock;
		if (this.throwCloneNotSupported) {
			cloneMethod._throws(CloneNotSupportedException.class);
		}
		if (this.throwCloneNotSupported || !mustCatch) {
			outer = cloneMethod.body();
			tryBlock = null;
			body = outer;
		} else {
			outer = cloneMethod.body();
			tryBlock = outer._try();
			body = tryBlock.body();
		}

		final JVar newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke("clone")));
		for (final JFieldVar field : definedClass.fields().values()) {
			if (field.type().isReference()) {
				final JClass fieldType = (JClass) field.type();
				final JFieldRef newField = JExpr.ref(newObjectVar, field);
				final JFieldRef fieldRef = JExpr._this().ref(field);
				if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
					final JClass elementType = fieldType.getTypeParameters().get(0);
					final JConditional ifNull = body._if(fieldRef.eq(JExpr._null()));
					ifNull._then().assign(newField, JExpr._null());
					ifNull._else().assign(newField, JExpr._new(apiConstructs.arrayListClass.narrow(elementType)).arg(JExpr._this().ref(field).invoke("size")));
					final JForEach forLoop = ifNull._else().forEach(elementType, "item", fieldRef);
					if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
						final JConditional ifStmt = forLoop.body()._if(forLoop.var().eq(JExpr._null()));
						ifStmt._then().invoke(newField, "add").arg(JExpr._null());
						ifStmt._else().invoke(newField, "add").arg(apiConstructs.castOnDemand(elementType, forLoop.var().invoke("clone")));
					} else {
						forLoop.body().invoke(newField, "add").arg(forLoop.var());
					}
				}
				if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
					final JConditional ifStmt = body._if(fieldRef.eq(JExpr._null()));
					ifStmt._then().assign(newField, JExpr._null());
					ifStmt._else().assign(newField, apiConstructs.castOnDemand(fieldType, JExpr._this().ref(field).invoke("clone")));
				} else {
					// body.assign(newField, JExpr._this().ref(field));
				}
			}
		}
		body._return(newObjectVar);

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(Exception.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}
	}

	private void generatePartialCloneMethod(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;
		final boolean mustCatch = mustCatch(apiConstructs, classOutline);

		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, "clone");
		final JVar clonePathParam = cloneMethod.param(JMod.FINAL, PropertyPath.class, "path");
		cloneMethod.annotate(Override.class);

		final JBlock outer;
		final JBlock body;
		final JTryBlock tryBlock;
		if (this.throwCloneNotSupported) {
			cloneMethod._throws(CloneNotSupportedException.class);
		}
		if (this.throwCloneNotSupported || !mustCatch) {
			outer = cloneMethod.body();
			tryBlock = null;
			body = outer;
		} else {
			outer = cloneMethod.body();
			tryBlock = outer._try();
			body = tryBlock.body();
		}
		JBlock currentBlock = body;
		final JVar newObjectVar;
		if (definedClass._extends() != null && apiConstructs.cloneableInterface.isAssignableFrom(definedClass._extends())) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke("clone").arg(clonePathParam)));
		} else {
			newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", null);
			final JTryBlock newObjectTry = currentBlock._try();
			final JBlock tryBody = newObjectTry.body();
			tryBody.assign(newObjectVar, JExpr.invoke("getClass").invoke("newInstance"));
			final JCatchBlock newObjectCatch = newObjectTry._catch((JClass)apiConstructs.codeModel._ref(Exception.class));
			final JVar exceptionVar = newObjectCatch.param("x");
			newObjectCatch.body()._throw(JExpr._new(apiConstructs.codeModel._ref(RuntimeException.class)).arg(exceptionVar));
		}

		for (final JFieldVar field : definedClass.fields().values()) {
			if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
				final JFieldRef newField = JExpr.ref(newObjectVar, field);
				final JFieldRef fieldRef = JExpr._this().ref(field);
				final JVar fieldPathVar = body.decl(JMod.FINAL, apiConstructs.codeModel._ref(PropertyPath.class), field.name() + "ClonePath", clonePathParam.invoke("get").arg(JExpr.lit(field.name())));
				final JExpression includesInvoke = fieldPathVar.invoke("includes");
				final JConditional ifHasClonePath = body._if(includesInvoke);
				currentBlock = ifHasClonePath._then();
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						final JConditional ifNull = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifNull._then().assign(newField, JExpr._new(apiConstructs.arrayListClass.narrow(elementType)).arg(fieldRef.invoke("size")));
						ifNull._else().assign(newField, JExpr._null());
						final JForEach forLoop = ifNull._then().forEach(elementType, "item", fieldRef);
						if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
							final JConditional ifStmt = forLoop.body()._if(forLoop.var().eq(JExpr._null()));
							ifStmt._then().invoke(newField, "add").arg(JExpr._null());
							ifStmt._else().invoke(newField, "add").arg(apiConstructs.castOnDemand(elementType, forLoop.var().invoke("clone").arg(fieldPathVar)));
						} else {
							forLoop.body().invoke(newField, "add").arg(forLoop.var());
						}
					} else if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
						final JConditional ifStmt = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifStmt._then().assign(newField, apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone").arg(fieldPathVar)));
						ifStmt._else().assign(newField, JExpr._null());
					} else if (apiConstructs.codeModel.ref(Cloneable.class).isAssignableFrom(fieldType)) {
						final JConditional ifStmt = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifStmt._then().assign(newField, apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone")));
						ifStmt._else().assign(newField, JExpr._null());
					} else {
						currentBlock.assign(newField, fieldRef);
					}
				} else {
					currentBlock.assign(newField, fieldRef);
				}
			}
		}
		body._return(newObjectVar);

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}

	}



	private void generateDefaultConstructor(final JDefinedClass definedClass) {
		final JMethod defaultConstructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		defaultConstructor.body().directStatement("// " + getMessage("defaultConstructor.bodyComment"));
		defaultConstructor.javadoc().append(getMessage("defaultConstructor.javadoc.desc"));
	}

	private void generateCopyConstructor(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;
		final JMethod constructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar otherParam = constructor.param(JMod.FINAL, definedClass, "other");
		final JDocComment docComment = constructor.javadoc();
		docComment.append(getMessage("copyConstructor.javadoc.desc", definedClass.name()));
		docComment.addParam(otherParam).append(getMessage("copyConstructor.javadoc.param.other", definedClass.name()));

		if(classOutline.getSuperClass() != null) {
			constructor.body().invoke("super").arg(otherParam);
		}

		final boolean mustCatch = mustCatch(apiConstructs, classOutline);

		final JBlock outer = constructor.body();
		final JTryBlock tryBlock;
		final JBlock body;
		if(mustCatch) {
			tryBlock = outer._try();
			body = tryBlock.body();
		} else {
			tryBlock = null;
			body = outer.block();
		}

		final JExpression newObjectVar = JExpr._this();
		for (final JFieldVar field : definedClass.fields().values()) {
			if (field.type().isReference()) {
				final JClass fieldType = (JClass) field.type();
				final JFieldRef newField = JExpr.ref(newObjectVar, field);
				final JFieldRef fieldRef = otherParam.ref(field);
				if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
					final JClass elementType = fieldType.getTypeParameters().get(0);
					final JConditional ifNull = body._if(fieldRef.eq(JExpr._null()));
					ifNull._then().assign(newField, JExpr._null());
					ifNull._else().assign(newField, JExpr._new(apiConstructs.arrayListClass.narrow(elementType)).arg(fieldRef.invoke("size")));
					final JForEach forLoop = ifNull._else().forEach(elementType, "item", fieldRef);
					if (this.narrow && apiConstructs.canInstantiate(elementType)) {
						final JConditional ifStmt = forLoop.body()._if(forLoop.var().eq(JExpr._null()));
						ifStmt._then().invoke(newField, "add").arg(JExpr._null());
						ifStmt._else().invoke(newField, "add").arg(JExpr._new(elementType).arg(forLoop.var()));
					} else if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
						final JConditional ifStmt = forLoop.body()._if(forLoop.var().eq(JExpr._null()));
						ifStmt._then().invoke(newField, "add").arg(JExpr._null());
						final JInvocation cloneInvocation = forLoop.var().invoke("clone");
						ifStmt._else().invoke(newField, "add").arg(apiConstructs.castOnDemand(elementType, cloneInvocation));
					} else {
						forLoop.body().invoke(newField, "add").arg(forLoop.var());
					}

					if(apiConstructs.hasPlugin(ImmutablePlugin.class)) {
						body.assign(newField, apiConstructs.unmodifiableList(newField));
					}
				} else if (this.narrow && apiConstructs.canInstantiate(fieldType)) {
					final JConditional ifStmt = body._if(fieldRef.eq(JExpr._null()));
					ifStmt._then().assign(newField, JExpr._null());
					ifStmt._else().assign(newField, JExpr._new(fieldType).arg(fieldRef));
				} else if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
					final JConditional ifStmt = body._if(fieldRef.eq(JExpr._null()));
					ifStmt._then().assign(newField, JExpr._null());
					ifStmt._else().assign(newField, apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone")));
				} else {
					body.assign(newField, fieldRef);
				}
			}
		}

		if(tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(Exception.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}
	}

	private void generatePartialCopyConstructor(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;
		final boolean mustCatch = mustCatch(apiConstructs, classOutline);
		final JMethod constructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar otherParam = constructor.param(JMod.FINAL, definedClass, "other");
		final JVar pathParam = constructor.param(JMod.FINAL, PropertyPath.class, "propertyPath");

		final JDocComment docComment = constructor.javadoc();
		docComment.append(getMessage("copyConstructor.javadoc.desc", definedClass.name()));
		docComment.addParam(otherParam).append(getMessage("copyConstructor.javadoc.param.other", definedClass.name()));

		if(classOutline.getSuperClass() != null) {
			constructor.body().invoke("super").arg(otherParam).arg(pathParam);
		}

		final JBlock outer;
		final JBlock body;
		final JTryBlock tryBlock;
		if (!mustCatch) {
			outer = constructor.body();
			tryBlock = null;
			body = outer;
		} else {
			outer = constructor.body();
			tryBlock = outer._try();
			body = tryBlock.body();
		}
		JBlock currentBlock = body;
		final JExpression newObjectVar = JExpr._this();
		for (final JFieldVar field : definedClass.fields().values()) {
			if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
				final JFieldRef newField = JExpr.ref(newObjectVar, field);
				final JFieldRef fieldRef = otherParam.ref(field);
				final JVar fieldPathVar = body.decl(JMod.FINAL, apiConstructs.codeModel._ref(PropertyPath.class), field.name() + "ClonePath", pathParam.invoke("get").arg(JExpr.lit(field.name())));
				final JExpression includesInvoke = fieldPathVar.invoke("includes");
				final JConditional ifHasClonePath = body._if(includesInvoke);
				currentBlock = ifHasClonePath._then();
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						final JConditional ifNull = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifNull._then().assign(newField, JExpr._new(apiConstructs.arrayListClass.narrow(elementType)).arg(fieldRef.invoke("size")));
						ifNull._else().assign(newField, JExpr._null());
						final JForEach forLoop = ifNull._then().forEach(elementType, "item", fieldRef);
						if (this.narrow && apiConstructs.canInstantiate(elementType)) {
							final JConditional ifStmt = forLoop.body()._if(forLoop.var().eq(JExpr._null()));
							ifStmt._then().invoke(newField, "add").arg(JExpr._null());
							ifStmt._else().invoke(newField, "add").arg(JExpr._new(elementType).arg(forLoop.var()).arg(fieldPathVar));
						} else if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
							final JConditional ifStmt = forLoop.body()._if(forLoop.var().eq(JExpr._null()));
							ifStmt._then().invoke(newField, "add").arg(JExpr._null());
							ifStmt._else().invoke(newField, "add").arg(apiConstructs.castOnDemand(elementType, forLoop.var().invoke("clone")));
						} else {
							forLoop.body().invoke(newField, "add").arg(forLoop.var());
						}
					} else if (this.narrow && apiConstructs.canInstantiate(fieldType)) {
						final JConditional ifStmt = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifStmt._then().assign(newField, JExpr._new(fieldType).arg(fieldRef).arg(pathParam));
						ifStmt._else().assign(newField, JExpr._null());
					} else if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
						final JConditional ifStmt = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifStmt._then().assign(newField, apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone")));
						ifStmt._else().assign(newField, JExpr._null());
					} else {
						currentBlock.assign(newField, fieldRef);
					}
				} else {
					currentBlock.assign(newField, fieldRef);
				}
			}
		}
		body._return(newObjectVar);

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(Exception.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}

	}

	private boolean mustCatch(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
			final JDefinedClass definedClass = classOutline.implClass;
			boolean mustCatch = false;
			for (final JFieldVar field : definedClass.fields().values()) {
				if (!apiConstructs.canInstantiate(field.type()) && !field.type().isPrimitive()) {
					final JClass fieldType = (JClass) field.type();
					if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						mustCatch |= apiConstructs.cloneThrows(elementType);
					} else {
						mustCatch |= apiConstructs.cloneThrows(fieldType);
					}
				}
			}
			return mustCatch;
		}



}
