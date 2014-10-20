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

import com.kscs.util.jaxb.*;
import com.sun.codemodel.*;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static com.kscs.util.plugins.xjc.PluginUtil.nullSafe;

/**
 * XJC Plugin to generate copy and partial copy methods
 */
public class DeepCopyPlugin extends Plugin {
	boolean generatePartialCloneMethod = true;
	boolean generateTransformer = true;
	private boolean generateTools = true;
	private boolean generateConstructor = true;
	private boolean narrow = false;

	private final ResourceBundle resources;

	public DeepCopyPlugin() {
		this.resources = ResourceBundle.getBundle(DeepCopyPlugin.class.getName());
	}

	private String getMessage(final String key, final Object... params) {
		return MessageFormat.format(this.resources.getString(key), params);
	}


	@Override
	public String getOptionName() {
		return "Xcopy";
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		PluginUtil.Arg<Boolean> arg = PluginUtil.parseBooleanArgument("copy-partial", this.generatePartialCloneMethod, opt, args, i);
		this.generatePartialCloneMethod = arg.getValue();
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("copy-generate-tools", this.generateTools, opt, args, i);
			this.generateTools = arg.getValue();
		}
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("copy-constructor", this.generateConstructor, opt, args, i);
			this.generateConstructor = arg.getValue();
		}
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("copy-narrow", this.narrow, opt, args, i);
			this.narrow = arg.getValue();
		}
		return arg.getArgsParsed();
	}

	@Override
	public String getUsage() {
		return new PluginUsageBuilder(this.resources, "usage")
				.addMain("copy")
				.addOption("copy-partial", this.generatePartialCloneMethod)
				.addOption("copy-constructor", this.generateConstructor)
				.addOption("copy-generate-tools", this.generateTools)
				.addOption("copy-narrow", this.narrow)
				.build();
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);


		if (this.generateTransformer) {
			if (this.generateTools) {
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, Copyable.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, PropertyTransformer.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, PropertyInfo.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, TransformerPath.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, Transformer.class.getName());
			}
			final SelectorGenerator selectorGenerator = new SelectorGenerator(apiConstructs, Transformer.class, "Transformer", "Transform", "propertyTransformer", apiConstructs.codeModel.ref(PropertyTransformer.class), apiConstructs.transformerPathClass);
			selectorGenerator.generateMetaFields();
		}

		if (this.generatePartialCloneMethod) {
			if (this.generateTools) {
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, PropertyTreeUse.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, PartialCopyable.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, PropertyTree.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, Selector.class.getName());
			}
			final SelectorGenerator selectorGenerator = new SelectorGenerator(apiConstructs, Selector.class, "Selector", "Select", null, null, apiConstructs.cloneGraphClass);
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

		final boolean mustCatch = "java.lang.Object".equals(definedClass._extends().fullName()) || apiConstructs.cloneThrows(definedClass._extends(), false) || mustCatch(apiConstructs, classOutline, new Predicate<JClass>() {
			@Override
			public boolean matches(final JClass arg) {
				return apiConstructs.cloneThrows(arg, false);
			}
		});

		final JMethod copyMethod = definedClass.method(JMod.PUBLIC, definedClass, apiConstructs.copyMethod);
		copyMethod.annotate(Override.class);

		final JBlock outer;
		final JBlock body;
		final JTryBlock tryBlock;
		if (!mustCatch) {
			outer = copyMethod.body();
			tryBlock = null;
			body = outer;
		} else {
			outer = copyMethod.body();
			tryBlock = outer._try();
			body = tryBlock.body();
		}

		final boolean superCopyable = apiConstructs.codeModel.ref(Copyable.class).isAssignableFrom(definedClass._extends());

		final JVar newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke(superCopyable ? apiConstructs.copyMethod : apiConstructs.cloneMethod)));
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
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.copyMethod))));
						} else if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
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

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}
	}


	private void generatePartialCopyMethod(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;
		final boolean mustCatch = mustCatch(apiConstructs, classOutline, new Predicate<JClass>() {
			@Override
			public boolean matches(final JClass fieldType) {
				return (!apiConstructs.partialCopyableInterface.isAssignableFrom(fieldType)) && apiConstructs.cloneThrows(fieldType, false);
			}
		});
		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, apiConstructs.copyMethod);
		final PartialCopyGenerator cloneGenerator = new PartialCopyGenerator(apiConstructs, cloneMethod);
		cloneMethod.annotate(Override.class);

		final JMethod cloneExceptMethod = generateConvenienceCloneMethod(definedClass, cloneMethod, apiConstructs.copyExceptMethod, apiConstructs.excludeConst);

		final JMethod cloneOnlyMethod = generateConvenienceCloneMethod(definedClass, cloneMethod, apiConstructs.copyOnlyMethod, apiConstructs.includeConst);

		final JBlock outer;
		final JBlock body;
		final JTryBlock tryBlock;
		if (!mustCatch) {
			outer = cloneMethod.body();
			tryBlock = null;
			body = outer;
		} else {
			outer = cloneMethod.body();
			tryBlock = outer._try();
			body = tryBlock.body();
		}
		//JBlock currentBlock = body;
		final JVar newObjectVar;
		final boolean superPartialCopyable = apiConstructs.partialCopyableInterface.isAssignableFrom(definedClass._extends());
		final boolean superCopyable = apiConstructs.copyableInterface.isAssignableFrom(definedClass._extends());
		final boolean superCloneable = apiConstructs.cloneableInterface.isAssignableFrom(definedClass._extends());
		if (superPartialCopyable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.copyMethod).arg(cloneGenerator.getPropertyTreeParam()).arg(cloneGenerator.getIncludeParam())));
		} else if(superCopyable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.copyMethod)));
		} else if(superCloneable) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.cloneMethod)));
		} else {
			newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", null);
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

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}

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
		generateGraphCopyConstructor(apiConstructs, classOutline, definedClass, immutablePlugin);
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
		final JBlock outer = constructor.body();
		final JTryBlock tryBlock;
		final JBlock body;

		final boolean mustCatch = mustCatch(apiConstructs, classOutline, new Predicate<JClass>() {
			@Override
			public boolean matches(final JClass fieldType) {
				return (!apiConstructs.canInstantiate(fieldType)) && apiConstructs.cloneThrows(fieldType, false);
			}
		});

		if (mustCatch) {
			tryBlock = outer._try();
			body = tryBlock.body();
		} else {
			tryBlock = null;
			body = outer.block();
		}

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
						} else if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
							final JForEach forLoop = apiConstructs.loop(body, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.cloneMethod))));
						} else {
							body.assign(newField, nullSafe(fieldRef, apiConstructs.newArrayList(elementType).arg(fieldRef)));
						}

						if (immutablePlugin != null) {
							immutablePlugin.immutableInit(apiConstructs, body, JExpr._this(), field);
						}

					} else if (this.narrow && apiConstructs.canInstantiate(fieldType)) {
						body.assign(newField, nullSafe(fieldRef, JExpr._new(fieldType).arg(fieldRef)));
					} else if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
						body.assign(newField, nullSafe(fieldRef, apiConstructs.castOnDemand(fieldType, fieldRef.invoke(apiConstructs.cloneMethod))));
					} else {
						body.assign(newField, fieldRef);
					}
				}
			}
		}

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}
	}


	void generateGraphCopyConstructor(final ApiConstructs apiConstructs, final ClassOutline classOutline, final JDefinedClass definedClass, final ImmutablePlugin immutablePlugin) {
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

		final JBlock outer;
		final JBlock body;
		final JTryBlock tryBlock;

		final boolean mustCatch = mustCatch(apiConstructs, classOutline, new Predicate<JClass>() {
			@Override
			public boolean matches(final JClass fieldType) {
				return (!apiConstructs.canInstantiate(fieldType)) && (!apiConstructs.partialCopyableInterface.isAssignableFrom(fieldType)) && apiConstructs.cloneThrows(fieldType, false);
			}
		});

		if (!mustCatch) {
			outer = constructor.body();
			tryBlock = null;
			body = outer;
		} else {
			outer = constructor.body();
			tryBlock = outer._try();
			body = tryBlock.body();
		}
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					cloneGenerator.generateFieldAssignment(body, field, JExpr._this(), otherParam);
				}
			}
		}

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}

	}

	void generateTransformingConstructor(final ApiConstructs apiConstructs, final ClassOutline classOutline, final JDefinedClass definedClass, final ImmutablePlugin immutablePlugin) {
		final JMethod constructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar otherParam = constructor.param(JMod.FINAL, classOutline.implClass, "other");
		final JVar transformerPathParam = constructor.param(JMod.FINAL, TransformerPath.class, "transformerPath");

		final JDocComment docComment = constructor.javadoc();
		docComment.append(getMessage("copyConstructor.javadoc.desc", definedClass.name()));
		docComment.addParam(otherParam).append(getMessage("copyConstructor.javadoc.param.other", definedClass.name()));
		docComment.addParam(transformerPathParam).append(getMessage("copyConstructor.javadoc.param.transformerPath", definedClass.name()));

		if (classOutline.getSuperClass() != null) {
			constructor.body().invoke("super").arg(otherParam).arg(transformerPathParam);
		}

		final JBlock outer;
		final JBlock body;
		final JTryBlock tryBlock;

		final boolean mustCatch = mustCatch(apiConstructs, classOutline, new Predicate<JClass>() {
			@Override
			public boolean matches(final JClass fieldType) {
				return (!apiConstructs.canInstantiate(fieldType)) && (!apiConstructs.partialCopyableInterface.isAssignableFrom(fieldType)) && apiConstructs.cloneThrows(fieldType, false);
			}
		});

		if (!mustCatch) {
			outer = constructor.body();
			tryBlock = null;
			body = outer;
		} else {
			outer = constructor.body();
			tryBlock = outer._try();
			body = tryBlock.body();
		}
		JBlock currentBlock;
		final JExpression newObjectVar = JExpr._this();
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = otherParam.ref(field);
					final JVar fieldPathVar = body.decl(JMod.FINAL, apiConstructs.codeModel._ref(TransformerPath.class), field.name() + "TransformerPath", transformerPathParam.invoke("get").arg(JExpr.lit(field.name())));
					currentBlock = body;
					if (field.type().isReference()) {
						final JClass fieldType = (JClass) field.type();
						if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
							final JClass elementType = fieldType.getTypeParameters().get(0);
							final JInvocation includesInvoke = createTransformerInvocation(apiConstructs, fieldOutline, definedClass, otherParam, transformerPathParam);

							if (this.narrow && apiConstructs.canInstantiate(elementType)) {
								final JForEach forLoop = apiConstructs.loop(currentBlock, fieldRef, elementType, newField, elementType);
								forLoop.body().invoke(newField, "add").arg(includesInvoke.arg(otherParam).arg(nullSafe(forLoop.var(), JExpr._new(elementType).arg(forLoop.var()).arg(fieldPathVar))));
							} else if (apiConstructs.partialCopyableInterface.isAssignableFrom(elementType)) {
								final JForEach forLoop = apiConstructs.loop(currentBlock, fieldRef, elementType, newField, elementType);
								forLoop.body().invoke(newField, "add").arg(includesInvoke.arg(otherParam).arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.copyMethod).arg(fieldPathVar)))));
							} else if (apiConstructs.copyableInterface.isAssignableFrom(elementType)) {
								final JForEach forLoop = apiConstructs.loop(currentBlock, fieldRef, elementType, newField, elementType);
								forLoop.body().invoke(newField, "add").arg(includesInvoke.arg(otherParam).arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.copyMethod)))));
							} else if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
								final JForEach forLoop = apiConstructs.loop(currentBlock, fieldRef, elementType, newField, elementType);
								forLoop.body().invoke(newField, "add").arg(includesInvoke.arg(otherParam).arg(nullSafe(forLoop.var(), apiConstructs.castOnDemand(elementType, forLoop.var().invoke(apiConstructs.cloneMethod)))));
							} else {
								currentBlock.assign(newField, includesInvoke.arg(otherParam).arg(nullSafe(fieldRef, apiConstructs.newArrayList(elementType).arg(fieldRef))));
							}
							if (immutablePlugin != null) {
								immutablePlugin.immutableInit(apiConstructs, body, JExpr._this(), field);
							}

						} else if (this.narrow && apiConstructs.canInstantiate(fieldType)) {
							currentBlock.assign(newField, nullSafe(fieldRef, JExpr._new(fieldType).arg(fieldRef).arg(fieldPathVar)));
						} else if (apiConstructs.partialCopyableInterface.isAssignableFrom(fieldType)) {
							currentBlock.assign(newField, nullSafe(fieldRef, apiConstructs.castOnDemand(fieldType, fieldRef.invoke(apiConstructs.copyMethod).arg(fieldPathVar))));
						} else if (apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
							currentBlock.assign(newField, createTransformerInvocation(apiConstructs, fieldOutline, definedClass, otherParam, transformerPathParam).arg(nullSafe(fieldRef, apiConstructs.castOnDemand(fieldType, fieldRef.invoke(apiConstructs.cloneMethod)))));
						} else {
							currentBlock.assign(newField, fieldRef);
						}
					} else {
						currentBlock.assign(newField, createTransformerInvocation(apiConstructs, fieldOutline, definedClass, otherParam, transformerPathParam).arg(fieldRef));
					}
				}
			}
		}

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}

	}

	private JInvocation createTransformerInvocation(final ApiConstructs apiConstructs, final FieldOutline fieldOutline, final JClass definedClass, final JVar copiedInstanceParam, final JVar transformerPathParam) {
		final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
		final JClass fieldType = (JClass) field.type();

		final JInvocation fieldPathVar = transformerPathParam.invoke("get").arg(JExpr.lit(field.name()));

		if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
			final JClass elementType = fieldType.getTypeParameters().get(0);

			return 	fieldPathVar.invoke("transform")
					.arg(JExpr._new(apiConstructs.codeModel._ref(PropertyInfo.class))
							.arg(JExpr.lit(field.name()))
							.arg(elementType.dotclass())
							.arg(definedClass.dotclass())
							.arg(JExpr.lit(0))
							.arg(JExpr.lit(fieldOutline.getPropertyInfo().isCollection() ? PropertyInfo.UNBOUNDED : 1))
							.arg(JExpr.lit(true)))
					.arg(copiedInstanceParam);
		} else {
			return fieldPathVar.invoke("transform")
								.arg(JExpr._new(apiConstructs.codeModel._ref(PropertyInfo.class))
										.arg(JExpr.lit(field.name()))
										.arg(fieldType.dotclass())
										.arg(definedClass.dotclass())
										.arg(JExpr.lit(fieldOutline.getPropertyInfo().isOptionalPrimitive() ? 0 : 1))
										.arg(JExpr.lit(1))
										.arg(JExpr.lit(true)))
								.arg(copiedInstanceParam);
		}

	}

	private boolean mustCatch(final ApiConstructs apiConstructs, final ClassOutline classOutline, final Predicate<JClass> fieldTypePredicate) {
		final JDefinedClass definedClass = classOutline.implClass;
		for (final JFieldVar field : definedClass.fields().values()) {
			if (field.type().isReference()) {
				JClass fieldType = (JClass) field.type();
				if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
					fieldType = fieldType.getTypeParameters().get(0);
				}
				if (fieldTypePredicate.matches(fieldType)) {
					return true;
				}
			}
		}
		return false;
	}

	private static interface Predicate<T> {
		boolean matches(final T arg);
	}
}
