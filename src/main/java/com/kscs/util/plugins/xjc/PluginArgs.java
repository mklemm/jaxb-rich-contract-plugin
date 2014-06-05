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

import java.util.HashMap;
import java.util.Map;

/**
 * @author mirko 2014-06-05
 */
public class PluginArgs {
	private final Map<String,Arg<?>> args = new HashMap<String, Arg<?>>();

	public <T> Arg<T> create(final String name, final T defaultValue) {
		final Arg<T> arg = new Arg<T>(name, defaultValue);
		this.args.put(name, arg);
		return arg;
	}

	public <T> Arg<T> get(final String name) {
		return (Arg<T>)this.args.get(name);
	}

	int parse(final String[] argList, final int index) throws BadCommandLineException {
		if(index < 0 || index >= argList.length) return 0;
		final String arg = argList[index];
		if(!arg.startsWith("-")) return 0;
		final int sepIndex = arg.indexOf('=');
		if(sepIndex > 0) {
			final String argName = arg.substring(1, sepIndex);
			final String argValue = arg.substring(sepIndex + 1);

			final Arg<?> anyArg = this.args.get(argName);
			if(anyArg != null) {
				anyArg.setValueString(argValue);
				return 1;
			} else {
				return 0;
			}

		} else {
			final Arg<Boolean> booleanArg = ((Arg<Boolean>)this.args.get(arg));
			if(booleanArg == null || !Boolean.class.isAssignableFrom(booleanArg.getArgType())) {
				return 0;
			} else {
				booleanArg.setValue(Boolean.TRUE);
				return 1;
			}
		}
	}
}
