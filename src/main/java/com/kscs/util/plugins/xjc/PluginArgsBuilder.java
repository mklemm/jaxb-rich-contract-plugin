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

import com.sun.tools.xjc.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for plugin command line arguments parser
 * @author mirko 2014-10-23
 */
@SuppressWarnings("unchecked")
public class PluginArgsBuilder {
	private final PluginUsageBuilder pluginUsageBuilder;
	private final Map<String, Ref<?>> argumentRefs = new HashMap<>();
	private final String pluginName;

	public PluginArgsBuilder(final Plugin pluginInstance, final String pluginName) {
		this.pluginUsageBuilder = new PluginUsageBuilder(pluginInstance.getClass(), pluginName);
		this.pluginName = pluginName;
		initFieldRefs(pluginInstance);
	}

	private void initFieldRefs(final Plugin pluginInstance) {
		final Field[] fields = pluginInstance.getClass().getDeclaredFields();
		for(final Field field : fields) {
			if((field.getModifiers() & Modifier.FINAL) == 0 && (field.getModifiers() & Modifier.STATIC) == 0 &&  (field.getModifiers() & Modifier.VOLATILE) != 0) {
				field.setAccessible(true);
				final FieldRef<?> fieldRef = new FieldRef<>(pluginInstance, field);
				this.withParameter(field.getName(), fieldRef);
			}
		}
	}

	public final PluginArgsBuilder withParameter(final String parameterName, final Ref<?> valueRef) {
		this.argumentRefs.put(parameterName, valueRef);
		return this;
	}

	public PluginArgs build() {
		return new PluginArgs(this.pluginName, Collections.unmodifiableMap(this.argumentRefs), this.pluginUsageBuilder);
	}

	private static class FieldRef<T> implements Ref<T> {
		private final Plugin pluginInstance;
		private final Field field;
		private final Class<T> argType;

		private FieldRef(final Plugin pluginInstance, final Field field) {
			this.pluginInstance = pluginInstance;
			this.field = field;
			this.argType = (Class<T>)field.getType();
		}

		@Override
		public void set(final T val) {
			try {
				this.field.set(this.pluginInstance, val);
			} catch (final IllegalAccessException e) {
				throw new RuntimeException("Argument parsing error: Field \""+ this.field.getName()+"\" cannot be set to value \""+val+"\".", e);
			}
		}

		@Override
		public T get() {
			try {
				return (T)this.field.get(this.pluginInstance);
			} catch (final IllegalAccessException e) {
				throw new RuntimeException("Argument error: Field \""+ this.field.getName()+"\" cannot be read.", e);
			}
		}

		@Override
		public Class<T> getType() {
			return this.argType;
		}
	}
}
