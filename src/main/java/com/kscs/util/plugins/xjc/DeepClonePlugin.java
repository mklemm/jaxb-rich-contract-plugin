package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by klemm0 on 18/02/14.
 */
public class DeepClonePlugin extends Plugin {
	public static final String BOOLEAN_OPTION_ERROR_MSG = " option must be either (\"true\",\"on\",\"y\",\"yes\") or (\"false\", \"off\", \"n\",\"no\").";

	private boolean throwCloneNotSupported = false;
	@Override
	public String getOptionName() {
		return "Xclone";
	}

	@Override
	public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException, IOException {
		final String arg = args[i].toLowerCase();
		if(arg.startsWith("-clone-throws=")) {
			boolean argTrue = isTrue(arg);
			boolean argFalse = isFalse(arg);
			if(!argTrue && !argFalse) {
				throw new BadCommandLineException("-clone-throws"+BOOLEAN_OPTION_ERROR_MSG);
			} else {
				this.throwCloneNotSupported = argTrue;
			}
			return 1;
		}
		return 0;
	}

	@Override
	public String getUsage() {
		return " -Xclone: Generates Cloneable JAXB classes.\n\t-clone-throws={yes|no}:\t declare CloneNotSupportedException to be thrown by 'clone()' (yes), or suppress throws clause (no). Default: no";
	}

	@Override
	public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
		final JCodeModel m = outline.getCodeModel();
		final JClass collectionClass = m.ref(Collection.class);
		final JClass cloneableInterface = m.ref(Cloneable.class);
		final JClass arrayListClass = m.ref(ArrayList.class);

		final Set<String> generatedClasses = new HashSet<String>();
		for(final ClassOutline classOutline : outline.getClasses()) {
			classOutline.implClass._implements(Cloneable.class);
			generatedClasses.add(classOutline.implClass.fullName());
		}

		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			final JMethod method = definedClass.method(JMod.PUBLIC, definedClass, "clone");
			method.annotate(Override.class);


			boolean mustCatch = cloneMethodThrows(generatedClasses, definedClass._extends());
			for(final JFieldVar field : definedClass.fields().values()) {
				if(field.type().isReference()) {
					final JClass fieldType = (JClass)field.type();
					if(collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						if(cloneableInterface.isAssignableFrom(elementType)) {
							mustCatch |= cloneMethodThrows(generatedClasses, elementType);
						}
					} else {
						if(cloneableInterface.isAssignableFrom(fieldType)) {
							mustCatch |= cloneMethodThrows(generatedClasses, fieldType);
						}
					}
				}
			}

			final JBlock outer;
			final JBlock body;
			final JTryBlock tryBlock;
			if(throwCloneNotSupported) {
				method._throws(CloneNotSupportedException.class);
			}
			if(throwCloneNotSupported || !mustCatch) {
				outer = method.body();
				tryBlock = null;
				body = outer;
			} else {
				outer = method.body();
				tryBlock = outer._try();
				body = tryBlock.body();
			}
			final JVar newObjectVar = body.decl(JMod.FINAL, definedClass, "newObject", JExpr.cast(definedClass, JExpr._super().invoke("clone")));
			for (final JFieldVar field : definedClass.fields().values()) {
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					if (collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						body.assign(newField, JExpr._new(arrayListClass.narrow(elementType)).arg(JExpr._this().ref(field).invoke("size")));
						final JForEach forLoop = body.forEach(elementType, "item", JExpr._this().ref(field));
						final JInvocation addInvoke = forLoop.body().invoke(newField, "add");
						if(cloneableInterface.isAssignableFrom(elementType)) {
							addInvoke.arg(castOnDemand(generatedClasses, elementType, forLoop.var().invoke("clone")));
						} else {
							addInvoke.arg(forLoop.var());
						}
					}
					if (cloneableInterface.isAssignableFrom(fieldType)) {
						body.assign(newField, castOnDemand(generatedClasses,fieldType, JExpr._this().ref(field).invoke("clone")));
					} else {
						// body.assign(newField, JExpr._this().ref(field));
					}
				}
			}
			body._return(newObjectVar);

			if(tryBlock != null) {
				JCatchBlock	catchBlock = tryBlock._catch(m.ref(CloneNotSupportedException.class));
				JVar exceptionVar = catchBlock.param("cnse");
				catchBlock.body()._throw(JExpr._new(m.ref(RuntimeException.class)).arg(exceptionVar));
			}
		}
		return true;
	}

	private boolean cloneMethodThrows(final Set<String> generatedClasses, final JClass fieldType) {
		try {
			return fieldType.fullName().equals(Object.class.getName()) || (!generatedClasses.contains(fieldType.fullName())
					&&
					Class.forName(fieldType.fullName()).getMethod("clone").getExceptionTypes().length > 0);
		} catch (Exception e) {
			System.err.println("WARNING: "+e);
			e.printStackTrace();
			return true;
		}
	}

	private boolean isTrue(final String arg) {
		return arg.endsWith("y") || arg.endsWith("true") || arg.endsWith("on") || arg.endsWith("yes");
	}

	private boolean isFalse(final String arg) {
		return arg.endsWith("n") || arg.endsWith("false") || arg.endsWith("off") || arg.endsWith("no");
	}

	private JExpression castOnDemand(final Set<String> generatedClasses, final JType fieldType, final JExpression expression) {
		return generatedClasses.contains(fieldType.fullName()) ? expression : JExpr.cast(fieldType, expression);
	}



}
