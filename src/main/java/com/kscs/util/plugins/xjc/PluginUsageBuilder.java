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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.*;

/**
 * Generates a formatted output of plugin usage information
 *
 * @author mirko 2014-04-03
 */
public class PluginUsageBuilder {
	private static final Map<Class<?>,String> OPTION_USAGES = new HashMap<Class<?>, String>() {{
		put(boolean.class, "-{2}.{0}='{true|false}' ({1})");
		put(Boolean.class, "-{2}.{0}='{true|false}' ({1})");
		put(String.class, "-{2}.{0}=<string> ({1})");
	}};
	public static final int OPTION_LINE_LENGTH = 70;
	public static final int HEADER_LINE_LENGTH = 80;

	private final StringWriter stringWriter = new StringWriter();
	private final PrintWriter writer = new PrintWriter(this.stringWriter);
	private final ResourceBundle resourceBundle;
	private final String keyBase;
	private final String pluginName;

	public PluginUsageBuilder(final ResourceBundle resourceBundle, final String keyBase, final String pluginName) {
		this.resourceBundle = resourceBundle;
		this.keyBase = keyBase;
		this.pluginName = pluginName;
		addMain(pluginName);
	}

	public PluginUsageBuilder(final Class<?> pluginClass, final String pluginName) {
		this(ResourceBundle.getBundle(pluginClass.getName()), "usage", pluginName);
	}

	private PluginUsageBuilder addMain(final String optionName) {
		this.writer.print(this.resourceBundle.getString(this.keyBase + ".usage"));
		this.writer.println(": -X" + optionName);
		this.writer.println();
		for (final String line : chopLines(PluginUsageBuilder.HEADER_LINE_LENGTH, this.resourceBundle.getString(this.keyBase))) {
			this.writer.println(line);
		}
		this.writer.println("\n"+this.resourceBundle.getString(this.keyBase + ".options") + ":");
		return this;
	}

	public <T> PluginUsageBuilder addOption(final String optionName, final T defaultValue) {
		final String key = this.keyBase + ".opt." + optionName;
		this.writer.println();
		this.writer.print("\t");
		this.writer.println(getOptionUsage(optionName, defaultValue)+" :");
		for (final String line : chopLines(PluginUsageBuilder.OPTION_LINE_LENGTH, this.resourceBundle.getString(key))) {
			this.writer.print("\t\t");
			this.writer.println(line);
		}
		this.writer.println();
		return this;
	}

	public <T> String getOptionUsage(final String optionName, final T defaultValue) {
		return MessageFormat.format(PluginUsageBuilder.OPTION_USAGES.get(defaultValue.getClass()), optionName, defaultValue, this.pluginName);
	}

	private static List<String> chopLines(final int maxLineLength, final String input) {
		final String[] words = input.split(" ");
		final List<String> result = new ArrayList<String>();
		StringBuilder sb = null;
		for (final String word : words) {
			if (word.length() > 0) {
				if (sb == null || sb.length() <= maxLineLength) {
					if (sb == null) {
						sb = new StringBuilder(maxLineLength);
					} else {
						sb.append(" ");
					}
					if (sb.indexOf("\n") >= 0) {
						result.add(sb.substring(0, sb.indexOf("\n")));
						final String rest = sb.substring(sb.indexOf("\n") + 1);
						sb = new StringBuilder(maxLineLength);
						sb.append(rest);
						sb.append(" ");
					}
					sb.append(word);
				} else {
					result.add(sb.toString());
					sb = new StringBuilder(maxLineLength);
					sb.append(word);
				}
			}
		}
		if(sb != null) result.add(sb.toString());
		return result;
	}


	public String build() {
		this.writer.close();
		return this.stringWriter.toString();
	}
}
