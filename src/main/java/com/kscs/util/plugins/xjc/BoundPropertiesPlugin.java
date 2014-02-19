package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.annotation.XmlTransient;
import java.beans.*;
import java.io.IOException;

/**
 * Created by klemm0 on 18/02/14.
 */
public class BoundPropertiesPlugin extends Plugin {
	public static final String BOOLEAN_OPTION_ERROR_MSG = " option must be either (\"true\",\"on\",\"y\",\"yes\") or (\"false\", \"off\", \"n\",\"no\").";
	private boolean constrained = true;
	private boolean bound = true;
	private boolean setterThrows = false;

	@Override
	public String getOptionName() {
		return "Xconstrained-properties";
	}

	@Override
	public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException, IOException {
		final String arg = args[i].toLowerCase();
		if(arg.startsWith("-constrained=")) {
			boolean argConstrained = isTrue(arg);
			boolean argNotConstrained = isFalse(arg);
			if(!argConstrained && !argNotConstrained) {
				throw new BadCommandLineException("-constrained"+BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.constrained = argConstrained;
			}
			return 1;
		} else if (arg.startsWith("-bound=")) {
			boolean argBound = isTrue(arg);
			boolean argNotBound = isFalse(arg);
			if(!argBound && !argNotBound) {
				throw new BadCommandLineException("-bound"+BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.bound = argBound;
			}
			return 1;
		} else if(arg.startsWith("-setter-throws=")) {
			boolean argSetterThrows = isTrue(arg);
			boolean argNoSetterThrows = isFalse(arg);
			if(!argSetterThrows && !argNoSetterThrows) {
				throw new BadCommandLineException("-setter-throws"+BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.setterThrows = argSetterThrows;
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
				"\t-setter-throws={yes|no}: \tDeclare setXXX methods to throw PropertyVetoException (yes), or rethrow as RuntimeException (no). Default: no";
	}

	@Override
	public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
		if(!constrained && !bound) {
			return true;
		}

		final JCodeModel m = outline.getCodeModel();
		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;

			if(constrained) createSupportProperty(outline, classOutline, VetoableChangeSupport.class, VetoableChangeListener.class, "vetoableChange");
			if(bound) createSupportProperty(outline, classOutline, PropertyChangeSupport.class, PropertyChangeListener.class, "propertyChange");


			for (final JFieldVar field : definedClass.fields().values()) {
				final JMethod oldSetter = definedClass.getMethod("set" + outline.getModel().getNameConverter().toPropertyName(field.name()), new JType[]{field.type()});
				if (oldSetter != null && !field.type().isArray()) {
					definedClass.methods().remove(oldSetter);
					final JMethod setter = definedClass.method(JMod.PUBLIC, m.VOID, "set" + outline.getModel().getNameConverter().toPropertyName(field.name()));
					final JVar setterArg = setter.param(JMod.FINAL, field.type(), "value");
					final JBlock body = setter.body();
					//setter._throws(PropertyVetoException.class);
					final JVar oldValueVar = body.decl(JMod.FINAL, field.type(), "oldValue", JExpr._this().ref(field));

					if(constrained) {
						final JTryBlock tryBlock;
						final JBlock block;
						if(setterThrows) {
							tryBlock = null;
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

					if(bound) {
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

		final String aspectNameCap = aspectName.substring(0,1).toUpperCase() + aspectName.substring(1);

		if (classOutline.getSuperClass() == null) { // only generate fields in topmost classes
			final JFieldVar supportField = definedClass.field(JMod.PROTECTED | JMod.FINAL | JMod.TRANSIENT, supportClass, aspectName+"Support", JExpr._new(m.ref(supportClass)).arg(JExpr._this()));
			final JMethod addMethod = definedClass.method(JMod.PUBLIC, m.VOID, "add"+aspectNameCap+"Listener");
			final JVar addParam = addMethod.param(JMod.FINAL, listenerClass, aspectName+"Listener");
			addMethod.body().invoke(JExpr._this().ref(supportField), "add" + aspectNameCap + "Listener").arg(addParam);
		}
		final JMethod withMethod = definedClass.method(JMod.PUBLIC, definedClass, "with"+aspectNameCap+"Listener");
		final JVar withParam = withMethod.param(JMod.FINAL, listenerClass, aspectName+"Listener");
		withMethod.body().invoke("add"+aspectNameCap+"Listener").arg(withParam);
		withMethod.body()._return(JExpr._this());

		if(classOutline.getSuperClass() != null) {
			withMethod.annotate(Override.class);
		}
	}

	private JInvocation invokeListener(JBlock block, JFieldVar field, JVar oldValueVar, JVar setterArg, final String aspectName) {
		final String aspectNameCap = aspectName.substring(0,1).toUpperCase() + aspectName.substring(1);
		final JInvocation fvcInvoke = block.invoke(JExpr._this().ref(aspectName + "Support"), "fire" + aspectNameCap);
		fvcInvoke.arg(JExpr.lit(field.name()));
		fvcInvoke.arg(oldValueVar);
		fvcInvoke.arg(setterArg);
		return fvcInvoke;
	}


}
