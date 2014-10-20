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

import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.sun.codemodel.*;
import com.sun.tools.xjc.outline.FieldOutline;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.kscs.util.plugins.xjc.PluginUtil.nullSafe;

/**
 * Helper class to generate fluent builder classes in two steps
 */
public class BuilderGenerator {
	private static final Logger LOGGER = Logger.getLogger(BuilderGenerator.class.getName());
	public static final String ITEM_VAR_NAME = "_item";
	protected final ApiConstructs apiConstructs;
	protected final JDefinedClass definedClass;
	protected final JDefinedClass builderClass;
	protected final TypeOutline classOutline;
	protected final ImmutablePlugin immutablePlugin;
	protected final Map<String, BuilderOutline> builderOutlines;
	private final JTypeVar parentBuilderTypeParam;
	private final JClass builderType;
	private final JFieldVar parentBuilderField;
	private final JFieldVar productField;
	private final boolean graphClone;
	private final boolean narrow;
	private final boolean implement;

	private final ResourceBundle resources;


	BuilderGenerator(final ApiConstructs apiConstructs, final Map<String, BuilderOutline> builderOutlines, final BuilderOutline builderOutline, final boolean graphClone, final boolean narrow) {
		this.apiConstructs = apiConstructs;
		this.builderOutlines = builderOutlines;
		this.classOutline = builderOutline.getClassOutline();
		this.definedClass = this.classOutline.getImplClass();
		this.immutablePlugin = apiConstructs.findPlugin(ImmutablePlugin.class);
		this.builderClass = builderOutline.getDefinedBuilderClass();
		this.graphClone = graphClone;
		this.narrow = narrow;
		this.resources = ResourceBundle.getBundle(BuilderGenerator.class.getName());
		this.implement = !this.builderClass.isInterface();

		this.parentBuilderTypeParam = this.builderClass.generify("TParentBuilder");
		this.builderType = this.builderClass.narrow(this.parentBuilderTypeParam);

		if (builderOutline.getClassOutline().getSuperClass() == null) {
			final JMethod endMethod = this.builderClass.method(JMod.PUBLIC, this.parentBuilderTypeParam, "end");
			if (this.implement) {
				this.parentBuilderField = this.builderClass.field(JMod.PROTECTED | JMod.FINAL, this.parentBuilderTypeParam, "_parentBuilder");
				this.productField = this.builderClass.field(JMod.PROTECTED | JMod.FINAL, this.definedClass, "_product");

				endMethod.body()._return(JExpr._this().ref(this.parentBuilderField));
			} else {
				this.parentBuilderField = null;
				this.productField = null;
			}
		} else {
			this.parentBuilderField = null;
			this.productField = null;
		}

		if(this.implement) {
			generateCopyConstructor();
			if(this.graphClone) {
				generateGraphCopyConstructor();
			}
		}

	}

	protected void generateBuilderMember(final FieldOutline fieldOutline, final JBlock initBody, final JVar productParam) {
		final String propertyName = fieldOutline.getPropertyInfo().getName(true);
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);
		final JType fieldType = fieldOutline.getRawType();

		if (fieldOutline.getPropertyInfo().isCollection()) {
			if (fieldOutline.getRawType().isArray()) {
				generateArrayProperty(initBody, productParam, fieldOutline, fieldType.elementType(), this.builderType);
			} else {
				final List<JClass> typeParameters = ((JClass) fieldType).getTypeParameters();
				final JClass elementType = typeParameters.get(0);
				generateCollectionProperty(initBody, productParam, fieldOutline, elementType);
			}
		} else {
			generateSingularProperty(initBody, productParam, fieldOutline, propertyName);
		}
	}

	private void generateSingularProperty(final JBlock initBody, final JVar productParam, final FieldOutline fieldOutline, final String propertyName) {
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);
		final JType fieldType = fieldOutline.getRawType();
		final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
		if (childBuilderOutline == null) {
			final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withMethod.param(JMod.FINAL, fieldType, fieldName);
			if (this.implement) {
				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, fieldType, fieldName);
				withMethod.body().assign(JExpr._this().ref(builderField), param);
				withMethod.body()._return(JExpr._this());
				initBody.assign(productParam.ref(fieldName), JExpr._this().ref(builderField));
			}
		} else {
			final JClass elementType = (JClass) fieldType;
			final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderClass.narrow(this.parentBuilderTypeParam));
			final JClass builderWithMethodReturnType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderClass.narrow(this.parentBuilderTypeParam).wildcard());

			final JMethod withValueMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withValueMethod.param(JMod.FINAL, elementType, fieldName);
			final JMethod withBuilderMethod = this.builderClass.method(JMod.PUBLIC, builderWithMethodReturnType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);

			if (this.implement) {
				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, builderFieldElementType, fieldName);
				withValueMethod.body().assign(JExpr._this().ref(builderField), nullSafe(param, JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(param).arg(JExpr.FALSE)));
				withValueMethod.body()._return(JExpr._this());
				withBuilderMethod.body()._return(JExpr._this().ref(builderField).assign(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE)));

				initBody.assign(productParam.ref(fieldName), nullSafe(JExpr._this().ref(builderField), JExpr._this().ref(builderField).invoke(ApiConstructs.BUILD_METHOD_NAME)));
			}

		}
	}

	private void generateCollectionProperty(final JBlock initBody, final JVar productParam, final FieldOutline fieldOutline, final JClass elementType) {
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);
		final String propertyName = fieldOutline.getPropertyInfo().getName(true);
		final JType fieldType = fieldOutline.getRawType();

		final JClass listType = this.apiConstructs.listClass.narrow(elementType.wildcard());
		final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
		final JVar addListParam = addListMethod.param(JMod.FINAL, listType, fieldName);

		final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
		final JVar withListParam = withListMethod.param(JMod.FINAL, listType, fieldName);

		final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
		final JVar addVarargsParam = addVarargsMethod.varParam(elementType, fieldName);

		final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
		final JVar withVarargsParam = withVarargsMethod.varParam(elementType, fieldName);

		if (this.implement) {
			addVarargsMethod.body().invoke(addListMethod).arg(this.apiConstructs.asList(addVarargsParam));
			addVarargsMethod.body()._return(JExpr._this());
			withVarargsMethod.body().invoke(withListMethod).arg(this.apiConstructs.asList(withVarargsParam));
			withVarargsMethod.body()._return(JExpr._this());
		}

		final JVar collectionVar;

		final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);

		if (childBuilderOutline == null) {
			if (this.implement) {
				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, fieldType, fieldName);

				final JConditional addIfNull = addListMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)));
				addListMethod.body().invoke(JExpr._this().ref(builderField), ApiConstructs.ADD_ALL).arg(addListParam);
				addListMethod.body()._return(JExpr._this());

				final JConditional withIfNull = withListMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
				withIfNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
				withListMethod.body()._return(JExpr.invoke(addListMethod).arg(withListParam));

				initBody.assign(productParam.ref(fieldName), JExpr._this().ref(builderField));
			}
		} else {
			final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
			final JClass builderWithMethodReturnType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType.wildcard());
			final JClass builderArrayListClass = this.apiConstructs.arrayListClass.narrow(builderFieldElementType);
			final JClass builderListClass = this.apiConstructs.listClass.narrow(builderFieldElementType);

			final JMethod addMethod = this.builderClass.method(JMod.PUBLIC, builderWithMethodReturnType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);

			if (this.implement) {
				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, builderListClass, fieldName);
				final JConditional addIfNull = addMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
				final JVar childBuilderVar = addMethod.body().decl(JMod.FINAL, builderFieldElementType, fieldName + "Builder", JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE));
				addMethod.body().add(JExpr._this().ref(builderField).invoke("add").arg(childBuilderVar));
				addMethod.body()._return(childBuilderVar);

				final JConditional addListIfNull = addListMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
				addListIfNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
				final JForEach addListForEach = addListMethod.body().forEach(elementType, BuilderGenerator.ITEM_VAR_NAME, addListParam);
				addListForEach.body().add(JExpr._this().ref(builderField).invoke("add").arg(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(addListForEach.var()).arg(JExpr.FALSE)));
				addListMethod.body()._return(JExpr._this());

				final JConditional withListIfNull = withListMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
				withListIfNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
				withListMethod.body()._return(JExpr.invoke(addListMethod).arg(withListParam));

				final JConditional ifNull = initBody._if(JExpr._this().ref(builderField).ne(JExpr._null()));
				collectionVar = ifNull._then().decl(JMod.FINAL, this.apiConstructs.listClass.narrow(elementType), fieldName, JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)).arg(JExpr._this().ref(builderField).invoke("size")));
				final JForEach initForEach = ifNull._then().forEach(builderFieldElementType, ITEM_VAR_NAME, JExpr._this().ref(builderField));
				initForEach.body().add(collectionVar.invoke("add").arg(initForEach.var().invoke(ApiConstructs.BUILD_METHOD_NAME)));
				ifNull._then().assign(productParam.ref(fieldName), collectionVar);
			}
		}

		if (this.immutablePlugin != null && this.implement) {
			this.immutablePlugin.immutableInit(this.apiConstructs, initBody, productParam, fieldOutline);
		}
	}


	protected void generateBuilderMemberOverride(final FieldOutline superFieldOutline, final FieldOutline fieldOutline, final String superPropertyName) {
		final JType fieldType = fieldOutline.getRawType();
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);

		if (superFieldOutline.getPropertyInfo().isCollection()) {
			if (!fieldType.isArray()) {
				final JClass elementType = ((JClass) fieldType).getTypeParameters().get(0);
				final JClass listType = this.apiConstructs.listClass.narrow(elementType.wildcard());

				final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
				final JVar addListParam = addListMethod.param(JMod.FINAL, listType, fieldName);
				final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
				final JVar addVarargsParam = addVarargsMethod.varParam(elementType, fieldName);
				final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withListParam = withListMethod.param(JMod.FINAL, listType, fieldName);
				final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withVarargsParam = withVarargsMethod.varParam(elementType, fieldName);

				if (this.implement) {
					addListMethod.annotate(Override.class);
					addListMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addListParam);
					addListMethod.body()._return(JExpr._this());

					addVarargsMethod.annotate(Override.class);
					addVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addVarargsParam);
					addVarargsMethod.body()._return(JExpr._this());

					withListMethod.annotate(Override.class);
					withListMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
					withListMethod.body()._return(JExpr._this());

					withVarargsMethod.annotate(Override.class);
					withVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
					withVarargsMethod.body()._return(JExpr._this());
				}

				final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);
				if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().getImplClass().isAbstract()) {
					final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType.wildcard());
					final JMethod addMethod = this.builderClass.method(JMod.PUBLIC, builderFieldElementType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
					if (this.implement) {
						addMethod.annotate(Override.class);
						addMethod.body()._return(JExpr.cast(builderFieldElementType, JExpr._super().invoke(addMethod)));
					}
				}
			} else {
				final JType elementType = fieldType.elementType();
				final JClass listType = this.apiConstructs.listClass.narrow(elementType);

				final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withListParam = withListMethod.param(JMod.FINAL, listType, fieldName);

				final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				final JVar withVarargsParam = withVarargsMethod.varParam(((JClass) fieldType).getTypeParameters().get(0), fieldName);

				if (this.implement) {
					withListMethod.annotate(Override.class);
					withListMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
					withListMethod.body()._return(JExpr._this());

					withVarargsMethod.annotate(Override.class);
					withVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
					withVarargsMethod.body()._return(JExpr._this());
				}
			}
		} else {
			final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
			final JVar param = withMethod.param(JMod.FINAL, superFieldOutline.getRawType(), fieldName);

			if (this.implement) {
				withMethod.annotate(Override.class);
				withMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(param);
				withMethod.body()._return(JExpr._this());
			}

			final BuilderOutline childBuilderOutline = getBuilderDeclaration(fieldType);
			if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().getImplClass().isAbstract()) {
				final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType.wildcard());
				final JMethod addMethod = this.builderClass.method(JMod.PUBLIC, builderFieldElementType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				if (this.implement) {
					addMethod.body()._return(JExpr.cast(builderFieldElementType, JExpr._super().invoke(addMethod)));
				}
			}
		}
	}

	protected JDefinedClass generateExtendsClause(final BuilderOutline superClassBuilder) {
		return this.builderClass._extends(superClassBuilder.getDefinedBuilderClass().narrow(this.parentBuilderTypeParam));
	}

	protected void generateImplementsClause() {
		if(this.classOutline instanceof DefinedClassOutline) {
			final DefinedClassOutline definedClassOutline = (DefinedClassOutline) this.classOutline;
			final GroupInterfacePlugin groupInterfacePlugin = this.apiConstructs.findPlugin(GroupInterfacePlugin.class);
			if (groupInterfacePlugin != null) {
				for (final InterfaceOutline<?> interfaceOutline : groupInterfacePlugin.getGroupInterfacesForClass(this.apiConstructs, definedClassOutline.getClassOutline())) {
					this.builderClass._implements(PluginUtil.getInnerClass(interfaceOutline.getImplClass(), ApiConstructs.BUILDER_INTERFACE_NAME).narrow(this.parentBuilderTypeParam));
				}
			}
		}
	}

	protected JMethod generateBuildMethod(final JMethod initMethod) {
		final JMethod buildMethod = this.builderClass.method(JMod.PUBLIC, this.definedClass, ApiConstructs.BUILD_METHOD_NAME);
		if(this.implement) {
			if (this.definedClass.isAbstract()) {
				buildMethod.body()._return(JExpr.cast(this.definedClass, JExpr._this().ref("_product")));
			} else {
				final JConditional ifStatement = buildMethod.body()._if(JExpr._this().ref("_product").eq(JExpr._null()));
				ifStatement._then()._return((JExpr._this().invoke(initMethod).arg(JExpr._new(this.definedClass))));
				ifStatement._else()._return(JExpr.cast(this.definedClass, JExpr._this().ref("_product")));
			}
		}
		return buildMethod;
	}

	protected JMethod generateBuilderMethod() {
		final JMethod builderMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.narrow(Void.class), ApiConstructs.BUILDER_METHOD_NAME);
		builderMethod.body()._return(JExpr._new(this.builderClass.narrow(Void.class)).arg(JExpr._null()).arg(JExpr._null()).arg(JExpr.FALSE));

		final JMethod copyOfMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.narrow(Void.class), "copyOf");
		final JVar otherParam = copyOfMethod.param(JMod.FINAL, this.definedClass, "other");
		copyOfMethod.body()._return(JExpr._new((this.builderClass.narrow(Void.class))).arg(JExpr._null()).arg(otherParam).arg(JExpr.TRUE));

		if (this.graphClone) {
			final JMethod partialCopyOfMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.narrow(Void.class), "copyOf");
			final JVar partialOtherParam = partialCopyOfMethod.param(JMod.FINAL, this.definedClass, "other");
			final JVar propertyPathParam = partialCopyOfMethod.param(JMod.FINAL, PropertyTree.class, "propertyTree");
			final JVar graphUseParam = partialCopyOfMethod.param(JMod.FINAL, PropertyTreeUse.class, "propertyTreeUse");
			partialCopyOfMethod.body()._return(JExpr._new((this.builderClass.narrow(Void.class))).arg(JExpr._null()).arg(partialOtherParam).arg(JExpr.TRUE).arg(propertyPathParam).arg(graphUseParam));

			generateConveniencePartialCopyMethod(partialCopyOfMethod, "copyExcept", this.apiConstructs.excludeConst);
			generateConveniencePartialCopyMethod(partialCopyOfMethod, "copyOnly", this.apiConstructs.includeConst);
		}
		return builderMethod;
	}

	private JMethod generateConveniencePartialCopyMethod(final JMethod partialCopyOfMethod, final String methodName, final JExpression propertyTreeUseArg) {
		final JMethod conveniencePartialCopyMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.narrow(Void.class), methodName);
		final JVar partialOtherParam = conveniencePartialCopyMethod.param(JMod.FINAL, this.definedClass, "other");
		final JVar propertyPathParam = conveniencePartialCopyMethod.param(JMod.FINAL, PropertyTree.class, "propertyTree");
		conveniencePartialCopyMethod.body()._return(JExpr.invoke(partialCopyOfMethod).arg(partialOtherParam).arg(propertyPathParam).arg(propertyTreeUseArg));
		return conveniencePartialCopyMethod;
	}

	final void generateCopyConstructor() {
		final JMethod constructor = this.builderClass.constructor(this.builderClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar parentBuilderParam = constructor.param(JMod.FINAL, this.parentBuilderTypeParam, "parentBuilder");
		final JVar otherParam = constructor.param(JMod.FINAL, this.classOutline.getImplClass(), "other");
		final JVar copyParam = constructor.param(JMod.FINAL, this.apiConstructs.codeModel.BOOLEAN, "copy");

		if (this.classOutline.getSuperClass() != null) {
			constructor.body().invoke("super").arg(parentBuilderParam).arg(otherParam).arg(copyParam);
		} else {
			constructor.body().assign(JExpr._this().ref(this.parentBuilderField), parentBuilderParam);
		}

		final JConditional ifStmt = constructor.body()._if(copyParam);
		final JBlock outer = ifStmt._then();

		if (this.classOutline.getSuperClass() == null) {
			outer.assign(JExpr._this().ref(this.productField), JExpr._null());
			ifStmt._else().assign(JExpr._this().ref(this.productField), otherParam);
		}

		final JBlock body;
		final JTryBlock tryBlock;

		final boolean mustCatch = mustCatch(this.apiConstructs, this.classOutline, new Predicate<JClass>() {
			@Override
			public boolean matches(final JClass fieldType) {
				return (!BuilderGenerator.this.apiConstructs.canInstantiate(fieldType)) && BuilderGenerator.this.apiConstructs.cloneThrows(fieldType, false);
			}
		});

		if (mustCatch) {
			tryBlock = outer._try();
			body = tryBlock.body();
		} else {
			tryBlock = null;
			body = outer.block();
		}

		final JExpression newObjectVar = JExpr._this();
		for (final FieldOutline fieldOutline : this.classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if (field.type().isReference()) {
					final JClass fieldType = (JClass) field.type();
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = otherParam.ref(field);
					if (this.apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
						final JClass elementType = fieldType.getTypeParameters().get(0);
						final BuilderOutline childBuilderOutline = this.builderOutlines.get(elementType.fullName());
						if (this.narrow && this.apiConstructs.canInstantiate(elementType)) {
							final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
							final JForEach forLoop = loop(body, fieldRef, elementType, newField, childBuilderType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), JExpr._new(childBuilderType).arg(JExpr._this()).arg(forLoop.var()).arg(JExpr.TRUE)));
						} else if (childBuilderOutline != null) {
							final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
							final JForEach forLoop = loop(body, fieldRef, elementType, newField, childBuilderType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(fieldRef, generateRuntimeTypeExpression(childBuilderType, forLoop.var(), null)));
						} else if (this.apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
							final JForEach forLoop = loop(body, fieldRef, elementType, newField, elementType);
							forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.apiConstructs.castOnDemand(elementType, forLoop.var().invoke("clone"))));
						} else {
							body.assign(newField, nullSafe(fieldRef, JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)).arg(fieldRef)));
						}

					} else {
						final BuilderOutline childBuilderOutline = this.builderOutlines.get(fieldType.fullName());
						if (this.narrow && this.apiConstructs.canInstantiate(fieldType)) {
							final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
							body.assign(newField, nullSafe(fieldRef, JExpr._new(childBuilderType).arg(JExpr._this()).arg(fieldRef).arg(JExpr.TRUE)));
						} else if (childBuilderOutline != null) {
							final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
							body.assign(newField, nullSafe(fieldRef, generateRuntimeTypeExpression(childBuilderType, fieldRef, null)));
						} else if (this.apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
							body.assign(newField, nullSafe(fieldRef, this.apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone"))));
						} else {
							body.assign(newField, fieldRef);
						}
					}
				} else {
					final JPrimitiveType fieldType = (JPrimitiveType) field.type();
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = otherParam.ref(field);
					body.assign(newField, fieldRef);
				}
			}
		}

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(this.apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(this.apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}
	}

	private JInvocation generateRuntimeTypeExpression(final JClass childBuilderType, final JExpression instanceVar, final JVar clonePathVar) {
		final JInvocation getConstructorInvocation = this.apiConstructs.builderUtilitiesClass.staticInvoke(ApiConstructs.GET_BUILDER)
				.arg(childBuilderType.dotclass()).arg(instanceVar).arg(JExpr._this()).arg(instanceVar).arg(JExpr.TRUE);
		if (clonePathVar != null) {
			getConstructorInvocation.arg(clonePathVar);
		}
		return getConstructorInvocation;
	}

	final void generateGraphCopyConstructor() {
		final JMethod constructor = this.builderClass.constructor(this.builderClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar parentBuilderParam = constructor.param(JMod.FINAL, this.parentBuilderTypeParam, "parentBuilder");
		final JVar otherParam = constructor.param(JMod.FINAL, this.classOutline.getImplClass(), "other");
		final JVar copyParam = constructor.param(JMod.FINAL, this.apiConstructs.codeModel.BOOLEAN, "copy");
		final PartialCopyGenerator cloneGenerator = new PartialCopyGenerator(this.apiConstructs, constructor);

		if (this.classOutline.getSuperClass() != null) {
			constructor.body().invoke("super").arg(parentBuilderParam).arg(otherParam).arg(copyParam);
		} else {
			constructor.body().assign(JExpr._this().ref(this.parentBuilderField), parentBuilderParam);
		}

		final JConditional ifStmt = constructor.body()._if(copyParam);
		final JBlock outer = ifStmt._then();

		if (this.classOutline.getSuperClass() == null) {
			outer.assign(JExpr._this().ref(this.productField), JExpr._null());
			ifStmt._else().assign(JExpr._this().ref(this.productField), otherParam);
		}

		final JBlock body;
		final JTryBlock tryBlock;

		final boolean mustCatch = mustCatch(this.apiConstructs, this.classOutline, new Predicate<JClass>() {
			@Override
			public boolean matches(final JClass fieldType) {
				return (!BuilderGenerator.this.apiConstructs.canInstantiate(fieldType)) && (!BuilderGenerator.this.apiConstructs.partialCopyableInterface.isAssignableFrom(fieldType)) && BuilderGenerator.this.apiConstructs.cloneThrows(fieldType, false);
			}
		});

		if (!mustCatch) {
			tryBlock = null;
			body = outer;
		} else {
			tryBlock = outer._try();
			body = tryBlock.body();
		}
		JBlock currentBlock;
		final JExpression newObjectVar = JExpr._this();
		for (final FieldOutline fieldOutline : this.classOutline.getDeclaredFields()) {
			final JFieldVar field = PluginUtil.getDeclaredField(fieldOutline);
			if (field != null) {
				if ((field.mods().getValue() & (JMod.FINAL | JMod.STATIC)) == 0) {
					final JFieldRef newField = JExpr.ref(newObjectVar, field);
					final JFieldRef fieldRef = otherParam.ref(field);
					final JVar fieldPathVar = cloneGenerator.generatePropertyTreeVarDeclaration(body, field);
					final JConditional ifHasClonePath = body._if(cloneGenerator.getIncludeCondition(fieldPathVar));
					currentBlock = ifHasClonePath._then();
					if (field.type().isReference()) {
						final JClass fieldType = (JClass) field.type();
						if (this.apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
							final JClass elementType = fieldType.getTypeParameters().get(0);
							final BuilderOutline childBuilderOutline = this.builderOutlines.get(elementType.fullName());
							if (this.narrow && this.apiConstructs.canInstantiate(elementType)) {
								final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
								final JForEach forLoop = loop(currentBlock, fieldRef, elementType, newField, childBuilderType);
								forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), JExpr._new(childBuilderType).arg(JExpr._this()).arg(forLoop.var()).arg(JExpr.TRUE).arg(fieldPathVar).arg(cloneGenerator.getIncludeParam())));
							} else if (childBuilderOutline != null) {
								final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
								final JForEach forLoop = loop(currentBlock, fieldRef, elementType, newField, childBuilderType);
								forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), generateRuntimeTypeExpression(childBuilderType, forLoop.var(), fieldPathVar)));
							} else if (this.apiConstructs.partialCopyableInterface.isAssignableFrom(elementType)) {
								final JForEach forLoop = loop(currentBlock, fieldRef, elementType, newField, elementType);
								forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.apiConstructs.castOnDemand(elementType, forLoop.var().invoke("clone").arg(fieldPathVar).arg(cloneGenerator.getIncludeParam()))));
							} else if (this.apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
								final JForEach forLoop = loop(currentBlock, fieldRef, elementType, newField, elementType);
								forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.apiConstructs.castOnDemand(elementType, forLoop.var().invoke("clone"))));
							} else {
								currentBlock.assign(newField, nullSafe(fieldRef, JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)).arg(fieldRef)));
							}

						} else {
							final BuilderOutline childBuilderOutline = this.builderOutlines.get(fieldType.fullName());
							if (this.narrow && this.apiConstructs.canInstantiate(fieldType)) {
								final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
								currentBlock.assign(newField, nullSafe(fieldRef, JExpr._new(childBuilderType).arg(JExpr._this()).arg(fieldRef).arg(JExpr.TRUE).arg(fieldPathVar).arg(cloneGenerator.getIncludeParam())));
							} else if (childBuilderOutline != null) {
								final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
								currentBlock.assign(newField, nullSafe(fieldRef, generateRuntimeTypeExpression(childBuilderType, fieldRef, fieldPathVar)));
							} else if (this.apiConstructs.partialCopyableInterface.isAssignableFrom(fieldType)) {
								currentBlock.assign(newField, nullSafe(fieldRef, this.apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone").arg(fieldPathVar).arg(cloneGenerator.getIncludeParam()))));
							} else if (this.apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
								currentBlock.assign(newField, nullSafe(fieldRef, this.apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone"))));
							} else {
								currentBlock.assign(newField, fieldRef);
							}
						}
					} else {
						currentBlock.assign(newField, fieldRef);
					}
				}
			}
		}

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(this.apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(this.apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}

	}



	private boolean mustCatch(final ApiConstructs apiConstructs, final TypeOutline classOutline, final Predicate<JClass> fieldTypePredicate) {
		final JDefinedClass definedClass = classOutline.getImplClass();
		for (final JFieldVar field : definedClass.fields().values()) {
			if (field.type().isReference()) {
				JClass fieldType = (JClass) field.type();
				if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
					fieldType = fieldType.getTypeParameters().get(0);
				}
				if (fieldTypePredicate.matches(fieldType)) {
					return true;
				}
			}
		}
		return false;
	}

	public void buildProperties() {
		final TypeOutline superClass = this.classOutline.getSuperClass();

		final JMethod initMethod;
		final JVar productParam;
		final JBlock initBody;
		if(this.implement) {
			initMethod = this.builderClass.method(JMod.PROTECTED, this.definedClass, ApiConstructs.INIT_METHOD_NAME);
			final JTypeVar typeVar = initMethod.generify("P", this.definedClass);
			initMethod.type(typeVar);
			productParam = initMethod.param(JMod.FINAL, typeVar, ApiConstructs.PRODUCT_INSTANCE_NAME);
			initBody = initMethod.body();
		} else {
			initMethod = null;
			initBody = null;
			productParam = null;
		}

		if(this.classOutline.getDeclaredFields() != null) {
			for (final FieldOutline fieldOutline : this.classOutline.getDeclaredFields()) {
				if (PluginUtil.hasGetter(this.definedClass, fieldOutline)) {
					generateBuilderMember(fieldOutline, initBody, productParam);
				}
			}
		} else {
			BuilderGenerator.LOGGER.warning("Interface " + this.classOutline.getImplClass().name() + " has no implementation.");
		}

		if (superClass != null) {
			generateExtendsClause(getBuilderDeclaration(superClass.getImplClass()));
			if(this.implement) initBody._return(JExpr._super().invoke(initMethod).arg(productParam));
			generateBuilderMemberOverrides(superClass);
		} else if(this.implement) {
			initBody._return(productParam);
		}

		generateImplementsClause();
		generateBuildMethod(initMethod);
		if (this.implement && !this.definedClass.isAbstract()) {
			generateBuilderMethod();
		}

	}


	private void generateBuilderMemberOverrides(final TypeOutline superClass) {
		final JDefinedClass definedSuperClass = superClass.getImplClass();
		for (final FieldOutline superFieldOutline : superClass.getDeclaredFields()) {
			if (PluginUtil.hasGetter(definedSuperClass, superFieldOutline)) {
				final String superPropertyName = superFieldOutline.getPropertyInfo().getName(true);
				generateBuilderMemberOverride(superFieldOutline, superFieldOutline, superPropertyName);
			}
		}

		if (superClass.getSuperClass() != null) {
			generateBuilderMemberOverrides(superClass.getSuperClass());
		}
	}

	protected BuilderOutline getBuilderDeclaration(final JType type) {
		return this.builderOutlines.get(type.fullName());
	}

	protected void generateArrayProperty(final JBlock initBody, final JVar productParam, final FieldOutline fieldOutline, final JType elementType, final JType builderType) {
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);
		final String propertyName = fieldOutline.getPropertyInfo().getName(true);
		final JType fieldType = fieldOutline.getRawType();

		final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
		final JVar withVarargsParam = withVarargsMethod.varParam(elementType, fieldName);
		if(this.implement) {
			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, fieldType, fieldName, JExpr._null());
			withVarargsMethod.body().assign(JExpr._this().ref(builderField), withVarargsParam);
			withVarargsMethod.body()._return(JExpr._this());

			initBody.assign(productParam.ref(fieldName), JExpr._this().ref(builderField));
		}
	}

	public JForEach loop(final JBlock block, final JFieldRef source, final JType sourceElementType, final JAssignmentTarget target, final JType targetElementType) {
		final JConditional ifNull = block._if(source.eq(JExpr._null()));
		ifNull._then().assign(target, JExpr._null());
		ifNull._else().assign(target, JExpr._new(this.apiConstructs.arrayListClass.narrow(targetElementType)));
		return ifNull._else().forEach(sourceElementType, BuilderGenerator.ITEM_VAR_NAME, source);
	}

	private static interface Predicate<T> {
		boolean matches(final T arg);
	}
}
