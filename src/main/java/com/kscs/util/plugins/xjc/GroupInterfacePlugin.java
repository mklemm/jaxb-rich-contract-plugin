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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.MappingNamespaceContext;
import com.kscs.util.plugins.xjc.base.Namespaces;
import com.kscs.util.plugins.xjc.base.Opt;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Generates interface declarations from &lt;group&gt; and &lt;attributeGroup&gt;
 * XSD declarations, and makes classes generated from complexTypes that use
 * these declarations implement the generated interface.
 */
public class GroupInterfacePlugin extends AbstractPlugin {
	private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
	@Opt
	private boolean declareSetters = true;
	@Opt
	private boolean declareBuilderInterface = true;
	@Opt
	private String upstreamEpisodeFile = "/META-INF/jaxb-interfaces.episode";
	@Opt
	private String downstreamEpisodeFile = "/META-INF/jaxb-interfaces.episode";
	private GroupInterfaceGenerator generator = null;
	public static final TransformerFactory TRANSFORMER_FACTORY;
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;
	private static final DocumentBuilder DOCUMENT_BUILDER;

	static {
		try {
			TRANSFORMER_FACTORY = TransformerFactory.newInstance();
			DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
			GroupInterfacePlugin.DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
			DOCUMENT_BUILDER = GroupInterfacePlugin.DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
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
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler)
			throws SAXException {
		generate(new ApiConstructs(outline, opt, errorHandler));
		return true;
	}

	public List<InterfaceOutline> getGroupInterfacesForClass(final ApiConstructs apiConstructs, final ClassOutline classOutline) throws SAXException {
		generate(apiConstructs);
		return this.generator.getGroupInterfacesForClass(classOutline);
	}

	private void generate(final ApiConstructs apiConstructs) throws SAXException {
		if(this.generator == null) {
			final URL interfaceEpisodeURL = getClass().getResource(this.upstreamEpisodeFile);
			final EpisodeBuilder episodeBuilder = new EpisodeBuilder(apiConstructs, this.downstreamEpisodeFile);
			final FluentBuilderPlugin fluentBuilderPlugin = apiConstructs.findPlugin(FluentBuilderPlugin.class);
			if(fluentBuilderPlugin != null) {
				this.generator = new GroupInterfaceGenerator(apiConstructs, interfaceEpisodeURL, episodeBuilder, this.declareSetters, this.declareBuilderInterface, fluentBuilderPlugin.newBuilderMethodName, fluentBuilderPlugin.newCopyBuilderMethodName);
			} else {
				this.generator = new GroupInterfaceGenerator(apiConstructs, interfaceEpisodeURL, episodeBuilder, this.declareSetters, false, null, null);
			}
			this.generator.generateGroupInterfaceModel();
			episodeBuilder.build();
		}
	}

	@Override
	public void onActivated(final Options opts) throws BadCommandLineException {
		generateDummyGroupUsages(opts);
	}

	/**
	 * Generates dummy complexTypes that implement the interface generated from group decls.
	 * These complexTypes will be transformed into classes by the default code generator.
	 * Later, this plugin will transform the classes into interface declarations.
	 * This approach avoids tedious re-implementation of the property generation code,
	 * with all the effects of settings, options, and customizations, in this plugin.
	 * @param opts Options given to XJC
	 * @throws BadCommandLineException
	 */
	private void generateDummyGroupUsages(final Options opts) throws BadCommandLineException {
		try {
			final Transformer transformer = GroupInterfacePlugin.TRANSFORMER_FACTORY.newTransformer();
			final XPath xPath = GroupInterfacePlugin.X_PATH_FACTORY.newXPath();
			final MappingNamespaceContext namespaceContext = new MappingNamespaceContext()
					.add("xs", Namespaces.XS_NS)
					.add("kscs", Namespaces.KSCS_BINDINGS_NS)
					.add("jxb", Namespaces.JAXB_NS);
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

				final InputSource newSchema = generateImplementationSchema(opts, transformer, currentGroups, schemaCopy.getSystemId());
				if(newSchema != null) {
					newGrammars.add(newSchema);
				}
			}

			for(final InputSource newGrammar : newGrammars) {
				opts.addGrammar(newGrammar);
			}
		} catch(final Exception e) {
			throw new BadCommandLineException(getMessage("error.plugin-setup", e));
		}
	}

	private static InputSource generateImplementationSchema(final Options opts, final Transformer transformer, final Groups namespaceGroups, final String systemId) throws TransformerException {
			if(!namespaceGroups.attGroupNames.isEmpty() || !namespaceGroups.modelGroupNames.isEmpty()) {
				final Document dummySchema = GroupInterfacePlugin.DOCUMENT_BUILDER.newDocument();
				dummySchema.setXmlVersion("1.0");
				// Treat "http://www.w3.org/XML/1998/namespace" namespace specially, because the ns prefix always has to be "xml" for this
				// and only this namespace.
				final String targetNamespacePrefix = Namespaces.XML_NS.equals(namespaceGroups.targetNamespace) ? "xml" : "tns";
				final Element rootEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:schema");
				rootEl.setAttribute("version", "1.0");
				rootEl.setAttribute("targetNamespace", namespaceGroups.targetNamespace);
				rootEl.setAttribute("elementFormDefault", "qualified");
				rootEl.setAttribute("xmlns:"+targetNamespacePrefix, namespaceGroups.targetNamespace);
				dummySchema.appendChild(rootEl);

				final Element importEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:include");
				importEl.setAttribute("schemaLocation", systemId);
				rootEl.appendChild(importEl);

				for (final String attGroupName : namespaceGroups.attGroupNames) {
					final Element complexTypeEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:complexType");
					complexTypeEl.setAttribute("name", attGroupName);
					rootEl.appendChild(complexTypeEl);

					final Element attGroupRefEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:attributeGroup");
					attGroupRefEl.setAttribute("ref", targetNamespacePrefix +":" + attGroupName);
					complexTypeEl.appendChild(attGroupRefEl);
				}

				for (final String modelGroupName : namespaceGroups.modelGroupNames) {
					final Element complexTypeEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:complexType");
					complexTypeEl.setAttribute("name", modelGroupName);
					rootEl.appendChild(complexTypeEl);

					final Element sequenceEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:sequence");
					complexTypeEl.appendChild(sequenceEl);

					final Element modelGroupRefEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:group");
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


	private static class Groups {
		public final String targetNamespace;
		public final List<String> attGroupNames = new ArrayList<>();
		public final List<String> modelGroupNames = new ArrayList<>();

		public Groups(final String targetNamespace) {
			this.targetNamespace = targetNamespace;
		}
	}
}
