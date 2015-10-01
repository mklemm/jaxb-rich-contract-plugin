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

package com.kscs.util.plugins.xjc.doc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.HtmlUsageBuilder;
import com.kscs.util.plugins.xjc.base.MarkdownPluginUsageBuilder;
import com.kscs.util.plugins.xjc.base.Option;
import com.kscs.util.plugins.xjc.base.PropertyDirectoryResourceBundle;

/**
 * @author Mirko Klemm 2015-02-19
 */
public class PluginDocumentationFormatter {
	public static final Pattern INDEX_PATTERN = Pattern.compile("^\\[\\d\\]: .*$");
	public static final ResourceBundle RES = PropertyDirectoryResourceBundle.getInstance(PluginDocumentationFormatter.class);
	private final ResourceBundle baseResourceBundle;
	private final ResourceBundle resourceBundle;
	private final Locale locale;
	private final AbstractPlugin plugin;

	protected PluginDocumentationFormatter(final AbstractPlugin plugin, final Locale locale) {
		this.plugin = plugin;
		this.baseResourceBundle = PropertyDirectoryResourceBundle.getInstance(AbstractPlugin.class, locale);
		this.resourceBundle = PropertyDirectoryResourceBundle.getInstance(plugin.getClass(), locale);
		this.locale = locale;
	}

	public Locale getLocale() {
		return this.locale;
	}

	public AbstractPlugin getPlugin() {
		return this.plugin;
	}

	public Document getUsageHtml(final Node parent) {
		final HtmlUsageBuilder pluginUsageBuilder = new HtmlUsageBuilder(this.baseResourceBundle, this.resourceBundle);
		pluginUsageBuilder.addMain(this.plugin.getOptionName().substring(1));
		for (final Option<?> option : this.plugin.getOptions()) {
			pluginUsageBuilder.addOption(option);
		}
		return pluginUsageBuilder.build(parent);
	}

	public String getUsageMarkdown() {
		final MarkdownPluginUsageBuilder pluginUsageBuilder = new MarkdownPluginUsageBuilder(this.baseResourceBundle, this.resourceBundle);
		pluginUsageBuilder.addMain(this.plugin.getOptionName().substring(1));
		for (final Option<?> option : this.plugin.getOptions()) {
			pluginUsageBuilder.addOption(option);
		}
		return pluginUsageBuilder.build();
	}

	public String getUsageFileName() {
		return this.plugin.getOptionName().substring(1) + (Locale.ROOT.equals(this.locale) ? "" : "_" + this.locale.toLanguageTag());
	}

	public String getConfigCheatSheet(final int tabAmount, final String argPrefix, final String argSuffix) {
		final class A {
			private String arg(final String s) {
				return argPrefix + s + argSuffix;
			}

			private String tab(final int tabAmount) {
				final StringBuilder sb = new StringBuilder();
				for (int i = 0; i < tabAmount; i++) {
					sb.append("    ");
				}
				return sb.toString();
			}
		}
		A a = new A();
		final StringWriter sw = new StringWriter();
		try(final PrintWriter w = new PrintWriter(sw)) {
			w.print(a.tab(tabAmount));
			w.println(a.arg(this.plugin.getOptionName()));
			for (final Option option : this.plugin.getOptions()) {
				w.print(a.tab(tabAmount + 1));
				w.println(a.arg(option.getName() + "=" + option.getStringValue()));
			}
			w.flush();
			return sw.toString();
		}
	}


}
