package com.kscs.util.plugins.xjc;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.outline.ClassOutline;

/**
 * Created by mirko on 25.03.14.
 */
public class BuilderOutline {
	private final JDefinedClass definedBuilderClass;
	private final ClassOutline classOutline;

	protected BuilderOutline(final ClassOutline classOutline) throws JClassAlreadyExistsException {
		this.classOutline = classOutline;
		final int mods = this.classOutline.implClass.isAbstract() ? JMod.PROTECTED | JMod.STATIC | JMod.ABSTRACT : JMod.PUBLIC | JMod.STATIC;
		this.definedBuilderClass = this.classOutline.implClass._class(mods, ApiConstructs.BUILDER_CLASS_NAME, ClassType.CLASS);
	}

	public JDefinedClass getDefinedBuilderClass() {
		return this.definedBuilderClass;
	}

	public ClassOutline getClassOutline() {
		return this.classOutline;
	}

}
