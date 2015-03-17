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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import sun.util.ResourceBundleEnumeration;

/**
 * @author Mirko Klemm 2015-02-25
 */
public class PropertyDirectoryResourceBundle extends ResourceBundle {
	private static final Logger LOGGER = Logger.getLogger(PropertyDirectoryResourceBundle.class.getName());
	private final Map<String, String> values;

	public static ResourceBundle getInstance(final String baseName) {
		return getBundle(baseName, Control.INSTANCE);
	}

	public static ResourceBundle getInstance(final String baseName, final Locale locale) {
		return getBundle(baseName, locale, Control.INSTANCE);
	}

	public static ResourceBundle getInstance(final Class<?> localizedClass) {
		return getBundle(localizedClass.getName(), Control.INSTANCE);
	}

	public static ResourceBundle getInstance(final Class<?> localizedClass, final Locale locale) {
		return getBundle(localizedClass.getName(), locale, Control.INSTANCE);
	}

	public PropertyDirectoryResourceBundle(final Map<String, String> values) {
		this.values = values;
	}

	@Override
	protected Object handleGetObject(final String key) {
		return this.values.get(key);
	}

	@Override
	public Enumeration<String> getKeys() {
		final ResourceBundle parent = this.parent;
		return new ResourceBundleEnumeration(this.values.keySet(), (parent != null ? parent.getKeys() : null));
	}

	@Override
	protected Set<String> handleKeySet() {
		return this.values.keySet();
	}

	public static class Control extends ResourceBundle.Control {
		public static final Control INSTANCE = new Control();
		final Map<String, PropertyDirectoryResourceBundle> cachedBundles = new HashMap<>();

		private Control() {
			// singleton
		}

		@Override
		public List<String> getFormats(final String baseName) {
			final List<String> formats = new ArrayList<>();
			formats.add("text");
			formats.addAll(super.getFormats(baseName));
			return Collections.unmodifiableList(formats);
		}

		@Override
		public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload) throws IllegalAccessException, InstantiationException, IOException {
			if ("text".equals(format)) {
				final String bundleLocalizedName = toBundleName(baseName, locale);
				if (!reload && this.cachedBundles.containsKey(bundleLocalizedName)) {
					return this.cachedBundles.get(bundleLocalizedName);
				}
				final String bundleIndexPath = baseName.replace('.', '/') + ".index";
				final Properties bundleIndex = new Properties();
				final InputStream inputStream = loader.getResourceAsStream(bundleIndexPath);
				if (inputStream != null) {
					PropertyDirectoryResourceBundle.LOGGER.finer("Loading text file resource index: " + bundleIndexPath);
					bundleIndex.load(inputStream);
				}
				final String bundleResourcePath = bundleLocalizedName.replace('.', '/');
				final Path dirPath = ResourceDirectory.fromResource(loader, bundleResourcePath);
				PropertyDirectoryResourceBundle.LOGGER.finer("Resolved directory path: " + dirPath);
				if(dirPath == null) {
					return super.newBundle(baseName, locale, format, loader, reload);
				}
				try(final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
					for (final Path filePath : directoryStream) {
						if (!bundleIndex.containsValue(filePath.getFileName().toString())) {
							final String fileName = filePath.getFileName().toString();
							bundleIndex.setProperty(fileName.substring(0, fileName.lastIndexOf('.')), fileName);
						}
					}
				}
				final Map<String, String> values = new HashMap<>();
				for (final Object key : bundleIndex.keySet()) {
					final String propertyValue = bundleIndex.getProperty(key.toString());
					final String textFileName = propertyValue == null || propertyValue.trim().length() == 0 ? key.toString() : propertyValue;
					final Path textFilePath = Paths.get(bundleResourcePath, textFileName);
					try {
						PropertyDirectoryResourceBundle.LOGGER.finest("Loading resource text file \"" + textFilePath + "\"");
						final URL textFileURL = loader.getResource(textFilePath.toString());
						PropertyDirectoryResourceBundle.LOGGER.finest("Resource text file \"" + textFilePath + "\" URL: " + textFileURL);
						final StringBuilder sb = new StringBuilder();
						try(final BufferedReader reader = new BufferedReader(new InputStreamReader(loader.getResourceAsStream(textFilePath.toString()), Charset.forName("UTF-8")))) {
							String line;
							while ((line = reader.readLine()) != null) {
								sb.append(line);
								sb.append("\n");
							}
						}
						PropertyDirectoryResourceBundle.LOGGER.finest("Text file \"" + textFilePath + "\" loaded.");
						values.put(key.toString(), sb.toString());
					} catch (final Exception e) {
						return super.newBundle(baseName, locale, format, loader, reload);
					}
				}
				final PropertyDirectoryResourceBundle bundle = new PropertyDirectoryResourceBundle(values);
				bundle.setParent(super.newBundle(baseName, locale, "java.properties", loader, reload));
				this.cachedBundles.put(bundleLocalizedName, bundle);
				return bundle;
			} else {
				return super.newBundle(baseName, locale, format, loader, reload);
			}
		}
	}
}
