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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Mirko Klemm 2015-02-18
 */
public class PlainTextUsageBuilder extends PluginUsageBuilder {
	private static final int MAX_LINE_LENGTH = 80;
	private static final int INDENT_MAX_LINE_LENGTH = 70;
	private final StringWriter stringWriter = new StringWriter();
	private final PrintWriter writer = new PrintWriter(this.stringWriter);

	public PlainTextUsageBuilder(final ResourceBundle baseResourceBundle, final ResourceBundle resourceBundle) {
		super(baseResourceBundle, resourceBundle);
	}

	public PlainTextUsageBuilder addMain(final String optionName) {
		this.writer.print(this.baseResourceBundle.getString(this.keyBase + ".usage"));
		this.writer.println(": -X" + optionName);
		this.writer.println();
		for (final String line : chopLines(PlainTextUsageBuilder.MAX_LINE_LENGTH, this.resourceBundle.getString(this.keyBase))) {
			this.writer.println(line);
		}
		return this;
	}

	public <T> PlainTextUsageBuilder addOption(final Option<?> option) {
		if(this.firstOption) {
			this.firstOption = false;
			this.writer.println("\n" + this.baseResourceBundle.getString(this.keyBase + ".options") + ":");
		}
		final String key = this.keyBase + "." + transformName(option.getName());
		this.writer.println();
		this.writer.print("\t-");
		this.writer.println(option.getName() + "=" + option.getChoice()+ " (" + option.getStringValue() + ")");
		for (final String line : chopLines(PlainTextUsageBuilder.INDENT_MAX_LINE_LENGTH, this.resourceBundle.getString(key))) {
			this.writer.print("\t\t");
			this.writer.println(line);
		}
		this.writer.println();
		return this;
	}

	private static List<String> chopLines(final int maxLineLength, final String input) {
		final String[] words = input.split(" ");
		final List<String> result = new ArrayList<>();
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
