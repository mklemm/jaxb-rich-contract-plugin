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
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class GroupInterfacePlugin extends Plugin {
	private static final Logger LOGGER = Logger.getLogger(GroupInterfacePlugin.class.getName());
	private boolean declareSetters = true;
	private boolean declareBuilderInterface = true;
	private GroupInterfaceGenerator generator = null;



	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		PluginUtil.Arg<Boolean> arg = PluginUtil.parseBooleanArgument("declare-setters", this.declareSetters, opt, args, i);
		this.declareSetters = arg.getValue();
		if(arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("declare-builder-interface", this.declareBuilderInterface, opt, args, i);
			this.declareBuilderInterface = arg.getValue();
		}
		return arg.getArgsParsed();
	}

	@Override
	public String getOptionName() {
		return "Xgroup-contract";
	}

	@Override
	public String getUsage() {
		return new PluginUsageBuilder(ResourceBundle.getBundle(GroupInterfacePlugin.class.getName()), "usage")
				.addMain("group-contract")
				.addOption("declare-setters", this.declareSetters).build();
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler)
			throws SAXException {

		if(this.generator == null) {
			this.generator = new GroupInterfaceGenerator(new ApiConstructs(outline, opt, errorHandler), this.declareSetters, this.declareBuilderInterface);
			this.generator.generateGroupInterfaceModel();
		}

		return true;
	}

	public List<InterfaceOutline<?>> getGroupInterfacesForClass(final ApiConstructs apiConstructs, final ClassOutline classOutline) {
		if(this.generator == null) {
			this.generator = new GroupInterfaceGenerator(apiConstructs, this.declareSetters, this.declareBuilderInterface);
			this.generator.generateGroupInterfaceModel();
		}
		return this.generator.getGroupInterfacesForClass(classOutline);
	}

}