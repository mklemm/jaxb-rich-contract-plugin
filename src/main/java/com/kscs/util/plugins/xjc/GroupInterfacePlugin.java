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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Namespaces;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.outline.TypeOutline;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;

/**
 * Generates interface declarations from &lt;group&gt; and &lt;attributeGroup&gt;
 * XSD declarations, and makes classes generated from complexTypes that use
 * these declarations implement the generated interface.
 */
public class GroupInterfacePlugin extends AbstractPlugin {
	@Opt
	private boolean declareSetters = true;
	@Opt
	private boolean declareBuilderInterface = true;
	@Opt
	private String supportInterfaceNameSuffix = "Lifecycle";
	@Opt
	private String upstreamEpisodeFile = "META-INF/jaxb-interfaces.episode";
	@Opt
	private String downstreamEpisodeFile = "META-INF/jaxb-interfaces.episode";
	private final GroupInterfaceModelProcessingStrategy complexTypeGeneratorStrategy;
	private GroupInterfaceGenerator generator = null;

	public GroupInterfacePlugin() {
		this.complexTypeGeneratorStrategy = getConfiguredObject("group-contract.complexTypeGeneratorStrategy", GroupInterfaceDirectStrategy.class);
	}

	@Override
	public List<String> getCustomizationURIs() {
		return Collections.singletonList(Namespaces.KSCS_BINDINGS_NS);
	}

	@Override
	public boolean isCustomizationTagName(final String nsUri, final String localName) {
		return Namespaces.KSCS_BINDINGS_NS.equals(nsUri) && "interface".equals(localName);
	}

	@Override
	public String getOptionName() {
		return "Xgroup-contract";
	}

	@Override
	public void onActivated(final Options opts) throws BadCommandLineException {
		this.complexTypeGeneratorStrategy.onPluginActivation(opts);
	}

	@Override
	public void postProcessModel(final Model model, final ErrorHandler errorHandler) {
		this.complexTypeGeneratorStrategy.onPostProcessModel(model, errorHandler);
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler)
			throws SAXException {
		generate(PluginContext.get(outline, opt, errorHandler));
		return true;
	}

	public List<TypeOutline> getGroupInterfacesForClass(final PluginContext pluginContext, final String className) throws SAXException {
		generate(pluginContext);
		return this.generator.getGroupInterfacesForClass(className);
	}

	private void generate(final PluginContext pluginContext) throws SAXException {
		if (this.generator == null) {
			try {
				final Enumeration<URL> interfaceEpisodeURLs = getClass().getClassLoader().getResources(this.upstreamEpisodeFile);
				final EpisodeBuilder episodeBuilder = new EpisodeBuilder(pluginContext, this.downstreamEpisodeFile);
				this.generator = new GroupInterfaceGenerator(pluginContext, interfaceEpisodeURLs, episodeBuilder, getSettings(pluginContext));
				this.generator.generateGroupInterfaceModel();
				episodeBuilder.build();
			} catch (final IOException iox) {
				throw new SAXException(iox);
			}
		}
	}

	public GroupInterfaceGeneratorSettings getSettings(final PluginContext pluginContext) {
		final FluentBuilderPlugin fluentBuilderPlugin = pluginContext.findPlugin(FluentBuilderPlugin.class);
		if (fluentBuilderPlugin != null) {
			final BuilderGeneratorSettings builderGeneratorSettings = fluentBuilderPlugin.getSettings();
			return new GroupInterfaceGeneratorSettings(this.declareSetters, this.declareBuilderInterface, this.supportInterfaceNameSuffix, builderGeneratorSettings);
		} else {
			return new GroupInterfaceGeneratorSettings(this.declareSetters, this.declareBuilderInterface, pluginContext.hasPlugin(ModifierPlugin.class) ? this.supportInterfaceNameSuffix : null, null);
		}
	}

	public final <T> T getConfiguredObject(final String systemPropertyName, final Class<T> defaultClass) {
		final String strategyClassName = System.getProperty(systemPropertyName);
		Class<T> strategyClass;
		if (strategyClassName == null || strategyClassName.trim().isEmpty()) {
			strategyClass = defaultClass;
		} else {
			try {
				strategyClass = ((Class<T>)Class.forName(strategyClassName));
			} catch (final ClassNotFoundException cnfe) {
				strategyClass = defaultClass;
			}
		}
		try {
			return strategyClass.getConstructor().newInstance();
		} catch (final NoSuchMethodException e) {
			throw new RuntimeException(getMessage("error.no-such-constructor", strategyClassName), e);
		} catch (final InvocationTargetException | InstantiationException | IllegalAccessException ex) {
			throw new RuntimeException(getMessage("error.cannot-instatiate-strategy", strategyClassName), ex);
		}
	}
}
