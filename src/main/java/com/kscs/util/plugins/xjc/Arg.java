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

import com.sun.tools.xjc.BadCommandLineException;

/**
 * Represents an argument to an XJC Plugin
 *
 * @author mirko 2014-06-05
 */
public class Arg<T> {
	private final String name;
	private final T defaultValue;
	private Object value;
	private boolean valueSet = false;
	private final Class<T> argType;

	Arg(final String name, final T defaultValue) {
		this.name = name;
		this.argType = (Class<T>) defaultValue.getClass();
		this.defaultValue = defaultValue;
	}

	Arg(final String name, final Class<T> argType, final T defaultValue) {
		this.name = name;
		this.argType = argType;
		this.defaultValue = defaultValue;
	}

	void clear() {
		this.value = null;
		this.valueSet = false;
	}

	void setValue(final T value) {
		this.value = value;
		this.valueSet = true;
	}

	void setValueString(final String s) throws BadCommandLineException {
		if (Boolean.class.isAssignableFrom(this.argType)) {
			parseBooleanArgument(s);
		} else if (Integer.class.isAssignableFrom(this.argType)) {
			this.value = Integer.valueOf(s);
		} else {
			this.value = s;
		}
	}

	public T getValue() {
		return this.valueSet ? (T) this.value : this.defaultValue;
	}

	public boolean isValueSet() {
		return this.valueSet;
	}

	public String getName() {
		return this.name;
	}

	public T getDefaultValue() {
		return this.defaultValue;
	}

	private static boolean isTrue(final String arg) {
		return arg.endsWith("y") || arg.endsWith("true") || arg.endsWith("on") || arg.endsWith("yes");
	}

	private static boolean isFalse(final String arg) {
		return arg.endsWith("n") || arg.endsWith("false") || arg.endsWith("off") || arg.endsWith("no");
	}

	public void parseBooleanArgument(final String arg) throws BadCommandLineException {
		final boolean argTrue = isTrue(arg);
		final boolean argFalse = isFalse(arg);
		if (!argTrue && !argFalse) {
			throw new BadCommandLineException("-" + this.name.toLowerCase() + " " + PluginUtil.BOOLEAN_OPTION_ERROR_MSG);
		} else {
			this.value = argTrue;
		}
	}

	public Class<T> getArgType() {
		return argType;
	}
}

