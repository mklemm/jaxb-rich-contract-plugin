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

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMod;
import com.sun.tools.xjc.outline.ClassOutline;

/**
 * @author mirko
 * 25.03.14
 *
 */
public class BuilderOutline {
	private final JDefinedClass definedBuilderClass;
	private final ClassOutline classOutline;

	protected BuilderOutline(final ClassOutline classOutline) throws JClassAlreadyExistsException {
		this.classOutline = classOutline;
		final int mods = this.classOutline.implClass.isAbstract() ? JMod.PROTECTED | JMod.STATIC | JMod.ABSTRACT : JMod.PUBLIC | JMod.STATIC;
		this.definedBuilderClass = this.classOutline.implClass._class(mods, ApiConstructs.BUILDER_CLASS_NAME, ClassType.CLASS);
	}

	public JDefinedClass getDefinedBuilderClass() {
		return this.definedBuilderClass;
	}

	public ClassOutline getClassOutline() {
		return this.classOutline;
	}
}
