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

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kscs.util.jaxb.BuilderUtilities;
import com.kscs.util.jaxb.Copyable;
import com.kscs.util.jaxb.PartialCopyable;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.sun.codemodel.JAssignmentTarget;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.fmt.JStaticJavaFile;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.EnumOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

/**
 * Common constants and constructs
 */
public class ApiConstructs {
	public static final String BUILDER_CLASS_NAME = "Builder";
	public static final String BUILDER_INTERFACE_NAME = "BuildSupport";
	public static final String BUILD_METHOD_NAME = "build";
	public static final String INIT_METHOD_NAME = "init";
	public static final String PRODUCT_INSTANCE_NAME = "product";
	public static final String ADD_METHOD_PREFIX = "add";
	public static final String WITH_METHOD_PREFIX = "with";
	public static final String NEW_OBJECT_VAR_NAME = "_newObject";
	public static final String ADD_ALL = "addAll";
	public static final String GET_BUILDER = "getBuilder";
	public static final String CLONE_METHOD_NAME = "clone";
	public static final String COPY_METHOD_NAME = "createCopy";
	public static final String COPY_EXCEPT_METHOD_NAME = "copyExcept";
	public static final String COPY_ONLY_METHOD_NAME = "copyOnly";
	public static final String BUILD_COPY_METHOD_NAME = "copyOf";
	public static final String NEW_BUILDER_METHOD_NAME = "builder";
	private static final String AS_LIST = "asList";
	private static final String UNMODIFIABLE_LIST = "unmodifiableList";
	public final JCodeModel codeModel;
	public final JClass arrayListClass;
	public final JClass listClass;
	public final JClass iterableClass;
	public final JClass collectionClass;
	public final Options opt;
	public final JClass cloneableInterface;
	public final Outline outline;
	public final ErrorHandler errorHandler;
	public final Map<QName, ClassOutline> classesBySchemaComponent;
	public final JClass partialCopyableInterface;
	public final JClass copyableInterface;
	public final JClass builderUtilitiesClass;
	public final JClass stringClass;
	public final JClass voidClass;
	public final JClass cloneGraphClass;
	public final JExpression excludeConst;
	public final JExpression includeConst;
	public final String cloneMethodName;
	public final String copyMethodName;
	public final String copyExceptMethodName;
	public final String copyOnlyMethodName;
	public final String buildCopyMethodName;
	public final String newBuilderMethodName;
	public final String newObjectVarName;
	private final JClass collectionsClass;
	private final JClass arraysClass;
	private final Map<String, ClassOutline> classes;
	private final Map<String, EnumOutline> enums;

	ApiConstructs(final Outline outline, final Options opt, final ErrorHandler errorHandler) {
		this.outline = outline;
		this.errorHandler = errorHandler;
		this.codeModel = outline.getCodeModel();
		this.opt = opt;
		this.arrayListClass = this.codeModel.ref(ArrayList.class);
		this.listClass = this.codeModel.ref(List.class);
		this.iterableClass = this.codeModel.ref(Collection.class);
		this.collectionClass = this.codeModel.ref(Collection.class);
		this.collectionsClass = this.codeModel.ref(Collections.class);
		this.arraysClass = this.codeModel.ref(Arrays.class);
		this.cloneableInterface = this.codeModel.ref(Cloneable.class);
		this.partialCopyableInterface = this.codeModel.ref(PartialCopyable.class);
		this.copyableInterface = this.codeModel.ref(Copyable.class);
		this.classes = new HashMap<>(outline.getClasses().size());
		this.classesBySchemaComponent = new HashMap<>(outline.getClasses().size());
		this.enums = new HashMap<>(outline.getEnums().size());
		this.builderUtilitiesClass = this.codeModel.ref(BuilderUtilities.class);
		this.cloneGraphClass = this.codeModel.ref(PropertyTree.class);
		this.stringClass = this.codeModel.ref(String.class);
		this.voidClass = this.codeModel.ref(Void.class);
		for (final ClassOutline classOutline : this.outline.getClasses()) {
			this.classes.put(classOutline.implClass.fullName(), classOutline);
			this.classesBySchemaComponent.put(classOutline.target.getTypeName(), classOutline);
		}
		for (final EnumOutline classOutline : this.outline.getEnums()) {
			this.enums.put(classOutline.clazz.fullName(), classOutline);
		}
		this.excludeConst = this.codeModel.ref(PropertyTreeUse.class).staticRef("EXCLUDE");
		this.includeConst = this.codeModel.ref(PropertyTreeUse.class).staticRef("INCLUDE");
		this.cloneMethodName = ApiConstructs.CLONE_METHOD_NAME;
		this.copyMethodName = ApiConstructs.COPY_METHOD_NAME;
		this.copyExceptMethodName = ApiConstructs.COPY_EXCEPT_METHOD_NAME;
		this.copyOnlyMethodName = ApiConstructs.COPY_ONLY_METHOD_NAME;
		this.buildCopyMethodName = ApiConstructs.BUILD_COPY_METHOD_NAME;
		this.newBuilderMethodName = ApiConstructs.NEW_BUILDER_METHOD_NAME;
		this.newObjectVarName = ApiConstructs.NEW_OBJECT_VAR_NAME;
	}

	private boolean cloneThrows(final Class<? extends Cloneable> cloneableClass) {
		if (cloneableClass.getSuperclass() == null) {
			// java.lang.Object.clone() throws CloneNotSupportedException
			return true;
		} else {
			try {
				final Method cloneMethod = cloneableClass.getMethod(this.cloneMethodName);
				final Class<?>[] exceptionTypes = cloneMethod.getExceptionTypes();
				return (exceptionTypes.length > 0 && CloneNotSupportedException.class.isAssignableFrom(exceptionTypes[0]));
			} catch (final NoSuchMethodException e) {
				return false;
			}
		}
	}

	public JInvocation asList(final JExpression expression) {
		return this.arraysClass.staticInvoke(ApiConstructs.AS_LIST).arg(expression);
	}

	public JInvocation unmodifiableList(final JExpression expression) {
		return this.collectionsClass.staticInvoke(ApiConstructs.UNMODIFIABLE_LIST).arg(expression);
	}

	public boolean hasPlugin(final Class<? extends Plugin> pluginClass) {
		for (final Plugin plugin : this.opt.activePlugins) {
			if (pluginClass.isInstance(plugin)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public <P extends Plugin> P findPlugin(final Class<P> pluginClass) {
		for (final Plugin plugin : this.opt.activePlugins) {
			if (pluginClass.isInstance(plugin)) {
				return (P) plugin;
			}
		}
		return null;
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

	EnumOutline getEnumOutline(final JType typeSpec) {
		return this.enums.get(typeSpec.fullName());
	}

	public JForEach loop(final JBlock block, final JFieldRef source, final JType sourceElementType, final JAssignmentTarget target, final JType targetElementType) {
		final JConditional ifNull = block._if(source.eq(JExpr._null()));
		ifNull._then().assign(target, JExpr._null());
		ifNull._else().assign(target, JExpr._new(this.arrayListClass.narrow(targetElementType)));
		return ifNull._else().forEach(sourceElementType, "_item", source);
	}

	public JInvocation newArrayList(final JClass elementType) {
		return JExpr._new(this.arrayListClass.narrow(elementType));
	}

	public BuilderOutline getReferencedBuilderOutline(final JType type) {
		try {
			BuilderOutline builderOutline = null;
			if (getClassOutline(type) == null && getEnumOutline(type) == null && type.isReference() && !type.isPrimitive() && !type.isArray() && type.fullName().contains(".")) {
				final Class<?> cls = Class.forName(type.binaryName());
				final JClass builderClass = getBuilderClass(cls);
				if (builderClass != null) {
					final ReferencedClassOutline referencedClassOutline = new ReferencedClassOutline(this.codeModel, cls);
					builderOutline = new BuilderOutline(referencedClassOutline, builderClass);
				}
			}
			return builderOutline;
		} catch (final Exception e) {
			return null;
		}

	}

	public JClass getBuilderClass(final Class<?> cls) {
		try {
			final Class<?> builderClass = Class.forName(cls.getName() + "$" + ApiConstructs.BUILDER_CLASS_NAME);
			return this.codeModel.ref(builderClass);
		} catch (final Exception e) {
			return null;
		}
	}

	public void writeSourceFile(final Class<?> classToBeWritten) {
		final String resourcePath = "/" + classToBeWritten.getName().replace('.', '/') + ".java";
		final JPackage jPackage = this.outline.getCodeModel()._package(classToBeWritten.getPackage().getName());
		final JStaticJavaFile javaFile = new JStaticJavaFile(jPackage, classToBeWritten.getSimpleName(), ApiConstructs.class.getResource(resourcePath), null);
		jPackage.addResourceFile(javaFile);
	}

	@SuppressWarnings("unchecked")
	JBlock catchCloneNotSupported(final JBlock body, final JClass elementType) {
		final Class<? extends Cloneable> elementRuntimeClass;
		try {
			elementRuntimeClass = (Class<? extends Cloneable>) Class.forName(elementType.binaryName());
		} catch (final ClassNotFoundException e) {
			return body;
		}
		if (!cloneThrows(elementRuntimeClass)) {
			return body;
		} else {
			final JTryBlock tryBlock = body._try();
			final JCatchBlock catchBlock = tryBlock._catch(this.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("e");
			catchBlock.body()._throw(JExpr._new(this.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
			return tryBlock.body();
		}
	}
}
