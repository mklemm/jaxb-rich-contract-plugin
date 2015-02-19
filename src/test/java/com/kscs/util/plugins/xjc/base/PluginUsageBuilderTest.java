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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import com.kscs.util.plugins.xjc.BoundPropertiesPlugin;
import com.kscs.util.plugins.xjc.DeepClonePlugin;
import com.kscs.util.plugins.xjc.DeepCopyPlugin;
import com.kscs.util.plugins.xjc.FluentBuilderPlugin;
import com.kscs.util.plugins.xjc.GroupInterfacePlugin;
import com.kscs.util.plugins.xjc.ImmutablePlugin;
import com.kscs.util.plugins.xjc.MetaPlugin;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test usage output
 */
public class PluginUsageBuilderTest {
	@Test
	public void testPluginUsageFluentBuilder() {
		final FluentBuilderPlugin plugin = new FluentBuilderPlugin();
		System.out.println(plugin.getUsage());
	}

	@Test
	public void testPluginUsageImmutable() {
		final ImmutablePlugin plugin = new ImmutablePlugin();
		System.out.println(plugin.getUsage());
	}

	@Test
	public void testPluginUsageGroupContract() {
		final GroupInterfacePlugin plugin = new GroupInterfacePlugin();
		System.out.println(plugin.getUsage());
	}

	@Test
	public void testPluginUsageDeepClone() {
		final DeepClonePlugin plugin = new DeepClonePlugin();
		System.out.println(plugin.getUsage());
	}

	@Test
	public void testPluginUsageDeepCopy() {
		final DeepCopyPlugin plugin = new DeepCopyPlugin();
		System.out.println(plugin.getUsage());
	}

	@Test
	public void testPluginUsageBoundProperties() {
		final BoundPropertiesPlugin plugin = new BoundPropertiesPlugin();
		System.out.println(plugin.getUsage());
	}

	@Test
	public void testPluginUsageMeta() {
		final MetaPlugin plugin = new MetaPlugin();
		System.out.println(plugin.getUsage());
	}

	@Test
	public void testPluginUsageHtml() throws TransformerException {
		final Element body = HtmlUsageBuilder.createBody("Plugin Usage", "body {font-family: sans-serif;}", false);
		new FluentBuilderPlugin().getUsageHtml(body);
		new ImmutablePlugin().getUsageHtml(body);
		new GroupInterfacePlugin().getUsageHtml(body);
		new DeepClonePlugin().getUsageHtml(body);
		new DeepCopyPlugin().getUsageHtml(body);
		new BoundPropertiesPlugin().getUsageHtml(body);
		final Document doc = new MetaPlugin().getUsageHtml(body);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");
		transformer.transform(new DOMSource(doc), new StreamResult(System.out));
	}
}
