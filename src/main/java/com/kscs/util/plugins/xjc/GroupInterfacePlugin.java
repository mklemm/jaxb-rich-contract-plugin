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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.text.MessageFormat;
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
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GroupInterfacePlugin extends Plugin {
	private static final Logger LOGGER = Logger.getLogger(GroupInterfacePlugin.class.getName());
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(GroupInterfacePlugin.class.getName());
	public static final String XS_NS = "http://www.w3.org/2001/XMLSchema";
	public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
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
		return new PluginUsageBuilder(GroupInterfacePlugin.RESOURCE_BUNDLE, "usage")
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
			final TransformerFactory transformerFactory = TransformerFactory.newInstance();
			final Transformer transformer = transformerFactory.newTransformer();
			final XPathFactory xPathFactory = XPathFactory.newInstance();
			final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			final XPath xPath = xPathFactory.newXPath();
			final NamespaceContext namespaceContext = new MappingNamespaceContext().add("xs", GroupInterfacePlugin.XS_NS);
			xPath.setNamespaceContext(namespaceContext);
			final XPathExpression attGroupExpression = xPath.compile("/xs:schema/xs:attributeGroup/@name");
			final XPathExpression modelGroupExpression = xPath.compile("/xs:schema/xs:group/@name");
			final XPathExpression targetNamespaceExpression = xPath.compile("/xs:schema/@targetNamespace");

			final List<InputSource> newGrammars = new ArrayList<>();
			for (final InputSource grammar : opts.getGrammars()) {
				final InputSource schemaCopy = new InputSource(grammar.getSystemId());

				final String targetNamespaceUri = targetNamespaceExpression.evaluate(schemaCopy);
				final NodeList attGroupNodes = (NodeList)attGroupExpression.evaluate(schemaCopy, XPathConstants.NODESET);
				final NodeList modelGroupNodes = (NodeList)modelGroupExpression.evaluate(schemaCopy, XPathConstants.NODESET);

				final Groups currentGroups = new Groups(targetNamespaceUri);

				for(int i = 0; i < attGroupNodes.getLength(); i++) {
					currentGroups.attGroupNames.add(attGroupNodes.item(i).getNodeValue());
				}

				for(int i = 0; i < modelGroupNodes.getLength(); i++) {
					currentGroups.modelGroupNames.add(modelGroupNodes.item(i).getNodeValue());
				}

				final InputSource newSchema = generateImplementationSchema(opts, transformer, documentBuilder, currentGroups, schemaCopy.getSystemId());
				if(newSchema != null) {
					newGrammars.add(newSchema);
				}
			}

			for(final InputSource newGrammar : newGrammars) {
				opts.addGrammar(newGrammar);
			}
		} catch(final Exception e) {
			throw new BadCommandLineException(MessageFormat.format(GroupInterfacePlugin.RESOURCE_BUNDLE.getString("error.plugin-setup"), e));
		}
	}

	private InputSource generateImplementationSchema(final Options opts,  final Transformer transformer, final DocumentBuilder documentBuilder, final Groups namespaceGroups, final String systemId) throws TransformerException {
			if(!namespaceGroups.attGroupNames.isEmpty() || !namespaceGroups.modelGroupNames.isEmpty()) {
				final Document dummySchema = documentBuilder.newDocument();
				dummySchema.setXmlVersion("1.0");
				final String targetNamespacePrefix = GroupInterfacePlugin.XML_NS.equals(namespaceGroups.targetNamespace) ? "xml" : "tns";
				final Element rootEl = dummySchema.createElementNS(GroupInterfacePlugin.XS_NS, "xs:schema");
				rootEl.setAttribute("version", "1.0");
				rootEl.setAttribute("targetNamespace", namespaceGroups.targetNamespace);
				rootEl.setAttribute("elementFormDefault", "qualified");
				rootEl.setAttribute("xmlns:"+targetNamespacePrefix, namespaceGroups.targetNamespace);
				dummySchema.appendChild(rootEl);

				final Element importEl = dummySchema.createElementNS(GroupInterfacePlugin.XS_NS, "xs:include");
				importEl.setAttribute("schemaLocation", systemId);
				rootEl.appendChild(importEl);

				for (final String attGroupName : namespaceGroups.attGroupNames) {
					final Element complexTypeEl = dummySchema.createElementNS(GroupInterfacePlugin.XS_NS, "xs:complexType");
					complexTypeEl.setAttribute("name", "__" + attGroupName + "_XXXXXX");
					rootEl.appendChild(complexTypeEl);

					final Element attGroupRefEl = dummySchema.createElementNS(GroupInterfacePlugin.XS_NS, "xs:attributeGroup");
					attGroupRefEl.setAttribute("ref", targetNamespacePrefix +":" + attGroupName);
					complexTypeEl.appendChild(attGroupRefEl);
				}

				for (final String modelGroupName : namespaceGroups.modelGroupNames) {
					final Element complexTypeEl = dummySchema.createElementNS(GroupInterfacePlugin.XS_NS, "xs:complexType");
					complexTypeEl.setAttribute("name", "__" + modelGroupName + "_XXXXXX");
					rootEl.appendChild(complexTypeEl);

					final Element sequenceEl = dummySchema.createElementNS(GroupInterfacePlugin.XS_NS, "xs:sequence");
					complexTypeEl.appendChild(sequenceEl);

					final Element modelGroupRefEl = dummySchema.createElementNS(GroupInterfacePlugin.XS_NS, "xs:group");
					modelGroupRefEl.setAttribute("ref", targetNamespacePrefix +":" + modelGroupName);
					sequenceEl.appendChild(modelGroupRefEl);
				}

				final StringWriter stringWriter = new StringWriter();
				final StreamResult streamResult = new StreamResult(stringWriter);

				transformer.transform(new DOMSource(dummySchema), streamResult);

				final Reader stringReader = new ResettableStringReader(stringWriter.toString());

				final InputSource inputSource = new InputSource(stringReader);
				final int suffixPos = systemId.lastIndexOf('.');
				final String baseName = suffixPos > 0 ? systemId.substring(0,suffixPos) : systemId;
				inputSource.setSystemId(baseName + "-impl.xsd");
				return inputSource;
			} else {
				return null;
			}
	}

	private static class MappingNamespaceContext implements NamespaceContext {
		private final Map<String, List<String>> namespacesByUri = new HashMap<>();
		private final HashMap<String, String> namespacesByPrefix = new HashMap<>();

		public MappingNamespaceContext add(final String prefix, final String namespaceUri) {
			putMapValue(this.namespacesByUri, namespaceUri, prefix);
			this.namespacesByPrefix.put(prefix, namespaceUri);
			return this;
		}

		@Override
		public String getNamespaceURI(final String prefix) {
			return this.namespacesByPrefix.get(prefix);
		}

		@Override
		public String getPrefix(final String namespaceURI) {
			return getPrefixes(namespaceURI).hasNext() ? (String) getPrefixes(namespaceURI).next() : null;
		}

		@Override
		public Iterator getPrefixes(final String namespaceURI) {
			return getMapValues(namespacesByUri, namespaceURI).iterator();
		}

		private static List<String> getMapValues(final Map<String, List<String>> map, final String key) {
			final List<String> val = map.get(key);
			return val == null ? Collections.<String>emptyList() : val;
		}

		private static void putMapValue(final Map<String, List<String>> map, final String key, final String value) {
			List<String> values = map.get(key);
			if (values == null) {
				values = new ArrayList<>();
				map.put(key, values);
			}
			values.add(value);
		}
	}

	private static class Groups {
		public final String targetNamespace;
		public final List<String> attGroupNames = new ArrayList<>();
		public final List<String> modelGroupNames = new ArrayList<>();

		public Groups(final String targetNamespace) {
			this.targetNamespace = targetNamespace;
		}
	}
}