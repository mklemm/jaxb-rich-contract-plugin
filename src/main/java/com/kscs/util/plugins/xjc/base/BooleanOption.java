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

package com.kscs.util.plugins.xjc.base;

import java.lang.reflect.Field;

/**
 * @author Mirko Klemm 2015-02-13
 */
public class BooleanOption extends Option<Boolean> {
	public BooleanOption(final String name, final AbstractPlugin plugin, final Field field) {
		super(name, plugin, field, "{y|n}");
	}

	@Override
	public void setStringValue(final String s) {
		set(parseBoolean(s));
	}

	@Override
	public String getStringValue() {
		return get() ? "y" : "n";
	}

	private  static Boolean parseBoolean(final String arg) {
		final boolean argTrue = isTrue(arg);
		final boolean argFalse = isFalse(arg);
		if (!argTrue && !argFalse) {
			return null;
		} else {
			return argTrue;
		}
	}

	private static boolean isTrue(final String arg) {
		return arg.endsWith("y") || arg.endsWith("true") || arg.endsWith("on") || arg.endsWith("yes");
	}

	private static boolean isFalse(final String arg) {
		return arg.endsWith("n") || arg.endsWith("false") || arg.endsWith("off") || arg.endsWith("no");
	}

}
