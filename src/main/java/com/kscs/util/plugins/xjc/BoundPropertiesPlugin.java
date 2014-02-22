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
import java.io.*;
import java.util.*;

/**
 * Created by klemm0 on 18/02/14.
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
	public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException, IOException {
		final String arg = args[i].toLowerCase();
		if (arg.startsWith("-constrained=")) {
			boolean argConstrained = isTrue(arg);
			boolean argNotConstrained = isFalse(arg);
			if (!argConstrained && !argNotConstrained) {
				throw new BadCommandLineException("-constrained" + BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.constrained = argConstrained;
			}
			return 1;
		} else if (arg.startsWith("-bound=")) {
			boolean argBound = isTrue(arg);
			boolean argNotBound = isFalse(arg);
			if (!argBound && !argNotBound) {
				throw new BadCommandLineException("-bound" + BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.bound = argBound;
			}
			return 1;
		} else if (arg.startsWith("-setter-throws=")) {
			boolean argSetterThrows = isTrue(arg);
			boolean argNoSetterThrows = isFalse(arg);
			if (!argSetterThrows && !argNoSetterThrows) {
				throw new BadCommandLineException("-setter-throws" + BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.setterThrows = argSetterThrows;
			}
			return 1;
		} else if (arg.startsWith("-generate-tools=")) {
			boolean argGenerateTools = isTrue(arg);
			boolean argNoGenerateTools = isFalse(arg);
			if (!argGenerateTools && !argNoGenerateTools) {
				throw new BadCommandLineException("-generate-tools" + BOOLEAN_OPTION_ERROR_MSG);
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
	public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
		if (!constrained && !bound) {
			return true;
		}

		final JCodeModel m = outline.getCodeModel();

		// generate bound collection helper classes
		writeSourceFile(opt.targetDir, BOUND_LIST_INTERFACE_NAME);
		writeSourceFile(opt.targetDir, EVENT_TYPE_ENUM_NAME);
		writeSourceFile(opt.targetDir, CHANGE_EVENT_CLASS_NAME);
		writeSourceFile(opt.targetDir, CHANGE_LISTENER_CLASS_NAME);
		writeSourceFile(opt.targetDir, VETOABLE_CHANGE_LISTENER_CLASS_NAME);
		writeSourceFile(opt.targetDir, PROXY_CLASS_NAME);

		for (final ClassOutline classOutline : outline.getClasses()) {
			System.out.println("Generating bound properties for class "+classOutline.implClass.name());

			final JDefinedClass definedClass = classOutline.implClass;

			// Create bound collection proxies
			for(final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
				if(fieldOutline.getPropertyInfo().isCollection() && !definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false)).type().isArray()) {
					generateProxyField(classOutline, fieldOutline);
					generateLazyProxyInitGetter(classOutline, fieldOutline);
				}
			}


			if (constrained && setterThrows) {
				for (final JMethod method : definedClass.methods()) {
					if (method.name().startsWith("with")
							&& !method.name().equals("withVetoableChangeListener")
							&& !method.name().equals("withPropertyChangeListener")
							) {
						method._throws(PropertyVetoException.class);
					}
				}
			}

			if (constrained)
				createSupportProperty(outline, classOutline, VetoableChangeSupport.class, VetoableChangeListener.class, "vetoableChange");
			if (bound)
				createSupportProperty(outline, classOutline, PropertyChangeSupport.class, PropertyChangeListener.class, "propertyChange");


			for (final JFieldVar field : definedClass.fields().values()) {
				//final JFieldVar field = definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
				System.out.println("---> Generating bound property for field "+field.name());
				final JMethod oldSetter = definedClass.getMethod("set" + outline.getModel().getNameConverter().toPropertyName(field.name()), new JType[]{field.type()});
				if (oldSetter != null && !field.type().isArray()) {
					definedClass.methods().remove(oldSetter);
					final JMethod setter = definedClass.method(JMod.PUBLIC, m.VOID, "set" + outline.getModel().getNameConverter().toPropertyName(field.name()));
					final JVar setterArg = setter.param(JMod.FINAL, field.type(), "value");
					final JBlock body = setter.body();
					final JVar oldValueVar = body.decl(JMod.FINAL, field.type(), "oldValue", JExpr._this().ref(field));

					if (constrained) {
						final JTryBlock tryBlock;
						final JBlock block;
						if (setterThrows) {
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

					if (bound) {
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

	private JInvocation invokeListener(JBlock block, JFieldVar field, JVar oldValueVar, JVar setterArg, final String aspectName) {
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
		final JClass elementType = ((JClass)collectionField.type()).getTypeParameters().get(0);
		return definedClass.field(JMod.PRIVATE | JMod.TRANSIENT, m.ref(BOUND_LIST_INTERFACE_NAME).narrow(elementType), collectionField.name()+"Proxy", JExpr._null() );
	}

	private JMethod generateLazyProxyInitGetter(final ClassOutline classOutline, final FieldOutline fieldOutline) {
		final JCodeModel m = classOutline.parent().getCodeModel();
		final JDefinedClass definedClass = classOutline.implClass;
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);
		final String getterName = "get"+fieldOutline.getPropertyInfo().getName(true);
		final JFieldVar collectionField = definedClass.fields().get(fieldName);
		final JClass elementType = ((JClass)collectionField.type()).getTypeParameters().get(0);
		final JClass proxyFieldType = m.ref(BOUND_LIST_INTERFACE_NAME).narrow(elementType);
		final JFieldRef collectionFieldRef = JExpr._this().ref(collectionField);
		final JFieldRef proxyField = JExpr._this().ref(collectionField.name()+"Proxy");
		final JMethod oldGetter = definedClass.getMethod(getterName, new JType[0]);
		definedClass.methods().remove(oldGetter);
		final JMethod newGetter = definedClass.method(JMod.PUBLIC, proxyFieldType, getterName);
		newGetter.body()._if(collectionFieldRef.eq(JExpr._null()))._then().assign(collectionFieldRef, JExpr._new(m.ref(ArrayList.class).narrow(elementType)));
		final JBlock ifProxyNull = newGetter.body()._if(proxyField.eq(JExpr._null()))._then();
		ifProxyNull.assign(proxyField, JExpr._new(m.ref(PROXY_CLASS_NAME).narrow(elementType)).arg(collectionFieldRef));
		newGetter.body()._return(proxyField);
		return newGetter;
	}

	private String loadBody(final String resourceName) {
		try {
			final StringBuilder sb = new StringBuilder();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + resourceName.replace('.','/')+".java")));
			try {
				String line;
				while((line = reader.readLine()) != null) {
					sb.append(line);
					sb.append("\n");
				}
				final String sourceCode = sb.toString();
				final int beginIndex = sourceCode.indexOf('{') + 1;
				final int endIndex = sourceCode.lastIndexOf("}");
				return sourceCode.substring(beginIndex, endIndex);
			} finally {
				reader.close();
			}
		} catch(IOException iox) {
			throw new RuntimeException(iox);
		}
	}

	private void writeSourceFile(final File targetDir, final String resourceName) {
		try {
			final String resourcePath = "/" + resourceName.replace('.', '/') + ".java";
			final File targetFile = new File(targetDir.getPath()+resourcePath);
			final File packageDir = targetFile.getParentFile();
			final boolean created = packageDir.mkdirs();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourcePath)));
			final BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile));
			try {
				String line;
				while((line = reader.readLine()) != null) {
					writer.write(line+"\n");
				}
			} finally {
				reader.close();
				writer.close();
			}
		} catch(IOException iox) {
			throw new RuntimeException(iox);
		}
	}



	public boolean isConstrained() {
		return constrained;
	}

	public boolean isBound() {
		return bound;
	}

	public boolean isSetterThrows() {
		return setterThrows;
	}
}
