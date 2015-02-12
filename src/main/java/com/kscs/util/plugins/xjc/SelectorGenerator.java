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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import com.kscs.util.plugins.xjc.common.PluginUtil;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * Generates Property Tree builder structures
 */
class SelectorGenerator {

	private static final Logger LOGGER = Logger.getLogger(SelectorGenerator.class.getName());

	final Class<?> selectorBaseClass;
	private final String selectorClassName ;
	private final String rootSelectorClassName;
	final String selectorParamName ;
	private final JType selectorParamType;
	private final JClass propertyPathClass;


	private final ApiConstructs apiConstructs;
	private final Map<String, MetaInfoOutline> infoClasses = new LinkedHashMap<>();

	public SelectorGenerator(final ApiConstructs apiConstructs, final Class<?> selectorBaseClass, final String selectorClassName, final String rootSelectorClassName, final String selectorParamName, final JType selectorParamType, final JClass propertyPathClass) {
		this.apiConstructs = apiConstructs;
		this.selectorBaseClass = selectorBaseClass;
		this.selectorClassName = selectorClassName;
		this.rootSelectorClassName = rootSelectorClassName;
		this.selectorParamName = selectorParamName;
		this.selectorParamType = selectorParamType;
		this.propertyPathClass = propertyPathClass;
		for(final ClassOutline classOutline : apiConstructs.outline.getClasses()) {
			this.infoClasses.put(classOutline.implClass.fullName(), generateMetaClass(classOutline));
		}
	}

	private MetaInfoOutline generateMetaClass(final ClassOutline classOutline) {
		try {
			final JDefinedClass definedClass = classOutline.implClass;
			final JDefinedClass selectorClass = definedClass._class(JMod.PUBLIC | JMod.STATIC, this.selectorClassName);
			final JTypeVar rootTypeParam = selectorClass.generify("TRoot");
			final JTypeVar parentTypeParam = selectorClass.generify("TParent");
			rootTypeParam.bound(this.apiConstructs.codeModel.ref(this.selectorBaseClass).narrow(rootTypeParam, this.apiConstructs.codeModel.wildcard()));
			//parentTypeParam.bound(this.apiConstructs.codeModel.ref(Selector.class).narrow(parentTypeParam, this.apiConstructs.codeModel.wildcard()));

			final JMethod constructor = selectorClass.constructor(JMod.PUBLIC);
			final JVar rootParam = constructor.param(JMod.FINAL, rootTypeParam, "root");
			final JVar parentParam = constructor.param(JMod.FINAL, parentTypeParam, "parent");
			final JVar propertyNameParam = constructor.param(JMod.FINAL, this.apiConstructs.stringClass, "propertyName");
			if(this.selectorParamName != null) {
				final JVar includeParam = constructor.param(JMod.FINAL, getSelectorParamType(this.apiConstructs.codeModel.wildcard(), definedClass.wildcard()), this.selectorParamName);
				constructor.body().invoke("super").arg(rootParam).arg(parentParam).arg(propertyNameParam).arg(includeParam);
			} else {
				constructor.body().invoke("super").arg(rootParam).arg(parentParam).arg(propertyNameParam);
			}
			final JClass productMapType = this.apiConstructs.codeModel.ref(Map.class).narrow(String.class).narrow(this.propertyPathClass);
			final JMethod buildChildrenMethod = selectorClass.method(JMod.PUBLIC, productMapType, "buildChildren");
			buildChildrenMethod.annotate(Override.class);
			final JVar productMapVar = buildChildrenMethod.body().decl(JMod.FINAL, productMapType, "products", JExpr._new(this.apiConstructs.codeModel.ref(HashMap.class).narrow(String.class).narrow(this.propertyPathClass)));


			if(classOutline.getSuperClass() == null ) {
				selectorClass._extends(this.apiConstructs.codeModel.ref(this.selectorBaseClass).narrow(rootTypeParam).narrow(parentTypeParam));
			}

			final JDefinedClass rootSelectorClass = definedClass._class(JMod.PUBLIC | JMod.STATIC, this.rootSelectorClassName);
			rootSelectorClass._extends(selectorClass.narrow(rootSelectorClass).narrow(Void.class));
			final JMethod rootSelectorConstructor = rootSelectorClass.constructor(JMod.NONE);
			if(this.selectorParamName != null) {
				final JVar rootSelectorClassIncludeParam = rootSelectorConstructor.param(JMod.FINAL, getSelectorParamType(this.apiConstructs.voidClass, definedClass), this.selectorParamName);
				rootSelectorConstructor.body().invoke("super").arg(JExpr._null()).arg(JExpr._null()).arg(JExpr._null()).arg(rootSelectorClassIncludeParam);
			} else {
				rootSelectorConstructor.body().invoke("super").arg(JExpr._null()).arg(JExpr._null()).arg(JExpr._null());
			}

			final JMethod rootMethod = rootSelectorClass.method(JMod.STATIC | JMod.PUBLIC, rootSelectorClass, "_root");
			if(this.selectorParamName != null) {
				final JVar rootIncludeParam = rootMethod.param(JMod.FINAL, getSelectorParamType(this.apiConstructs.voidClass, definedClass), this.selectorParamName);
				rootMethod.body()._return(JExpr._new(rootSelectorClass).arg(rootIncludeParam));
			} else {
				rootMethod.body()._return(JExpr._new(rootSelectorClass));
			}


			return new MetaInfoOutline(this, classOutline, selectorClass, rootTypeParam, parentTypeParam, buildChildrenMethod, productMapVar);
		} catch (final JClassAlreadyExistsException e) {
			SelectorGenerator.LOGGER.warning("Attempt to generate already existing class");
			return null;
		}
	}

	public void generateMetaFields() {
		for(final MetaInfoOutline entry : this.infoClasses.values()) {
			entry.generateMetaFields();
		}
	}

	ApiConstructs getApiConstructs() {
		return this.apiConstructs;
	}

	MetaInfoOutline getInfoClass(final JType mainType) {
		return this.infoClasses.get(mainType.fullName());
	}

	JType getSelectorParamType(final JClass instanceType, final JType propertyType) {
		if(this.selectorParamType.isReference()) {
			return ((JClass)this.selectorParamType).narrow(instanceType).narrow(propertyType);
		} else {
			return this.selectorParamType;
		}
	}
}

class MetaInfoOutline {
	private final SelectorGenerator selectorGenerator;
	private final ClassOutline classOutline;
	private final JDefinedClass selectorClass;
	private final JTypeVar parentTypeParam;
	private final JTypeVar rootTypeParam;
	private final JMethod buildChildrenMethod;
	private final JVar productMapVar;

	MetaInfoOutline(final SelectorGenerator selectorGenerator, final ClassOutline classOutline, final JDefinedClass selectorClass, final JTypeVar rootTypeParam, final JTypeVar parentTypeParam, final JMethod buildChildrenMethod, final JVar productMapVar) {
		this.selectorGenerator = selectorGenerator;
		this.classOutline = classOutline;
		this.selectorClass = selectorClass;
		this.rootTypeParam = rootTypeParam;
		this.parentTypeParam = parentTypeParam;
		this.buildChildrenMethod = buildChildrenMethod;
		this.buildChildrenMethod.body().add(productMapVar.invoke("putAll").arg(JExpr._super().invoke(buildChildrenMethod)));
		this.productMapVar = productMapVar;
	}

	public void generateMetaFields() {
		if(this.classOutline.getSuperClass() != null) {
			this.selectorClass._extends(this.selectorGenerator.getInfoClass(this.classOutline.getSuperClass().implClass).selectorClass.narrow(this.rootTypeParam).narrow(this.parentTypeParam));
		}
		for (final FieldOutline fieldOutline : this.classOutline.getDeclaredFields()) {
			final JFieldVar definedField = PluginUtil.getDeclaredField(fieldOutline);
			if (definedField != null) {
				final JType elementType = PluginUtil.getElementType(fieldOutline);
				if (elementType.isReference()) {
					final ClassOutline modelClass = this.selectorGenerator.getApiConstructs().getClassOutline(elementType);
					final JClass returnType;
					if (modelClass != null) {
						returnType = this.selectorGenerator.getInfoClass(modelClass.implClass).selectorClass.narrow(this.rootTypeParam).narrow(this.selectorClass.narrow(this.rootTypeParam).narrow(this.parentTypeParam));
					} else {
						returnType = this.selectorGenerator.getApiConstructs().codeModel.ref(this.selectorGenerator.selectorBaseClass).narrow(this.rootTypeParam).narrow(this.selectorClass.narrow(this.rootTypeParam).narrow(this.parentTypeParam));
					}
					final JFieldVar includeField = this.selectorClass.field(JMod.PRIVATE, returnType, definedField.name(), JExpr._null());
					final JFieldRef fieldRef = JExpr._this().ref(includeField);
					final JMethod includeMethod = this.selectorClass.method(JMod.PUBLIC, returnType, definedField.name());
					if(this.selectorGenerator.selectorParamName != null) {
						final JVar includeParam = includeMethod.param(JMod.FINAL, this.selectorGenerator.getSelectorParamType(this.classOutline.implClass, elementType), this.selectorGenerator.selectorParamName);
						includeMethod.body()._return(JOp.cond(fieldRef.eq(JExpr._null()), fieldRef.assign(JExpr._new(returnType).arg(JExpr._this().ref("_root")).arg(JExpr._this()).arg(JExpr.lit(definedField.name())).arg(includeParam)), fieldRef));
					} else {
						includeMethod.body()._return(JOp.cond(fieldRef.eq(JExpr._null()), fieldRef.assign(JExpr._new(returnType).arg(JExpr._this().ref("_root")).arg(JExpr._this()).arg(JExpr.lit(definedField.name()))), fieldRef));
					}

					this.buildChildrenMethod.body()._if(fieldRef.ne(JExpr._null()))._then().add(this.productMapVar.invoke("put").arg(JExpr.lit(definedField.name())).arg(fieldRef.invoke("init")));
				}
			}
		}
		this.buildChildrenMethod.body()._return(this.productMapVar);
	}
}
