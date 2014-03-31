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

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plugin to generate fluent Builders for generated classes
 */
public class FluentBuilderPlugin extends Plugin {
	private boolean supportBuilderChain = true;

	public FluentBuilderPlugin() {
		// Needed by JAXB Framework
	}

	protected FluentBuilderPlugin(final boolean supportBuilderChain) {
		this.supportBuilderChain = supportBuilderChain;
	}

	@Override
	public String getOptionName() {
		return "Xfluent-builder";
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		final PluginUtil.Arg<Boolean> arg = PluginUtil.parseBooleanArgument("support-builder-chain", this.supportBuilderChain, opt, args, i);
		this.supportBuilderChain = arg.getValue();
		return arg.getArgsParsed();
	}

	@Override
	public String getUsage() {
		return " -Xfluent-builder: Generates an inner \"fluent builder\" for each of the generated classes.\n\tOptions:\n\t\t-support-builder-chain=<yes/no>:\tGenerate extended fluent builders that will allow child objects to be created in the fluent chain.";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {

		final Map<String, BuilderOutline> builderClasses = new LinkedHashMap<String, BuilderOutline>(outline.getClasses().size());
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt);

		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			try {
				final BuilderOutline builderOutline = new BuilderOutline(classOutline);
				builderClasses.put(definedClass.fullName(), builderOutline);
			} catch (final JClassAlreadyExistsException caex) {
				errorHandler.warning(new SAXParseException("Class \"" + definedClass.name() + "\" already contains inner class \"Builder\". Skipping generation of fluent builder.", classOutline.target.getLocator(), caex));
			}
		}


		for (final BuilderOutline builderOutline : builderClasses.values()) {
			final BuilderGenerator builderGenerator = this.supportBuilderChain ? new ChainedBuilderGenerator(apiConstructs, builderClasses, builderOutline) : new SimpleBuilderGenerator(apiConstructs, builderClasses, builderOutline);
			builderGenerator.buildProperties();
		}
		return true;
	}

}
