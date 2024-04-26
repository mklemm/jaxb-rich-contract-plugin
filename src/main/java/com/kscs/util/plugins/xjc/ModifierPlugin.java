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

import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.outline.DefinedClassOutline;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Mirko Klemm 2015-03-14
 */
public class ModifierPlugin extends AbstractPlugin {

	@Opt
	protected String modifierClassName = "Modifier";

	@Opt
	protected String modifierMethodName = "modifier";

	@Override
	public String getOptionName() {
		return "Xmodifier";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final PluginContext pluginContext = PluginContext.get(outline, opt, errorHandler);
		for (final ClassOutline classOutline : outline.getClasses()) {
			try {
				final GroupInterfacePlugin groupInterfacePlugin = pluginContext.findPlugin(GroupInterfacePlugin.class);
				if (groupInterfacePlugin != null) {
					ModifierGenerator.generateClass(pluginContext, new DefinedClassOutline(classOutline), this.modifierClassName, this.modifierClassName, groupInterfacePlugin.getGroupInterfacesForClass(pluginContext, classOutline.implClass.fullName()), this.modifierMethodName);
				} else {
					ModifierGenerator.generateClass(pluginContext, new DefinedClassOutline(classOutline), this.modifierClassName, this.modifierMethodName);
				}
			} catch (final JClassAlreadyExistsException e) {
				errorHandler.error(new SAXParseException(e.getMessage(), classOutline.target.getLocator()));
			}
		}
		return true;
	}

}
