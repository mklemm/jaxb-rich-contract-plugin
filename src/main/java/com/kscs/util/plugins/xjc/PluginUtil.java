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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import com.sun.codemodel.JAssignmentTarget;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;

/**
 * Common utilities for XJC plugins
 */
public final class PluginUtil {
	public static final String BOOLEAN_OPTION_ERROR_MSG = " option must be either (\"true\",\"on\",\"y\",\"yes\") or (\"false\", \"off\", \"n\",\"no\").";
	public static final String STRING_OPTION_ERROR_MSG = " option must not be empty.";

	public static void writeSourceFile(final Class<?> thisClass, final File targetDir, final String resourceName) {
		try {
			final String resourcePath = "/" + resourceName.replace('.', '/') + ".java";
			final File targetFile = new File(targetDir.getPath() + resourcePath);
			final File packageDir = targetFile.getParentFile();
			final boolean created = packageDir.mkdirs();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(thisClass.getResourceAsStream(resourcePath)));
			final BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					writer.write(line + "\n");
				}
			} finally {
				reader.close();
				writer.close();
			}
		} catch (final IOException iox) {
			throw new RuntimeException(iox);
		}
	}

	public static boolean hasPlugin(final Options opt, final Class<? extends Plugin> pluginType) {
		for (final Plugin plugin : opt.activePlugins) {
			if (pluginType.isInstance(plugin)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static <P extends Plugin> P findPlugin(final Options opt, final Class<P> pluginType) {
		for (final Plugin plugin : opt.activePlugins) {
			if (pluginType.isInstance(plugin)) {
				return (P) plugin;
			}
		}
		return null;
	}

	public static Arg<Boolean> parseBooleanArgument(final String name, final boolean defaultValue, final Options opt, final String[] args, final int i) throws BadCommandLineException {
		final String arg = args[i].toLowerCase();
		if (arg.startsWith("-" + name.toLowerCase() + "=")) {
			final boolean argTrue = PluginUtil.isTrue(arg);
			final boolean argFalse = PluginUtil.isFalse(arg);
			if (!argTrue && !argFalse) {
				throw new BadCommandLineException("-" + name.toLowerCase() + " " + PluginUtil.BOOLEAN_OPTION_ERROR_MSG);
			} else {
				return new Arg<Boolean>(argTrue, 1);
			}
		}
		return new Arg<Boolean>(defaultValue, 0);
	}

	public static Arg<String> parseStringArgument(final String name, final String defaultValue, final Options opt, final String[] args, final int i) throws BadCommandLineException {
		final String arg = args[i].toLowerCase();
		if (arg.startsWith("-" + name.toLowerCase() + "=")) {
			if (arg.isEmpty()) {
				throw new BadCommandLineException("-" + name.toLowerCase() + " " + PluginUtil.STRING_OPTION_ERROR_MSG);
			} else {
				return new Arg<String>(arg, 1);
			}
		}
		return new Arg<String>(defaultValue, 0);
	}

	private static boolean isTrue(final String arg) {
		return arg.endsWith("y") || arg.endsWith("true") || arg.endsWith("on") || arg.endsWith("yes");
	}

	private static boolean isFalse(final String arg) {
		return arg.endsWith("n") || arg.endsWith("false") || arg.endsWith("off") || arg.endsWith("no");
	}

	public static JFieldVar getDeclaredField(final FieldOutline fieldOutline) {
		return fieldOutline.parent().implClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
	}

	public static ClassOutline getClassOutline(final Outline outline, final JType typeRef) {
		for (final ClassOutline classOutline : outline.getClasses()) {
			if (typeRef.fullName().equals(classOutline.implClass.fullName())) {
				return classOutline;
			}
		}
		return null;
	}

	public static JDefinedClass getClassDefinition(final JCodeModel codeModel, final JType typeRef) {
		return codeModel._getClass(typeRef.fullName());
	}

	public static JExpression castOnDemand(final Set<String> generatedClasses, final JType fieldType, final JExpression expression) {
		return generatedClasses.contains(fieldType.fullName()) ? expression : JExpr.cast(fieldType, expression);
	}

	public static boolean isSuperGenerated(final ClassOutline classOutline) {
		return classOutline.getSuperClass() != null;
	}

	public static final class Arg<T> {
		private final T value;
		private final int argsParsed;

		Arg(final T value, final int argsParsed) {
			this.value = value;
			this.argsParsed = argsParsed;
		}

		public T getValue() {
			return this.value;
		}

		public int getArgsParsed() {
			return this.argsParsed;
		}
	}

	public static JExpression nullSafe(final JExpression test, final JExpression source) {
		return JOp.cond(test.eq(JExpr._null()), JExpr._null(), source);
	}

	public static JExpression nullSafe(final FieldOutline test, final JExpression source) {
		return nullSafe(JExpr.ref(test.getPropertyInfo().getName(false)), source);
	}

	public static JExpression nullSafe(final PropertyOutline test, final JExpression source) {
		return nullSafe(JExpr.ref(test.getFieldName()), source);
	}

	public static JBlock ifNull(final JBlock block, final JExpression test, final JAssignmentTarget target) {
		final JConditional ifNull = block._if(test.eq(JExpr._null()));
		ifNull._then().assign(target, JExpr._null());
		return ifNull._else();
	}

	public static JType getElementType(final FieldOutline fieldOutline) {
		final JFieldVar definedField = PluginUtil.getDeclaredField(fieldOutline);
		if (definedField != null) {
			if (fieldOutline.getPropertyInfo().isCollection()) {
				return definedField.type().isArray() ? definedField.type().elementType() : ((JClass) definedField.type()).getTypeParameters().get(0);
			} else {
				return definedField.type();
			}
		} else {
			return null;
		}
	}

	public static boolean hasGetter(final JDefinedClass jClass, final FieldOutline fieldOutline) {
		for(final JMethod method : jClass.methods()) {
			if((method.name().equals("get"+ fieldOutline.getPropertyInfo().getName(true))
					|| method.name().equals("is"+ fieldOutline.getPropertyInfo().getName(true))) && method.params().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasModifier(final int modifier, final int testModifier) {
		return (modifier & testModifier) == testModifier;
	}

	public static JDefinedClass getInnerClass(final JDefinedClass parentClass, final String innerClassName) {
		final Iterator<JDefinedClass> iterator = parentClass.classes();
		while(iterator.hasNext()) {
			final JDefinedClass innerClass = iterator.next();
			if(innerClass.name().equals(innerClassName)) {
				return innerClass;
			}
		}
		return null;
	}
}
