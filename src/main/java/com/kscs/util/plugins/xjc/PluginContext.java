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

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.kscs.util.jaxb.Buildable;
import com.kscs.util.jaxb.Copyable;
import com.kscs.util.jaxb.PartialCopyable;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.kscs.util.plugins.xjc.codemodel.JDirectInnerClassRef;
import com.kscs.util.plugins.xjc.codemodel.JTypedInvocation;
import com.kscs.util.plugins.xjc.outline.PropertyOutline;
import com.sun.codemodel.JAssignmentTarget;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.codemodel.fmt.JStaticJavaFile;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CCustomizable;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.EnumOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSDeclaration;

/**
 * Common context for all plugins implemented by this
 * plugin package
 */
public class PluginContext extends Plugin {
	public static final String FLUENT_CLASS_NAME = "Fluent";
	public static final String FLUENT_INTERFACE_NAME = "FluentSupport";
	public static final String BUILDER_CLASS_NAME = "Builder";
	public static final String BUILDER_INTERFACE_NAME = "BuildSupport";
	public static final String WRAPPER_CLASS_NAME = "Wrapper";
	public static final String MODIFIER_CLASS_NAME = "Modifier";
	public static final String WRAPPER_INTERFACE_NAME = "WrapSupport";
	public static final String MODIFIER_INTERFACE_NAME = "ModifySupport";
	public static final String BUILD_METHOD_NAME = "build";
	public static final String MODIFY_METHOD_NAME = "apply";
	public static final String INIT_METHOD_NAME = "init";
	public static final String ADD_METHOD_PREFIX = "add";
	public static final String WITH_METHOD_PREFIX = "with";
	public static final String NEW_OBJECT_VAR_NAME = "_newObject";
	public static final String ADD_ALL = "addAll";
	public static final String CLONE_METHOD_NAME = "clone";
	public static final String COPY_METHOD_NAME = "createCopy";
	public static final String COPY_EXCEPT_METHOD_NAME = "copyExcept";
	public static final String COPY_ONLY_METHOD_NAME = "copyOnly";
	public static final String BUILD_COPY_METHOD_NAME = "copyOf";
	public static final String NEW_BUILDER_METHOD_NAME = "builder";
	public static final String NEW_MODIFIER_METHOD_NAME = "modifier";
	public static final String NEW_COPY_BUILDER_METHOD_NAME = "newCopyBuilder";
	public static final String COPY_TO_METHOD_NAME = "copyTo";
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
	public final JClass stringClass;
	public final JClass voidClass;
	public final JType voidType;
	public final JClass buildableClass;
	public final JClass buildableInterface;
	public final JClass cloneGraphClass;
	public final JExpression excludeConst;
	public final JExpression includeConst;
	public final String cloneMethodName;
	public final String copyMethodName;
	public final String copyExceptMethodName;
	public final String copyOnlyMethodName;
	public final String buildCopyMethodName;
	public final String newBuilderMethodName;
	public final String newModifierMethodName;
	public final String newCopyBuilderMethodName;
	public final String newObjectVarName;
	private final JClass collectionsClass;
	private final JClass arraysClass;
	private final Map<String, ClassOutline> classes;
	private final Map<String, EnumOutline> enums;

	private PluginContext(final Outline outline, final Options opt, final ErrorHandler errorHandler) {
		this.outline = outline;
		this.errorHandler = errorHandler;
		this.codeModel = outline.getCodeModel();
		this.opt = opt;
		this.arrayListClass = this.codeModel.ref(ArrayList.class);
		this.listClass = this.codeModel.ref(List.class);
		this.iterableClass = this.codeModel.ref(Iterable.class);
		this.collectionClass = this.codeModel.ref(Collection.class);
		this.collectionsClass = this.codeModel.ref(Collections.class);
		this.arraysClass = this.codeModel.ref(Arrays.class);
		this.cloneableInterface = this.codeModel.ref(Cloneable.class);
		this.partialCopyableInterface = this.codeModel.ref(PartialCopyable.class);
		this.copyableInterface = this.codeModel.ref(Copyable.class);
		this.classes = new HashMap<>(outline.getClasses().size());
		this.classesBySchemaComponent = new HashMap<>(outline.getClasses().size());
		this.enums = new HashMap<>(outline.getEnums().size());
		this.cloneGraphClass = this.codeModel.ref(PropertyTree.class);
		this.stringClass = this.codeModel.ref(String.class);
		this.voidClass = this.codeModel.ref(Void.class);
		this.voidType = this.codeModel.VOID;
		for (final ClassOutline classOutline : this.outline.getClasses()) {
			this.classes.put(classOutline.implClass.fullName(), classOutline);
			this.classesBySchemaComponent.put(classOutline.target.getTypeName(), classOutline);
		}
		for (final EnumOutline classOutline : this.outline.getEnums()) {
			this.enums.put(classOutline.clazz.fullName(), classOutline);
		}
		this.excludeConst = this.codeModel.ref(PropertyTreeUse.class).staticRef("EXCLUDE");
		this.includeConst = this.codeModel.ref(PropertyTreeUse.class).staticRef("INCLUDE");
		this.cloneMethodName = PluginContext.CLONE_METHOD_NAME;
		this.copyMethodName = PluginContext.COPY_METHOD_NAME;
		this.copyExceptMethodName = PluginContext.COPY_EXCEPT_METHOD_NAME;
		this.copyOnlyMethodName = PluginContext.COPY_ONLY_METHOD_NAME;
		this.buildCopyMethodName = PluginContext.BUILD_COPY_METHOD_NAME;
		this.newBuilderMethodName = PluginContext.NEW_BUILDER_METHOD_NAME;
		this.newModifierMethodName = PluginContext.NEW_MODIFIER_METHOD_NAME;
		this.newCopyBuilderMethodName = PluginContext.NEW_COPY_BUILDER_METHOD_NAME;
		this.newObjectVarName = PluginContext.NEW_OBJECT_VAR_NAME;
		this.buildableInterface = this.codeModel.ref(Buildable.class);
		this.buildableClass = this.codeModel.ref(Buildable.PrimitiveBuildable.class);
	}

	public static PluginContext get(final Outline outline, final Options opt, final ErrorHandler errorHandler) {
		PluginContext context = findPlugin(opt.getAllPlugins(), PluginContext.class);
		if (context == null) {
			context = new PluginContext(outline, opt, errorHandler);
			opt.getAllPlugins().add(context);
		}
		return context;
	}

	public static <T> T coalesce(final T... args) {
		for (final T t : args) {
			if (t != null) {
				return t;
			}
		}
		return null;
	}

	public static Class<?> findInnerClass(final Class<?> outer, final String name) {
		for (final Class<?> innerClass : outer.getDeclaredClasses()) {
			if (name.equals(innerClass.getSimpleName())) {
				return innerClass;
			}
		}
		return null;
	}

	public static QName getQName(final XSDeclaration declaration) {
		return new QName(declaration.getTargetNamespace(), declaration.getName());
	}

	public static QName getQName(final Class<?> boundClass) {
		return new QName(getNamespaceUri(boundClass), getLocalName(boundClass));
	}

	private static String getNamespaceUri(final Class<?> boundClass) {
		final XmlRootElement elementAnnotation = boundClass.getAnnotation(XmlRootElement.class);
		if (elementAnnotation != null && !"##default".equals(elementAnnotation.namespace())) {
			return elementAnnotation.namespace();
		} else {
			final XmlType xmlTypeAnnotation = boundClass.getAnnotation(XmlType.class);
			if (xmlTypeAnnotation != null && !"##default".equals(xmlTypeAnnotation.namespace())) {
				return xmlTypeAnnotation.namespace();
			} else {
				return getNamespaceUri(boundClass.getPackage());
			}
		}
	}

	private static String getLocalName(final Class<?> boundClass) {
		final XmlRootElement elementAnnotation = boundClass.getAnnotation(XmlRootElement.class);
		if (elementAnnotation != null && !"##default".equals(elementAnnotation.name())) {
			return elementAnnotation.name();
		} else {
			final XmlType xmlTypeAnnotation = boundClass.getAnnotation(XmlType.class);
			if (xmlTypeAnnotation != null && !"##default".equals(xmlTypeAnnotation.name())) {
				return xmlTypeAnnotation.name();
			} else {
				return boundClass.getSimpleName();
			}
		}
	}

	private static String getNamespaceUri(final Package pkg) {
		final XmlSchema xmlSchemaAnnotation = pkg.getAnnotation(XmlSchema.class);
		return xmlSchemaAnnotation != null && !"##default".equals(xmlSchemaAnnotation.namespace()) ? xmlSchemaAnnotation.namespace() : null;
	}

	public static JMethod findGetter(final FieldOutline field) {
		final ClassOutline classOutline = field.parent();
		String propertyName = field.getPropertyInfo().getName(true);
		if ("Any".equals(propertyName)) {
			propertyName = "Content";
		}
		String getterName = "get" + propertyName;
		JMethod m = classOutline.implClass.getMethod(getterName, new JType[0]);
		if (m == null) {
			getterName = "is" + propertyName;
			m = classOutline.implClass.getMethod(getterName, new JType[0]);
		}
		return m;
	}

	public static JMethod findSetter(final FieldOutline field) {
		final ClassOutline classOutline = field.parent();
		String propertyName = field.getPropertyInfo().getName(true);
		if ("Any".equals(propertyName)) {
			propertyName = "Content";
		}
		final String setterName = "set" + propertyName;
		for (final JMethod method : classOutline.implClass.methods()) {
			if (method.name().equals(setterName) && method.listParams().length == 1) {
				return method;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <P extends Plugin> P findPlugin(final Options opt, final Class<P> pluginClass) {
		return findPlugin(opt.activePlugins, pluginClass);
	}

	@SuppressWarnings("unchecked")
	public static <P extends Plugin> P findPlugin(final Iterable<Plugin> pluginCollection, final Class<P> pluginClass) {
		for (final Plugin plugin : pluginCollection) {
			if (pluginClass.isInstance(plugin)) {
				return (P) plugin;
			}
		}
		return null;
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
		return this.arraysClass.staticInvoke(PluginContext.AS_LIST).arg(expression);
	}

	public JInvocation unmodifiableList(final JExpression expression) {
		return this.collectionsClass.staticInvoke(PluginContext.UNMODIFIABLE_LIST).arg(expression);
	}

	public boolean hasPlugin(final Class<? extends Plugin> pluginClass) {
		for (final Plugin plugin : this.opt.activePlugins) {
			if (pluginClass.isInstance(plugin)) {
				return true;
			}
		}
		return false;
	}

	public <P extends Plugin> P findPlugin(final Class<P> pluginClass) {
		return findPlugin(this.opt, pluginClass);
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

	public JForEach loop(final JBlock block, final JExpression source, final JType sourceElementType, final JAssignmentTarget target, final JType targetElementType) {
		final JConditional ifNull = block._if(source.eq(JExpr._null()));
		ifNull._then().assign(target, JExpr._null());
		ifNull._else().assign(target, JExpr._new(this.arrayListClass.narrow(targetElementType)));
		return ifNull._else().forEach(sourceElementType, "_item", source);
	}

	public JInvocation newArrayList(final JClass elementType) {
		return JExpr._new(this.arrayListClass.narrow(elementType));
	}


	public JDirectInnerClassRef ref(final JClass outer, final String name, final boolean isInterface, final boolean isAbstract, final JClass superClass) {
		return new JDirectInnerClassRef(outer, name, isInterface, isAbstract, superClass);
	}

	public JDirectInnerClassRef ref(final JClass outer, final String name, final boolean isInterface) {
		return new JDirectInnerClassRef(outer, name, isInterface, false, null);
	}

	public JDirectInnerClassRef ref(final JClass outer, final String name) {
		return new JDirectInnerClassRef(outer, name, false, false, null);
	}

	public JTypedInvocation invoke(final JExpression lhs, final String method) {
		return new JTypedInvocation(lhs, method);
	}

	public JTypedInvocation invoke(final JType lhs, final String method) {
		return new JTypedInvocation(lhs, method);
	}

	public JTypedInvocation invoke(final String method) {
		return new JTypedInvocation(method);
	}

	public JTypedInvocation _super() {
		return invoke("super");
	}

	public CopyGenerator createCopyGenerator(final JMethod method, final boolean partial) {
		return partial ? new PartialCopyGenerator(this, method) : new FullCopyGenerator();
	}

	public void writeSourceFile(final Class<?> classToBeWritten) {
		final String resourcePath = "/" + classToBeWritten.getName().replace('.', '/') + ".java";
		final JPackage jPackage = this.outline.getCodeModel()._package(classToBeWritten.getPackage().getName());
		final JStaticJavaFile javaFile = new JStaticJavaFile(jPackage, classToBeWritten.getSimpleName(), PluginContext.class.getResource(resourcePath), null);
		jPackage.addResourceFile(javaFile);
	}

	public void generateImmutableFieldInit(final JBlock body, final JExpression object, final JFieldVar field) {
		final ImmutablePlugin immutablePlugin = findPlugin(ImmutablePlugin.class);
		if (immutablePlugin != null) {
			immutablePlugin.immutableInit(this, body, object, field);
		}
	}

	public void generateImmutableFieldInit(final JBlock body, final JExpression object, final PropertyOutline propertyOutline) {
		final ImmutablePlugin immutablePlugin = findPlugin(ImmutablePlugin.class);
		if (immutablePlugin != null) {
			immutablePlugin.immutableInit(this, body, object, propertyOutline);
		}
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

	boolean mustCatch(final JClass fieldType) {
		final Class<? extends Cloneable> elementRuntimeClass;
		try {
			elementRuntimeClass = (Class<? extends Cloneable>) Class.forName(fieldType.binaryName());
		} catch (final ClassNotFoundException e) {
			return false;
		}
		return cloneThrows(elementRuntimeClass);
	}

	public <T> T getCustomization(final Class<T> customizationClass, final Deque<CCustomizable> schemaComponents) {
		final QName qName = getQName(customizationClass);
		if (!schemaComponents.isEmpty()) {
			final CCustomizable schemaComponent = schemaComponents.pop();
			final CPluginCustomization pluginCustomization = schemaComponent.getCustomizations().find(qName.getNamespaceURI(), qName.getLocalPart());
			if (pluginCustomization == null) {
				return getCustomization(customizationClass, schemaComponents);
			} else {
				pluginCustomization.markAsAcknowledged();
				return JAXB.unmarshal(new DOMSource(pluginCustomization.element), customizationClass);
			}
		} else {
			final CPluginCustomization pluginCustomization = this.outline.getModel().getCustomizations().find(qName.getNamespaceURI(), qName.getLocalPart());
			if (pluginCustomization != null) {
				pluginCustomization.markAsAcknowledged();
				return JAXB.unmarshal(new DOMSource(pluginCustomization.element), customizationClass);
			} else {
				return null;
			}
		}
	}

	public <T> T getCustomization(final Class<T> customizationClass, final CCustomizable... schemaComponents) {
		final Deque<CCustomizable> schemaComponentDeque = new ArrayDeque<>(Arrays.asList(schemaComponents));
		return getCustomization(customizationClass, schemaComponentDeque);
	}

	public <T> T getCustomization(final Class<T> customizationClass, final T defaultValue, final CCustomizable... schemaComponents) {
		final T val = getCustomization(customizationClass, schemaComponents);
		return val == null ? defaultValue : val;
	}

	@Override
	public String getOptionName() {
		return "-X_pluginContext_";
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		return true;
	}

	public JTypedInvocation _new(final JClass type) {
		return new JTypedInvocation(type);
	}
}
