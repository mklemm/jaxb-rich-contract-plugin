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

package com.kscs.util.plugins.xjc.base;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Locale;
import java.util.ResourceBundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * @author Mirko Klemm 2015-02-18
 */
public class HtmlUsageBuilder extends PluginUsageBuilder {
	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
	private static final DocumentBuilder DOCUMENT_BUILDER;
	private static final String NAMESPACE = "http://www.w3.org/1999/xhtml";

	static {
		try {
			HtmlUsageBuilder.DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
			DOCUMENT_BUILDER = HtmlUsageBuilder.DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	private final Document document;
	private Element dl = null;
	private String pluginName = null;

	public HtmlUsageBuilder(final ResourceBundle baseResourceBundle, final ResourceBundle resourceBundle) {
		super(baseResourceBundle, resourceBundle);
		this.document = HtmlUsageBuilder.DOCUMENT_BUILDER.newDocument();
		this.document.setXmlStandalone(true);
		newElement(this.document, "section");
	}

	public static Element createBody(final String title, final String css, final boolean linkCss) {
		final Document document = HtmlUsageBuilder.DOCUMENT_BUILDER.newDocument();
		document.setXmlStandalone(true);
		final Element html = newElement(document, "html");
		html.setAttribute("lang", Locale.getDefault().getLanguage());
		html.setAttributeNS(Namespaces.XML_NS,"xml:lang", Locale.getDefault().getLanguage());
		final Element head = newElement(html, "head");
		newElement(head, "title", title);
		if (linkCss) {
			final Element link = newElement(head, "link");
			link.setAttribute("type", "text/css");
			link.setAttribute("rel", "stylesheet");
			link.setAttribute("href", css);
		} else {
			final Element style = newElement(head, "style", css);
			style.setAttribute("type", "text/css");
		}
		return newElement(html, "body");
	}

	private static Element newElement(final Node parent, final String name, final String textContent) {
		final Element el = newElement(parent, name);
		el.setTextContent(textContent);
		return el;
	}

	private static Text newText(final Node parent, final String textContent) {
		final Text text = parent.getOwnerDocument().createTextNode(textContent);
		parent.appendChild(text);
		return text;
	}

	private static Element newElement(final Node parent, final String name) {
		final Element el = ((parent instanceof Document) ? (Document) parent : parent.getOwnerDocument()).createElementNS(HtmlUsageBuilder.NAMESPACE, name);
		parent.appendChild(el);
		return el;
	}

	public HtmlUsageBuilder addMain(final String optionName) {
		this.pluginName = optionName;
		newElement("a").setAttribute("name", optionName);
		final Element h1 = newElement("h1");
		newText(h1, optionName);
		final Element p = newElement("p");
		p.setTextContent(this.resourceBundle.getString(this.keyBase));
		return this;
	}

	public <T> HtmlUsageBuilder addOption(final Option<?> option) {
		if (this.dl == null) {
			final Element h2 = newElement("h2");
			h2.setTextContent(this.baseResourceBundle.getString(this.keyBase + ".options"));
			this.dl = newElement("dl");
		}
		final String key = this.keyBase + "." + transformName(option.getName());
		final Element dt = newElement(this.dl, "dt");
		newElement(dt, "a").setAttribute("name", this.pluginName + "." + option.getName());
		newText(dt, "-" + option.getName() + "=");
		newElement(dt, "span", option.getChoice()).setAttribute("class","choice");
		newElement(dt, "span", option.getStringValue()).setAttribute("class", "default");
		newElement(this.dl, "dd", this.resourceBundle.getString(key));
		return this;
	}

	public Document build(final Node parent) {
		final Node importedNode = parent.getOwnerDocument().importNode(this.document.getDocumentElement(), true);
		parent.appendChild(importedNode);
		return parent.getOwnerDocument();
	}

	private Element newElement(final String name) {
		final Element el = this.document.createElementNS(HtmlUsageBuilder.NAMESPACE, name);
		this.document.getDocumentElement().appendChild(el);
		return el;
	}

}
