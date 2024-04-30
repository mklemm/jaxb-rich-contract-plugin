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

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.logging.Logger;


@SuppressWarnings({ "nls", "boxing" })
public class BeanDiff {
	private static final Logger LOGGER = Logger.getLogger(BeanDiff.class.getName());
	private final String packagePrefix;
	private final Set<String> ignoreNames;
	private final DecimalFormat decimalFormat;
	private final Stack<String> propertyStack = new Stack<String>();
	private final HashSet<Object> nodePath = new HashSet<Object>();

	public static class Difference {
		private final String propertyName;
		private final Object expected;
		private final Object actual;

		Difference(final String propertyName, final Object expected, final Object actual) {
			this.propertyName = propertyName;
			this.expected = expected;
			this.actual = actual;
		}

		/**
		 * @return the expected
		 */
		public Object getExpected() {
			return this.expected;
		}

		/**
		 * @return the actual
		 */
		public Object getActual() {
			return this.actual;
		}

		/**
		 * @return the propertyName
		 */
		public String getPropertyName() {
			return this.propertyName;
		}
	}

	public BeanDiff(final String packagePrefix, final String... ignoreNames) {
		this(packagePrefix, 19, 10, ignoreNames);
	}

	public BeanDiff(final String packagePrefix, final int decimalPrecision, final int decimalScale, final String... ignoreNames) {
		this.packagePrefix = packagePrefix;
		this.ignoreNames = new HashSet<String>(Arrays.asList(ignoreNames));

		final char[] intPart = new char[decimalPrecision - decimalScale];
		final char[] fracPart = new char[decimalScale];
		Arrays.fill(intPart, '0');
		Arrays.fill(fracPart, '0');
		this.decimalFormat = new DecimalFormat(new String(intPart) + "." + new String(fracPart), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	}

	public List<Difference> findDifferences(final Object o1, final Object o2) {
		final List<Difference> differences = new LinkedList<Difference>();
		findDifferences(o1, o2, differences);
		return differences;
	}

	private void findDifferences(final Object o1, final Object o2, final List<Difference> differences) {
		try {
			if (!this.nodePath.contains(o1)) {
				this.nodePath.add(o1);

				final BeanInfo beanInfo = Introspector.getBeanInfo(o1.getClass());

				for (final PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
					this.propertyStack.push("." + propertyDescriptor.getName());
					if (propertyDescriptor instanceof IndexedPropertyDescriptor) {
						assertArrayPropertyValuesAreEqual(o1, o2, (IndexedPropertyDescriptor) propertyDescriptor, beanInfo, differences);
					} else if (propertyDescriptor.getPropertyType().isArray()) {
						assertArrayPropertyValuesAreEqual(o1, o2, propertyDescriptor, differences);
					} else if (List.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
						assertListPropertyValuesAreEqual(o1, o2, propertyDescriptor, differences);
					} else if (Map.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
						assertMappedPropertyValuesAreEqual(o1, o2, propertyDescriptor, differences);
					} else {
						assertPropertyValuesAreEqual(o1, o2, propertyDescriptor, differences);
					}
					this.propertyStack.pop();
				}

				this.nodePath.remove(o1);
			} else {
				BeanDiff.LOGGER.fine("Ignoring circle: " + o1);
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void assertPropertyValuesAreEqual(final Object o1, final Object o2, final PropertyDescriptor propertyDescriptor,
			final List<Difference> differences)
			throws IllegalAccessException,
			InvocationTargetException {
		Method getter = propertyDescriptor.getReadMethod();
		if (getter == null) {
			try {
				getter = propertyDescriptor.getWriteMethod().getDeclaringClass()
						.getMethod("is" + propertyDescriptor.getWriteMethod().getName().substring(3));
			} catch (NoSuchMethodException e) {
				getter = null;
			}
		}
		final Object val1 = getter.invoke(o1);
		final Object val2 = getter.invoke(o2);
		if ((val1 != null && val2 == null) || (val1 == null && val2 != null) || propertyDescriptor.getPropertyType().isPrimitive()) {
			BeanDiff.addDiff(differences, createDiff(propertyDescriptor.getPropertyType(), propertyDescriptor.getName(), val1, val2));
		} else if (val1 != null && !val1.getClass().isAssignableFrom(val2.getClass())) {
			this.propertyStack.push(".class");
			BeanDiff.addDiff(differences,
					createDiff(propertyDescriptor.getPropertyType(), propertyDescriptor.getName(), val1.getClass().getName(), val2.getClass()
							.getName()));
			this.propertyStack.pop();
		} else if (val1 != null && propertyDescriptor.getPropertyType().getPackage().getName().startsWith(this.packagePrefix)) {
			findDifferences(val1, val2, differences);
		} else if (val1 instanceof Calendar) {
			BeanDiff.addDiff(differences,
					createDiff(o1.getClass(), propertyDescriptor.getName(), ((Calendar) val1).getTime(), ((Calendar) val2).getTime()));
		} else {
			BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), val1, val2));
		}
	}

	private void assertListPropertyValuesAreEqual(final Object o1, final Object o2, final PropertyDescriptor propertyDescriptor,
			final List<Difference> differences)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		final Method getter = propertyDescriptor.getReadMethod();
		final List<?> l1 = (List<?>) getter.invoke(o1);
		final List<?> l2 = (List<?>) getter.invoke(o2);
		this.propertyStack.push(".size()");
		BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), l1.size(), l2.size()));
		this.propertyStack.pop();
		if (l1.size() == l2.size()) {
			for (int i = 0; i < l1.size(); i++) {
				this.propertyStack.push("[" + i + "]");
				final Object val1 = l1.get(i);
				final Object val2 = l2.get(i);
				if (val1 != null && val2 != null && val1.getClass().getPackage().getName().startsWith(this.packagePrefix)) {
					findDifferences(val1, val2, differences);
				} else {
					BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), val1, val2));
				}
				this.propertyStack.pop();
			}
		}
	}

	private void assertArrayPropertyValuesAreEqual(final Object o1, final Object o2, final PropertyDescriptor propertyDescriptor,
			final List<Difference> differences) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {

		final Method getter = propertyDescriptor.getReadMethod();
		final Object[] l1 = BeanDiff.toObjectArray(getter.invoke(o1));
		final Object[] l2 = BeanDiff.toObjectArray(getter.invoke(o2));
		this.propertyStack.push(".size()");
		BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), l1.length, l2.length));
		this.propertyStack.pop();
		if (l1.length == l2.length) {
			for (int i = 0; i < l1.length; i++) {
				this.propertyStack.push("[" + i + "]");
				final Object val1 = l1[i];
				final Object val2 = l2[i];
				if (val1 != null && val2 != null && val1.getClass().getPackage().getName().startsWith(this.packagePrefix)) {
					findDifferences(val1, val2, differences);
				} else {
					BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), val1, val2));
				}
				this.propertyStack.pop();
			}
		}

	}

	private void assertArrayPropertyValuesAreEqual(final Object o1, final Object o2, final IndexedPropertyDescriptor propertyDescriptor,
			final BeanInfo beanInfo, final List<Difference> differences) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		final Method getter = propertyDescriptor.getIndexedReadMethod();
		Method lengthGetter = null;
		for (final PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
			if (pd.getName().equals(propertyDescriptor.getName() + "Length")) {
				lengthGetter = pd.getReadMethod();
			}
		}
		if (lengthGetter != null) {
			final int length = ((Integer) lengthGetter.invoke(o1)).intValue();
			final int length2 = ((Integer) lengthGetter.invoke(o1)).intValue();
			this.propertyStack.push("length");
			BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), length, length2));
			this.propertyStack.pop();
			for (int i = 0; i < length; i++) {
				this.propertyStack.push("[" + i + "]");
				final Object val1 = getter.invoke(o1, i);
				final Object val2 = getter.invoke(o2, i);
				if (val1 != null && val2 != null && val1.getClass().getPackage().getName().startsWith(this.packagePrefix)) {
					findDifferences(val1, val2, differences);
				} else {
					BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), val1, val2));
				}
				this.propertyStack.pop();
			}
		} else {
			createArrayDiff(o1, o2, differences);
		}
	}

	private void assertMappedPropertyValuesAreEqual(final Object o1, final Object o2, final PropertyDescriptor propertyDescriptor,
			final List<Difference> differences)
			throws IllegalAccessException, InvocationTargetException {
		final Method getter = propertyDescriptor.getReadMethod();
		if (getter == null) {
			return;
		}
		final Map<?, ?> map1 = (Map<?, ?>) getter.invoke(o1);
		final Map<?, ?> map2 = (Map<?, ?>) getter.invoke(o2);
		if (map1 != null && map2 != null) {
			for (final Object key1 : map1.keySet()) {
				this.propertyStack.push("[" + key1 + "]");
				BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), map1.get(key1), map2.get(key1)));
				this.propertyStack.pop();
			}
			for (final Object key1 : map1.keySet()) {
				this.propertyStack.push("[" + key1 + "]");
				BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), map1.get(key1), map2.get(key1)));
				this.propertyStack.pop();
			}
		} else {
			BeanDiff.addDiff(differences, createDiff(o1.getClass(), propertyDescriptor.getName(), map1, map2));
		}
	}

	private Difference createDiff(final Class<?> type, final String propertyName, final Object expected, final Object actual) {
		if (!isIgnored(type, propertyName) && expected != actual) {
			if (actual == null) {
				return new Difference(getPropertyPath(), expected, actual);
			} else if ((expected instanceof BigDecimal) || (expected instanceof Double)) {
				final String expectedString = this.decimalFormat.format(expected);
				final String actualString = this.decimalFormat.format(actual);
				if (!expectedString.equals(actualString)) {
					return new Difference(getPropertyPath(), expectedString, actualString);
				}
			} else if ((expected instanceof Comparable)) {
				if (((Comparable) expected).compareTo(actual) != 0) {
					return new Difference(getPropertyPath(), expected, actual);
				} else if (expected == null && actual != null) {
					return new Difference(getPropertyPath(), expected, actual);
				}
			} else if (expected != null && !expected.equals(actual)) {
				return new Difference(getPropertyPath(), expected, actual);
			} else if (expected == null && actual != null) {
				return new Difference(getPropertyPath(), expected, actual);
			}
		}
		return null;
	}

	private static void addDiff(final List<Difference> differences, final Difference diff) {
		if (diff != null) {
			differences.add(diff);
		}
	}

	private boolean isIgnored(final Class<?> type, final String propertyName) {
		return this.ignoreNames.contains(type.getSimpleName() + "." + propertyName)
				|| this.ignoreNames.contains(propertyName);
	}

	private void createArrayDiff(final Object expected, final Object actual, final List<Difference> differences) {
		if (!Arrays.deepEquals((Object[]) expected, (Object[]) actual)) {
			differences.add(new Difference("arrays", expected, actual));
		}
	}

	private String createPropertyString(final Object obj, final PropertyDescriptor property) {
		return property.getPropertyType().getName() + " " + property.getReadMethod().getDeclaringClass().getName() + "." + property.getName() + " ("
				+ obj + ")";
	}

	private String getPropertyPath() {
		final StringBuilder sb = new StringBuilder();
		for (final String propertyName : this.propertyStack) {
			sb.append(propertyName);
		}
		return sb.toString();
	}

	public static Object[] toObjectArray(Object array) {
		int length = Array.getLength(array);
		Object[] ret = new Object[length];
		for (int i = 0; i < length; i++)
			ret[i] = Array.get(array, i);
		return ret;

	}
}
