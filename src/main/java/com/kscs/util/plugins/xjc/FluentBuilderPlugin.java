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

import com.kscs.util.jaxb.BuilderUtilities;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.kscs.util.jaxb.Selector;
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMod;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plugin to generate fluent Builders for generated classes
 */
public class FluentBuilderPlugin extends AbstractPlugin {
	private volatile boolean generateTools = true;
	private volatile boolean narrow = true;
	private volatile boolean partial = true;

	public FluentBuilderPlugin() {
		super("fluent-builder");
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {


		final Map<String, BuilderOutline> builderClasses = new LinkedHashMap<String, BuilderOutline>(outline.getClasses().size());
		final ApiConstructs apiConstructs = new ApiConstructs(outline, opt, errorHandler);

		if (this.generateTools) {
			apiConstructs.writeSourceFile(BuilderUtilities.class);
		}

		if(this.partial) {
			if(this.generateTools) {
				apiConstructs.writeSourceFile(PropertyTreeUse.class);
				apiConstructs.writeSourceFile(PropertyTree.class);
				apiConstructs.writeSourceFile(Selector.class);
			}
			if(apiConstructs.findPlugin(DeepCopyPlugin.class) == null) {
				final SelectorGenerator selectorGenerator = new SelectorGenerator(apiConstructs, Selector.class, "_Selector", "Select", null, null, apiConstructs.cloneGraphClass);
							selectorGenerator.generateMetaFields();
			}
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			try {
				final BuilderOutline builderOutline = new BuilderOutline(new DefinedClassOutline(classOutline), classOutline.implClass._class(JMod.PUBLIC | JMod.STATIC, ApiConstructs.BUILDER_CLASS_NAME, ClassType.CLASS));
				builderClasses.put(definedClass.fullName(), builderOutline);
			} catch (final JClassAlreadyExistsException caex) {
				errorHandler.warning(new SAXParseException("Class \"" + definedClass.name() + "\" already contains inner class \"Builder\". Skipping generation of fluent builder.", classOutline.target.getLocator(), caex));
			}
		}


		for (final BuilderOutline builderOutline : builderClasses.values()) {
			final BuilderGenerator builderGenerator = new BuilderGenerator(apiConstructs, builderClasses, builderOutline, this.partial, this.narrow);
			builderGenerator.buildProperties();
		}
		return true;
	}

}
