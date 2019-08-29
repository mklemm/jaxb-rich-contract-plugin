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

import com.kscs.util.jaxb.Buildable;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.plugins.xjc.codemodel.ClassName;
import com.kscs.util.plugins.xjc.codemodel.GenerifiedClass;
import com.kscs.util.plugins.xjc.outline.DefinedClassOutline;
import com.kscs.util.plugins.xjc.outline.DefinedInterfaceOutline;
import com.kscs.util.plugins.xjc.outline.DefinedPropertyOutline;
import com.kscs.util.plugins.xjc.outline.DefinedTypeOutline;
import com.kscs.util.plugins.xjc.outline.PropertyOutline;
import com.kscs.util.plugins.xjc.outline.ReferencedClassOutline;
import com.kscs.util.plugins.xjc.outline.TypeOutline;
import com.sun.codemodel.JAssignmentTarget;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CCustomizable;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.CNonElement;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.model.nav.NType;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.bind.v2.model.core.TypeInfo;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.impl.ElementDecl;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.namespace.QName;
import java.beans.Introspector;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.kscs.util.plugins.xjc.base.PluginUtil.nullSafe;

/**
 * Helper class to generate fluent builder classes in two steps
 */
class BuilderGenerator {
	public static final String PRODUCT_VAR_NAME = "_product";
	public static final String PARENT_BUILDER_TYPE_PARAMETER_NAME = "_B";
	public static final String PRODUCT_TYPE_PARAMETER_NAME = "_P";
	public static final String OTHER_PARAM_NAME = "_other";
	public static final String OTHER_VAR_NAME = "_my";
	public static final String PARENT_BUILDER_PARAM_NAME = "_parentBuilder";
	public static final String STORED_VALUE_PARAM_NAME = "_storedValue";
	public static final String NEW_BUILDER_VAR_NAME = "_newBuilder";
	public static final String COPY_FLAG_PARAM_NAME = "_copy";
	private static final String ITEM_VAR_NAME = "_item";
	private final PluginContext pluginContext;
	private final JDefinedClass definedClass;
	private final GenerifiedClass builderClass;
	private final DefinedTypeOutline typeOutline;
	private final Map<String, BuilderOutline> builderOutlines;
	private final JFieldVar parentBuilderField;
	private final JAssignmentTarget storedValueField;
	private final boolean implement;
	private final BuilderGeneratorSettings settings;
	private final ResourceBundle resources;

	BuilderGenerator(final PluginContext pluginContext, final Map<String, BuilderOutline> builderOutlines, final BuilderOutline builderOutline, final BuilderGeneratorSettings settings) {
		this.pluginContext = pluginContext;
		this.settings = settings;
		this.builderOutlines = builderOutlines;
		this.typeOutline = (DefinedTypeOutline)builderOutline.getClassOutline();
		this.definedClass = this.typeOutline.getImplClass();
		this.builderClass = new GenerifiedClass(builderOutline.getDefinedBuilderClass(), BuilderGenerator.PARENT_BUILDER_TYPE_PARAMETER_NAME);
		this.resources = ResourceBundle.getBundle(BuilderGenerator.class.getName());
		this.implement = !this.builderClass.raw.isInterface();
		if (builderOutline.getClassOutline().getSuperClass() == null) {
			final JMethod endMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.typeParam, this.settings.getEndMethodName());
			if (this.implement) {
				this.parentBuilderField = this.builderClass.raw.field(JMod.PROTECTED | JMod.FINAL, this.builderClass.typeParam, BuilderGenerator.PARENT_BUILDER_PARAM_NAME);
				endMethod.body()._return(JExpr._this().ref(this.parentBuilderField));
				this.storedValueField = this.settings.isCopyAlways() ? null : this.builderClass.raw.field(JMod.PROTECTED | JMod.FINAL, this.definedClass, BuilderGenerator.STORED_VALUE_PARAM_NAME);
			} else {
				this.parentBuilderField = null;
				this.storedValueField = null;
			}
		} else {
			this.parentBuilderField = null;
			this.storedValueField = this.implement ? JExpr.ref(BuilderGenerator.STORED_VALUE_PARAM_NAME) : null;
		}
		if (this.implement) {
			generateCopyConstructor(false);
			if (this.settings.isGeneratingPartialCopy()) {
				generateCopyConstructor(true);
			}
		}
	}

	void generateBuilderMember(final PropertyOutline propertyOutline, final JBlock initBody, final JVar productParam) {
		final JType fieldType = propertyOutline.getRawType();
		if (propertyOutline.isCollection()) {
			if (propertyOutline.getRawType().isArray()) {
				generateArrayProperty(initBody, productParam, propertyOutline, fieldType.elementType(), this.builderClass.type);
			} else {
				final List<JClass> typeParameters = ((JClass)fieldType).getTypeParameters();
				final JClass elementType = typeParameters.get(0);
				generateCollectionProperty(initBody, productParam, propertyOutline, elementType);
				if (propertyOutline.getChoiceProperties().size() > 1) {
					generateCollectionChoiceProperty(propertyOutline);
				}
			}
		} else {
			if (propertyOutline.getChoiceProperties().size() > 1) {
				//throw new UnsupportedOperationException("Singular Properties with multiple references not currently supported.");
				generateSingularChoiceProperty(initBody, productParam, propertyOutline);
			} else {
				generateSingularProperty(initBody, productParam, propertyOutline);
			}
		}
	}
	private JType getTagRefType(final PropertyOutline.TagRef tagRef, final Outline outline, final Aspect aspect) {
		final TypeInfo<NType, NClass> typeInfo = tagRef.getTypeInfo();
		final JType type;
		if (typeInfo instanceof CClassInfo) {
			type = ((CClassInfo) typeInfo).toType(outline, aspect);
		} else if (typeInfo instanceof CElementInfo) {
			final List<CNonElement> refs = ((CElementInfo) typeInfo).getProperty().ref();
			// This feels dirty but am not sure what we do if we get multiple refs
			if (refs.size() == 1) {
				try {
					type = ((CClassInfo) refs.get(0)).toType(outline, aspect);
				} catch (Exception e) {
					throw new RuntimeException(String.format("Unexpected type %s for tagRef %s",
							refs.get(0).getClass().getCanonicalName(),
							tagRef.getTagName()));
				}
			} else {
				throw new RuntimeException(String.format("Expecting one ref type for tagRef %s, found %s",
						tagRef.getTagName(),
						refs.size()));
			}
		} else {
			throw new RuntimeException(String.format("Unexpected type %s for tagRef %s",
					typeInfo.getClass().getCanonicalName(),
					tagRef.getTagName()));
		}
		return type;
	}

	private void generateSingularChoiceProperty(final JBlock initBody, final JVar productParam, final PropertyOutline propertyOutline) {
		// First create the builder field, init and withXXX methods for the supertype of the choices
	    JFieldVar superTypeBuilderField = generateSingularChoiceSuperTypeProperty(initBody, productParam, propertyOutline)
				.orElseThrow(() -> new RuntimeException(String.format(
				        "Expecting to have a builderField for property %s", propertyOutline.getFieldName())));

	    // Now create the withXXX methods for each choice type
		for (final PropertyOutline.TagRef typeInfo : propertyOutline.getChoiceProperties()) {
			final QName elementName = typeInfo.getTagName();
			final JType elementType = getTagRefType(typeInfo, this.pluginContext.outline, Aspect.EXPOSED);
			final String fieldName = this.pluginContext.toVariableName(elementName.getLocalPart());
			final String propertyName = this.pluginContext.toPropertyName(elementName.getLocalPart());
			final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);
			final BuilderOutline choiceChildBuilderOutline = getBuilderDeclaration(propertyOutline.getElementType());
			if (childBuilderOutline == null) {
			    // TODO not sure when we will come in here so throw ex to highlight in testing
				throw new RuntimeException(String.format(
						"Don't think we should ever come in here, fieldName: %s", propertyOutline.getFieldName()));
			} else {
				final JClass builderFieldElementType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
				final JClass builderWithMethodReturnType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());
				final JMethod withValueMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
				final JVar param = withValueMethod.param(JMod.FINAL, elementType, fieldName);
				generateWithMethodJavadoc(withValueMethod, param, propertyOutline.getSchemaAnnotationText().orElse(null));
				final JMethod withBuilderMethod = this.builderClass.raw.method(JMod.PUBLIC, builderWithMethodReturnType, PluginContext.WITH_METHOD_PREFIX + propertyName);
				generateBuilderMethodJavadoc(withBuilderMethod, "with", fieldName, propertyOutline.getSchemaAnnotationText().orElse(null));
				if (this.implement) {

					// Generate the withXXX method that takes a value and returns the parent builder
					withValueMethod.body().assign(JExpr._this().ref(superTypeBuilderField), nullSafe(param, JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(param).arg(JExpr.FALSE)));
					withValueMethod.body()._return(JExpr._this());

					// Generate the withXXX method that takes no args and returns a new child builder for that type
					JVar childBuilder = withBuilderMethod.body().decl(
					        JMod.FINAL,
							builderWithMethodReturnType,
							fieldName + this.settings.getBuilderFieldSuffix(),
							JExpr._new(builderFieldElementType)
									.arg(JExpr._this())
									.arg(JExpr._null())
									.arg(JExpr.FALSE));

					withBuilderMethod.body().assign(
							JExpr._this().ref(superTypeBuilderField),
							childBuilder);
					withBuilderMethod.body()._return(childBuilder);
				}
			}
		}
	}

	private void generateCollectionChoiceProperty(final PropertyOutline propertyOutline) {
		for (final PropertyOutline.TagRef tagRef : propertyOutline.getChoiceProperties()) {
			final TypeInfo<NType,NClass> typeInfo = tagRef.getTypeInfo();
			final QName elementName = tagRef.getTagName();
			final JType elementType = typeInfo.getType().toType(this.pluginContext.outline, Aspect.EXPOSED);
			generateAddMethods(propertyOutline, elementName, elementType);
		}
	}

	private void generateAddMethods(final PropertyOutline propertyOutline,
	                                final QName elementName, final JType jType) {
		final JClass elementType = jType.boxify();
		final JClass iterableType = this.pluginContext.iterableClass.narrow(elementType.wildcard());
		final String fieldName = this.pluginContext.toVariableName(elementName.getLocalPart());
		final String propertyName = this.pluginContext.toPropertyName(elementName.getLocalPart());
		final JMethod addIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		final JVar addIterableParam = addIterableMethod.param(JMod.FINAL, iterableType, fieldName + "_");
		generateAddMethodJavadoc(addIterableMethod, addIterableParam, propertyOutline.getSchemaAnnotationText().orElse(null));
		final JMethod addVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		final JVar addVarargsParam = addVarargsMethod.varParam(elementType, fieldName + "_");
		generateAddMethodJavadoc(addVarargsMethod, addVarargsParam, propertyOutline.getSchemaAnnotationText().orElse(null));
		final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);
		final JMethod addMethod;
		if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().getImplClass().isAbstract()) {
			final JClass builderWithMethodReturnType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());
			addMethod = this.builderClass.raw.method(JMod.PUBLIC, builderWithMethodReturnType, PluginContext.ADD_METHOD_PREFIX + propertyName);
			generateBuilderMethodJavadoc(addMethod, "add", fieldName, propertyOutline.getSchemaAnnotationText().orElse(null));
		} else {
			addMethod = null;
		}
		if (this.implement) {
			final BuilderOutline choiceChildBuilderOutline = getBuilderDeclaration(propertyOutline.getElementType());
			final JClass childBuilderType = childBuilderOutline == null ? this.pluginContext.buildableInterface : childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
			final JClass builderFieldElementType = choiceChildBuilderOutline == null ? this.pluginContext.buildableInterface : choiceChildBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
			final JClass builderArrayListClass = this.pluginContext.arrayListClass.narrow(builderFieldElementType);
			final JFieldVar builderField = this.builderClass.raw.fields().get(propertyOutline.getFieldName());
			addVarargsMethod.body()._return(JExpr.invoke(addIterableMethod).arg(this.pluginContext.asList(addVarargsParam)));
			if (addMethod == null) {
				addIterableMethod.body()._return(JExpr.invoke(PluginContext.ADD_METHOD_PREFIX + propertyOutline.getBaseName()).arg(addIterableParam));
			} else {
				final JConditional addIterableIfParamNull = addIterableMethod.body()._if(addIterableParam.ne(JExpr._null()));
				final JConditional addIterableIfNull = addIterableIfParamNull._then()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIterableIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
				final JForEach addIterableForEach = addIterableIfParamNull._then().forEach(elementType, BuilderGenerator.ITEM_VAR_NAME, addIterableParam);
				final JExpression builderCreationExpression = JExpr._new(childBuilderType).arg(JExpr._this()).arg(addIterableForEach.var()).arg(this.settings.isCopyAlways() ? JExpr.TRUE : JExpr.FALSE);
				addIterableForEach.body().add(JExpr._this().ref(builderField).invoke("add").arg(builderCreationExpression));
				addIterableMethod.body()._return(JExpr._this());

				final JConditional addIfNull = addMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
				final JVar childBuilderVar = addMethod.body().decl(JMod.FINAL, childBuilderType, fieldName + this.settings.getBuilderFieldSuffix(), JExpr._new(childBuilderType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE));
				addMethod.body().add(JExpr._this().ref(builderField).invoke("add").arg(childBuilderVar));
				addMethod.body()._return(childBuilderVar);
			}
		}
	}


	private void overrideAddMethods(final PropertyOutline propertyOutline,
	                                final QName elementName, final JType elementType) {
		final JClass iterableType = this.pluginContext.iterableClass.narrow(elementType instanceof JClass ? ((JClass)elementType).wildcard() : elementType);
		final String fieldName = this.pluginContext.toVariableName(elementName.getLocalPart());
		final String propertyName = this.pluginContext.toPropertyName(elementName.getLocalPart());
		final JMethod addIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		addIterableMethod.annotate(Override.class);
		final JVar addIterableParam = addIterableMethod.param(JMod.FINAL, iterableType, fieldName + "_");
		generateAddMethodJavadoc(addIterableMethod, addIterableParam, propertyOutline.getSchemaAnnotationText().orElse(null));
		final JMethod addVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		addVarargsMethod.annotate(Override.class);
		final JVar addVarargsParam = addVarargsMethod.varParam(elementType, fieldName + "_");
		generateAddMethodJavadoc(addVarargsMethod, addVarargsParam, propertyOutline.getSchemaAnnotationText().orElse(null));
		final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);
		final JMethod addMethod;
		if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().getImplClass().isAbstract()) {
			addMethod = this.builderClass.raw.method(JMod.PUBLIC, childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard()), PluginContext.ADD_METHOD_PREFIX + propertyName);
			addMethod.annotate(Override.class);
			generateBuilderMethodJavadoc(addMethod, "add", fieldName, propertyOutline.getSchemaAnnotationText().orElse(null));
		} else {
			addMethod = null;
		}
		if (this.implement) {
			addVarargsMethod.body().add(JExpr._super().invoke(addVarargsMethod).arg(addVarargsParam));
			addVarargsMethod.body()._return(JExpr._this());
			addIterableMethod.body().add(JExpr._super().invoke(addIterableMethod).arg(addIterableParam));
			addIterableMethod.body()._return(JExpr._this());
			if (addMethod != null) {
				addMethod.body()._return(JExpr.cast(childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard()), JExpr._super().invoke(addMethod)));
			}
		}
	}

	private void generateCollectionProperty(final JBlock initBody, final JVar productParam, final PropertyOutline propertyOutline, final JClass elementType) {
		final String fieldName = propertyOutline.getFieldName();
		final String propertyName = propertyOutline.getBaseName();
		final JClass iterableType = this.pluginContext.iterableClass.narrow(elementType.wildcard());
		final JMethod addIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		final JVar addIterableParam = addIterableMethod.param(JMod.FINAL, iterableType, fieldName);
		generateAddMethodJavadoc(addIterableMethod, addIterableParam, propertyOutline.getSchemaAnnotationText().orElse(null));
		final JMethod withIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
		final JVar withIterableParam = withIterableMethod.param(JMod.FINAL, iterableType, fieldName);
		generateWithMethodJavadoc(withIterableMethod, withIterableParam, propertyOutline.getSchemaAnnotationText().orElse(null));
		final JMethod addVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		final JVar addVarargsParam = addVarargsMethod.varParam(elementType, fieldName);
		generateAddMethodJavadoc(addVarargsMethod, addVarargsParam, propertyOutline.getSchemaAnnotationText().orElse(null));
		final JMethod withVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
		final JVar withVarargsParam = withVarargsMethod.varParam(elementType, fieldName);
		generateWithMethodJavadoc(withVarargsMethod, withVarargsParam, propertyOutline.getSchemaAnnotationText().orElse(null));
		final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);
		final JMethod addMethod;
		if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().getImplClass().isAbstract()) {
			final JClass builderWithMethodReturnType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());
			addMethod = this.builderClass.raw.method(JMod.PUBLIC, builderWithMethodReturnType, PluginContext.ADD_METHOD_PREFIX + propertyName);
			generateBuilderMethodJavadoc(addMethod, "add", propertyName, propertyOutline.getSchemaAnnotationText().orElse(null));
		} else {
			addMethod = null;
		}
		if (this.implement) {
			final JClass childBuilderType = childBuilderOutline == null ? this.pluginContext.buildableInterface : childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
			final JClass builderArrayListClass = this.pluginContext.arrayListClass.narrow(childBuilderType);
			final JClass builderListClass = this.pluginContext.listClass.narrow(childBuilderType);
			final JFieldVar builderField = this.builderClass.raw.field(JMod.PRIVATE, builderListClass, fieldName);
			addVarargsMethod.body().invoke(addIterableMethod).arg(this.pluginContext.asList(addVarargsParam));
			addVarargsMethod.body()._return(JExpr._this());
			withVarargsMethod.body().invoke(withIterableMethod).arg(this.pluginContext.asList(withVarargsParam));
			withVarargsMethod.body()._return(JExpr._this());
			final JConditional addIterableIfParamNull = addIterableMethod.body()._if(addIterableParam.ne(JExpr._null()));
			final JConditional addIterableIfNull = addIterableIfParamNull._then()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
			addIterableIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
			final JForEach jForEach = addIterableIfParamNull._then().forEach(elementType, BuilderGenerator.ITEM_VAR_NAME, addIterableParam);
			final JExpression builderCreationExpression = childBuilderOutline == null
					? JExpr._new(this.pluginContext.buildableClass).arg(jForEach.var())
					: JExpr._new(childBuilderType).arg(JExpr._this()).arg(jForEach.var()).arg(this.settings.isCopyAlways() ? JExpr.TRUE : JExpr.FALSE);
			jForEach.body().add(JExpr._this().ref(builderField).invoke("add").arg(builderCreationExpression));
			addIterableMethod.body()._return(JExpr._this());
			final JConditional withIterableIfNull = withIterableMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
			withIterableIfNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
			withIterableMethod.body()._return(JExpr.invoke(addIterableMethod).arg(withIterableParam));
			final JConditional ifNull = initBody._if(JExpr._this().ref(builderField).ne(JExpr._null()));
			final JVar collectionVar = ifNull._then().decl(JMod.FINAL, this.pluginContext.listClass.narrow(elementType), fieldName, JExpr._new(this.pluginContext.arrayListClass.narrow(elementType)).arg(JExpr._this().ref(builderField).invoke("size")));
			final JForEach initForEach = ifNull._then().forEach(childBuilderType, BuilderGenerator.ITEM_VAR_NAME, JExpr._this().ref(builderField));
			final JInvocation buildMethodInvocation = initForEach.var().invoke(this.settings.getBuildMethodName());
			final JExpression buildExpression = childBuilderOutline == null ? JExpr.cast(elementType, buildMethodInvocation) : buildMethodInvocation;
			initForEach.body().add(collectionVar.invoke("add").arg(buildExpression));
			ifNull._then().assign(productParam.ref(fieldName), collectionVar);
			if (addMethod != null) {
				final JConditional addIfNull = addMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
				final JVar childBuilderVar = addMethod.body().decl(JMod.FINAL, childBuilderType, fieldName + this.settings.getBuilderFieldSuffix(), JExpr._new(childBuilderType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE));
				addMethod.body().add(JExpr._this().ref(builderField).invoke("add").arg(childBuilderVar));
				addMethod.body()._return(childBuilderVar);
			}
			this.pluginContext.generateImmutableFieldInit(initBody, productParam, propertyOutline);
		}
	}

	private void generateSingularProperty(final JBlock initBody, final JVar productParam, final PropertyOutline propertyOutline) {
		final String propertyName = propertyOutline.getBaseName();
		final String fieldName = propertyOutline.getFieldName();
		final JType fieldType = propertyOutline.getRawType();
		final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
		if (childBuilderOutline == null) {
			final JMethod withMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withMethod.param(JMod.FINAL, fieldType, fieldName);
			generateWithMethodJavadoc(withMethod, param, propertyOutline.getSchemaAnnotationText().orElse(null));
			final JFieldVar builderField;
			if (this.implement) {
				builderField = this.builderClass.raw.field(JMod.PRIVATE, fieldType, fieldName);
				withMethod.body().assign(JExpr._this().ref(builderField), param);
				withMethod.body()._return(JExpr._this());
				initBody.assign(productParam.ref(fieldName), JExpr._this().ref(builderField));
			}
		} else {
			final JClass elementType = (JClass)fieldType;
			final JClass builderFieldElementType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
			final JClass builderWithMethodReturnType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());
			final JMethod withValueMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withValueMethod.param(JMod.FINAL, elementType, fieldName);
			generateWithMethodJavadoc(withValueMethod, param, propertyOutline.getSchemaAnnotationText().orElse(null));
			final JMethod withBuilderMethod = childBuilderOutline.getClassOutline().getImplClass().isAbstract() ? null : this.builderClass.raw.method(JMod.PUBLIC, builderWithMethodReturnType, PluginContext.WITH_METHOD_PREFIX + propertyName);
			if (withBuilderMethod != null) {
				generateBuilderMethodJavadoc(withBuilderMethod, "with", fieldName, propertyOutline.getSchemaAnnotationText().orElse(null));
			}
			if (this.implement) {
				final JFieldVar builderField = this.builderClass.raw.field(JMod.PRIVATE, builderFieldElementType, fieldName);
				withValueMethod.body().assign(JExpr._this().ref(builderField), nullSafe(param, JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(param).arg(this.settings.isCopyAlways() ? JExpr.TRUE : JExpr.FALSE)));
				withValueMethod.body()._return(JExpr._this());
				if (withBuilderMethod != null) {
					withBuilderMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()))._then()._return(JExpr._this().ref(builderField));
					withBuilderMethod.body()._return(JExpr._this().ref(builderField).assign(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE)));
				}
				initBody.assign(productParam.ref(fieldName), nullSafe(JExpr._this().ref(builderField), JExpr._this().ref(builderField).invoke(this.settings.getBuildMethodName())));
			}
		}
	}

	private Optional<JFieldVar> generateSingularChoiceSuperTypeProperty(final JBlock initBody, final JVar productParam, final PropertyOutline propertyOutline) {
		final String propertyName = propertyOutline.getBaseName();
		final String fieldName = propertyOutline.getFieldName();
		final JType fieldType = propertyOutline.getRawType();
		final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
		JFieldVar builderField = null;
		if (childBuilderOutline == null) {
			final JMethod withMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withMethod.param(JMod.FINAL, fieldType, fieldName);
			generateWithMethodJavadoc(withMethod, param, propertyOutline.getSchemaAnnotationText().orElse(null));
			if (this.implement) {

				// TODO what do we do about a choice between multiple things of the same type, e.g. a choice
				// of xs:string or a choice where all options are the same complex type

				// We have a singular prop that represents a choice so it needs to be of type Buildable
				builderField = this.builderClass.raw.field(JMod.PRIVATE, this.pluginContext.buildableInterface, fieldName);
				// Produce a withXXX method for the supertype of the choices (or Object if it doesn't have one)
				// The method will create a primitive builder, seeded with the passed choice item.
				withMethod.body().assign(
						JExpr._this().ref(builderField),
						nullSafe(param, JExpr._new(this.pluginContext.buildableClass).arg(param)));
				withMethod.body()._return(JExpr._this());

				// In init(), call the build() method of the builder of the chosen choice
				initBody.assign(
						productParam.ref(fieldName),
						nullSafe(
								JExpr._this().ref(builderField),
								this.pluginContext.castOnDemand(
										fieldType,
										JExpr._this().ref(builderField).invoke(this.settings.getBuildMethodName()))));
			}
		} else {
			// TODO not sure if we will get here for a singular choice prop as it should never have a child builder
			// as it will either be Object or some interface.  Throw an exception for now to highlight it in testing
			throw new RuntimeException(String.format(
					"Don't think we should ever come in here, fieldName: %s", propertyOutline.getFieldName()));
		}
		return Optional.of(builderField);
	}

	void generateBuilderMemberOverride(final PropertyOutline superPropertyOutline, final PropertyOutline propertyOutline, final String superPropertyName) throws SAXException {
		final JType fieldType = propertyOutline.getRawType();
		final String fieldName = propertyOutline.getFieldName();
		if (superPropertyOutline.isCollection()) {
			if (!fieldType.isArray()) {
				if (superPropertyOutline.getChoiceProperties().size() > 1) {
					for (final PropertyOutline.TagRef tagRef : propertyOutline.getChoiceProperties()) {
						final QName elementName = tagRef.getTagName();
						final JType elementType;
						try {
							elementType = getTagRefType(tagRef, this.pluginContext.outline, Aspect.EXPOSED);
							overrideAddMethods(superPropertyOutline, elementName, elementType);
						} catch (Exception e) {
							pluginContext.errorHandler.warning(
							        new SAXParseException("Encountered unsupported child type \""+tagRef.getTypeInfo()+"\" in choice children collection. Unable to generate choice expansion.",
											this.pluginContext.outline.getModel().getLocator(),
											e));
						}
					}
				}
				final JClass elementType = ((JClass)fieldType).getTypeParameters().get(0);
				final JClass iterableType = this.pluginContext.iterableClass.narrow(elementType.wildcard());
				final JClass collectionType = this.pluginContext.collectionClass.narrow(elementType.wildcard());
				final JMethod addIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + superPropertyName);
				final JVar addIterableParam = addIterableMethod.param(JMod.FINAL, iterableType, fieldName);
				generateAddMethodJavadoc(addIterableMethod, addIterableParam, propertyOutline.getSchemaAnnotationText().orElse(null));
				final JMethod addVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + superPropertyName);
				final JVar addVarargsParam = addVarargsMethod.varParam(elementType, fieldName);
				generateAddMethodJavadoc(addVarargsMethod, addVarargsParam, propertyOutline.getSchemaAnnotationText().orElse(null));
				final JMethod withIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withIterableParam = withIterableMethod.param(JMod.FINAL, iterableType, fieldName);
				generateWithMethodJavadoc(withIterableMethod, withIterableParam, propertyOutline.getSchemaAnnotationText().orElse(null));
				final JMethod withVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withVarargsParam = withVarargsMethod.varParam(elementType, fieldName);
				generateWithMethodJavadoc(withVarargsMethod, withVarargsParam, propertyOutline.getSchemaAnnotationText().orElse(null));
				if (this.implement) {
					addIterableMethod.annotate(Override.class);
					addIterableMethod.body().invoke(JExpr._super(), PluginContext.ADD_METHOD_PREFIX + superPropertyName).arg(addIterableParam);
					addIterableMethod.body()._return(JExpr._this());
					addVarargsMethod.annotate(Override.class);
					addVarargsMethod.body().invoke(JExpr._super(), PluginContext.ADD_METHOD_PREFIX + superPropertyName).arg(addVarargsParam);
					addVarargsMethod.body()._return(JExpr._this());
					withIterableMethod.annotate(Override.class);
					withIterableMethod.body().invoke(JExpr._super(), PluginContext.WITH_METHOD_PREFIX + superPropertyName).arg(withIterableParam);
					withIterableMethod.body()._return(JExpr._this());
					withVarargsMethod.annotate(Override.class);
					withVarargsMethod.body().invoke(JExpr._super(), PluginContext.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
					withVarargsMethod.body()._return(JExpr._this());
				}
				final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);
				if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().getImplClass().isAbstract()) {
					final JClass builderFieldElementType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());
					final JMethod addMethod = this.builderClass.raw.method(JMod.PUBLIC, builderFieldElementType, PluginContext.ADD_METHOD_PREFIX + superPropertyName);
					generateBuilderMethodJavadoc(addMethod, "add", superPropertyOutline.getFieldName(), propertyOutline.getSchemaAnnotationText().orElse(null));
					if (this.implement) {
						addMethod.annotate(Override.class);
						addMethod.body()._return(JExpr.cast(builderFieldElementType, JExpr._super().invoke(addMethod)));
					}
				}
			} else {
				final JType elementType = fieldType.elementType();
				final JClass iterableType = this.pluginContext.iterableClass.narrow(elementType);
				final JMethod withVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withVarargsParam = withVarargsMethod.varParam(((JClass)fieldType).getTypeParameters().get(0), fieldName);
				generateWithMethodJavadoc(withVarargsMethod, withVarargsParam, propertyOutline.getSchemaAnnotationText().orElse(null));
				if (this.implement) {
					withVarargsMethod.annotate(Override.class);
					withVarargsMethod.body().invoke(JExpr._super(), PluginContext.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
					withVarargsMethod.body()._return(JExpr._this());
				}
			}
		} else {
			final JMethod withMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
			final JVar param = withMethod.param(JMod.FINAL, superPropertyOutline.getRawType(), fieldName);
			generateWithMethodJavadoc(withMethod, param, propertyOutline.getSchemaAnnotationText().orElse(null));
			if (this.implement) {
				withMethod.annotate(Override.class);
				withMethod.body().invoke(JExpr._super(), PluginContext.WITH_METHOD_PREFIX + superPropertyName).arg(param);
				withMethod.body()._return(JExpr._this());
			}
			final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
			if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().getImplClass().isAbstract()) {
				final JClass builderFieldElementType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());
				final JMethod addMethod = this.builderClass.raw.method(JMod.PUBLIC, builderFieldElementType, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				generateBuilderMethodJavadoc(addMethod, "with", superPropertyOutline.getFieldName(), propertyOutline.getSchemaAnnotationText().orElse(null));
				if (this.implement) {
					addMethod.body()._return(JExpr.cast(builderFieldElementType, JExpr._super().invoke(addMethod)));
				}
			}
		}
	}

	JDefinedClass generateExtendsClause(final BuilderOutline superClassBuilder) {
		return this.builderClass.raw._extends(superClassBuilder.getBuilderClass().narrow(this.builderClass.typeParam));
	}

	void generateImplementsClause() throws SAXException {
		if (this.typeOutline.isLocal()) {
			final GroupInterfacePlugin groupInterfacePlugin = this.pluginContext.findPlugin(GroupInterfacePlugin.class);
			if (groupInterfacePlugin != null) {
				for (final TypeOutline interfaceOutline : groupInterfacePlugin.getGroupInterfacesForClass(this.pluginContext, this.typeOutline.getImplClass().fullName())) {
					final JClass parentClass = interfaceOutline.getImplClass();
					this.builderClass.raw._implements(getBuilderInterface(parentClass).narrow(this.builderClass.typeParam));
				}
			}
			this.builderClass.raw._implements(Buildable.class);
		}
	}

	private JClass getBuilderInterface(final JClass parentClass) {
		return this.pluginContext.ref(parentClass, PluginContext.BUILDER_INTERFACE_NAME, true, false, this.pluginContext.codeModel.ref(Object.class));
	}

	JMethod generateBuildMethod(final JMethod initMethod) {
		final JMethod buildMethod = this.builderClass.raw.method(JMod.PUBLIC, this.definedClass, this.settings.getBuildMethodName());
		if (!(this.builderClass.type._extends() == null || this.builderClass.type._extends().name().equals("java.lang.Object"))) {
			buildMethod.annotate(Override.class);
		}
		if (this.implement) {
			final JExpression buildExpression = JExpr._this().invoke(initMethod).arg(JExpr._new(this.definedClass));
			if (this.settings.isCopyAlways()) {
				buildMethod.body()._return(buildExpression);
			} else if (this.definedClass.isAbstract()) {
				buildMethod.body()._return(JExpr.cast(this.definedClass, this.storedValueField));
			} else {
				final JConditional jConditional = buildMethod.body()._if(this.storedValueField.eq(JExpr._null()));
				jConditional._then()._return(buildExpression);
				jConditional._else()._return(JExpr.cast(this.definedClass, this.storedValueField));
			}
		}
		return buildMethod;
	}

	JMethod generateNewBuilderMethod() {
		final JMethod builderMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.raw.narrow(Void.class), this.settings.getNewBuilderMethodName());
		builderMethod.body()._return(JExpr._new(this.builderClass.raw.narrow(Void.class)).arg(JExpr._null()).arg(JExpr._null()).arg(JExpr.FALSE));
		return builderMethod;
	}

	JMethod generateCopyOfMethod(final TypeOutline paramType, final boolean partial) {
		if (paramType.getSuperClass() != null) {
			generateCopyOfMethod(paramType.getSuperClass(), partial);
		}
		final JMethod copyOfMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.raw.narrow(Void.class), this.pluginContext.buildCopyMethodName);
		final JTypeVar copyOfMethodTypeParam = copyOfMethod.generify(BuilderGenerator.PARENT_BUILDER_TYPE_PARAMETER_NAME);
		copyOfMethod.type(this.builderClass.raw.narrow(copyOfMethodTypeParam));
		final JVar otherParam = copyOfMethod.param(JMod.FINAL, paramType.getImplClass(), BuilderGenerator.OTHER_PARAM_NAME);
		final CopyGenerator copyGenerator = this.pluginContext.createCopyGenerator(copyOfMethod, partial);
		final JVar newBuilderVar = copyOfMethod.body().decl(JMod.FINAL, copyOfMethod.type(), BuilderGenerator.NEW_BUILDER_VAR_NAME, JExpr._new(copyOfMethod.type()).arg(JExpr._null()).arg(JExpr._null()).arg(JExpr.FALSE));
		copyOfMethod.body().add(copyGenerator.generatePartialArgs(this.pluginContext.invoke(otherParam, this.settings.getCopyToMethodName()).arg(newBuilderVar)));
		copyOfMethod.body()._return(newBuilderVar);
		return copyOfMethod;
	}

	JMethod generateNewCopyBuilderMethod(final boolean partial) {
		final JDefinedClass typeDefinition = this.typeOutline.isInterface() && ((DefinedInterfaceOutline)this.typeOutline).getSupportInterface() != null ? ((DefinedInterfaceOutline)this.typeOutline).getSupportInterface() : this.definedClass;
		final int mods = this.implement ? this.definedClass.isAbstract() ? JMod.PUBLIC | JMod.ABSTRACT : JMod.PUBLIC : JMod.NONE;
		final JMethod copyBuilderMethod = typeDefinition.method(mods, this.builderClass.raw, this.settings.getNewCopyBuilderMethodName());
		final JTypeVar copyBuilderMethodTypeParam = copyBuilderMethod.generify(BuilderGenerator.PARENT_BUILDER_TYPE_PARAMETER_NAME);
		final JVar parentBuilderParam = copyBuilderMethod.param(JMod.FINAL, copyBuilderMethodTypeParam, BuilderGenerator.PARENT_BUILDER_PARAM_NAME);
		final CopyGenerator copyGenerator = this.pluginContext.createCopyGenerator(copyBuilderMethod, partial);
		copyBuilderMethod.type(this.builderClass.raw.narrow(copyBuilderMethodTypeParam));
		final JMethod copyBuilderConvenienceMethod = typeDefinition.method(mods, this.builderClass.raw.narrow(this.pluginContext.voidClass), this.settings.getNewCopyBuilderMethodName());
		final CopyGenerator copyConvenienceGenerator = this.pluginContext.createCopyGenerator(copyBuilderConvenienceMethod, partial);
		if (this.implement && !this.definedClass.isAbstract()) {
			copyBuilderMethod.body()._return(copyGenerator.generatePartialArgs(this.pluginContext._new((JClass)copyBuilderMethod.type()).arg(parentBuilderParam).arg(JExpr._this()).arg(JExpr.TRUE)));
			copyBuilderConvenienceMethod.body()._return(copyConvenienceGenerator.generatePartialArgs(this.pluginContext.invoke(this.settings.getNewCopyBuilderMethodName()).arg(JExpr._null())));
		}
		if (this.typeOutline.getSuperClass() != null) {
			copyBuilderMethod.annotate(Override.class);
			copyBuilderConvenienceMethod.annotate(Override.class);
		}
		return copyBuilderMethod;
	}

	private JMethod generateConveniencePartialCopyMethod(final TypeOutline paramType, final JMethod partialCopyOfMethod, final String methodName, final JExpression propertyTreeUseArg) {
		if (paramType.getSuperClass() != null) {
			generateConveniencePartialCopyMethod(paramType.getSuperClass(), partialCopyOfMethod, methodName, propertyTreeUseArg);
		}
		final JMethod conveniencePartialCopyMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.raw.narrow(Void.class), methodName);
		final JVar partialOtherParam = conveniencePartialCopyMethod.param(JMod.FINAL, paramType.getImplClass(), BuilderGenerator.OTHER_PARAM_NAME);
		final JVar propertyPathParam = conveniencePartialCopyMethod.param(JMod.FINAL, PropertyTree.class, PartialCopyGenerator.PROPERTY_TREE_PARAM_NAME);
		conveniencePartialCopyMethod.body()._return(JExpr.invoke(partialCopyOfMethod).arg(partialOtherParam).arg(propertyPathParam).arg(propertyTreeUseArg));
		return conveniencePartialCopyMethod;
	}

	final void generateCopyOfBuilderMethods() {
		if (this.implement) {
			final JMethod builderCopyOfValueMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.BUILD_COPY_METHOD_NAME);
			JVar paramOtherValue = builderCopyOfValueMethod.param(JMod.FINAL, this.definedClass, BuilderGenerator.OTHER_PARAM_NAME);
			builderCopyOfValueMethod.body()
				.add(JExpr.invoke(paramOtherValue, PluginContext.COPY_TO_METHOD_NAME).arg(JExpr._this()))
				._return(JExpr._this());

			final JMethod builderCopyOfBuilderMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.BUILD_COPY_METHOD_NAME);
			JVar paramOtherBuilder = builderCopyOfBuilderMethod.param(JMod.FINAL, this.builderClass.raw, BuilderGenerator.OTHER_PARAM_NAME);
			builderCopyOfBuilderMethod.body()._return(JExpr.invoke(builderCopyOfValueMethod).arg(JExpr.invoke(paramOtherBuilder, PluginContext.BUILD_METHOD_NAME)));
		}
	}

	final void generateCopyToMethod(final boolean partial) {
		if (this.implement) {
			final JDefinedClass typeDefinition = this.typeOutline.isInterface() && ((DefinedInterfaceOutline)this.typeOutline).getSupportInterface() != null ? ((DefinedInterfaceOutline)this.typeOutline).getSupportInterface() : this.definedClass;
			final JMethod copyToMethod = typeDefinition.method(JMod.PUBLIC, this.pluginContext.voidType, this.settings.getCopyToMethodName());
			final JTypeVar typeVar = copyToMethod.generify(BuilderGenerator.PARENT_BUILDER_TYPE_PARAMETER_NAME);
			final JVar otherParam = copyToMethod.param(JMod.FINAL, this.builderClass.raw.narrow(typeVar), BuilderGenerator.OTHER_PARAM_NAME);
			final CopyGenerator cloneGenerator = this.pluginContext.createCopyGenerator(copyToMethod, partial);
			final JBlock body = copyToMethod.body();
			final JVar otherRef;
			if (this.typeOutline.getSuperClass() != null) {
				body.add(cloneGenerator.generatePartialArgs(this.pluginContext.invoke(JExpr._super(), copyToMethod.name()).arg(otherParam)));
			}
			otherRef = otherParam;
			generateFieldCopyExpressions(cloneGenerator, body, otherRef, JExpr._this());
			copyToMethod.javadoc().append(JavadocUtils.hardWrapTextForJavadoc(getMessage("javadoc.method.copyTo")));
			copyToMethod.javadoc().addParam(otherParam).append(JavadocUtils.hardWrapTextForJavadoc(getMessage("javadoc.method.copyTo.param.other")));
		}
	}

	final void generateCopyConstructor(final boolean partial) {
		final JMethod constructor = this.builderClass.raw.constructor(this.builderClass.raw.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar parentBuilderParam = constructor.param(JMod.FINAL, this.builderClass.typeParam, BuilderGenerator.PARENT_BUILDER_PARAM_NAME);
		final JVar otherParam = constructor.param(JMod.FINAL, this.typeOutline.getImplClass(), BuilderGenerator.OTHER_PARAM_NAME);
		final JVar copyParam = constructor.param(JMod.FINAL, this.pluginContext.codeModel.BOOLEAN, BuilderGenerator.COPY_FLAG_PARAM_NAME);
		final CopyGenerator cloneGenerator = this.pluginContext.createCopyGenerator(constructor, partial);
		if (this.typeOutline.getSuperClass() != null) {
			constructor.body().add(cloneGenerator.generatePartialArgs(this.pluginContext._super().arg(parentBuilderParam).arg(otherParam).arg(copyParam)));
		} else {
			constructor.body().assign(JExpr._this().ref(this.parentBuilderField), parentBuilderParam);
		}
		final JConditional ifNullStmt = constructor.body()._if(otherParam.ne(JExpr._null()));
		final JBlock body;
		if (!this.settings.isCopyAlways() && this.typeOutline.getSuperClass() == null) {
			final JConditional ifCopyStmt = ifNullStmt._then()._if(copyParam);
			ifCopyStmt._else().assign(this.storedValueField, otherParam);
			ifNullStmt._else().assign(this.storedValueField, JExpr._null());
			body = ifCopyStmt._then();
			body.assign(this.storedValueField, JExpr._null());
		} else {
			body = ifNullStmt._then();
		}
		generateFieldCopyExpressions(cloneGenerator, body, JExpr._this(), otherParam);
	}

	private void generateFieldCopyExpressions(final CopyGenerator cloneGenerator, final JBlock body, final JExpression targetObject, final JExpression sourceObject) {
		for (final DefinedPropertyOutline fieldOutline : this.typeOutline.getDeclaredFields()) {
			final JFieldVar field = fieldOutline.getFieldVar();
			if (field != null) {
				if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					final JFieldRef targetField = targetObject.ref(field.name());
					final JFieldRef sourceRef = sourceObject.ref(field.name());
					final PropertyTreeVarGenerator treeVarGenerator = cloneGenerator.createPropertyTreeVarGenerator(body, fieldOutline.getFieldName());
					final JType fieldType = fieldOutline.getRawType();
					final JBlock currentBlock = treeVarGenerator.generateEnclosingBlock(body);
					if (fieldType.isReference()) {
						final JClass fieldClass = (JClass)fieldType;
						if (this.pluginContext.collectionClass.isAssignableFrom(fieldClass)) {
							final JClass elementType = fieldClass.getTypeParameters().get(0);
							final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);
							if (this.settings.isGeneratingNarrowCopy() && this.pluginContext.canInstantiate(elementType)) {
								final JClass childBuilderType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
								final JForEach forLoop = loop(currentBlock, sourceRef, elementType, targetField, childBuilderType);
								forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), treeVarGenerator.generatePartialArgs(this.pluginContext.invoke(elementType, this.pluginContext.buildCopyMethodName).narrow(this.builderClass.type).arg(forLoop.var()))));
							} else if (childBuilderOutline != null) {
								final JClass childBuilderType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
								final JForEach forLoop = loop(currentBlock, sourceRef, elementType, targetField, childBuilderType);
								forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), treeVarGenerator.generatePartialArgs(this.pluginContext.invoke(forLoop.var(), this.settings.getNewCopyBuilderMethodName()).arg(targetObject))));
							} else if (this.pluginContext.partialCopyableInterface.isAssignableFrom(elementType)) {
								final JForEach forLoop = loop(currentBlock, sourceRef, elementType, targetField, elementType);
								forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), JExpr._new(this.pluginContext.buildableClass).arg(treeVarGenerator.generatePartialArgs(forLoop.var().invoke(this.pluginContext.copyMethodName)))));
							} else if (this.pluginContext.copyableInterface.isAssignableFrom(elementType)) {
								final JForEach forLoop = loop(currentBlock, sourceRef, elementType, targetField, elementType);
								forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), JExpr._new(this.pluginContext.buildableClass).arg(forLoop.var().invoke(this.pluginContext.copyMethodName))));
							} else if (this.pluginContext.cloneableInterface.isAssignableFrom(elementType)) {
								final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(currentBlock, elementType);
								final JForEach forLoop = loop(maybeTryBlock, sourceRef, elementType, targetField, this.pluginContext.buildableInterface);
								forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), JExpr._new(this.pluginContext.buildableClass).arg(forLoop.var().invoke(this.pluginContext.cloneMethodName))));
							} else {
								final JForEach forLoop = loop(currentBlock, sourceRef, elementType, targetField, this.pluginContext.buildableInterface);
								forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), JExpr._new(this.pluginContext.buildableClass).arg(forLoop.var())));
							}
						} else {
							final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
							if (this.settings.isGeneratingNarrowCopy() && this.pluginContext.canInstantiate(fieldType)) {
								currentBlock.assign(targetField, nullSafe(sourceRef, treeVarGenerator.generatePartialArgs(this.pluginContext.invoke(fieldType, this.pluginContext.buildCopyMethodName).narrow(this.builderClass.type).arg(sourceRef))));
							} else if (childBuilderOutline != null) {
								currentBlock.assign(targetField, nullSafe(sourceRef, treeVarGenerator.generatePartialArgs(this.pluginContext.invoke(sourceRef, this.settings.getNewCopyBuilderMethodName()).arg(targetObject))));
							} else if (this.pluginContext.partialCopyableInterface.isAssignableFrom(fieldClass)) {
								currentBlock.assign(targetField, nullSafe(sourceRef, this.pluginContext.castOnDemand(fieldType, treeVarGenerator.generatePartialArgs(sourceRef.invoke(this.pluginContext.copyMethodName)))));
							} else if (this.pluginContext.copyableInterface.isAssignableFrom(fieldClass)) {
								currentBlock.assign(targetField, nullSafe(sourceRef, this.pluginContext.castOnDemand(fieldType, sourceRef.invoke(this.pluginContext.copyMethodName))));
							} else if (this.pluginContext.cloneableInterface.isAssignableFrom(fieldClass)) {
								final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(currentBlock, fieldClass);
								maybeTryBlock.assign(targetField, nullSafe(sourceRef, this.pluginContext.castOnDemand(fieldType, sourceRef.invoke(this.pluginContext.cloneMethodName))));
							} else if (this.pluginContext.buildableInterface.isAssignableFrom(fieldClass)) {
								currentBlock.assign(
										targetField,
										nullSafe(sourceRef, JExpr._new(this.pluginContext.buildableClass).arg(sourceRef)));
							} else if (fieldOutline.getChoiceProperties().size() > 1) {
							    // TODO don't think this will work if choice items have no builder
								// Handle single choice properties
								currentBlock.assign(
										targetField,
										nullSafe(sourceRef, JExpr._new(this.pluginContext.buildableClass).arg(sourceRef)));
							} else {
								currentBlock.assign(targetField, sourceRef);
							}
						}
					} else {
						currentBlock.assign(targetField, sourceRef);
					}
				}
			}
		}
	}

	public void buildProperties() throws SAXException {
		final TypeOutline superClass = this.typeOutline.getSuperClass();
		final JMethod initMethod;
		final JVar productParam;
		final JBlock initBody;
		if (this.implement) {
			initMethod = this.builderClass.raw.method(JMod.PROTECTED, this.definedClass, PluginContext.INIT_METHOD_NAME);
			final JTypeVar typeVar = initMethod.generify(BuilderGenerator.PRODUCT_TYPE_PARAMETER_NAME, this.definedClass);
			initMethod.type(typeVar);
			productParam = initMethod.param(JMod.FINAL, typeVar, BuilderGenerator.PRODUCT_VAR_NAME);
			initBody = initMethod.body();
		} else {
			initMethod = null;
			initBody = null;
			productParam = null;
		}
		generateDefinedClassJavadoc();

		if (this.typeOutline.getDeclaredFields() != null) {
			for (final PropertyOutline fieldOutline : this.typeOutline.getDeclaredFields()) {
				if (fieldOutline.hasGetter()) {
					generateBuilderMember(fieldOutline, initBody, productParam);
				}
			}
		}
		if (superClass != null) {
			generateExtendsClause(getBuilderDeclaration(superClass.getImplClass()));
			if (this.implement) initBody._return(JExpr._super().invoke(initMethod).arg(productParam));
			generateBuilderMemberOverrides(superClass);
		} else if (this.implement) {
			initBody._return(productParam);
		}
		generateImplementsClause();
		generateBuildMethod(initMethod);
		generateCopyToMethod(false);
		generateNewCopyBuilderMethod(false);
		if (this.implement && !this.definedClass.isAbstract()) {
			generateNewBuilderMethod();
			generateCopyOfMethod(this.typeOutline, false);
		}
		if (this.settings.isGeneratingPartialCopy()) {
			generateCopyToMethod(true);
			generateNewCopyBuilderMethod(true);
			if (this.implement && !this.definedClass.isAbstract()) {
				final JMethod partialCopyOfMethod = generateCopyOfMethod(this.typeOutline, true);
				generateConveniencePartialCopyMethod(this.typeOutline, partialCopyOfMethod, this.pluginContext.copyExceptMethodName, this.pluginContext.excludeConst);
				generateConveniencePartialCopyMethod(this.typeOutline, partialCopyOfMethod, this.pluginContext.copyOnlyMethodName, this.pluginContext.includeConst);
			}
		}
		generateCopyOfBuilderMethods();
	}

	private void generateDefinedClassJavadoc() {
		if (settings.isGeneratingJavadocFromAnnotations()) {

			// xjc seems to add annotations at the class level for named and anonymous complex types
			// but doesn't wrap them to a sensible width. If we hard wrap the existing javadoc then
            // it messes up all the <pre> blocks. It is not worth the effort/risk to try and wrap everything
			// this is not a pre block.
//		    JavadocUtils.hardWrapCommentText(this.definedClass.javadoc());

			// Add our field level annotations to the getters/setters on the defined class
			this.typeOutline.getImplClass().methods().forEach(jMethod -> {
				final String fieldName = getCorrespondingFieldName(jMethod);
				this.typeOutline.getDeclaredFields().stream()
						.filter(typeOutline -> typeOutline.getFieldName().equals(fieldName))
						.findAny()
						.flatMap(DefinedPropertyOutline::getSchemaAnnotationText)
						.ifPresent(schemaAnnotation -> {
							if (settings.isGeneratingJavadocFromAnnotations()) {
								JavadocUtils.appendJavadocParagraph(jMethod, schemaAnnotation);
							}
						});
			});
		}
	}

	private String getCorrespondingFieldName(final JMethod jMethod) {
		String methodName = jMethod.name();
		return Introspector.decapitalize(methodName.substring(methodName.startsWith("is") ? 2 : 3));
	}

	private void generateBuilderMemberOverrides(final TypeOutline superClass) throws SAXException {
		if (superClass.getDeclaredFields() != null) {
			for (final PropertyOutline superFieldOutline : superClass.getDeclaredFields()) {
				if (superFieldOutline.hasGetter()) {
					final String superPropertyName = superFieldOutline.getBaseName();
					generateBuilderMemberOverride(superFieldOutline, superFieldOutline, superPropertyName);
				}
			}
		}
		if (superClass.getSuperClass() != null) {
			generateBuilderMemberOverrides(superClass.getSuperClass());
		}
	}

	BuilderOutline getBuilderDeclaration(final JType type) {
		BuilderOutline builderOutline = this.builderOutlines.get(type.fullName());
		if (builderOutline == null) {
			builderOutline = getReferencedBuilderOutline(type);
		}
		return builderOutline;
	}

	void generateArrayProperty(final JBlock initBody, final JVar productParam, final PropertyOutline fieldOutline, final JType elementType, final JType builderType) {
		final String fieldName = fieldOutline.getFieldName();
		final String propertyName = fieldOutline.getBaseName();
		final JType fieldType = fieldOutline.getRawType();
		final JMethod withVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, builderType, PluginContext.WITH_METHOD_PREFIX + propertyName);
		final JVar withVarargsParam = withVarargsMethod.varParam(elementType, fieldName);
		if (this.implement) {
			final JFieldVar builderField = this.builderClass.raw.field(JMod.PRIVATE, fieldType, fieldName, JExpr._null());
			withVarargsMethod.body().assign(JExpr._this().ref(builderField), withVarargsParam);
			withVarargsMethod.body()._return(JExpr._this());
			initBody.assign(productParam.ref(fieldName), JExpr._this().ref(builderField));
		}
	}

	JForEach loop(final JBlock block, final JExpression source, final JType sourceElementType, final JAssignmentTarget target, final JType targetElementType) {
		final JConditional ifNull = block._if(source.eq(JExpr._null()));
		ifNull._then().assign(target, JExpr._null());
		ifNull._else().assign(target, JExpr._new(this.pluginContext.arrayListClass.narrow(targetElementType)));
		return ifNull._else().forEach(sourceElementType, BuilderGenerator.ITEM_VAR_NAME, source);
	}

	private void generateAddMethodJavadoc(final JMethod method, final JVar param, final String schemaAnnotation) {
		final String propertyName = param.name();
		JavadocUtils.appendJavadocCommentParagraphs(
					method.javadoc(),
					settings.isGeneratingJavadocFromAnnotations() ? schemaAnnotation : null,
					MessageFormat.format(this.resources.getString("comment.addMethod"), propertyName))
				.addParam(param)
				.append(JavadocUtils.hardWrapTextForJavadoc(MessageFormat.format(
						this.resources.getString("comment.addMethod.param"),
						propertyName)));
	}

	private void generateWithMethodJavadoc(final JMethod method, final JVar param, final String schemaAnnotation) {
		final String propertyName = param.name();
		JavadocUtils.appendJavadocCommentParagraphs(
					method.javadoc(),
					settings.isGeneratingJavadocFromAnnotations() ? schemaAnnotation : null,
					MessageFormat.format(this.resources.getString("comment.withMethod"), propertyName))
				.addParam(param)
				.append(JavadocUtils.hardWrapTextForJavadoc(MessageFormat.format(
						this.resources.getString("comment.withMethod.param"),
						propertyName)));
	}

	private void generateBuilderMethodJavadoc(final JMethod method, final String methodPrefix, final String propertyName, final String schemaAnnotation) {
		final String endMethodClassName = method.type().erasure().fullName();
		JavadocUtils.appendJavadocCommentParagraphs(
					method.javadoc(),
					settings.isGeneratingJavadocFromAnnotations() ? schemaAnnotation : null,
					MessageFormat.format(
							this.resources.getString("comment." + methodPrefix + "BuilderMethod"),
							propertyName,
							endMethodClassName))
				.addReturn()
				.append(JavadocUtils.hardWrapTextForJavadoc(MessageFormat.format(
						this.resources.getString("comment." + methodPrefix + "BuilderMethod.return"),
						propertyName,
						endMethodClassName)));
	}


	private BuilderOutline getReferencedBuilderOutline(final JType type) {
		BuilderOutline builderOutline = null;
		if (this.pluginContext.getClassOutline(type) == null && this.pluginContext.getEnumOutline(type) == null && type.isReference() && !type.isPrimitive() && !type.isArray() && type.fullName().contains(".")) {
			final Class<?> runtimeParentClass;
			try {
				runtimeParentClass = Class.forName(type.binaryName());
			} catch (final ClassNotFoundException e) {
				return null;
			}
			final JClass builderClass = reflectRuntimeInnerClass(runtimeParentClass, this.settings.getBuilderClassName());
			if (builderClass != null) {
				final ReferencedClassOutline referencedClassOutline = new ReferencedClassOutline(this.pluginContext.codeModel, runtimeParentClass);
				builderOutline = new BuilderOutline(referencedClassOutline, builderClass);
			}
		}
		return builderOutline;
	}

	private JClass reflectRuntimeInnerClass(final Class<?> runtimeParentClass, final ClassName className) {
		final JClass parentClass = this.pluginContext.codeModel.ref(runtimeParentClass);
		final String innerClassName = className.getName(runtimeParentClass.isInterface());
		final Class<?> runtimeInnerClass = PluginContext.findInnerClass(runtimeParentClass, innerClassName);
		if (runtimeInnerClass != null) {
			final JClass innerSuperClass = runtimeParentClass.getSuperclass() != null ? this.pluginContext.codeModel.ref(runtimeInnerClass.getSuperclass()) : null;
			return this.pluginContext.ref(parentClass, innerClassName, runtimeInnerClass.isInterface(), Modifier.isAbstract(runtimeInnerClass.getModifiers()), innerSuperClass);
		} else {
			return null;
		}
	}

	private String getMessage(final String resourceKey, final Object... args) {
		return MessageFormat.format(this.resources.getString(resourceKey), args);
	}

}
