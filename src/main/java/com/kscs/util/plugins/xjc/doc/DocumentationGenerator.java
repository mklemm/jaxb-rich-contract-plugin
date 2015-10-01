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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kscs.util.plugins.xjc.BoundPropertiesPlugin;
import com.kscs.util.plugins.xjc.DeepClonePlugin;
import com.kscs.util.plugins.xjc.DeepCopyPlugin;
import com.kscs.util.plugins.xjc.FluentBuilderPlugin;
import com.kscs.util.plugins.xjc.GroupInterfacePlugin;
import com.kscs.util.plugins.xjc.ImmutablePlugin;
import com.kscs.util.plugins.xjc.MetaPlugin;
import com.kscs.util.plugins.xjc.ModifierPlugin;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;

/**
 * @author Mirko Klemm 2015-10-01
 */
public final class DocumentationGenerator {
	public static final List<Locale> LOCALES = Arrays.asList(Locale.ROOT, Locale.GERMAN);
	public static final java.util.List<AbstractPlugin> PLUGINS = Arrays.asList(
			new FluentBuilderPlugin(),
			new ImmutablePlugin(),
			new ModifierPlugin(),
			new GroupInterfacePlugin(),
			new DeepClonePlugin(),
			new DeepCopyPlugin(),
			new BoundPropertiesPlugin(),
			new MetaPlugin());

	public final Path siteDirectory;
	public final Map<Locale,Path> readmeFiles = new HashMap<>();
	public final Map<Locale,List<PluginDocumentationFormatter>> pluginDocumentationFormatters = new HashMap<>();

	public DocumentationGenerator(final Path siteDirectory, final Path readmeFileBase, final List<AbstractPlugin> plugins, final List<Locale> locales) {
		this.siteDirectory = siteDirectory;

		for(final Locale locale:locales) {

			final String readmeBaseFullName = readmeFileBase.toString();
			final String readmeBaseBaseName = readmeBaseFullName.substring(0, readmeBaseFullName.lastIndexOf('.'));
			final String readmeBaseSuffix = readmeBaseFullName.substring(readmeBaseFullName.lastIndexOf('.'));

			final Path localeReadme = Locale.ROOT.equals(locale) ? readmeFileBase : Paths.get(readmeBaseBaseName + "_" + locale.toLanguageTag() + readmeBaseSuffix);
			this.readmeFiles.put(locale, localeReadme);

			final List<PluginDocumentationFormatter> list = new ArrayList<>();
			this.pluginDocumentationFormatters.put(locale, list);
			for (final AbstractPlugin plugin : plugins) {
				list.add(new PluginDocumentationFormatter(plugin, locale));
			}
		}
	}

	public static void main(final String[] args) throws IOException {
		final Path siteDirectory = args.length > 0 ? Paths.get(args[0]) : Paths.get("src/site/markdown");
		final Path readmeFile = args.length > 1 ? Paths.get(args[1]) : Paths.get("README.md");
		final DocumentationGenerator documentationGenerator = new DocumentationGenerator(siteDirectory, readmeFile, PLUGINS, LOCALES);
		documentationGenerator.printMarkdownUsageFiles();
		documentationGenerator.printReadme();
	}

	private void printReadme() throws IOException {
		for(final Locale locale : this.pluginDocumentationFormatters.keySet()) {
			final Path readme = this.readmeFiles.get(locale);

			try (final PrintStream ps = new PrintStream(readme.toFile())) {
				for (final String filename : Arrays.asList("index", "getting", "history", "usage")) {
					printFile(getLocalizedFile(filename, locale), ps);
				}
				for (final PluginDocumentationFormatter pluginDocumentationFormatter : this.pluginDocumentationFormatters.get(locale)) {
					printFile(this.siteDirectory.resolve(pluginDocumentationFormatter.getUsageFileName() + ".md"), ps);
				}
				int i = 1;
				for (final PluginDocumentationFormatter plugin : this.pluginDocumentationFormatters.get(locale)) {
					ps.printf("[%d]: #%s\n", i++, plugin.getPlugin().getOptionName().substring(1));
				}
			}
		}
	}

	private static void printFile(final Path filename, final PrintStream p) throws IOException {
		for (final String line : Files.readAllLines(filename, Charset.defaultCharset())) {
			if(!PluginDocumentationFormatter.INDEX_PATTERN.matcher(line).matches()) {
				p.println(line);
			}
		}
	}

	private void printMarkdownUsageFiles() throws IOException {
		printGeneralUsageFile("usage", "usage-header", "usage-footer");
		for(final Locale locale:this.pluginDocumentationFormatters.keySet()) {
			for (final PluginDocumentationFormatter pluginDocumentationFormatter : this.pluginDocumentationFormatters.get(locale)) {
				final String usageMarkdown = pluginDocumentationFormatter.getUsageMarkdown();
				if (this.siteDirectory != null) {
					Files.write(this.siteDirectory.resolve(pluginDocumentationFormatter.getUsageFileName() + ".md"), usageMarkdown.getBytes());
				}
			}
		}
	}

	private void printGeneralUsageFile(final String usageFileBaseName, final String headerFileBaseName, final String footerFileBaseName) throws IOException {
		for(final Locale locale:this.pluginDocumentationFormatters.keySet()) {
			final Path usageFile = this.siteDirectory.resolve(usageFileBaseName + (Locale.ROOT.equals(locale) ? "" :  "_" + locale.toLanguageTag()) + ".md");
			final Path headerFile = getLocalizedFile(headerFileBaseName, locale);
			final Path footerFile = getLocalizedFile(footerFileBaseName, locale);
			try (final PrintStream p = new PrintStream(usageFile.toFile())) {
				for (final String line : Files.readAllLines(headerFile, Charset.defaultCharset())) {
					p.println(line);
				}
				for (final PluginDocumentationFormatter pluginDocumentationFormatter : this.pluginDocumentationFormatters.get(locale)) {
					p.print(pluginDocumentationFormatter.getConfigCheatSheet(6, "<arg>-", "</arg>"));
				}
				for (final String line : Files.readAllLines(footerFile, Charset.defaultCharset())) {
					p.println(line);
				}
			}
		}
	}

	private Path getLocalizedFile(final String baseName, final Locale locale) {
		Path localizedFile = this.siteDirectory.resolve(baseName + "_" + locale.toLanguageTag() + ".md");
		if(!Files.exists(localizedFile)) {
			localizedFile = this.siteDirectory.resolve(baseName + ".md");
		}
		return localizedFile;
	}

}
