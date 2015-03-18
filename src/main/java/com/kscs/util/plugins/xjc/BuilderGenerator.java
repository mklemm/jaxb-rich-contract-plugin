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

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.plugins.xjc.codemodel.ClassName;
import com.kscs.util.plugins.xjc.codemodel.GenerifiedClass;
import com.kscs.util.plugins.xjc.codemodel.JTypedInvocation;
import com.kscs.util.plugins.xjc.outline.DefinedInterfaceOutline;
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
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;
import com.sun.codemodel.JVar;
import org.xml.sax.SAXException;

import static com.kscs.util.plugins.xjc.base.PluginUtil.nullSafe;

/**
 * Helper class to generate fluent builder classes in two steps
 */
class BuilderGenerator {
	public static final String PRODUCT_VAR_NAME = "_product";
	public static final String PARENT_BUILDER_TYPE_PARAMETER_NAME = "B";
	public static final String PRODUCT_TYPE_PARAMETER_NAME = "P";
	public static final String OTHER_PARAM_NAME = "_other";
	public static final String OTHER_VAR_NAME = "_my";
	public static final String PARENT_BUILDER_PARAM_NAME = "_parentBuilder";
	public static final String NEW_BUILDER_VAR_NAME = "_newBuilder";
	public static final String COPY_FLAG_PARAM_NAME = "_copy";
	private static final String ITEM_VAR_NAME = "_item";
	private final PluginContext pluginContext;
	private final JDefinedClass definedClass;
	private final GenerifiedClass builderClass;
	private final DefinedTypeOutline typeOutline;
	private final Map<String, BuilderOutline> builderOutlines;
	private final JFieldVar parentBuilderField;
	private final JFieldVar productField;
	private final boolean implement;
	private final BuilderGeneratorSettings settings;

	private final ResourceBundle resources;

	BuilderGenerator(final PluginContext pluginContext, final Map<String, BuilderOutline> builderOutlines, final BuilderOutline builderOutline, final BuilderGeneratorSettings settings) {
		this.pluginContext = pluginContext;
		this.settings = settings;
		this.builderOutlines = builderOutlines;
		this.typeOutline = (DefinedTypeOutline) builderOutline.getClassOutline();
		this.definedClass = this.typeOutline.getImplClass();
		this.builderClass = new GenerifiedClass(builderOutline.getDefinedBuilderClass(), BuilderGenerator.PARENT_BUILDER_TYPE_PARAMETER_NAME);
		this.resources = ResourceBundle.getBundle(BuilderGenerator.class.getName());
		this.implement = !this.builderClass.raw.isInterface();
		if (builderOutline.getClassOutline().getSuperClass() == null) {
			final JMethod endMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.typeParam, "end");
			if (this.implement) {
				this.parentBuilderField = this.builderClass.raw.field(JMod.PROTECTED | JMod.FINAL, this.builderClass.typeParam, BuilderGenerator.PARENT_BUILDER_PARAM_NAME);
				this.productField = this.builderClass.raw.field(JMod.PROTECTED | JMod.FINAL, this.definedClass, BuilderGenerator.OTHER_PARAM_NAME);
				endMethod.body()._return(JExpr._this().ref(this.parentBuilderField));
			} else {
				this.parentBuilderField = null;
				this.productField = null;
			}
		} else {
			this.parentBuilderField = null;
			this.productField = null;
		}

		if (this.implement) {
			generateCopyConstructor(false);
			if (this.settings.isGeneratingPartialCopy()) {
				generateCopyConstructor(true);
			}
		}
	}

	void generateBuilderMember(final PropertyOutline fieldOutline, final JBlock initBody, final JVar productParam) {
		final String propertyName = fieldOutline.getBaseName();
		final String fieldName = fieldOutline.getFieldName();
		final JType fieldType = fieldOutline.getRawType();

		if (fieldOutline.isCollection()) {
			if (fieldOutline.getRawType().isArray()) {
				generateArrayProperty(initBody, productParam, fieldOutline, fieldType.elementType(), this.builderClass.type);
			} else {
				final List<JClass> typeParameters = ((JClass) fieldType).getTypeParameters();
				final JClass elementType = typeParameters.get(0);
				generateCollectionProperty(initBody, productParam, fieldOutline, elementType);
			}
		} else {
			generateSingularProperty(initBody, productParam, fieldOutline, propertyName);
		}
	}

	private void generateSingularProperty(final JBlock initBody, final JVar productParam, final PropertyOutline fieldOutline, final String propertyName) {
		final String fieldName = fieldOutline.getFieldName();
		final JType fieldType = fieldOutline.getRawType();
		final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
		if (childBuilderOutline == null) {
			final JMethod withMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withMethod.param(JMod.FINAL, fieldType, fieldName);
			generateWithMethodJavadoc(withMethod, param);
			if (this.implement) {
				final JFieldVar builderField = this.builderClass.raw.field(JMod.PRIVATE, fieldType, fieldName);
				withMethod.body().assign(JExpr._this().ref(builderField), param);
				withMethod.body()._return(JExpr._this());
				initBody.assign(productParam.ref(fieldName), JExpr._this().ref(builderField));
			}
		} else {
			final JClass elementType = (JClass) fieldType;
			final JClass builderFieldElementType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
			final JClass builderWithMethodReturnType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());

			final JMethod withValueMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withValueMethod.param(JMod.FINAL, elementType, fieldName);
			generateWithMethodJavadoc(withValueMethod, param);

			final JMethod withBuilderMethod = this.builderClass.raw.method(JMod.PUBLIC, builderWithMethodReturnType, PluginContext.WITH_METHOD_PREFIX + propertyName);
			generateWithBuilderMethodJavadoc(withBuilderMethod, fieldOutline);

			if (this.implement) {
				final JFieldVar builderField = this.builderClass.raw.field(JMod.PRIVATE, builderFieldElementType, fieldName);
				withValueMethod.body().assign(JExpr._this().ref(builderField), nullSafe(param, JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(param).arg(JExpr.FALSE)));
				withValueMethod.body()._return(JExpr._this());
				withBuilderMethod.body()._return(JExpr._this().ref(builderField).assign(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE)));

				initBody.assign(productParam.ref(fieldName), nullSafe(JExpr._this().ref(builderField), JExpr._this().ref(builderField).invoke(PluginContext.BUILD_METHOD_NAME)));
			}

		}
	}

	private void generateCollectionProperty(final JBlock initBody, final JVar productParam, final PropertyOutline fieldOutline, final JClass elementType) {
		final String fieldName = fieldOutline.getFieldName();
		final String propertyName = fieldOutline.getBaseName();
		final JType fieldType = fieldOutline.getRawType();

		final JClass collectionType = this.pluginContext.collectionClass.narrow(elementType.wildcard());
		final JClass iterableType = this.pluginContext.iterableClass.narrow(elementType.wildcard());
		final JMethod addListMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		final JVar addListParam = addListMethod.param(JMod.FINAL, collectionType, fieldName);
		generateAddMethodJavadoc(addListMethod, addListParam);

		final JMethod withListMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
		final JVar withListParam = withListMethod.param(JMod.FINAL, collectionType, fieldName);
		generateWithMethodJavadoc(withListMethod, withListParam);

		final JMethod addIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		final JVar addIterableParam = addIterableMethod.param(JMod.FINAL, iterableType, fieldName);
		generateAddMethodJavadoc(addIterableMethod, addIterableParam);

		final JMethod withIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
		final JVar withIterableParam = withIterableMethod.param(JMod.FINAL, iterableType, fieldName);
		generateWithMethodJavadoc(withIterableMethod, withIterableParam);

		final JMethod addVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + propertyName);
		final JVar addVarargsParam = addVarargsMethod.varParam(elementType, fieldName);
		generateAddMethodJavadoc(addVarargsMethod, addVarargsParam);

		final JMethod withVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + propertyName);
		final JVar withVarargsParam = withVarargsMethod.varParam(elementType, fieldName);
		generateWithMethodJavadoc(withVarargsMethod, withVarargsParam);

		if (this.implement) {
			addVarargsMethod.body().invoke(addListMethod).arg(this.pluginContext.asList(addVarargsParam));
			addVarargsMethod.body()._return(JExpr._this());
			withVarargsMethod.body().invoke(withListMethod).arg(this.pluginContext.asList(withVarargsParam));
			withVarargsMethod.body()._return(JExpr._this());
		}

		final JVar collectionVar;

		final BuilderOutline childBuilderOutline;
		childBuilderOutline = getBuilderDeclaration(elementType);
		if (childBuilderOutline == null) {
			if (this.implement) {
				final JFieldVar builderField = this.builderClass.raw.field(JMod.PRIVATE, fieldType, fieldName);

				final JConditional addIfNull = addListMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(this.pluginContext.arrayListClass.narrow(elementType)));
				addListMethod.body().invoke(JExpr._this().ref(builderField), PluginContext.ADD_ALL).arg(addListParam);
				addListMethod.body()._return(JExpr._this());

				final JConditional withIfNull = withListMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
				withIfNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
				withListMethod.body()._return(JExpr.invoke(addListMethod).arg(withListParam));

				final JConditional addIterableIfParamNull = addIterableMethod.body()._if(addIterableParam.ne(JExpr._null()));
				final JConditional addIterableIfNull = addIterableIfParamNull._then()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIterableIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(this.pluginContext.arrayListClass.narrow(elementType)));
				final JForEach jForEach = addIterableIfParamNull._then().forEach(elementType, BuilderGenerator.ITEM_VAR_NAME, addIterableParam);
				jForEach.body().add(JExpr._this().ref(builderField).invoke("add").arg(jForEach.var()));
				addIterableMethod.body()._return(JExpr._this());

				final JConditional withIterableIfNull = withIterableMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
				withIterableIfNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
				withIterableMethod.body()._return(JExpr.invoke(addIterableMethod).arg(withIterableParam));

				initBody.assign(productParam.ref(fieldName), JExpr._this().ref(builderField));
			}
		} else {
			final JClass builderFieldElementType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
			final JClass builderWithMethodReturnType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());
			final JClass builderArrayListClass = this.pluginContext.arrayListClass.narrow(builderFieldElementType);
			final JClass builderListClass = this.pluginContext.listClass.narrow(builderFieldElementType);

			final JMethod addMethod = this.builderClass.raw.method(JMod.PUBLIC, builderWithMethodReturnType, PluginContext.ADD_METHOD_PREFIX + propertyName);
			generateAddBuilderMethodJavadoc(addMethod, fieldOutline);

			if (this.implement) {
				final JFieldVar builderField = this.builderClass.raw.field(JMod.PRIVATE, builderListClass, fieldName);
				final JConditional addIfNull = addMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
				final JVar childBuilderVar = addMethod.body().decl(JMod.FINAL, builderFieldElementType, fieldName + this.settings.getBuilderFieldSuffix(), JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE));
				addMethod.body().add(JExpr._this().ref(builderField).invoke("add").arg(childBuilderVar));
				addMethod.body()._return(childBuilderVar);

				final JConditional addListIfParamNull = addListMethod.body()._if(addListParam.ne(JExpr._null()));
				final JConditional addListIfNull = addListIfParamNull._then()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addListIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
				final JForEach addListForEach = addListIfParamNull._then().forEach(elementType, BuilderGenerator.ITEM_VAR_NAME, addListParam);
				addListForEach.body().add(JExpr._this().ref(builderField).invoke("add").arg(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(addListForEach.var()).arg(JExpr.FALSE)));
				addListMethod.body()._return(JExpr._this());

				final JConditional withListIfNull = withListMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
				withListIfNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
				withListMethod.body()._return(JExpr.invoke(addListMethod).arg(withListParam));

				final JConditional addIterableIfParamNull = addIterableMethod.body()._if(addIterableParam.ne(JExpr._null()));
				final JConditional addIterableIfNull = addIterableIfParamNull._then()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIterableIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
				final JForEach addIterableForEach = addIterableIfParamNull._then().forEach(elementType, BuilderGenerator.ITEM_VAR_NAME, addIterableParam);
				addIterableForEach.body().add(JExpr._this().ref(builderField).invoke("add").arg(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(addIterableForEach.var()).arg(JExpr.FALSE)));
				addIterableMethod.body()._return(JExpr._this());

				final JConditional withIterableIfNull = withIterableMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
				withIterableIfNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
				withIterableMethod.body()._return(JExpr.invoke(addIterableMethod).arg(withIterableParam));

				final JConditional ifNull = initBody._if(JExpr._this().ref(builderField).ne(JExpr._null()));
				collectionVar = ifNull._then().decl(JMod.FINAL, this.pluginContext.listClass.narrow(elementType), fieldName, JExpr._new(this.pluginContext.arrayListClass.narrow(elementType)).arg(JExpr._this().ref(builderField).invoke("size")));
				final JForEach initForEach = ifNull._then().forEach(builderFieldElementType, BuilderGenerator.ITEM_VAR_NAME, JExpr._this().ref(builderField));
				initForEach.body().add(collectionVar.invoke("add").arg(initForEach.var().invoke(PluginContext.BUILD_METHOD_NAME)));
				ifNull._then().assign(productParam.ref(fieldName), collectionVar);
			}
		}

		if (this.implement) {
			this.pluginContext.generateImmutableFieldInit(initBody, productParam, fieldOutline);
		}
	}

	void generateBuilderMemberOverride(final PropertyOutline superFieldOutline, final PropertyOutline fieldOutline, final String superPropertyName) {
		final JType fieldType = fieldOutline.getRawType();
		final String fieldName = fieldOutline.getFieldName();

		if (superFieldOutline.isCollection()) {
			if (!fieldType.isArray()) {
				final JClass elementType = ((JClass) fieldType).getTypeParameters().get(0);
				final JClass iterableType = this.pluginContext.iterableClass.narrow(elementType.wildcard());
				final JClass collectionType = this.pluginContext.collectionClass.narrow(elementType.wildcard());

				final JMethod addListMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + superPropertyName);
				final JVar addListParam = addListMethod.param(JMod.FINAL, collectionType, fieldName);
				generateAddMethodJavadoc(addListMethod, addListParam);

				final JMethod addIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + superPropertyName);
				final JVar addIterableParam = addIterableMethod.param(JMod.FINAL, iterableType, fieldName);
				generateAddMethodJavadoc(addIterableMethod, addIterableParam);

				final JMethod addVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.ADD_METHOD_PREFIX + superPropertyName);
				final JVar addVarargsParam = addVarargsMethod.varParam(elementType, fieldName);
				generateAddMethodJavadoc(addVarargsMethod, addVarargsParam);

				final JMethod withListMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withListParam = withListMethod.param(JMod.FINAL, collectionType, fieldName);
				generateWithMethodJavadoc(withListMethod, withListParam);

				final JMethod withIterableMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withIterableParam = withIterableMethod.param(JMod.FINAL, iterableType, fieldName);
				generateWithMethodJavadoc(withIterableMethod, withIterableParam);

				final JMethod withVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withVarargsParam = withVarargsMethod.varParam(elementType, fieldName);
				generateWithMethodJavadoc(withVarargsMethod, withVarargsParam);

				if (this.implement) {
					addListMethod.annotate(Override.class);
					addListMethod.body().invoke(JExpr._super(), PluginContext.ADD_METHOD_PREFIX + superPropertyName).arg(addListParam);
					addListMethod.body()._return(JExpr._this());

					addIterableMethod.annotate(Override.class);
					addIterableMethod.body().invoke(JExpr._super(), PluginContext.ADD_METHOD_PREFIX + superPropertyName).arg(addIterableParam);
					addIterableMethod.body()._return(JExpr._this());

					addVarargsMethod.annotate(Override.class);
					addVarargsMethod.body().invoke(JExpr._super(), PluginContext.ADD_METHOD_PREFIX + superPropertyName).arg(addVarargsParam);
					addVarargsMethod.body()._return(JExpr._this());

					withListMethod.annotate(Override.class);
					withListMethod.body().invoke(JExpr._super(), PluginContext.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
					withListMethod.body()._return(JExpr._this());

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
					generateAddBuilderMethodJavadoc(addMethod, superFieldOutline);
					if (this.implement) {
						addMethod.annotate(Override.class);
						addMethod.body()._return(JExpr.cast(builderFieldElementType, JExpr._super().invoke(addMethod)));
					}
				}
			} else {
				final JType elementType = fieldType.elementType();
				final JClass iterableType = this.pluginContext.iterableClass.narrow(elementType);

				final JMethod withListMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withListParam = withListMethod.param(JMod.FINAL, iterableType, fieldName);
				generateWithMethodJavadoc(withListMethod, withListParam);

				final JMethod withVarargsMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withVarargsParam = withVarargsMethod.varParam(((JClass) fieldType).getTypeParameters().get(0), fieldName);
				generateWithMethodJavadoc(withVarargsMethod, withVarargsParam);

				if (this.implement) {
					withListMethod.annotate(Override.class);
					withListMethod.body().invoke(JExpr._super(), PluginContext.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
					withListMethod.body()._return(JExpr._this());

					withVarargsMethod.annotate(Override.class);
					withVarargsMethod.body().invoke(JExpr._super(), PluginContext.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
					withVarargsMethod.body()._return(JExpr._this());
				}
			}
		} else {
			final JMethod withMethod = this.builderClass.raw.method(JMod.PUBLIC, this.builderClass.type, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
			final JVar param = withMethod.param(JMod.FINAL, superFieldOutline.getRawType(), fieldName);
			generateWithMethodJavadoc(withMethod, param);

			if (this.implement) {
				withMethod.annotate(Override.class);
				withMethod.body().invoke(JExpr._super(), PluginContext.WITH_METHOD_PREFIX + superPropertyName).arg(param);
				withMethod.body()._return(JExpr._this());
			}

			final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
			if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().getImplClass().isAbstract()) {
				final JClass builderFieldElementType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type.wildcard());
				final JMethod addMethod = this.builderClass.raw.method(JMod.PUBLIC, builderFieldElementType, PluginContext.WITH_METHOD_PREFIX + superPropertyName);
				generateWithBuilderMethodJavadoc(addMethod, superFieldOutline);
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
		}
	}

	private JClass getBuilderInterface(final JClass parentClass) {
		return this.pluginContext.ref(parentClass, PluginContext.BUILDER_INTERFACE_NAME, true, false, this.pluginContext.codeModel.ref(Object.class));
	}

	JMethod generateBuildMethod(final JMethod initMethod) {
		final JMethod buildMethod = this.builderClass.raw.method(JMod.PUBLIC, this.definedClass, PluginContext.BUILD_METHOD_NAME);
		if (this.implement) {
			if (this.definedClass.isAbstract()) {
				buildMethod.body()._return(JExpr.cast(this.definedClass, JExpr._this().ref(BuilderGenerator.OTHER_PARAM_NAME)));
			} else {
				final JConditional ifStatement = buildMethod.body()._if(JExpr._this().ref(BuilderGenerator.OTHER_PARAM_NAME).eq(JExpr._null()));
				ifStatement._then()._return(JExpr._this().invoke(initMethod).arg(JExpr._new(this.definedClass)));
				ifStatement._else()._return(JExpr.cast(this.definedClass, JExpr._this().ref(BuilderGenerator.OTHER_PARAM_NAME)));
			}
		}
		return buildMethod;
	}

	JMethod generateNewBuilderMethod() {
		final JMethod builderMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.raw.narrow(Void.class), this.settings.getNewBuilderMethodName());
		builderMethod.body()._return(JExpr._new(this.builderClass.raw.narrow(Void.class)).arg(JExpr._null()).arg(JExpr._null()).arg(JExpr.FALSE));
		return builderMethod;
	}

	JMethod generateCopyOfMethod(final boolean partial) {
		final JMethod copyOfMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.raw.narrow(Void.class), this.pluginContext.buildCopyMethodName);
		final JTypeVar copyOfMethodTypeParam = copyOfMethod.generify(BuilderGenerator.PARENT_BUILDER_TYPE_PARAMETER_NAME);
		copyOfMethod.type(this.builderClass.raw.narrow(copyOfMethodTypeParam));
		final JVar otherParam = copyOfMethod.param(JMod.FINAL, getInheritanceRoot(this.typeOutline).getImplClass(), BuilderGenerator.OTHER_PARAM_NAME);
		final CopyGenerator copyGenerator = this.pluginContext.createCopyGenerator(copyOfMethod, partial);
		final JVar newBuilderVar = copyOfMethod.body().decl(JMod.FINAL, copyOfMethod.type(), BuilderGenerator.NEW_BUILDER_VAR_NAME, JExpr._new(copyOfMethod.type()).arg(JExpr._null()).arg(JExpr._null()).arg(JExpr.FALSE));
		copyOfMethod.body().add(copyGenerator.generatePartialArgs(this.pluginContext.invoke(otherParam, this.settings.getCopyToMethodName()).arg(newBuilderVar)));
		copyOfMethod.body()._return(newBuilderVar);
		return copyOfMethod;
	}

	private TypeOutline getInheritanceRoot(final TypeOutline typeOutline) {
		return typeOutline.getSuperClass() != null ? getInheritanceRoot(typeOutline.getSuperClass()) : typeOutline;
	}

	JMethod generateNewCopyBuilderMethod(final boolean partial) {
		final JDefinedClass typeDefinition = this.typeOutline.isInterface() && ((DefinedInterfaceOutline) this.typeOutline).getSupportInterface() != null ? ((DefinedInterfaceOutline) this.typeOutline).getSupportInterface() : this.definedClass;
		final int mods = this.implement ? this.definedClass.isAbstract() ? JMod.PUBLIC | JMod.ABSTRACT : JMod.PUBLIC : JMod.NONE;
		final JMethod copyBuilderMethod = typeDefinition.method(mods, this.builderClass.raw, this.settings.getNewCopyBuilderMethodName());
		final JTypeVar copyBuilderMethodTypeParam = copyBuilderMethod.generify(BuilderGenerator.PARENT_BUILDER_TYPE_PARAMETER_NAME);
		final CopyGenerator copyGenerator = this.pluginContext.createCopyGenerator(copyBuilderMethod, partial);
		copyBuilderMethod.type(this.builderClass.raw.narrow(copyBuilderMethodTypeParam));
		if (this.implement && !this.definedClass.isAbstract()) {
			copyBuilderMethod.body()._return(copyGenerator.generatePartialArgs(this.pluginContext.invoke(this.pluginContext.buildCopyMethodName).arg(JExpr._this())));
		}
		return copyBuilderMethod;
	}

	private JMethod generateConveniencePartialCopyMethod(final JMethod partialCopyOfMethod, final String methodName, final JExpression propertyTreeUseArg) {
		final JMethod conveniencePartialCopyMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.raw.narrow(Void.class), methodName);
		final JVar partialOtherParam = conveniencePartialCopyMethod.param(JMod.FINAL, this.definedClass, BuilderGenerator.OTHER_PARAM_NAME);
		final JVar propertyPathParam = conveniencePartialCopyMethod.param(JMod.FINAL, PropertyTree.class, PartialCopyGenerator.PROPERTY_TREE_PARAM_NAME);
		conveniencePartialCopyMethod.body()._return(JExpr.invoke(partialCopyOfMethod).arg(partialOtherParam).arg(propertyPathParam).arg(propertyTreeUseArg));
		return conveniencePartialCopyMethod;
	}

	final void generateCopyToMethod(final boolean partial) {
		if (this.implement) {
			final JDefinedClass typeDefinition = this.typeOutline.isInterface() && ((DefinedInterfaceOutline) this.typeOutline).getSupportInterface() != null ? ((DefinedInterfaceOutline) this.typeOutline).getSupportInterface() : this.definedClass;
			final JMethod copyToMethod = typeDefinition.method(JMod.PUBLIC, this.pluginContext.voidType, this.settings.getCopyToMethodName());
			final JTypeVar typeVar = copyToMethod.generify(BuilderGenerator.PARENT_BUILDER_TYPE_PARAMETER_NAME);
			final JVar otherParam = copyToMethod.param(JMod.FINAL, getBuilderDeclaration(getInheritanceRoot(this.typeOutline).getImplClass()).getBuilderClass().narrow(typeVar), BuilderGenerator.OTHER_PARAM_NAME);
			final CopyGenerator cloneGenerator = this.pluginContext.createCopyGenerator(copyToMethod, partial);

			final JBlock body = copyToMethod.body();
			final JVar otherRef;
			if (this.typeOutline.getSuperClass() != null) {
				body.add(cloneGenerator.generatePartialArgs(this.pluginContext.invoke(JExpr._super(), copyToMethod.name()).arg(otherParam)));
				otherRef = body.decl(JMod.FINAL, this.builderClass.raw.narrow(typeVar), BuilderGenerator.OTHER_VAR_NAME, JExpr.cast( this.builderClass.raw.narrow(typeVar), otherParam));
			} else {
				otherRef = otherParam;
			}

			for (final PropertyOutline fieldOutline : this.typeOutline.getDeclaredFields()) {
				final JFieldVar field = fieldOutline.getFieldVar();
				if (field != null && (field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					final JFieldRef targetField = JExpr.ref(otherRef, field);
					final JFieldRef sourceField = JExpr._this().ref(field);
					generateFieldCopyExpression(cloneGenerator, body, fieldOutline, targetField, sourceField);
				}
			}
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

		final JConditional ifStmt = constructor.body()._if(copyParam);
		final JBlock body = ifStmt._then();

		if (this.typeOutline.getSuperClass() == null) {
			body.assign(JExpr._this().ref(this.productField), JExpr._null());
			ifStmt._else().assign(JExpr._this().ref(this.productField), otherParam);
		}

		final JExpression newObjectVar = JExpr._this();
		for (final PropertyOutline fieldOutline : this.typeOutline.getDeclaredFields()) {
			final JFieldVar field = fieldOutline.getFieldVar();
			if (field != null) {
				if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = otherParam.ref(field);
					generateFieldCopyExpression(cloneGenerator, body, fieldOutline, newField, fieldRef);
				}
			}
		}

	}

	private void generateFieldCopyExpression(final CopyGenerator cloneGenerator, final JBlock body, final PropertyOutline property, final JFieldRef targetField, final JFieldRef sourceField) {
		final PropertyTreeVarGenerator treeVarGenerator = cloneGenerator.createPropertyTreeVarGenerator(body, property.getFieldName());
		final JType fieldType = property.getRawType();
		final JBlock currentBlock = treeVarGenerator.generateEnclosingBlock(body);
		if (fieldType.isReference()) {
			final JClass fieldClass = (JClass) fieldType;
			if (this.pluginContext.collectionClass.isAssignableFrom(fieldClass)) {
				final JClass elementType = fieldClass.getTypeParameters().get(0);
				final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);
				if (this.settings.isGeneratingNarrowCopy() && this.pluginContext.canInstantiate(elementType)) {
					final JClass childBuilderType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
					final JForEach forLoop = loop(currentBlock, sourceField, elementType, targetField, childBuilderType);
					forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), treeVarGenerator.generatePartialArgs(JExpr._new(childBuilderType).arg(JExpr._this()).arg(forLoop.var()).arg(JExpr.TRUE))));
				} else if (childBuilderOutline != null) {
					final JClass childBuilderType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
					final JForEach forLoop = loop(currentBlock, sourceField, elementType, targetField, childBuilderType);
					forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), generateRuntimeTypeExpression(forLoop.var(), treeVarGenerator)));
				} else if (this.pluginContext.partialCopyableInterface.isAssignableFrom(elementType)) {
					final JForEach forLoop = loop(currentBlock, sourceField, elementType, targetField, elementType);
					forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), this.pluginContext.castOnDemand(elementType, treeVarGenerator.generatePartialArgs(forLoop.var().invoke(this.pluginContext.copyMethodName)))));
				} else if (this.pluginContext.cloneableInterface.isAssignableFrom(elementType)) {
					final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(currentBlock, elementType);
					final JForEach forLoop = loop(maybeTryBlock, sourceField, elementType, targetField, elementType);
					forLoop.body().invoke(targetField, "add").arg(nullSafe(forLoop.var(), this.pluginContext.castOnDemand(elementType, forLoop.var().invoke(this.pluginContext.cloneMethodName))));
				} else {
					currentBlock.assign(targetField, nullSafe(sourceField, JExpr._new(this.pluginContext.arrayListClass.narrow(elementType)).arg(sourceField)));
				}
			} else {
				final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
				if (this.settings.isGeneratingNarrowCopy() && this.pluginContext.canInstantiate(fieldType)) {
					final JClass childBuilderType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
					currentBlock.assign(targetField, nullSafe(sourceField, treeVarGenerator.generatePartialArgs(JExpr._new(childBuilderType).arg(JExpr._this()).arg(sourceField).arg(JExpr.TRUE))));
				} else if (childBuilderOutline != null) {
					final JClass childBuilderType = childBuilderOutline.getBuilderClass().narrow(this.builderClass.type);
					currentBlock.assign(targetField, nullSafe(sourceField, generateRuntimeTypeExpression(sourceField, treeVarGenerator)));
				} else if (this.pluginContext.partialCopyableInterface.isAssignableFrom(fieldClass)) {
					currentBlock.assign(targetField, nullSafe(sourceField, this.pluginContext.castOnDemand(fieldType, treeVarGenerator.generatePartialArgs(sourceField.invoke(this.pluginContext.copyMethodName)))));
				} else if (this.pluginContext.cloneableInterface.isAssignableFrom(fieldClass)) {
					final JBlock maybeTryBlock = this.pluginContext.catchCloneNotSupported(currentBlock, fieldClass);
					maybeTryBlock.assign(targetField, nullSafe(sourceField, this.pluginContext.castOnDemand(fieldType, sourceField.invoke(this.pluginContext.cloneMethodName))));
				} else {
					currentBlock.assign(targetField, sourceField);
				}
			}
		} else {
			currentBlock.assign(targetField, sourceField);
		}
	}

	private JExpression generateRuntimeTypeExpression(final JExpression instanceVar, final PropertyTreeVarGenerator treeVarGenerator) {
		final JTypedInvocation getConstructorInvocation = this.pluginContext.invoke(instanceVar, this.settings.getNewCopyBuilderMethodName()).narrow(this.builderClass.type);
		return treeVarGenerator.generatePartialArgs(getConstructorInvocation);
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
			generateCopyOfMethod(false);
		}

		if (this.settings.isGeneratingPartialCopy()) {
			generateCopyToMethod(true);
			generateNewCopyBuilderMethod(true);
			if (this.implement && !this.definedClass.isAbstract()) {
				final JMethod partialCopyOfMethod = generateCopyOfMethod(true);
				generateConveniencePartialCopyMethod(partialCopyOfMethod, this.pluginContext.copyExceptMethodName, this.pluginContext.excludeConst);
				generateConveniencePartialCopyMethod(partialCopyOfMethod, this.pluginContext.copyOnlyMethodName, this.pluginContext.includeConst);
			}
		}
	}


	private void generateBuilderMemberOverrides(final TypeOutline superClass) {
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

	JForEach loop(final JBlock block, final JFieldRef source, final JType sourceElementType, final JAssignmentTarget target, final JType targetElementType) {
		final JConditional ifNull = block._if(source.eq(JExpr._null()));
		ifNull._then().assign(target, JExpr._null());
		ifNull._else().assign(target, JExpr._new(this.pluginContext.arrayListClass.narrow(targetElementType)));
		return ifNull._else().forEach(sourceElementType, BuilderGenerator.ITEM_VAR_NAME, source);
	}

	private void generateAddMethodJavadoc(final JMethod method, final JVar param) {
		final String propertyName = param.name();
		method.javadoc().append(MessageFormat.format(this.resources.getString("comment.addMethod"), propertyName))
				.addParam(param).append(MessageFormat.format(this.resources.getString("comment.addMethod.param"), propertyName));
	}

	private void generateWithMethodJavadoc(final JMethod method, final JVar param) {
		final String propertyName = param.name();
		method.javadoc().append(MessageFormat.format(this.resources.getString("comment.withMethod"), propertyName))
				.addParam(param).append(MessageFormat.format(this.resources.getString("comment.withMethod.param"), propertyName));
	}

	private void generateAddBuilderMethodJavadoc(final JMethod method, final PropertyOutline propertyOutline) {
		final String propertyName = propertyOutline.getFieldName();
		final String endMethodClassName = method.type().erasure().fullName();
		method.javadoc().append(MessageFormat.format(this.resources.getString("comment.addBuilderMethod"), propertyName, endMethodClassName))
				.addReturn().append(MessageFormat.format(this.resources.getString("comment.addBuilderMethod.return"), propertyName, endMethodClassName));
	}

	private void generateWithBuilderMethodJavadoc(final JMethod method, final PropertyOutline propertyOutline) {
		final String propertyName = propertyOutline.getFieldName();
		final String endMethodClassName = method.type().erasure().fullName();
		method.javadoc().append(MessageFormat.format(this.resources.getString("comment.withBuilderMethod"), propertyName, endMethodClassName))
				.addReturn().append(MessageFormat.format(this.resources.getString("comment.withBuilderMethod.return"), propertyName, endMethodClassName));
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

}
