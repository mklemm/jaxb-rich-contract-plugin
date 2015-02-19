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

import com.kscs.util.plugins.xjc.BoundPropertiesPlugin;
import com.kscs.util.plugins.xjc.DeepClonePlugin;
import com.kscs.util.plugins.xjc.DeepCopyPlugin;
import com.kscs.util.plugins.xjc.FluentBuilderPlugin;
import com.kscs.util.plugins.xjc.GroupInterfacePlugin;
import com.kscs.util.plugins.xjc.ImmutablePlugin;
import com.kscs.util.plugins.xjc.MetaPlugin;
import org.junit.Test;

/**
 * @author Mirko Klemm 2015-02-15
 */
public class AbstractPluginTest {
	@Test
		public void testPluginUsageFluentBuilder() {
			final FluentBuilderPlugin plugin = new FluentBuilderPlugin();
			plugin.printInvocation(System.out, 6);
		}

		@Test
		public void testPluginUsageImmutable() {
			final ImmutablePlugin plugin = new ImmutablePlugin();
			plugin.printInvocation(System.out, 6);
		}

		@Test
		public void testPluginUsageGroupContract() {
			final GroupInterfacePlugin plugin = new GroupInterfacePlugin();
			plugin.printInvocation(System.out, 6);
		}

		@Test
		public void testPluginUsageDeepClone() {
			final DeepClonePlugin plugin = new DeepClonePlugin();
			plugin.printInvocation(System.out, 6);
		}

		@Test
		public void testPluginUsageDeepCopy() {
			final DeepCopyPlugin plugin = new DeepCopyPlugin();
			plugin.printInvocation(System.out, 6);
		}

		@Test
		public void testPluginUsageBoundProperties() {
			final BoundPropertiesPlugin plugin = new BoundPropertiesPlugin();
			plugin.printInvocation(System.out, 6);
		}

		@Test
		public void testPluginUsageMeta() {
			final MetaPlugin plugin = new MetaPlugin();
			plugin.printInvocation(System.out, 6);
		}
}
