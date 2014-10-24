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

import com.sun.tools.xjc.BadCommandLineException;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the arguments of a Plugin
 */
@SuppressWarnings("unchecked")
public class PluginArgs {
	public static final Conv<Boolean> CONV_BOOLEAN = new Conv<Boolean>() {
		@Override
		public Boolean valueOf(final String s) {
			final String norm = s.trim().toLowerCase();
			return "y".equals(norm) || "true".equals(norm) || "yes".equals(norm) || "j".equals(norm);
		}
	};

	public static final Conv<Integer> CONV_INTEGER = new Conv<Integer>() {
		@Override
		public Integer valueOf(final String s) {
			return Integer.parseInt(s);
		}
	};


	public static final Conv<String> CONV_STRING = new Conv<String>() {
		@Override
		public String valueOf(final String s) {
			return s;
		}
	};

	private static final Map<Class<?>, Conv<?>> TYPE_MAPPERS = new HashMap<Class<?>, Conv<?>>() {{
		put(int.class, PluginArgs.CONV_INTEGER);
		put(String.class, PluginArgs.CONV_STRING);
		put(boolean.class, PluginArgs.CONV_BOOLEAN);
		put(Boolean.class, PluginArgs.CONV_BOOLEAN);
	}};

	private final Map<String,Ref<?>> argRefs;
	private final PluginUsageBuilder usageBuilder;
	private String usageString = null;
	private final String pluginName;

	public PluginArgs(final String pluginName, final Map<String, Ref<?>> argRefs, final PluginUsageBuilder usageBuilder) {
		this.argRefs = argRefs;
		this.usageBuilder = usageBuilder;
		this.pluginName = pluginName;
	}

	int parse(final String[] argList, final int index) throws BadCommandLineException {
		if(index < 0 || index >= argList.length) return 0;
		final String arg = argList[index];
		if(!arg.startsWith("-"+pluginName)) {
			return 0;
		}
		final int sepIndex = arg.indexOf('=');
		if(sepIndex > 0) {
			final String argName = arg.substring(2 + pluginName.length(), sepIndex);
			final String argValue = arg.substring(sepIndex + 1);

			final Ref<Object> anyArg = (Ref<Object>)this.argRefs.get(argName);
			if(anyArg != null) {
				anyArg.set(convertType(anyArg.getType(), argValue));
				return 1;
			} else {
				return 0;
			}

		} else {
			return 0;
		}

	}

	private Object convertType(final Class<?> targetType, final String stringVal) throws BadCommandLineException {
		try {
			return PluginArgs.TYPE_MAPPERS.get(targetType).valueOf(stringVal);
		} catch (final Exception e) {
			throw new BadCommandLineException("Value \""+stringVal+"\" cannot be converted to type \""+targetType.getName()+"\".",e);
		}
	}

	private static interface Conv<T> {
		T valueOf(final String s);
	}

	public String getUsageText() {
		if(this.usageString == null) {
			for(final Map.Entry<String,Ref<?>> entry : this.argRefs.entrySet()) {
				this.usageBuilder.addOption(entry.getKey(), entry.getValue().get());
			}
			this.usageString = this.usageBuilder.build();
		}
		return this.usageString;
	}

	public String getPluginActivation() {
		return "X"+this.pluginName;
	}
}
