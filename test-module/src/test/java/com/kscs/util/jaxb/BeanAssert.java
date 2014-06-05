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
package com.kscs.util.jaxb;

import com.kscs.util.test.BeanDiff;
import org.junit.Assert;

import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings({"nls", "boxing"})
public class BeanAssert extends BeanDiff {
	private static final Logger LOGGER = Logger.getLogger(BeanAssert.class.getName());

	public BeanAssert(final String packagePrefix, final String... ignoreNames) {
		super(packagePrefix, 19, 10, ignoreNames);
	}

	public BeanAssert(final String packagePrefix, final int decimalPrecision, final int decimalScale, final String... ignoreNames) {
		super(packagePrefix, decimalPrecision, decimalScale, ignoreNames);
	}

	public void assertPropertyEquality(final Object o1, final Object o2) {
		final List<Difference> differences = findDifferences(o1, o2);
		final StringBuilder diffs1 = new StringBuilder();
		for (final Difference diff : differences) {
			diffs1.append(" \t").append(diff.getPropertyName()).append(": expected=").append(diff.getExpected()).append(", actual=").append(diff.getActual()).append("\n");
			BeanAssert.LOGGER.fine("Assertion Failure: " + diff.getPropertyName() + ": expected=" + diff.getExpected() + ", actual="
					+ diff.getActual());
		}
		final String diffs = diffs1.toString();
		Assert.assertTrue("differences:\n" + diffs, differences.isEmpty());
	}


}
