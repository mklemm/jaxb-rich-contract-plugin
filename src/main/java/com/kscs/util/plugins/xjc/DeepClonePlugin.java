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
 * XJC Plugin to generate Object.clone() implementation method
 */
public class DeepClonePlugin extends Plugin {
	private boolean throwCloneNotSupported = false;

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
		return arg.getArgsParsed();
	}

	@Override
	public String getUsage() {
		return new PluginUsageBuilder(this.resources, "usage")
				.addMain("clone")
				.addOption("clone-throws", this.throwCloneNotSupported)
				.build();
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);


		for (final ClassOutline classOutline : outline.getClasses()) {
			classOutline.implClass._implements(Cloneable.class);
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			generateCloneMethod(apiConstructs, classOutline);
		}
		return true;

	}

	private void generateCloneMethod(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;

		final boolean mustCatch = "java.lang.Object".equals(definedClass._extends().fullName()) || apiConstructs.cloneThrows(definedClass._extends(), this.throwCloneNotSupported) || mustCatch(apiConstructs, classOutline, new Predicate<JClass>() {
			@Override
			public boolean matches(final JClass arg) {
				return apiConstructs.cloneThrows(arg, DeepClonePlugin.this.throwCloneNotSupported);
			}
		});

		final JMethod cloneMethod = definedClass.method(JMod.PUBLIC, definedClass, apiConstructs.cloneMethod);
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

		final JVar newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke(apiConstructs.cloneMethod)));
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = JExpr._this().ref(field);
					if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						if (apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
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
