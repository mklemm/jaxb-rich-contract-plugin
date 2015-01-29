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

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSAttGroupDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GroupInterfacePlugin extends Plugin {
	private static final Logger LOGGER = Logger.getLogger(GroupInterfacePlugin.class.getName());
	public static final String XS_NS = "http://www.w3.org/2001/XMLSchema";
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

	@Override
	public void onActivated(final Options opts) throws BadCommandLineException {
		try {
			final XPathFactory xPathFactory = XPathFactory.newInstance();
			final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			final XPath xPath = xPathFactory.newXPath();
			final NamespaceContext namespaceContext = new MappingNamespaceContext().add("xs", XS_NS);
			xPath.setNamespaceContext(namespaceContext);
			final XPathExpression attGroupExpression = xPath.compile("/xs:schema/xs:attributeGroup/@name");
			final XPathExpression modelGroupExpression = xPath.compile("/xs:schema/xs:group/@name");
			final XPathExpression targetNamespaceExpression = xPath.compile("/xs:schema/@targetNamespace");

			final HashMap<String, List<String>> attGroupQNamesByNamespaceUri = new HashMap<>();
			final HashMap<String, List<String>> modelGroupQNamesByNamespaceUri = new HashMap<>();
			for (final InputSource grammar : opts.getGrammars()) {
				final String targetNamespaceUri = targetNamespaceExpression.evaluate(grammar);
				final NodeList attGroupNodes = (NodeList)attGroupExpression.evaluate(grammar, XPathConstants.NODESET);
				final NodeList modelGroupNodes = (NodeList)modelGroupExpression.evaluate(grammar, XPathConstants.NODESET);

				if(attGroupNodes.getLength() > 0 && modelGroupNodes.getLength() > 0) {
					final Document dummySchema = documentBuilder.newDocument();
					dummySchema.setXmlVersion("1.0");
					final Element rootEl = dummySchema.createElementNS(XS_NS, "xs:schema");
					rootEl.setAttribute("targetNamespace", targetNamespaceUri+"/__dummy");
				}
			}
		} catch(final Exception e) {
			throw new BadCommandLineException("Error setting up group-interface-plugin" + e);
		}
	}

	@Override
	public void postProcessModel(final Model model, final ErrorHandler errorHandler) {
		final Iterator<XSAttGroupDecl> iterator = model.schemaComponent.iterateAttGroupDecls();
		while(iterator.hasNext()) {
			final XSAttGroupDecl attGroupDecl = iterator.next();

		}
	}

	private static class MappingNamespaceContext implements NamespaceContext {
		private final Map<String,List<String>> namespacesByUri = new HashMap<>();
		private final HashMap<String,String> namespacesByPrefix = new HashMap<>();

		public MappingNamespaceContext add(final String prefix, final String namespaceUri) {
			putMapValue(this.namespacesByUri, namespaceUri, prefix);
			this.namespacesByPrefix.put(prefix,namespaceUri);
			return this;
		}

		@Override
		public String getNamespaceURI(final String prefix) {
			return this.namespacesByPrefix.get(prefix);
		}

		@Override
		public String getPrefix(final String namespaceURI) {
			return getPrefixes(namespaceURI).hasNext() ? (String)getPrefixes(namespaceURI).next() : null;
		}

		@Override
		public Iterator getPrefixes(final String namespaceURI) {
			return getMapValues(namespacesByUri, namespaceURI).iterator();
		}
	}

	private static List<String> getMapValues(final Map<String,List<String>> map, final String key) {
		final List<String> val = map.get(key);
		return val == null ? Collections.<String>emptyList() : val;
	}

	private static void putMapValue(final Map<String,List<String>> map, final String key, final String value) {
		List<String> values = map.get(key);
		if(values == null) {
			values = new ArrayList<>();
			map.put(key, values);
		}
		values.add(value);
	}
}