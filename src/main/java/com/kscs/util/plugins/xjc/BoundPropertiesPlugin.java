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

import com.sun.codemodel.*;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.beans.*;
import java.util.ArrayList;

/**
 * XJC Plugin generated conatrained and bound JavaBeans properties
 */
public class BoundPropertiesPlugin extends Plugin {
	public static final String BOOLEAN_OPTION_ERROR_MSG = " option must be either (\"true\",\"on\",\"y\",\"yes\") or (\"false\", \"off\", \"n\",\"no\").";
	public static final String CHANGE_EVENT_CLASS_NAME = "com.kscs.util.jaxb.CollectionChangeEvent";
	public static final String CHANGE_LISTENER_CLASS_NAME = "com.kscs.util.jaxb.CollectionChangeListener";
	public static final String VETOABLE_CHANGE_LISTENER_CLASS_NAME = "com.kscs.util.jaxb.VetoableCollectionChangeListener";
	public static final String EVENT_TYPE_ENUM_NAME = "com.kscs.util.jaxb.CollectionChangeEventType";
	public static final String BOUND_LIST_INTERFACE_NAME = "com.kscs.util.jaxb.BoundList";
	public static final String PROXY_CLASS_NAME = "com.kscs.util.jaxb.BoundListProxy";


	private boolean constrained = true;
	private boolean bound = true;
	private boolean setterThrows = false;
	private boolean generateTools = true;

	@Override
	public String getOptionName() {
		return "Xconstrained-properties";
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException {
		final String arg = args[i].toLowerCase();
		if (arg.startsWith("-constrained=")) {
			final boolean argConstrained = isTrue(arg);
			final boolean argNotConstrained = isFalse(arg);
			if (!argConstrained && !argNotConstrained) {
				throw new BadCommandLineException("-constrained" + BoundPropertiesPlugin.BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.constrained = argConstrained;
			}
			return 1;
		} else if (arg.startsWith("-bound=")) {
			final boolean argBound = isTrue(arg);
			final boolean argNotBound = isFalse(arg);
			if (!argBound && !argNotBound) {
				throw new BadCommandLineException("-bound" + BoundPropertiesPlugin.BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.bound = argBound;
			}
			return 1;
		} else if (arg.startsWith("-setter-throws=")) {
			final boolean argSetterThrows = isTrue(arg);
			final boolean argNoSetterThrows = isFalse(arg);
			if (!argSetterThrows && !argNoSetterThrows) {
				throw new BadCommandLineException("-setter-throws" + BoundPropertiesPlugin.BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.setterThrows = argSetterThrows;
			}
			return 1;
		} else if (arg.startsWith("-generate-tools=")) {
			final boolean argGenerateTools = isTrue(arg);
			final boolean argNoGenerateTools = isFalse(arg);
			if (!argGenerateTools && !argNoGenerateTools) {
				throw new BadCommandLineException("-generate-tools" + BoundPropertiesPlugin.BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.generateTools = argGenerateTools;
			}
			return 1;
		}
		return 0;
	}


	private boolean isTrue(final String arg) {
		return arg.endsWith("y") || arg.endsWith("true") || arg.endsWith("on") || arg.endsWith("yes");
	}

	private boolean isFalse(final String arg) {
		return arg.endsWith("n") || arg.endsWith("false") || arg.endsWith("off") || arg.endsWith("no");
	}

	@Override
	public String getUsage() {
		return "-Xconstrained-properties: Generate bound properties for JAXB serializable classes.\n" +
				"\t-constrained={yes|no}: \tswitch \"constrained\" property contract generation on/off. Default: yes\n" +
				"\t-bound={yes|no}: \tswitch \"bound\" property contract generation on/off. Default: yes\n" +
				"\t-setter-throws={yes|no}: \tDeclare setXXX methods to throw PropertyVetoException (yes), or rethrow as RuntimeException (no). Default: no\n" +
				"\t-generate-tools={yes|no}: \tGenerate helper classes needed for collection change event detection. Turn off in modules that import other generated modules. Default: yes\n";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		if (!this.constrained && !this.bound) {
			return true;
		}

		final JCodeModel m = outline.getCodeModel();

		if (this.generateTools) {
			// generate bound collection helper classes
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, BoundPropertiesPlugin.BOUND_LIST_INTERFACE_NAME);
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, BoundPropertiesPlugin.EVENT_TYPE_ENUM_NAME);
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, BoundPropertiesPlugin.CHANGE_EVENT_CLASS_NAME);
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, BoundPropertiesPlugin.CHANGE_LISTENER_CLASS_NAME);
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, BoundPropertiesPlugin.VETOABLE_CHANGE_LISTENER_CLASS_NAME);
			PluginUtil.writeSourceFile(getClass(), opt.targetDir, BoundPropertiesPlugin.PROXY_CLASS_NAME);
		}

		final int setterAccess = PluginUtil.hasPlugin(opt, ImmutablePlugin.class) ? JMod.PROTECTED : JMod.PUBLIC;

		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;

			// Create bound collection proxies
			for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
				if (fieldOutline.getPropertyInfo().isCollection() && !definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false)).type().isArray()) {
					generateProxyField(classOutline, fieldOutline);
					generateLazyProxyInitGetter(classOutline, fieldOutline);
				}
			}


			if (this.constrained && this.setterThrows) {
				for (final JMethod method : definedClass.methods()) {
					if (method.name().startsWith("with")
							&& !"withVetoableChangeListener".equals(method.name())
							&& !"withPropertyChangeListener".equals(method.name())
							) {
						method._throws(PropertyVetoException.class);
					}
				}
			}

			if (this.constrained)
				createSupportProperty(outline, classOutline, VetoableChangeSupport.class, VetoableChangeListener.class, "vetoableChange");
			if (this.bound)
				createSupportProperty(outline, classOutline, PropertyChangeSupport.class, PropertyChangeListener.class, "propertyChange");


			for (final JFieldVar field : definedClass.fields().values()) {
				//final JFieldVar field = definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
				final JMethod oldSetter = definedClass.getMethod("set" + outline.getModel().getNameConverter().toPropertyName(field.name()), new JType[]{field.type()});
				if (oldSetter != null && !field.type().isArray()) {
					definedClass.methods().remove(oldSetter);
					final JMethod setter = definedClass.method(setterAccess, m.VOID, "set" + outline.getModel().getNameConverter().toPropertyName(field.name()));
					final JVar setterArg = setter.param(JMod.FINAL, field.type(), "value");
					final JBlock body = setter.body();
					final JVar oldValueVar = body.decl(JMod.FINAL, field.type(), "oldValue", JExpr._this().ref(field));

					if (this.constrained) {
						final JTryBlock tryBlock;
						final JBlock block;
						if (this.setterThrows) {
							block = body;
							setter._throws(PropertyVetoException.class);
						} else {
							tryBlock = body._try();
							block = tryBlock.body();
							final JCatchBlock catchBlock = tryBlock._catch(m.ref(PropertyVetoException.class));
							final JVar exceptionVar = catchBlock.param("x");
							catchBlock.body()._throw(JExpr._new(m.ref(RuntimeException.class)).arg(exceptionVar));
						}
						invokeListener(block, field, oldValueVar, setterArg, "vetoableChange");
					}

					body.assign(JExpr._this().ref(field), setterArg);

					if (this.bound) {
						invokeListener(body, field, oldValueVar, setterArg, "propertyChange");
					}
				}
			}
		}
		return true;
	}

	private void createSupportProperty(final Outline outline,
										final ClassOutline classOutline,
										final Class<?> supportClass,
										final Class<?> listenerClass,
										final String aspectName) {
		final JCodeModel m = outline.getCodeModel();
		final JDefinedClass definedClass = classOutline.implClass;

		final String aspectNameCap = aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1);

		if (classOutline.getSuperClass() == null) { // only generate fields in topmost classes
			final JFieldVar supportField = definedClass.field(JMod.PROTECTED | JMod.FINAL | JMod.TRANSIENT, supportClass, aspectName + "Support", JExpr._new(m.ref(supportClass)).arg(JExpr._this()));
			final JMethod addMethod = definedClass.method(JMod.PUBLIC, m.VOID, "add" + aspectNameCap + "Listener");
			final JVar addParam = addMethod.param(JMod.FINAL, listenerClass, aspectName + "Listener");
			addMethod.body().invoke(JExpr._this().ref(supportField), "add" + aspectNameCap + "Listener").arg(addParam);
		}
		final JMethod withMethod = definedClass.method(JMod.PUBLIC, definedClass, "with" + aspectNameCap + "Listener");
		final JVar withParam = withMethod.param(JMod.FINAL, listenerClass, aspectName + "Listener");
		withMethod.body().invoke("add" + aspectNameCap + "Listener").arg(withParam);
		withMethod.body()._return(JExpr._this());

		if (classOutline.getSuperClass() != null) {
			withMethod.annotate(Override.class);
		}
	}

	private JInvocation invokeListener(final JBlock block, final JFieldVar field, final JVar oldValueVar, final JVar setterArg, final String aspectName) {
		final String aspectNameCap = aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1);
		final JInvocation fvcInvoke = block.invoke(JExpr._this().ref(aspectName + "Support"), "fire" + aspectNameCap);
		fvcInvoke.arg(JExpr.lit(field.name()));
		fvcInvoke.arg(oldValueVar);
		fvcInvoke.arg(setterArg);
		return fvcInvoke;
	}

	private JFieldVar generateProxyField(final ClassOutline classOutline, final FieldOutline fieldOutline) {
		final JCodeModel m = classOutline.parent().getCodeModel();
		final JDefinedClass definedClass = classOutline.implClass;
		final JFieldVar collectionField = definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
		final JClass elementType = ((JClass) collectionField.type()).getTypeParameters().get(0);
		return definedClass.field(JMod.PRIVATE | JMod.TRANSIENT, m.ref(BoundPropertiesPlugin.BOUND_LIST_INTERFACE_NAME).narrow(elementType), collectionField.name() + "Proxy", JExpr._null());
	}

	private JMethod generateLazyProxyInitGetter(final ClassOutline classOutline, final FieldOutline fieldOutline) {
		final JCodeModel m = classOutline.parent().getCodeModel();
		final JDefinedClass definedClass = classOutline.implClass;
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);
		final String getterName = "get" + fieldOutline.getPropertyInfo().getName(true);
		final JFieldVar collectionField = definedClass.fields().get(fieldName);
		final JClass elementType = ((JClass) collectionField.type()).getTypeParameters().get(0);
		final JClass proxyFieldType = m.ref(BoundPropertiesPlugin.BOUND_LIST_INTERFACE_NAME).narrow(elementType);
		final JFieldRef collectionFieldRef = JExpr._this().ref(collectionField);
		final JFieldRef proxyField = JExpr._this().ref(collectionField.name() + "Proxy");
		final JMethod oldGetter = definedClass.getMethod(getterName, new JType[0]);
		definedClass.methods().remove(oldGetter);
		final JMethod newGetter = definedClass.method(JMod.PUBLIC, proxyFieldType, getterName);
		newGetter.body()._if(collectionFieldRef.eq(JExpr._null()))._then().assign(collectionFieldRef, JExpr._new(m.ref(ArrayList.class).narrow(elementType)));
		final JBlock ifProxyNull = newGetter.body()._if(proxyField.eq(JExpr._null()))._then();
		ifProxyNull.assign(proxyField, JExpr._new(m.ref(BoundPropertiesPlugin.PROXY_CLASS_NAME).narrow(elementType)).arg(collectionFieldRef));
		newGetter.body()._return(proxyField);
		return newGetter;
	}

	public boolean isConstrained() {
		return this.constrained;
	}

	public boolean isSetterThrows() {
		return this.setterThrows;
	}
}
