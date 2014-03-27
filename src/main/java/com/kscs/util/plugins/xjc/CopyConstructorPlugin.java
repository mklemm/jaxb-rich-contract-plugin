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

import static com.kscs.util.plugins.xjc.PluginUtil.castOnDemand;

/**
 * @author mirko 2014-03-27
 *
 */
public class CopyConstructorPlugin extends Plugin {
	private boolean generatePartialInit;
	private boolean generateTools;

	private final ResourceBundle resources;

	public CopyConstructorPlugin() {
		this.resources = ResourceBundle.getBundle(CopyConstructorPlugin.class.getSimpleName());
	}

	private String getMessage(final String key, final Object... params) {
		return MessageFormat.format(this.resources.getString(key), params);
	}

	@Override
	public String getOptionName() {
		return "Xcopy-constructor";
	}

	@Override
	public String getUsage() {
		return "-Xcopy-constructor: Generates a copy-constructor for each generated class, optionally with 'partial' initialization as with the clone plugin.";
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		PluginUtil.Arg<Boolean> arg = PluginUtil.parseBooleanArgument("partial-init", this.generatePartialInit, opt, args, i);
		this.generatePartialInit = arg.getValue();

		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("generate-tools", this.generateTools, opt, args, i);
			this.generateTools = arg.getValue();
		}
		return arg.getArgsParsed();
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final JCodeModel m = outline.getCodeModel();
		final JClass collectionClass = m.ref(Collection.class);
		final JClass cloneableInterface = m.ref(Cloneable.class);
		final JClass pathCloneableInterface = m.ref(PathCloneable.class);
		final JClass arrayListClass = m.ref(ArrayList.class);

		if (this.generatePartialInit && this.generateTools) {
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, PathCloneable.class.getName());
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, PropertyPath.class.getName());
		}

		final Set<String> generatedClasses = new HashSet<String>();
		for (final ClassOutline classOutline : outline.getClasses()) {
			generatedClasses.add(classOutline.implClass.fullName());
		}


		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;

			generateDefaultConstructor(definedClass);
			generateCopyConstructor(m, collectionClass, cloneableInterface, arrayListClass, generatedClasses, definedClass);
			if (this.generatePartialInit) {
				generatePartialCloneMethod(m, collectionClass, pathCloneableInterface, arrayListClass, generatedClasses, definedClass);
			}
		}
		return true;
	}

	private boolean mustCatch(final JClass collectionClass, final JClass cloneableInterface, final Set<String> generatedClasses, final JDefinedClass definedClass) {
		boolean mustCatch = cloneMethodThrows(generatedClasses, definedClass._extends());
		for (final JFieldVar field : definedClass.fields().values()) {
			if (field.type().isReference()) {
				final JClass fieldType = (JClass) field.type();
				if (collectionClass.isAssignableFrom(fieldType)) {
					final JClass elementType = fieldType.getTypeParameters().get(0);
					if (cloneableInterface.isAssignableFrom(elementType)) {
						mustCatch |= cloneMethodThrows(generatedClasses, elementType);
					}
				} else {
					if (cloneableInterface.isAssignableFrom(fieldType)) {
						mustCatch |= cloneMethodThrows(generatedClasses, fieldType);
					}
				}
			}
		}
		return mustCatch;
	}

	private void generateDefaultConstructor(final JDefinedClass definedClass) {
		final JMethod defaultConstructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		defaultConstructor.body().directStatement("// " + getMessage("defaultConstructor.javadoc.desc"));
		defaultConstructor.javadoc().append(getMessage("defaultConstructor.bodyComment"));
	}

	private void generateCopyConstructor(final JCodeModel m, final JClass collectionClass, final JClass cloneableInterface, final JClass arrayListClass, final Set<String> generatedClasses, final JDefinedClass definedClass) {
		final boolean mustCatch = mustCatch(collectionClass, cloneableInterface, generatedClasses, definedClass);

		final JMethod constructor = definedClass.constructor(definedClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar otherParam = constructor.param(JMod.FINAL, definedClass, "other");
		final JDocComment docComment = constructor.javadoc();
		docComment.append(getMessage("copyConstructor.javadoc.desc", definedClass.name()));
		docComment.addParam(otherParam).append(getMessage("copyConstructor.javadoc.param.other", definedClass.name()));
		docComment.addThrows(InstantiationException.class).append(getMessage("copyConstructor.javadoc.exception"));

		constructor.body().invoke("super").arg(otherParam);

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

		final JExpression newObjectVar = JExpr._this();
		for (final JFieldVar field : definedClass.fields().values()) {
			if (field.type().isReference()) {
				final JClass fieldType = (JClass) field.type();
				final JFieldRef newField = JExpr.ref(newObjectVar, field);
				final JFieldRef fieldRef = JExpr._this().ref(field);
				if (collectionClass.isAssignableFrom(fieldType)) {
					final JClass elementType = fieldType.getTypeParameters().get(0);
					final JConditional ifNull = body._if(fieldRef.eq(JExpr._null()));
					ifNull._then().assign(newField, JExpr._null());
					ifNull._else().assign(newField, JExpr._new(arrayListClass.narrow(elementType)).arg(JExpr._this().ref(field).invoke("size")));
					final JForEach forLoop = ifNull._else().forEach(elementType, "item", fieldRef);
					if (cloneableInterface.isAssignableFrom(elementType)) {
						final JConditional ifStmt = forLoop.body()._if(forLoop.var().eq(JExpr._null()));
						ifStmt._then().invoke(newField, "add").arg(JExpr._null());
						ifStmt._else().invoke(newField, "add").arg(castOnDemand(generatedClasses, elementType, forLoop.var().invoke("clone")));
					} else {
						forLoop.body().invoke(newField, "add").arg(forLoop.var());
					}
				}
				if (cloneableInterface.isAssignableFrom(fieldType)) {
					final JConditional ifStmt = body._if(fieldRef.eq(JExpr._null()));
					ifStmt._then().assign(newField, JExpr._null());
					ifStmt._else().assign(newField, castOnDemand(generatedClasses, fieldType, JExpr._this().ref(field).invoke("clone")));
				} else {
					// body.assign(newField, JExpr._this().ref(field));
				}
			}
		}
		body._return(newObjectVar);

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(m.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(m.ref(RuntimeException.class)).arg(exceptionVar));
		}
	}

	private void generatePartialCloneMethod(final JCodeModel m, final JClass collectionClass, final JClass cloneableInterface, final JClass arrayListClass, final Set<String> generatedClasses, final JDefinedClass definedClass) {
		final boolean mustCatch = false; //mustCatch(collectionClass, cloneableInterface, generatedClasses, definedClass);

		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, "clone");
		final JVar clonePathParam = cloneMethod.param(JMod.FINAL, PropertyPath.class, "path");
		cloneMethod.annotate(Override.class);

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
		JBlock currentBlock = body;
		final JVar newObjectVar;
		if (definedClass._extends() != null && cloneableInterface.isAssignableFrom(definedClass._extends())) {
			newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke("clone").arg(clonePathParam)));
		} else {
			newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", null);
			final JTryBlock newObjectTry = currentBlock._try();
			final JBlock tryBody = newObjectTry.body();
			tryBody.assign(newObjectVar, JExpr.invoke("getClass").invoke("newInstance"));
			final JCatchBlock newObjectCatch = newObjectTry._catch((JClass) m._ref(Exception.class));
			final JVar exceptionVar = newObjectCatch.param("x");
			newObjectCatch.body()._throw(JExpr._new(m._ref(RuntimeException.class)).arg(exceptionVar));
		}

		for (final JFieldVar field : definedClass.fields().values()) {
			if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
				final JFieldRef newField = JExpr.ref(newObjectVar, field);
				final JFieldRef fieldRef = JExpr._this().ref(field);
				final JVar fieldPathVar = body.decl(JMod.FINAL, m._ref(PropertyPath.class), field.name() + "ClonePath", clonePathParam.invoke("get").arg(JExpr.lit(field.name())));
				final JExpression includesInvoke = fieldPathVar.invoke("includes");
				final JConditional ifHasClonePath = body._if(includesInvoke);
				currentBlock = ifHasClonePath._then();
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					if (collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						final JConditional ifNull = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifNull._then().assign(newField, JExpr._new(arrayListClass.narrow(elementType)).arg(fieldRef.invoke("size")));
						ifNull._else().assign(newField, JExpr._null());
						final JForEach forLoop = ifNull._then().forEach(elementType, "item", fieldRef);
						if (cloneableInterface.isAssignableFrom(elementType)) {
							final JConditional ifStmt = forLoop.body()._if(forLoop.var().eq(JExpr._null()));
							ifStmt._then().invoke(newField, "add").arg(JExpr._null());
							ifStmt._else().invoke(newField, "add").arg(castOnDemand(generatedClasses, elementType, forLoop.var().invoke("clone").arg(fieldPathVar)));
						} else {
							forLoop.body().invoke(newField, "add").arg(forLoop.var());
						}
					} else if (cloneableInterface.isAssignableFrom(fieldType)) {
						final JConditional ifStmt = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifStmt._then().assign(newField, castOnDemand(generatedClasses, fieldType, fieldRef.invoke("clone").arg(fieldPathVar)));
						ifStmt._else().assign(newField, JExpr._null());
					} else if (m.ref(Cloneable.class).isAssignableFrom(fieldType)) {
						final JConditional ifStmt = currentBlock._if(fieldRef.ne(JExpr._null()));
						ifStmt._then().assign(newField, castOnDemand(generatedClasses, fieldType, fieldRef.invoke("clone")));
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
			final JCatchBlock catchBlock = tryBlock._catch(m.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(m.ref(RuntimeException.class)).arg(exceptionVar));
		}

	}

	private boolean cloneMethodThrows(final Set<String> generatedClasses, final JClass fieldType) {
		try {
			return fieldType.fullName().equals(Object.class.getName()) || (!generatedClasses.contains(fieldType.fullName())
					&&
					Class.forName(fieldType.fullName()).getMethod("clone").getExceptionTypes().length > 0);
		} catch (final Exception e) {
			System.err.println("WARNING: " + e);
			e.printStackTrace();
			return true;
		}
	}

}
