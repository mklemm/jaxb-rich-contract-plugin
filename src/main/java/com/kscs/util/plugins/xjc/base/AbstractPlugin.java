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

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;

/**
 * Common base class for plugins, manages command line parsing,
 * i18n resource bundles, and printout of "usage" information.
 *
 * @author Mirko Klemm 2015-02-07
 */
public abstract class AbstractPlugin extends Plugin {
	private final ResourceBundle baseResourceBundle;
	private final ResourceBundle resourceBundle;
	private final List<Option<?>> options;

	protected AbstractPlugin() {
		this.baseResourceBundle = PropertyDirectoryResourceBundle.getInstance(AbstractPlugin.class);
		this.resourceBundle = PropertyDirectoryResourceBundle.getInstance(getClass());
		this.options = buildOptions(this, getClass());
	}

	private static List<Option<?>> buildOptions(final AbstractPlugin plugin, final Class<?> pluginClass) {
		final List<Option<?>> options = new ArrayList<>();
		if (pluginClass.getSuperclass() != null) {
			options.addAll(buildOptions(plugin, pluginClass.getSuperclass()));
		}
		for (final Field field : pluginClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Opt.class)) {
				final String optionName = field.getAnnotation(Opt.class).value().isEmpty() ? field.getName() : field.getAnnotation(Opt.class).value();
				if (field.getType().equals(String.class)) {
					options.add(new StringOption(optionName, plugin, field));
				} else if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
					options.add(new BooleanOption(optionName, plugin, field));
				}
			}
		}
		return options;
	}


	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		if (('-' + getOptionName()).equals(args[i])) {
			return parseOptions(args, i + 1) + 1;
		}
		return 0;
	}

	private int parseOptions(final String[] args, final int startIndex) throws BadCommandLineException {
		int count = 0;
		for (final Option<?> option : this.options) {
			int i;
			for (i = startIndex; i < args.length && !args[i].startsWith("-X"); i++) {
				if (option.matches(args[i])) {
					i++;
					if (args.length > i && !args[i].startsWith("-")) {
						option.setStringValue(args[i]);
						count += 2;
					} else {
						throw new BadCommandLineException(MessageFormat.format(this.baseResourceBundle.getString("exception.missingArgument"), option.getName()));
					}
				} else {
					if (option.tryParse(args[i])) {
						count++;
					}
				}
			}
		}
		return count;
	}

	@Override
	public String getUsage() {
		final PlainTextUsageBuilder pluginUsageBuilder = new PlainTextUsageBuilder(this.baseResourceBundle, this.resourceBundle);
		pluginUsageBuilder.addMain(getOptionName().substring(1));
		for (final Option<?> option : this.options) {
			pluginUsageBuilder.addOption(option);
		}
		return pluginUsageBuilder.build();
	}


	protected String getMessage(final String key, final Object... args) {
		return MessageFormat.format(this.resourceBundle.getString(key), args);
	}

	protected String getMessage(final String key) {
		return this.resourceBundle.getString(key);
	}

	public List<Option<?>> getOptions() {
		return this.options;
	}
}
