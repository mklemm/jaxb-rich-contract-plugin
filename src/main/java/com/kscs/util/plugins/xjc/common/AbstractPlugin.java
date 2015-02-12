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

package com.kscs.util.plugins.xjc.common;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import com.kscs.util.plugins.xjc.PluginUsageBuilder;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;

/**
 * @author Mirko Klemm 2015-02-07
 */
public abstract class AbstractPlugin extends Plugin {
	private static final ResourceBundle BASE_RESOURCE_BUNDLE = ResourceBundle.getBundle(AbstractPlugin.class.getName());
	private final ResourceBundle resourceBundle;

	protected AbstractPlugin() {
		this.resourceBundle = ResourceBundle.getBundle(getClass().getName());
	}

	protected static Boolean parseBoolean(final String arg) {
		final boolean argTrue = isTrue(arg);
		final boolean argFalse = isFalse(arg);
		if (!argTrue && !argFalse) {
			return null;
		} else {
			return argTrue;
		}
	}

	private static boolean isTrue(final String arg) {
		return arg.endsWith("y") || arg.endsWith("true") || arg.endsWith("on") || arg.endsWith("yes");
	}

	private static boolean isFalse(final String arg) {
		return arg.endsWith("n") || arg.endsWith("false") || arg.endsWith("off") || arg.endsWith("no");
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		int currentIndex = i;
		if (('-' + getOptionName()).equals(args[i])) {
			currentIndex = parseOptions(args, i, getSetters());
		}
		return currentIndex - i;
	}

	private int parseOptions(final String[] args, int i, final Map<String, Setter<String>> setters) throws BadCommandLineException {
		for (final String name : setters.keySet()) {
			if (args.length > i + 1) {
				if (args[i + 1].equalsIgnoreCase(name)) {
					if (args.length > i + 2 && !args[i + 2].startsWith("-")) {
						setters.get(name).set(args[i + 2]);
						i += 2;
					} else {
						throw new BadCommandLineException(MessageFormat.format(AbstractPlugin.BASE_RESOURCE_BUNDLE.getString("exception.missingArgument"), name));
					}
				} else if (args[i + 1].toLowerCase().startsWith(name + "=")) {
					setters.get(name).set(args[i + 1].substring(name.length() + 1));
					i++;
				}
			} else {
				return 0;
			}
		}
		return i;
	}

	protected abstract Map<String, Setter<String>> getSetters();

	@Override
	public String getUsage() {
		final PluginUsageBuilder pluginUsageBuilder = new PluginUsageBuilder(this.resourceBundle);
		return buildUsage(pluginUsageBuilder).build();
	}

	protected abstract PluginUsageBuilder buildUsage(final PluginUsageBuilder pluginUsageBuilder);

	protected String getMessage(final String key, final Object... args) {
		return MessageFormat.format(this.resourceBundle.getString(key), args);
	}

	protected String getMessage(final String key) {
		return this.resourceBundle.getString(key);
	}
}
