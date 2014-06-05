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
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * @author mirko 2014-06-05
 */
public class RichContractPlugin extends Plugin {
	private final PluginArgs args = new PluginArgs();

	private final Arg<Boolean> generateTools = this.args.create("generate-tools", true);
	private final Arg<Boolean> generateBoundProperties = this.args.create("bound-properties", false);
	private final Arg<Boolean> generateConstrainedProperties = this.args.create("constrained-properties", false);
	private final Arg<Boolean> constrainedSetterThrows = this.args.create("constrained-setter-throws", true);
	private final Arg<Boolean> generateClone = this.args.create("clone", true);
	private final Arg<Boolean> generatePartialClone = this.args.create("partial-clone", true);
	private final Arg<Boolean> cloneThrows = this.args.create("clone-throws", true);
	private final Arg<Boolean> generateCopyConstructor = this.args.create("copy-constructor", true);
	private final Arg<Boolean> generatePartialCopyConstructor = this.args.create("partial-copy-constructor", true);
	private final Arg<Boolean> generateBuildFromCopy = this.args.create("builder-from-copy", true);
	private final Arg<Boolean> generatePartialBuildFromCopy = this.args.create("partial-builder-from-copy", true);
	private final Arg<Boolean> generateImmutable = this.args.create("immutable", false);
	private final Arg<Boolean> generateFluentBuilder = this.args.create("fluent-builder", true);


	private ApiConstructs apiConstructs = null;
	private BoundPropertiesPlugin boundPropertiesPlugin = null;
	private DeepClonePlugin deepClonePlugin = null;
	private FluentBuilderPlugin fluentBuilderPlugin = null;
	private GroupInterfacePlugin groupInterfacePlugin = null;



	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException {
		return this.args.parse(args, i);
	}

	@Override
	public String getOptionName() {
		return "Xrich-contract";
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {


		return false;
	}
}
