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

import com.kscs.util.jaxb.Buildable;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.kscs.util.jaxb.Selector;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.codemodel.ClassName;
import com.kscs.util.plugins.xjc.outline.DefinedClassOutline;
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMod;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CEnumLeafInfo;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.EnumOutline;
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
	@Opt
	private final String rootSelectorClassName = "Select";
	@Opt
	protected String newBuilderMethodName = PluginContext.NEW_BUILDER_METHOD_NAME;
	@Opt
	protected String newCopyBuilderMethodName = PluginContext.NEW_COPY_BUILDER_METHOD_NAME;
	@Opt
	protected String copyToMethodName = PluginContext.COPY_TO_METHOD_NAME;
	@Opt
	protected String builderFieldSuffix = "_Builder";
	@Opt
	protected boolean generateTools = true;
	@Opt
	protected boolean narrow = false;
	@Opt
	protected boolean copyPartial = true;
	@Opt
	protected String selectorClassName = "Selector";
	@Opt
	protected String builderClassName = PluginContext.BUILDER_CLASS_NAME;
	@Opt
	protected String builderInterfaceName = PluginContext.BUILDER_INTERFACE_NAME;
	@Opt
	protected boolean copyAlways = false;
	@Opt
	protected String buildMethodName = PluginContext.BUILD_METHOD_NAME;
	@Opt
	protected String endMethodName = "end";
	@Opt
	protected boolean generateJavadocFromAnnotations = false;

	@Override
	public String getOptionName() {
		return "Xfluent-builder";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final Map<String, BuilderOutline> builderClasses = new LinkedHashMap<>(outline.getClasses().size());
		final PluginContext pluginContext = PluginContext.get(outline, opt, errorHandler);

		if(this.generateTools) {
			pluginContext.writeSourceFile(Buildable.class);
		}
		if (this.copyPartial) {
			if (this.generateTools) {
				pluginContext.writeSourceFile(PropertyTreeUse.class);
				pluginContext.writeSourceFile(PropertyTree.class);
				pluginContext.writeSourceFile(Selector.class);
			}
			if (pluginContext.findPlugin(DeepCopyPlugin.class) == null) {
				final SelectorGenerator selectorGenerator = new SelectorGenerator(pluginContext, Selector.class, this.selectorClassName, this.rootSelectorClassName, null, null, pluginContext.cloneGraphClass);
				selectorGenerator.generateMetaFields();
			}
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			try {
				final BuilderOutline builderOutline = new BuilderOutline(new DefinedClassOutline(classOutline),
						classOutline.implClass._class(JMod.PUBLIC | JMod.STATIC, this.builderClassName, ClassType.CLASS));
				builderClasses.put(definedClass.fullName(), builderOutline);
			} catch (final JClassAlreadyExistsException caex) {
				errorHandler.warning(new SAXParseException(getMessage("error.builderClassExists", definedClass.name()), classOutline.target.getLocator(), caex));
			}
		}

		for (final BuilderOutline builderOutline : builderClasses.values()) {
			final BuilderGenerator builderGenerator = new BuilderGenerator(pluginContext, builderClasses, builderOutline, getSettings());
			builderGenerator.buildProperties();
		}

		// Add class level javadoc for enums (i.e. enumerated simple types), if required
		// xjc seems to add the field level javadoc itself
		if (getSettings().isGeneratingJavadocFromAnnotations()) {
			for (final EnumOutline enumOutline : outline.getEnums()) {
				if (enumOutline.getTarget() instanceof CEnumLeafInfo) {
					final String schemaAnnotation = SchemaAnnotationUtils.getEnumAnnotationDescription(
					        (CEnumLeafInfo) enumOutline.getTarget());
					JavadocUtils.appendJavadocParagraph(enumOutline.getImplClass(), schemaAnnotation);
				}
			}
		}

		return true;
	}

	public BuilderGeneratorSettings getSettings() {
		return new BuilderGeneratorSettings(this.copyPartial, this.narrow, this.newBuilderMethodName, this.newCopyBuilderMethodName, this.builderFieldSuffix,
				new ClassName(this.builderInterfaceName, this.builderClassName), this.copyToMethodName,
				this.copyAlways, this.buildMethodName, this.endMethodName, generateJavadocFromAnnotations);
	}
}
