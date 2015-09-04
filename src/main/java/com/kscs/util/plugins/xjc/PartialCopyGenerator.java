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

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.kscs.util.plugins.xjc.base.PropertyDirectoryResourceBundle;
import com.kscs.util.plugins.xjc.codemodel.JTypedInvocation;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JVar;

/**
 * @author mirko 2014-06-04
 */
class PartialCopyGenerator implements CopyGenerator {
	public static final ResourceBundle RESOURCE_BUNDLE = PropertyDirectoryResourceBundle.getInstance(PartialCopyGenerator.class);
	public static final String PROPERTY_TREE_PARAM_NAME = "_propertyTree";
	public static final String PROPERTY_TREE_USE_PARAM_NAME = "_propertyTreeUse";

	private final PluginContext pluginContext;
	private final JVar propertyTreeUseParam;
	private final JVar propertyTreeParam;
	private final JMethod copyMethod;

	public PartialCopyGenerator(final PluginContext pluginContext, final JMethod copyMethod) {
		this(pluginContext, copyMethod, PartialCopyGenerator.PROPERTY_TREE_PARAM_NAME, PartialCopyGenerator.PROPERTY_TREE_USE_PARAM_NAME);
	}

	public PartialCopyGenerator(final PluginContext pluginContext, final JMethod copyMethod, final String propertyTreeParamName, final String propertyTreeUseParamName) {
		this.pluginContext = pluginContext;
		this.copyMethod = copyMethod;
		this.propertyTreeParam = copyMethod.param(JMod.FINAL, PropertyTree.class, propertyTreeParamName);
		this.propertyTreeUseParam = copyMethod.param(JMod.FINAL, PropertyTreeUse.class, propertyTreeUseParamName);
	}

	public PluginContext getPluginContext() {
		return this.pluginContext;
	}

	@Override
	public JVar getPropertyTreeUseParam() {
		return this.propertyTreeUseParam;
	}

	@Override
	public JVar getPropertyTreeParam() {
		return this.propertyTreeParam;
	}

	@Override
	public JTypedInvocation generatePartialArgs(final JTypedInvocation invocation) {
		return invocation.arg(this.propertyTreeParam).arg(this.propertyTreeUseParam);
	}

	@Override
	public void generatePartialArgs(final JDocComment javadoc) {
		javadoc	.addParam(this.propertyTreeParam)
				.append(PartialCopyGenerator.RESOURCE_BUNDLE.getString("javadoc.method.param.propertyTree"));
		javadoc	.addParam(this.propertyTreeUseParam)
				.append(MessageFormat.format(PartialCopyGenerator.RESOURCE_BUNDLE.getString("javadoc.method.param.propertyTreeUse"), this.propertyTreeParam.name()));
	}

	public JMethod getCopyMethod() {
		return this.copyMethod;
	}

	public class TreeVarGenerator implements PropertyTreeVarGenerator {
		private final JVar fieldPathVar;

		public TreeVarGenerator(final JBlock body, final String fieldName) {
			this.fieldPathVar = body.decl(JMod.FINAL,
					PartialCopyGenerator.this.pluginContext.codeModel._ref(PropertyTree.class),
					fieldName + "PropertyTree",
					JOp.cond(PartialCopyGenerator.this.propertyTreeParam.eq(JExpr._null()), JExpr._null(),PartialCopyGenerator.this.propertyTreeParam.invoke("get").arg(JExpr.lit(fieldName)))
			);
		}

		@Override
		public JVar getPropertyTreeVar() {
			return this.fieldPathVar;
		}

		@Override
		public JExpression generatePartialArgs(final JExpression expression) {
			if(expression instanceof JInvocation) {
				return ((JInvocation)expression).arg(this.fieldPathVar).arg(getPropertyTreeUseParam());
			} else if(expression instanceof JTypedInvocation) {
				return ((JTypedInvocation)expression).arg(this.fieldPathVar).arg(getPropertyTreeUseParam());
			} else {
				return expression;
			}
		}

		@Override
		public JBlock generateEnclosingBlock(final JBlock body) {
			return  body._if(getIncludeCondition(this.fieldPathVar))._then();
		}

		private JExpression getIncludeCondition(final JVar fieldPathVar) {
			return JOp.cond(
					PartialCopyGenerator.this.propertyTreeUseParam.eq(PartialCopyGenerator.this.pluginContext.includeConst),
					fieldPathVar.ne(JExpr._null()),
					fieldPathVar.eq(JExpr._null()).cor(fieldPathVar.invoke("isLeaf").not())
			);
		}
	}




	@Override
	public PropertyTreeVarGenerator createPropertyTreeVarGenerator(final JBlock body, final String fieldName) {
		return new TreeVarGenerator(body, fieldName);
	}
}
