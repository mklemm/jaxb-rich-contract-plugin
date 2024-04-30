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

import com.kscs.util.jaxb.Copyable;
import com.kscs.util.jaxb.PartialCopyable;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.kscs.util.jaxb.Selector;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.sun.codemodel.JMethod;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * XJC Plugin to generate copy and partial copy methods
 */
public class DeepCopyPlugin extends AbstractPlugin {
	public static final String PROPERTY_TREE_PARAM_NAME = "_propertyTree";
	public static final String PROPERTY_TREE_USE_PARAM_NAME = "_propertyTreeUse";
	@Opt("partial") protected boolean generatePartialCloneMethod = true;
	@Opt protected boolean generateTools = true;
	@Opt("constructor") protected boolean generateConstructor = true;
	@Opt protected boolean narrow = false;
	@Opt
	protected String selectorClassName = "Selector";
	@Opt
	protected final String rootSelectorClassName = "Select";

	@Override
	public String getOptionName() {
		return "Xcopy";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final PluginContext pluginContext = PluginContext.get(outline, opt, errorHandler);

		if(this.generateTools) {
			pluginContext.writeSourceFile(Copyable.class);
		}

		if (this.generatePartialCloneMethod) {
			if (this.generateTools) {
				pluginContext.writeSourceFile(PropertyTreeUse.class);
				pluginContext.writeSourceFile(PartialCopyable.class);
				pluginContext.writeSourceFile(PropertyTree.class);
				pluginContext.writeSourceFile(Selector.class);
			}
			final SelectorGenerator selectorGenerator = new SelectorGenerator(pluginContext, Selector.class, this.selectorClassName, this.rootSelectorClassName, null, null, pluginContext.cloneGraphClass);
			selectorGenerator.generateMetaFields();
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			classOutline.implClass._implements(Copyable.class);
			if(this.generatePartialCloneMethod) {
				classOutline.implClass._implements(PartialCopyable.class);
			}
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			final DeepCopyGenerator deepCopyGenerator = new DeepCopyGenerator(pluginContext, classOutline);
			deepCopyGenerator.generateCreateCopyMethod(false);
			if (this.generatePartialCloneMethod) {
				final JMethod partialCopyMethod =  deepCopyGenerator.generateCreateCopyMethod(true);
				deepCopyGenerator.generateConveniencePartialCopyMethod(partialCopyMethod, pluginContext.copyExceptMethodName, pluginContext.excludeConst);
				deepCopyGenerator.generateConveniencePartialCopyMethod(partialCopyMethod, pluginContext.copyOnlyMethodName, pluginContext.includeConst);
			}
			if (this.generateConstructor) {
				deepCopyGenerator.generateDefaultConstructor();
				deepCopyGenerator.generateCopyConstructor(false);
				if (this.generatePartialCloneMethod) {
					deepCopyGenerator.generateCopyConstructor(true);
				}
			}
		}
		return true;

	}




}
