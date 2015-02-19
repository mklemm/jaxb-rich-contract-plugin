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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import com.kscs.util.plugins.xjc.base.HtmlUsageBuilder;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Mirko Klemm 2015-02-19
 */
public class UsagePrinter {
	public static final ResourceBundle RES = ResourceBundle.getBundle(UsagePrinter.class.getName());
	public static void main(final String[] args) throws TransformerException {
		if(args.length < 1) {
			printUsage(new StreamResult(System.out), Locale.getDefault());
		} else {
			final File file = new File(args[0]);
			if(file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			final int dotIndex = args[0].lastIndexOf('.');
			final String basename = args[0].substring(0, dotIndex);
			final String suffix = args[0].substring(dotIndex);
			for(final String localeSpec : Arrays.asList("en", "de")) {
				final Locale locale = Locale.forLanguageTag(localeSpec);
				printUsage(new StreamResult(basename + "_" + localeSpec + suffix), locale);
			}
		}
	}

	private static void printUsage(final StreamResult sr, final Locale locale) throws TransformerException {
		final Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(locale);
		final Element body = HtmlUsageBuilder.createBody(RES.getString("title"), "usage.css", true);
		new FluentBuilderPlugin().getUsageHtml(body);
		new ImmutablePlugin().getUsageHtml(body);
		new GroupInterfacePlugin().getUsageHtml(body);
		new DeepClonePlugin().getUsageHtml(body);
		new DeepCopyPlugin().getUsageHtml(body);
		new BoundPropertiesPlugin().getUsageHtml(body);
		final Document doc = new MetaPlugin().getUsageHtml(body);
		Locale.setDefault(defaultLocale);
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");
		transformer.transform(new DOMSource(doc), sr);
	}
}
