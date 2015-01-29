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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import com.kscs.util.jaxb.BuilderUtilities;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.kscs.util.jaxb.Selector;
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMod;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Plugin to generate fluent Builders for generated classes
 */
public class FluentBuilderPlugin extends Plugin {
	private boolean generateTools = true;
	private boolean narrow = true;
	private boolean copyPartial = true;
	private final ResourceBundle resources;


	public FluentBuilderPlugin() {
		this.resources = ResourceBundle.getBundle(FluentBuilderPlugin.class.getName());
	}

	private String getMessage(final String key, final Object... params) {
		return MessageFormat.format(this.resources.getString(key), params);
	}

	@Override
	public String getOptionName() {
		return "Xfluent-builder";
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		PluginUtil.Arg<Boolean> arg = PluginUtil.parseBooleanArgument("fluent-builder-generate-tools", this.generateTools, opt, args, i);
		this.generateTools = arg.getValue();
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("fluent-builder-copy-partial", this.copyPartial, opt, args, i);
			this.copyPartial = arg.getValue();
		}
		if (arg.getArgsParsed() == 0) {
			arg = PluginUtil.parseBooleanArgument("fluent-builder-narrow", this.narrow, opt, args, i);
			this.narrow = arg.getValue();
		}
		return arg.getArgsParsed();
	}

	@Override
	public String getUsage() {
		final PluginUsageBuilder pluginUsageBuilder = new PluginUsageBuilder(this.resources, "usage").addMain("fluent-builder").addOption("fluent-builder-generate-tools", this.generateTools).addOption("fluent-builder-copy-partial", this.copyPartial).addOption("fluent-builder-narrow", this.narrow);

		return pluginUsageBuilder.build();
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {


		final Map<String, BuilderOutline> builderClasses = new LinkedHashMap<String, BuilderOutline>(outline.getClasses().size());
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);

		if (this.generateTools) {
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, BuilderUtilities.class.getName());
		}

		if(this.copyPartial) {
			if(this.generateTools) {
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, PropertyTreeUse.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, PropertyTree.class.getName());
				PluginUtil.writeSourceFile(getClass(), opt.targetDir, Selector.class.getName());
			}
			if(apiConstructs.findPlugin(DeepCopyPlugin.class) == null) {
				final SelectorGenerator selectorGenerator = new SelectorGenerator(apiConstructs, Selector.class, "Selector", "Select", null, null, apiConstructs.cloneGraphClass);
							selectorGenerator.generateMetaFields();
			}
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			try {
				final BuilderOutline builderOutline = new BuilderOutline(new DefinedClassOutline(apiConstructs, classOutline), classOutline.implClass._class(JMod.PUBLIC | JMod.STATIC, ApiConstructs.BUILDER_CLASS_NAME, ClassType.CLASS));
				builderClasses.put(definedClass.fullName(), builderOutline);
			} catch (final JClassAlreadyExistsException caex) {
				errorHandler.warning(new SAXParseException("Class \"" + definedClass.name() + "\" already contains inner class \"Builder\". Skipping generation of fluent builder.", classOutline.target.getLocator(), caex));
			}
		}

		for (final BuilderOutline builderOutline : builderClasses.values()) {
			final BuilderGenerator builderGenerator = new BuilderGenerator(apiConstructs, builderClasses, builderOutline, this.copyPartial, this.narrow);
			builderGenerator.buildProperties();
		}
		return true;
	}

}
