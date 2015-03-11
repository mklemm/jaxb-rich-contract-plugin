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

package com.kscs.util.plugins.xjc.codemodel;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JTypeVar;

/**
 * @author Mirko Klemm 2015-03-06
 */
public class JDirectInnerClassRef extends JClass {
	private final boolean isInterface;
	private final boolean isAbstract;
	private final JClass superClass;
	private final JClass outer;
	private final String name;

	public JDirectInnerClassRef(final JClass outer, final String name, final boolean isInterface, final boolean isAbstract, final JClass superClass) {
		super(outer.owner());
		this.outer = outer;
		this.name = name;
		this.isInterface = isInterface;
		this.isAbstract = isAbstract;
		this.superClass = superClass;
	}

	@Override
	public String fullName() {
		return this.outer.fullName() + "." + this.name;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public JPackage _package() {
		return this.outer._package();
	}

	@Override
	public JClass _extends() {
		return this.superClass;
	}

	@Override
	public Iterator<JClass> _implements() {
		return Collections.<JClass>emptyList().iterator();
	}

	@Override
	public boolean isInterface() {
		return this.isInterface;
	}

	@Override
	public boolean isAbstract() {
		return this.isAbstract;
	}

	@Override
	protected JClass substituteParams(final JTypeVar[] variables, final List<JClass> bindings) {
		return this;
	}

	@Override
	public JClass outer() {
		return this.outer;
	}
}
