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

import java.util.ArrayList;
import java.util.List;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JStatement;
import com.sun.codemodel.JType;

/**
 * @author Mirko Klemm 2015-03-06
 */
public class JTypedInvocation extends JExpressionImpl implements JStatement {
	private final List<JType> typeArguments = new ArrayList<>();
	private final JExpression lhs;
	private final String method;
	private final JType type;
	private final boolean constructor;
	private final List<JExpression> args = new ArrayList<>();

	public JTypedInvocation(final JExpression lhs, final String method) {
		this.type = null;
		this.constructor = false;
		this.lhs = lhs;
		this.method = method;
	}

	public JTypedInvocation(final JClass type) {
		this.type = type;
		this.constructor = true;
		this.lhs = null;
		this.method = null;
	}

	@Override
	public void generate(JFormatter f) {
		if (this.lhs != null) {
			f = f.g(this.lhs).p('.');
		}
		if (this.constructor) {
			f = f.p("new").g(this.type).p('(');
		} else {
			if (!this.typeArguments.isEmpty()) {
				f = f.p("<");
				boolean first = true;
				for (final JType type : this.typeArguments) {
					if (!first) {
						f = f.p(", ");
					} else {
						first = false;
					}
					f = f.g(type);
				}
				f = f.p(">");
			}
			f = f.p(this.method).p('(');
		}
		f.g(this.args);
		f.p(')');
	}

	@Override
	public void state(final JFormatter f) {
		f.g(this).p(";").nl();
	}

	public JTypedInvocation arg(final JExpression var) {
		this.args.add(var);
		return this;
	}

	public JTypedInvocation narrow(final JType typeArg) {
		this.typeArguments.add(typeArg);
		return this;
	}
}
