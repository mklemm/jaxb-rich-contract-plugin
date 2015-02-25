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
import java.util.ResourceBundle;

/**
 * @author Mirko Klemm 2015-02-24
 */
public class MarkdownPluginUsageBuilder extends PluginUsageBuilder {
	private final StringWriter stringWriter = new StringWriter();
	private final PrintWriter writer = new PrintWriter(this.stringWriter);

	public MarkdownPluginUsageBuilder(final ResourceBundle baseResourceBundle, final ResourceBundle resourceBundle) {
		super(baseResourceBundle, resourceBundle);
	}

	public MarkdownPluginUsageBuilder addMain(final String optionName) {
		this.writer.println("## " + optionName);
		for(final Section section:getSections()) {
			this.writer.println("### " + section.title);
			if(section.body != null) {
				this.writer.println(section.body);
				this.writer.println();
			}
		}
		this.writer.println("### " + this.baseResourceBundle.getString(this.keyBase + ".usage"));
		this.writer.println("#### -X" + optionName);
		return this;
	}

	public <T> MarkdownPluginUsageBuilder addOption(final Option<?> option) {
		if (this.firstOption) {
			this.firstOption = false;
			this.writer.println("\n#### " + this.baseResourceBundle.getString(this.keyBase + ".options"));
		}
		final String key = this.keyBase + "." + transformName(option.getName());
		this.writer.println();
		this.writer.print("##### -");
		this.writer.println(option.getName() + "=`" + option.getChoice() + "` (" + option.getStringValue() + ")");
		this.writer.println(this.resourceBundle.getString(key));
		this.writer.println();
		return this;
	}

	public String build() {
		this.writer.close();
		return this.stringWriter.toString();
	}
}
