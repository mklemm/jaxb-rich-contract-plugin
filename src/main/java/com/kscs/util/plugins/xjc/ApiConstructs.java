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

import com.kscs.util.jaxb.PathCloneable;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Common constants and constructs
 */
public class ApiConstructs {
	public static final String BUILDER_CLASS_NAME = "Builder";
	public static final String BUILD_METHOD_NAME = "build";
	public static final String BUILDER_METHOD_NAME = "builder";
	public static final String INIT_METHOD_NAME = "init";
	public static final String PRODUCT_INSTANCE_NAME = "product";
	public static final String ADD_METHOD_PREFIX = "add";
	public static final String WITH_METHOD_PREFIX = "with";
	public static final String AS_LIST = "asList";
	public static final String UNMODIFIABLE_LIST = "unmodifiableList";
	public static final String ADD_ALL = "addAll";

	final JCodeModel codeModel;
	final JClass arrayListClass;
	final JClass listClass;
	final JClass collectionClass;
	final JClass collectionsClass;
	final JClass arraysClass;
	final Options opt;
	final JClass cloneableInterface;
	final Outline outline;
	final ErrorHandler errorHandler;
	final Map<String, ClassOutline> classes;
	public JClass pathCloneableInterface;

	ApiConstructs(final Outline outline, final Options opt, final ErrorHandler errorHandler) {
		this.outline = outline;
		this.errorHandler = errorHandler;
		this.codeModel = outline.getCodeModel();
		this.opt = opt;
		this.arrayListClass = this.codeModel.ref(ArrayList.class);
		this.listClass = this.codeModel.ref(List.class);
		this.collectionClass = this.codeModel.ref(Collection.class);
		this.collectionsClass = this.codeModel.ref(Collections.class);
		this.arraysClass = this.codeModel.ref(Arrays.class);
		this.cloneableInterface = this.codeModel.ref(Cloneable.class);
		this.pathCloneableInterface = this.codeModel.ref(PathCloneable.class);
		this.classes = new HashMap<String, ClassOutline>(outline.getClasses().size());
		for(final ClassOutline classOutline : this.outline.getClasses()) {
			this.classes.put(classOutline.implClass.fullName(), classOutline);
		}
	}

	public JInvocation asList(final JExpression expression) {
		return this.arraysClass.staticInvoke(ApiConstructs.AS_LIST).arg(expression);
	}

	public JInvocation unmodifiableList(final JExpression expression) {
		return this.collectionsClass.staticInvoke(ApiConstructs.UNMODIFIABLE_LIST).arg(expression);
	}

	public boolean hasPlugin(final Class<? extends Plugin> pluginClass) {
		return PluginUtil.hasPlugin(this.opt, pluginClass);
	}

	public <P extends Plugin> P findPlugin(final Class<P> pluginClass) {
		return PluginUtil.findPlugin(this.opt, pluginClass);
	}

	public boolean canInstantiate(final JType type) {
		return getClassOutline(type) != null && !((JClass) type).isAbstract();
	}

	public JExpression castOnDemand(final JType fieldType, final JExpression expression) {
		return this.classes.containsKey(fieldType.fullName()) ? expression : JExpr.cast(fieldType, expression);
	}

	public ClassOutline getClassOutline(final JType typeSpec) {
		return this.classes.get(typeSpec.fullName());
	}

	public boolean cloneThrows(final JType cloneableType, final boolean cloneThrows) {
		try {
			if(cloneableType.fullName().equals("java.lang.Object")) {
				return false;
			} else if (getClassOutline(cloneableType) != null) {
				return cloneThrows;
			} else if(cloneableType.isReference()) {
				final Class<?> runtimeClass = Class.forName(cloneableType.fullName());
				final Method cloneMethod = runtimeClass.getMethod("clone");
				return cloneMethod.getExceptionTypes() != null && cloneMethod.getExceptionTypes().length > 0 && cloneMethod.getExceptionTypes()[0].equals(CloneNotSupportedException.class);
			} else {
				return false;
			}
		} catch(final ClassNotFoundException cnfx) {
			return false;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

}
