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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import com.kscs.util.plugins.xjc.BoundPropertiesPlugin;
import com.kscs.util.plugins.xjc.DeepClonePlugin;
import com.kscs.util.plugins.xjc.DeepCopyPlugin;
import com.kscs.util.plugins.xjc.FluentBuilderPlugin;
import com.kscs.util.plugins.xjc.GroupInterfacePlugin;
import com.kscs.util.plugins.xjc.ImmutablePlugin;
import com.kscs.util.plugins.xjc.MetaPlugin;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.HtmlUsageBuilder;
import com.kscs.util.plugins.xjc.base.MarkdownPluginUsageBuilder;
import com.kscs.util.plugins.xjc.base.Option;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Mirko Klemm 2015-02-19
 */
public class PluginDocPrinter {
	public static final ResourceBundle RES = ResourceBundle.getBundle(PluginDocPrinter.class.getName());
	public static final java.util.List<AbstractPlugin> PLUGINS = Arrays.asList(
			new FluentBuilderPlugin(),
			new ImmutablePlugin(),
			new GroupInterfacePlugin(),
			new DeepClonePlugin(),
			new DeepCopyPlugin(),
			new BoundPropertiesPlugin(),
			new MetaPlugin());
	private final ResourceBundle baseResourceBundle;
	private final ResourceBundle resourceBundle;
	private final Locale locale;
	private final AbstractPlugin plugin;

	protected PluginDocPrinter(final AbstractPlugin plugin, final Locale locale) {
		this.plugin = plugin;
		this.baseResourceBundle = ResourceBundle.getBundle(AbstractPlugin.class.getName(), locale);
		this.resourceBundle = ResourceBundle.getBundle(plugin.getClass().getName(), locale);
		this.locale = locale;
	}

	public static void main(final String[] args) throws IOException {
		printMarkdown(args.length > 0 ? new DirectorySink(new File(args[0])) : new StreamSink(System.out));
		if (args.length == 2) {
			try (final PrintStream ps = new PrintStream(new FileOutputStream(new File(args[1])))) {
				printReadme(Paths.get(args[0]), ps);
			}
		}
	}

	private static void printReadme(final Path directory, final PrintStream p) throws IOException {
		for (final String filename : Arrays.asList("index", "getting", "history", "usage")) {
			final Path path = directory.resolve(filename + ".md");
			for (final String line : Files.readAllLines(path, Charset.forName("UTF-8"))) {
				p.println(line);
			}
		}
		printMarkdown(Locale.ROOT, new StreamSink(p));
	}

	private static void printMarkdown(final Sink sink) {
		for (final Locale localeSpec : Arrays.asList(Locale.ROOT, Locale.GERMAN)) {
			printMarkdown(localeSpec, sink);
		}
	}

	private static void printMarkdown(final Locale localeSpec, final Sink sink) {
		for (final AbstractPlugin plugin : PluginDocPrinter.PLUGINS) {
			final PluginDocPrinter pluginDocPrinter = new PluginDocPrinter(plugin, localeSpec);
			sink.push(pluginDocPrinter);
		}
	}

	private static String arg(final String s) {
		return "<arg>-" + s + "</arg>";
	}

	private static String tab(final int tabAmount) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tabAmount; i++) {
			sb.append("\t");
		}
		return sb.toString();
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

	public void printInvocation(final PrintStream w, final int tabAmount) {
		w.print(tab(tabAmount));
		w.println(arg(this.plugin.getOptionName()));
		for (final Option option : this.plugin.getOptions()) {
			w.print(tab(tabAmount + 1));
			w.println(arg(option.getName() + "=" + option.getStringValue()));
		}
	}

	private static interface Sink {
		void push(final PluginDocPrinter pluginDocPrinter);
	}

	private static class StreamSink implements Sink {
		private final PrintStream stream;

		public StreamSink(final PrintStream stream) {
			this.stream = stream;
		}

		@Override
		public void push(final PluginDocPrinter pluginDocPrinter) {
			this.stream.println(pluginDocPrinter.getUsageMarkdown());
		}
	}

	private static class DirectorySink implements Sink {
		private final File directory;

		public DirectorySink(final File directory) {
			this.directory = directory;
			directory.mkdirs();
		}

		public void push(final PluginDocPrinter pluginDocPrinter) {
			final String s = Locale.ROOT.equals(pluginDocPrinter.getLocale()) ? "" : ("_" + pluginDocPrinter.getLocale().toLanguageTag());
			try (final PrintStream p = new PrintStream(new FileOutputStream(new File(this.directory, pluginDocPrinter.getPlugin().getOptionName().substring(1) + s + ".md")))) {
				p.println(pluginDocPrinter.getUsageMarkdown());
			} catch (final FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
