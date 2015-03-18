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

import com.kscs.util.plugins.xjc.codemodel.ClassName;

/**
 * @author Mirko Klemm 2015-03-05
 */
public class BuilderGeneratorSettings {
	private final boolean generatingPartialCopy;
	private final boolean generatingNarrowCopy;
	private final String newBuilderMethodName;
	private final String newCopyBuilderMethodName;
	private final String builderFieldSuffix;
	private final ClassName builderClassName;
	private final String copyToMethodName;

	public BuilderGeneratorSettings(final boolean generatingPartialCopy, final boolean generatingNarrowCopy, final String newBuilderMethodName, final String newCopyBuilderMethodName, final String builderFieldSuffix, final ClassName builderClassName, final String copyToMethodName) {
		this.generatingPartialCopy = generatingPartialCopy;
		this.generatingNarrowCopy = generatingNarrowCopy;
		this.newBuilderMethodName = newBuilderMethodName;
		this.newCopyBuilderMethodName = newCopyBuilderMethodName;
		this.builderFieldSuffix = builderFieldSuffix;
		this.builderClassName = builderClassName;
		this.copyToMethodName = copyToMethodName;
	}

	public boolean isGeneratingPartialCopy() {
		return this.generatingPartialCopy;
	}

	public boolean isGeneratingNarrowCopy() {
		return this.generatingNarrowCopy;
	}

	public String getNewBuilderMethodName() {
		return this.newBuilderMethodName;
	}

	public String getNewCopyBuilderMethodName() {
		return this.newCopyBuilderMethodName;
	}

	public String getBuilderFieldSuffix() {
		return this.builderFieldSuffix;
	}

	public ClassName getBuilderClassName() {
		return this.builderClassName;
	}

	public String getCopyToMethodName() {
		return this.copyToMethodName;
	}

}
