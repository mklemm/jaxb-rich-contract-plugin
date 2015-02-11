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

import java.util.Iterator;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * Common utilities for XJC plugins
 */
public final class PluginUtil {
	public static final String BOOLEAN_OPTION_ERROR_MSG = " option must be either (\"true\",\"on\",\"y\",\"yes\") or (\"false\", \"off\", \"n\",\"no\").";
	public static final String STRING_OPTION_ERROR_MSG = " option must not be empty.";

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

	public static JFieldVar getDeclaredField(final FieldOutline fieldOutline) {
		return fieldOutline.parent().implClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
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
