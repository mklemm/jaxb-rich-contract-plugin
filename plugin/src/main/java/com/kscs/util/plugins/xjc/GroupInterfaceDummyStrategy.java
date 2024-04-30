package com.kscs.util.plugins.xjc;/*
 * GNU General Public License
 *
 * Copyright (c) 2018 Klemm Software Consulting, Mirko Klemm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

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
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.kscs.util.jaxb.bindings.Interface;
import com.kscs.util.plugins.xjc.base.MappingNamespaceContext;
import com.kscs.util.plugins.xjc.base.Namespaces;
import com.kscs.util.plugins.xjc.base.XPathContext;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.Model;

import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

public class GroupInterfaceDummyStrategy implements GroupInterfaceModelProcessingStrategy {
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(GroupInterfaceDummyStrategy.class.getName());
	private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();
	public static final TransformerFactory TRANSFORMER_FACTORY;
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;
	private static final DocumentBuilder DOCUMENT_BUILDER;

	static {
		try {
			TRANSFORMER_FACTORY = TransformerFactory.newInstance();
			DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
			GroupInterfaceDummyStrategy.DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
			DOCUMENT_BUILDER = GroupInterfaceDummyStrategy.DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates dummy complexTypes that implement the interface generated from group decls.
	 * These complexTypes will be transformed into classes by the default code generator.
	 * Later, this plugin will transform the classes into interface declarations.
	 * This approach avoids tedious re-implementation of the property generation code,
	 * with all the effects of settings, options, and customizations, in this plugin.
	 *
	 * @param opts Options given to XJC
	 * @throws BadCommandLineException
	 */
	public void generateDummyGroupUsages(final Options opts) throws BadCommandLineException {
		try {
			final Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
			final XPath xPath = X_PATH_FACTORY.newXPath();
			final MappingNamespaceContext namespaceContext = new MappingNamespaceContext()
					.add("xs", Namespaces.XS_NS)
					.add("kscs", Namespaces.KSCS_BINDINGS_NS)
					.add("jxb", Namespaces.JAXB_NS);
			xPath.setNamespaceContext(namespaceContext);
			final XPathExpression attGroupExpression = xPath.compile("/xs:schema/xs:attributeGroup");
			final XPathExpression modelGroupExpression = xPath.compile("/xs:schema/xs:group");
			final XPathExpression targetNamespaceExpression = xPath.compile("/xs:schema/@targetNamespace");
			final Map<String, List<String>> includeMappings = findIncludeMappings(xPath, opts.getGrammars());
			final List<InputSource> newGrammars = new ArrayList<>();
			for (final InputSource grammarSource : opts.getGrammars()) {
				final Document grammar = copyInputSource(grammarSource);
				final String declaredTargetNamespaceUri = targetNamespaceExpression.evaluate(grammar);
				for (final String targetNamespaceUri : declaredTargetNamespaceUri == null ? includeMappings.get(grammarSource.getSystemId()) : Collections.singletonList(declaredTargetNamespaceUri)) {
					final NodeList attGroupNodes = (NodeList)attGroupExpression.evaluate(grammar, XPathConstants.NODESET);
					final NodeList modelGroupNodes = (NodeList)modelGroupExpression.evaluate(grammar, XPathConstants.NODESET);
					final Groups currentGroups = new Groups(targetNamespaceUri);
					for (int i = 0; i < attGroupNodes.getLength(); i++) {
						final Element node = (Element)attGroupNodes.item(i);
						currentGroups.attGroupNames.add(node.getAttribute("name"));
					}
					for (int i = 0; i < modelGroupNodes.getLength(); i++) {
						final Element node = (Element)modelGroupNodes.item(i);
						currentGroups.modelGroupNames.add(node.getAttribute("name"));
					}
					final InputSource newSchema = generateImplementationSchema(opts, transformer, currentGroups, grammarSource.getSystemId());
					if (newSchema != null) {
						newGrammars.add(newSchema);
					}
				}
			}
			for (final InputSource newGrammar : newGrammars) {
				opts.addGrammar(newGrammar);
			}
		} catch (final Exception e) {
			throw new BadCommandLineException(formatMessage("error.plugin-setup", e));
		}
	}

	private static Document copyInputSource(final InputSource inputSource) throws IOException, SAXException {
		final Reader characterStream = inputSource.getCharacterStream(); // aliasing because of possible getCharacterStream side effects
		final InputStream byteStream = inputSource.getByteStream(); // aliasing because of possible getByteStream side effects
		final var validSystemIdOrNull = inputSource.getSystemId().equals("null") ? null : inputSource.getSystemId();
		if (characterStream != null) {
			final StringWriter stringWriter = new StringWriter();
			final BufferedReader bufferedReader = new BufferedReader(characterStream);
			try (final PrintWriter printWriter = new PrintWriter(stringWriter)) {
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					printWriter.println(line);
				}
			}
			inputSource.setCharacterStream(new StringReader(stringWriter.toString()));
			final InputSource copy = new InputSource(new StringReader(stringWriter.toString()));
			copy.setSystemId(validSystemIdOrNull);
			copy.setPublicId(inputSource.getPublicId());
			copy.setEncoding(inputSource.getEncoding());
			return DOCUMENT_BUILDER.parse(copy);
		} else if (byteStream != null) {
			final int blockSize = 8192;
			final byte[] buffer = new byte[blockSize];
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			int bytesRead;
			while ((bytesRead = byteStream.read(buffer, 0, blockSize)) > -1) {
				byteArrayOutputStream.write(buffer, 0, bytesRead);
			}
			byteArrayOutputStream.close();
			final byte[] allBytes = byteArrayOutputStream.toByteArray();
			inputSource.setByteStream(new ByteArrayInputStream(allBytes));
			final InputSource copy = new InputSource(new ByteArrayInputStream(allBytes));
			copy.setSystemId(validSystemIdOrNull);
			copy.setPublicId(inputSource.getPublicId());
			copy.setEncoding(inputSource.getEncoding());
			return DOCUMENT_BUILDER.parse(copy);
		} else {
			final InputSource copy = new InputSource(validSystemIdOrNull);
			copy.setPublicId(inputSource.getPublicId());
			copy.setEncoding(inputSource.getEncoding());
			return DOCUMENT_BUILDER.parse(copy);
		}
	}

	private static InputSource generateImplementationSchema(final Options opts, final Transformer transformer, final Groups namespaceGroups, final String systemId) throws TransformerException {
		if (!namespaceGroups.attGroupNames.isEmpty() || !namespaceGroups.modelGroupNames.isEmpty()) {
			final Document dummySchema = DOCUMENT_BUILDER.newDocument();
			dummySchema.setXmlVersion("1.0");
			// Treat "http://www.w3.org/XML/1998/namespace" namespace specially, because the ns prefix always has to be "xml" for this
			// and only this namespace.
			final String targetNamespacePrefix = Namespaces.XML_NS.equals(namespaceGroups.targetNamespace) ? "xml" : "tns";
			final Element rootEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:schema");
			rootEl.setAttribute("version", "1.0");
			rootEl.setAttribute("targetNamespace", namespaceGroups.targetNamespace);
			rootEl.setAttribute("elementFormDefault", "qualified");
			rootEl.setAttribute("xmlns:" + targetNamespacePrefix, namespaceGroups.targetNamespace);
			dummySchema.appendChild(rootEl);
			final Element importEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:include");
			importEl.setAttribute("schemaLocation", systemId);
			rootEl.appendChild(importEl);
			for (final String attGroupName : namespaceGroups.attGroupNames) {
				final Element complexTypeEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:complexType");
				complexTypeEl.setAttribute("name", attGroupName);
				rootEl.appendChild(complexTypeEl);
				final Element attGroupRefEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:attributeGroup");
				attGroupRefEl.setAttribute("ref", targetNamespacePrefix + ":" + attGroupName);
				complexTypeEl.appendChild(attGroupRefEl);
			}
			for (final String modelGroupName : namespaceGroups.modelGroupNames) {
				final Element complexTypeEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:complexType");
				complexTypeEl.setAttribute("name", modelGroupName);
				rootEl.appendChild(complexTypeEl);
				final Element sequenceEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:sequence");
				complexTypeEl.appendChild(sequenceEl);
				final Element modelGroupRefEl = dummySchema.createElementNS(Namespaces.XS_NS, "xs:group");
				modelGroupRefEl.setAttribute("ref", targetNamespacePrefix + ":" + modelGroupName);
				sequenceEl.appendChild(modelGroupRefEl);
			}
			final StringWriter stringWriter = new StringWriter();
			final StreamResult streamResult = new StreamResult(stringWriter);
			transformer.transform(new DOMSource(dummySchema), streamResult);
			final Reader stringReader = new ResettableStringReader(stringWriter.toString());
			final InputSource inputSource = new InputSource(stringReader);
			final int suffixPos = systemId.lastIndexOf('.');
			final String baseName = suffixPos > 0 ? systemId.substring(0, suffixPos) : systemId;
			inputSource.setSystemId(baseName + "-impl.xsd");
			return inputSource;
		} else {
			return null;
		}
	}

	private static Map<String, List<String>> findIncludeMappings(final XPath xPath, final InputSource[] grammars) throws XPathExpressionException {
		final XPathExpression includeExpression = xPath.compile("/xs:schema/xs:include/@schemaLocation");
		final XPathExpression targetNamespaceExpression = xPath.compile("/xs:schema/@targetNamespace");
		final Map<String, List<String>> mappings = new HashMap<>();
		for (final InputSource grammar : grammars) {
			final Node targetNamespaceNode = (Node)targetNamespaceExpression.evaluate(grammar, XPathConstants.NODE);
			final NodeList includeNodes = (NodeList)includeExpression.evaluate(grammar, XPathConstants.NODESET);
			if (targetNamespaceNode != null) {
				for (int i = 0; i < includeNodes.getLength(); i++) {
					final String includedSchema = includeNodes.item(i).getNodeValue();
					List<String> includers = mappings.get(includedSchema);
					if (includers == null) {
						includers = new ArrayList<>();
						mappings.put(includedSchema, includers);
					}
					includers.add(targetNamespaceNode.getNodeValue());
				}
			}
		}
		return mappings;
	}

	private void processCustomizations(final Element elementAnnotation, final NodeList childAnnotations) {
		final Interface interfaceCustomization = JAXB.unmarshal(new DOMSource(elementAnnotation), Interface.class);
	}

	@Override
	public void onPostProcessModel(final Model model, final ErrorHandler errorHandler) {
		// no op
	}

	@Override
	public void onPluginActivation(final Options opts) throws BadCommandLineException {
		generateDummyGroupUsages(opts);
	}

	private static class Groups {
		public final String targetNamespace;
		public final List<String> attGroupNames = new ArrayList<>();
		public final List<String> modelGroupNames = new ArrayList<>();

		public Groups(final String targetNamespace) {
			this.targetNamespace = targetNamespace;
		}
	}

	private static class CustomizationParser {
		private CustomizationContext rootCustomizationContext = new CustomizationContext("global", null, null, null, null);
		private final XPath xPath;
		private final XPathContext elementDefsExpression;
		private final XPathContext attributeDefsExpression;
		private final XPathContext attGroupDefsExpression;
		private final XPathContext modelGroupDefsExpression;
		private final XPathContext descendantElementDefsExpression;
		private final XPathContext descendantInterfaceExpression;
		private final XPathContext targetPathExpression;
		private final XPathContext schemaLocationExpression;
		private final XPathContext customizationExpression;
		private final InputSource[] bindFiles;
		private final Map<String, InputSource> grammars;
		private final Unmarshaller unmarshaller;

		CustomizationParser(final XPath xPath, final InputSource[] grammars, final InputSource[] bindFiles) throws XPathExpressionException, JAXBException {
			this.xPath = xPath;
			this.elementDefsExpression = new XPathContext(xPath.compile("xs:element[xs:annotation/xs:appInfo/kscs:interface]"));
			this.attributeDefsExpression = new XPathContext(xPath.compile("xs:attribute[xs:annotation/xs:appInfo/kscs:interface]"));
			this.attGroupDefsExpression = new XPathContext(xPath.compile("xs:attributeGroup[xs:annotation/xs:appInfo/kscs:interface or xs:attribute[xs:annotation/xs:appInfo/kscs:interface]]"));
			this.modelGroupDefsExpression = new XPathContext(xPath.compile("xs:group[xs:annotation/xs:appInfo/kscs:interface or descendant::xs:element[xs:annotation/xs:appInfo/kscs:interface]]"));
			this.descendantElementDefsExpression = new XPathContext(xPath.compile("descendant::xs:element[xs:annotation/xs:appInfo/kscs:interface]"));
			this.descendantInterfaceExpression = new XPathContext(xPath.compile("descendant::kscs:interface"));
			this.targetPathExpression = new XPathContext(xPath.compile("ancestor::jxb:bindings/@node"));
			this.schemaLocationExpression = new XPathContext(xPath.compile("ancestor::jxb:bindings/@schemaLocation"));
			this.customizationExpression = new XPathContext(xPath.compile("xs:annotation/xs:appInfo/kscs:interface"));
			this.grammars = new HashMap<>(grammars.length);
			for (final InputSource grammar : grammars) {
				this.grammars.put(grammar.getSystemId(), grammar);
			}
			this.bindFiles = bindFiles;
			this.unmarshaller = JAXBContext.newInstance(Interface.class).createUnmarshaller();
		}

		private void inlineBindings() throws XPathExpressionException, JAXBException {
			for (final InputSource bindFile : this.bindFiles) {
				for (final Element customizationElement : this.descendantInterfaceExpression.selectElements(bindFile)) {
					StringBuilder targetPathBuilder = null;
					final List<Element> targetPathNodes = this.targetPathExpression.selectElements(customizationElement);
					for (int t = targetPathNodes.size() - 1; t <= 0; t--) {
						final Node node = targetPathNodes.get(t);
						if (targetPathBuilder == null) {
							targetPathBuilder = new StringBuilder();
						} else {
							targetPathBuilder.append("/");
						}
						targetPathBuilder.append(node.getNodeValue());
					}
					if (targetPathBuilder != null) {
						final XPathContext targetPath = new XPathContext(this.xPath.compile(targetPathBuilder.toString()));
						final String schemaLocation = this.schemaLocationExpression.selectText(customizationElement);
						final URI bindFileUri = URI.create(bindFile.getSystemId());
						final InputSource grammar = this.grammars.get(bindFileUri.resolve(schemaLocation).toString());
						final Element targetElement = targetPath.selectElement(grammar);
						if (targetElement != null) {
							createAnnotation(targetElement, customizationElement);
						}
					} else {
						this.rootCustomizationContext = new CustomizationContext("global", null, null, this.unmarshaller.unmarshal(new DOMSource(customizationElement), Interface.class).getValue(), null);
					}
				}
			}
		}

		private void createAnnotation(final Element targetElement, final Element customizationElement) {
			final Document document = targetElement.getOwnerDocument();
			final Node imported = document.importNode(customizationElement, true);
			final Element annotationEl = document.createElementNS(Namespaces.XS_NS, "xs:annotation");
			final Element appInfoEl = document.createElementNS(Namespaces.XS_NS, "xs:appInfo");
			appInfoEl.appendChild(imported);
			annotationEl.appendChild(appInfoEl);
			targetElement.appendChild(annotationEl);
		}

		void parse() throws XPathExpressionException, JAXBException, IOException, SAXException {
			inlineBindings();
			for (final InputSource grammar : this.grammars.values()) {
				final Document doc = DOCUMENT_BUILDER.parse(grammar);
				final Element schemaEl = doc.getDocumentElement();
				final String targetNamespace = schemaEl.getAttribute("targetNamespace");
				final CustomizationContext schemaContext = parse(this.rootCustomizationContext, schemaEl, targetNamespace);
				for (final Element elementEl : this.elementDefsExpression.selectElements(schemaEl)) {
					parse(schemaContext, elementEl, targetNamespace);
				}
				for (final Element attributeEl : this.attributeDefsExpression.selectElements(schemaEl)) {
					parse(schemaContext, attributeEl, targetNamespace);
				}
				for (final Element groupEl : this.modelGroupDefsExpression.selectElements(schemaEl)) {
					final CustomizationContext groupContext = parse(schemaContext, groupEl, targetNamespace);
					for (final Element elementEl : this.descendantElementDefsExpression.selectElements(groupEl)) {
						parse(groupContext, elementEl, targetNamespace);
					}
				}
				for (final Element attGroupEl : this.attGroupDefsExpression.selectElements(schemaEl)) {
					final CustomizationContext groupContext = parse(schemaContext, attGroupEl, targetNamespace);
					for (final Element attributeEl : this.descendantElementDefsExpression.selectElements(attGroupEl)) {
						parse(groupContext, attributeEl, targetNamespace);
					}
				}
			}
		}

		private CustomizationContext parse(final CustomizationContext parent, final Element groupElement, final String targetNamespace) throws XPathExpressionException, JAXBException {
			final Element interfaceCustomizationEl = this.customizationExpression.selectElement(groupElement);
			final Interface interfaceCustomization = interfaceCustomizationEl != null ? this.unmarshaller.unmarshal(interfaceCustomizationEl, Interface.class).getValue() : null;
			return new CustomizationContext(groupElement.getLocalName(), targetNamespace, groupElement.getAttribute("name"), interfaceCustomization, parent);
		}
	}

	public static class CustomizationContext {
		private final String type;
		private final String name;
		private final String targetNamespace;
		private final Interface item;
		private final CustomizationContext parent;
		private final Map<String, CustomizationContext> children = new HashMap<>();

		public CustomizationContext(final String type, final String targetNamespace, final String name, final Interface item, final CustomizationContext parent) {
			this.type = type;
			this.targetNamespace = targetNamespace;
			this.name = name;
			this.item = item;
			this.parent = parent;
			this.parent.children.put(getQualifiedName(), this);
		}

		public final String getQualifiedName() {
			return this.type + (this.targetNamespace == null ? "::" : "::{" + this.targetNamespace + "}") + (this.name == null ? "" : this.name);
		}

		public String getType() {
			return this.type;
		}

		public String getName() {
			return this.name;
		}

		public Interface getItem() {
			return this.item;
		}

		public CustomizationContext getParent() {
			return this.parent;
		}

		public Map<String, CustomizationContext> getChildren() {
			return this.children;
		}

		public boolean isEmpty() {
			return this.item == null && this.children.isEmpty();
		}
	}

	private static String formatMessage(final String messageKey, final Object... args) {
		return MessageFormat.format(RESOURCE_BUNDLE.getString(messageKey), args);
	}
}
