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
import org.junit.Test;

/**
 * Test for {@link com.kscs.util.plugins.xjc.PluginArgsBuilder}
 */
public class PluginArgsBuilderTest {
	@Test
	public void testParseCommandLineFluentBuilder() throws BadCommandLineException {
		final FluentBuilderPlugin plugin = new FluentBuilderPlugin();

		final String[] args = {"-fluent-builder.narrow=y", "-otherplug.arg=0", "-fluent-builder.constructor=no", "-fluent-builder.wrongArg=1"};

		for(int i=0; i < args.length; i++) {
			i += plugin.parseArgument(null, args, i);
		}
	}
}
