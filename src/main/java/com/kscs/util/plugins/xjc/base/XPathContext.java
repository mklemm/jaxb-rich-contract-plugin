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

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * @author Mirko Klemm 2015-03-10
 */
public class XPathContext {
	private final XPathExpression expression;

	public XPathContext(final XPathExpression expression) {
		this.expression = expression;
	}

	public Element selectElement(final Node node) throws XPathExpressionException {
		return (Element)this.expression.evaluate(node, XPathConstants.NODE);
	}

	public Element selectElement(final InputSource src) throws XPathExpressionException {
		return (Element)this.expression.evaluate(src, XPathConstants.NODE);
	}

	public List<Element> selectElements(final Node node) throws XPathExpressionException {
		final NodeList nodeList = (NodeList)this.expression.evaluate(node, XPathConstants.NODESET);
		final List<Element> result = new ArrayList<>(nodeList.getLength());
		for(int i = 0; i < nodeList.getLength(); i++) {
			result.add((Element)nodeList.item(i));
		}
		return result;
	}

	public List<Element> selectElements(final InputSource src) throws XPathExpressionException {
		final NodeList nodeList = (NodeList)this.expression.evaluate(src, XPathConstants.NODESET);
		final List<Element> result = new ArrayList<>(nodeList.getLength());
		for(int i = 0; i < nodeList.getLength(); i++) {
			result.add((Element)nodeList.item(i));
		}
		return result;
	}
	public Attr selectAttribute(final Node node) throws XPathExpressionException {
		return (Attr)this.expression.evaluate(node, XPathConstants.NODE);
	}

	public Attr selectAttribute(final InputSource src) throws XPathExpressionException {
		return (Attr)this.expression.evaluate(src, XPathConstants.NODE);
	}

	public List<Attr> selectAttributes(final Node node) throws XPathExpressionException {
		final NodeList nodeList = (NodeList)this.expression.evaluate(node, XPathConstants.NODESET);
		final List<Attr> result = new ArrayList<>(nodeList.getLength());
		for(int i = 0; i < nodeList.getLength(); i++) {
			result.add((Attr)nodeList.item(i));
		}
		return result;
	}

	public List<Attr> selectAttributes(final InputSource src) throws XPathExpressionException {
		final NodeList nodeList = (NodeList)this.expression.evaluate(src, XPathConstants.NODESET);
		final List<Attr> result = new ArrayList<>(nodeList.getLength());
		for(int i = 0; i < nodeList.getLength(); i++) {
			result.add((Attr)nodeList.item(i));
		}
		return result;
	}

	public String selectText(final Node node) throws XPathExpressionException {
		return (String)this.expression.evaluate(node, XPathConstants.STRING);
	}

	public String selectText(final InputSource src) throws XPathExpressionException {
		return (String)this.expression.evaluate(src, XPathConstants.STRING);
	}

	public List<String> selectTextNodes(final Node node) throws XPathExpressionException {
		final NodeList nodeList = (NodeList)this.expression.evaluate(node, XPathConstants.NODESET);
		final List<String> result = new ArrayList<>(nodeList.getLength());
		for(int i = 0; i < nodeList.getLength(); i++) {
			result.add(((Text)nodeList.item(i)).getWholeText());
		}
		return result;
	}

	public List<String> selectTextNodes(final InputSource src) throws XPathExpressionException {
		final NodeList nodeList = (NodeList)this.expression.evaluate(src, XPathConstants.NODESET);
		final List<String> result = new ArrayList<>(nodeList.getLength());
		for(int i = 0; i < nodeList.getLength(); i++) {
			result.add(((Text)nodeList.item(i)).getWholeText());
		}
		return result;
	}


}
