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

import com.kscs.util.jaxb.PropertyPath;
import com.sun.codemodel.*;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.kscs.util.plugins.xjc.PluginUtil.nullSafe;

/**
 * Helper class to generate fluent builder classes in two steps
 */
public class ChainedBuilderGenerator extends BuilderGenerator {
	private static final Logger LOGGER = Logger.getLogger(ChainedBuilderGenerator.class.getName());
	private final JTypeVar parentBuilderTypeParam;
	private final JClass builderType;
	private final boolean narrow = true;
	private final boolean partialClone = true;
	private final JFieldVar parentBuilderField;
	private final JFieldVar productField;

	private final ResourceBundle resources;


	ChainedBuilderGenerator(final ApiConstructs apiConstructs, final Map<String, BuilderOutline> builderOutlines, final BuilderOutline builderOutline) {
		super(apiConstructs, builderOutlines, builderOutline);
		this.resources = ResourceBundle.getBundle(ChainedBuilderGenerator.class.getName());

		this.parentBuilderTypeParam = this.builderClass.generify("TParentBuilder");
		this.builderType = this.builderClass.narrow(this.parentBuilderTypeParam);

		if (builderOutline.getClassOutline().getSuperClass() == null) {
			this.parentBuilderField = this.builderClass.field(JMod.PROTECTED | JMod.FINAL, this.parentBuilderTypeParam, "_parentBuilder");
			this.productField = this.builderClass.field(JMod.PROTECTED | JMod.FINAL, this.definedClass, "_product");

			final JMethod endMethod = this.builderClass.method(JMod.PUBLIC, this.parentBuilderTypeParam, "end");
			endMethod.body()._return(JExpr._this().ref(this.parentBuilderField));

		} else {
			this.parentBuilderField = null;
			this.productField = null;
		}

		generateCopyConstructor();
		if (this.partialClone) {
			generatePartialCopyConstructor();
		}

	}

	private String getMessage(final String key, final Object... params) {
		return MessageFormat.format(this.resources.getString(key), params);
	}

	protected void generateBuilderMember(final FieldOutline fieldOutline, final JFieldVar declaredField, final JBlock initBody, final JVar productParam) {
		final String propertyName = fieldOutline.getPropertyInfo().getName(true);
		if (fieldOutline.getPropertyInfo().isCollection()) {
			if (declaredField.type().isArray()) {
				generateArrayProperty(initBody, productParam, declaredField, propertyName, declaredField.type().elementType(), this.builderType);
			} else {
				final List<JClass> typeParameters = ((JClass) declaredField.type()).getTypeParameters();
				final JClass elementType = typeParameters.get(0);
				generateCollectionProperty(initBody, productParam, declaredField, propertyName, elementType);
			}
		} else {
			generateSingularProperty(initBody, productParam, declaredField, propertyName);
		}
	}

	private void generateSingularProperty(final JBlock initBody, final JVar productParam, final JFieldVar declaredField, final String propertyName) {
		final BuilderOutline childBuilderOutline = getBuilderDeclaration(declaredField.type());
		if (childBuilderOutline == null) {
			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name());
			final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withMethod.param(JMod.FINAL, declaredField.type(), declaredField.name());
			withMethod.body().assign(JExpr._this().ref(builderField), param);
			withMethod.body()._return(JExpr._this());
			initBody.assign(productParam.ref(declaredField), JExpr._this().ref(builderField));
		} else {
			final JClass elementType = (JClass) declaredField.type();
			final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderClass.narrow(this.parentBuilderTypeParam));
			final JClass builderWithMethodReturnType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderClass.narrow(this.parentBuilderTypeParam).wildcard());
			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, builderFieldElementType, declaredField.name());

			final JMethod withValueMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withValueMethod.param(JMod.FINAL, elementType, declaredField.name());
			withValueMethod.body().assign(JExpr._this().ref(builderField), JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(param).arg(JExpr.FALSE));
			withValueMethod.body()._return(JExpr._this());

			final JMethod withBuilderMethod = this.builderClass.method(JMod.PUBLIC, builderWithMethodReturnType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			withBuilderMethod.body()._return(JExpr._this().ref(builderField).assign(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE)));

			initBody.assign(productParam.ref(declaredField), nullSafe(JExpr._this().ref(builderField), JExpr._this().ref(builderField).invoke(ApiConstructs.BUILD_METHOD_NAME)));
		}
	}

	private void generateCollectionProperty(final JBlock initBody, final JVar productParam, final JFieldVar declaredField, final String propertyName, final JClass elementType) {
		final JClass listType = this.apiConstructs.listClass.narrow(elementType.wildcard());
		final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
		final JVar addListParam = addListMethod.param(JMod.FINAL, listType, declaredField.name());

		final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
		final JVar withListParam = withListMethod.param(JMod.FINAL, listType, declaredField.name());

		final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
		final JVar addVarargsParam = addVarargsMethod.varParam(elementType, declaredField.name());
		addVarargsMethod.body().invoke(addListMethod).arg(this.apiConstructs.asList(addVarargsParam));
		addVarargsMethod.body()._return(JExpr._this());

		final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
		final JVar withVarargsParam = withVarargsMethod.varParam(elementType, declaredField.name());
		withVarargsMethod.body().invoke(withListMethod).arg(this.apiConstructs.asList(withVarargsParam));
		withVarargsMethod.body()._return(JExpr._this());

		final JVar collectionVar;

		final BuilderOutline childBuilderOutline = getBuilderDeclaration(elementType);

		if (childBuilderOutline == null) {
			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name());
			JConditional ifNull = addListMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
			ifNull._then().assign(JExpr._this().ref(builderField), JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)));
			addListMethod.body().invoke(JExpr._this().ref(builderField), ApiConstructs.ADD_ALL).arg(addListParam);
			addListMethod.body()._return(JExpr._this());

			ifNull = withListMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
			ifNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
			withListMethod.body()._return(JExpr.invoke(addListMethod).arg(withListParam));

			initBody.assign(productParam.ref(declaredField), JExpr._this().ref(builderField));
		} else {
			final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
			final JClass builderWithMethodReturnType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType.wildcard());
			final JClass builderArrayListClass = this.apiConstructs.arrayListClass.narrow(builderFieldElementType);
			final JClass builderListClass = this.apiConstructs.listClass.narrow(builderFieldElementType);
			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, builderListClass, declaredField.name());

			final JMethod addMethod = this.builderClass.method(JMod.PUBLIC, builderWithMethodReturnType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
			JConditional ifNull = addMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
			ifNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
			final JVar childBuilderVar = addMethod.body().decl(JMod.FINAL, builderFieldElementType, declaredField.name() + "Builder", JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(JExpr._null()).arg(JExpr.FALSE));
			addMethod.body().add(JExpr._this().ref(builderField).invoke("add").arg(childBuilderVar));
			addMethod.body()._return(childBuilderVar);

			ifNull = addListMethod.body()._if(JExpr._this().ref(builderField).eq(JExpr._null()));
			ifNull._then().assign(JExpr._this().ref(builderField), JExpr._new(builderArrayListClass));
			final JForEach addListForEach = addListMethod.body().forEach(elementType, "item", addListParam);
			addListForEach.body().add(JExpr._this().ref(builderField).invoke("add").arg(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(addListForEach.var()).arg(JExpr.FALSE)));
			addListMethod.body()._return(JExpr._this());

			ifNull = withListMethod.body()._if(JExpr._this().ref(builderField).ne(JExpr._null()));
			ifNull._then().add(JExpr._this().ref(builderField).invoke("clear"));
			withListMethod.body()._return(JExpr.invoke(addListMethod).arg(withListParam));

			ifNull = initBody._if(JExpr._this().ref(builderField).ne(JExpr._null()));
			collectionVar = ifNull._then().decl(JMod.FINAL, this.apiConstructs.listClass.narrow(elementType), declaredField.name(), JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)).arg(JExpr._this().ref(builderField).invoke("size")));
			final JForEach initForEach = ifNull._then().forEach(builderFieldElementType, "item", JExpr._this().ref(builderField));
			initForEach.body().add(collectionVar.invoke("add").arg(initForEach.var().invoke(ApiConstructs.BUILD_METHOD_NAME)));
			ifNull._then().assign(productParam.ref(declaredField), collectionVar);
		}

		if (this.immutablePlugin != null) {
			this.immutablePlugin.immutableInit(this.apiConstructs, initBody, productParam, declaredField);
		}
	}


	protected void generateBuilderMemberOverride(final FieldOutline superFieldOutline, final JFieldVar declaredSuperField, final String superPropertyName) {
		if (superFieldOutline.getPropertyInfo().isCollection()) {
			if (!declaredSuperField.type().isArray()) {
				final JClass elementType = ((JClass) declaredSuperField.type()).getTypeParameters().get(0);
				final JClass listType = this.apiConstructs.listClass.narrow(elementType.wildcard());

				final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
				addListMethod.annotate(Override.class);
				final JVar addListParam = addListMethod.param(JMod.FINAL, listType, declaredSuperField.name());
				addListMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addListParam);
				addListMethod.body()._return(JExpr._this());

				final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
				addVarargsMethod.annotate(Override.class);
				final JVar addVarargsParam = addVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
				addVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addVarargsParam);
				addVarargsMethod.body()._return(JExpr._this());

				final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				withListMethod.annotate(Override.class);
				final JVar withListParam = withListMethod.param(JMod.FINAL, listType, declaredSuperField.name());
				withListMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
				withListMethod.body()._return(JExpr._this());

				final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				withVarargsMethod.annotate(Override.class);
				final JVar withVarargsParam = withVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
				withVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
				withVarargsMethod.body()._return(JExpr._this());
				final BuilderOutline childBuilderOutline = getBuilderDeclaration(declaredSuperField.type());
				if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().implClass.isAbstract()) {
					final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType.wildcard());
					final JMethod addMethod = this.builderClass.method(JMod.PUBLIC, builderFieldElementType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
					addMethod.body()._return(JExpr.cast(builderFieldElementType, JExpr._super().invoke(addMethod)));
				}
			} else {
				final JType elementType = declaredSuperField.type().elementType();
				final JClass listType = this.apiConstructs.listClass.narrow(elementType);

				final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				withListMethod.annotate(Override.class);
				final JVar withListParam = withListMethod.param(JMod.FINAL, listType, declaredSuperField.name());
				withListMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
				withListMethod.body()._return(JExpr._this());

				final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				withVarargsMethod.annotate(Override.class);
				final JVar withVarargsParam = withVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
				withVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
				withVarargsMethod.body()._return(JExpr._this());
			}
		} else {
			final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
			withMethod.annotate(Override.class);
			final JVar param = withMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
			withMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(param);
			withMethod.body()._return(JExpr._this());

			final BuilderOutline childBuilderOutline = getBuilderDeclaration(declaredSuperField.type());
			if (childBuilderOutline != null && !childBuilderOutline.getClassOutline().implClass.isAbstract()) {
				final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType.wildcard());
				final JMethod addMethod = this.builderClass.method(JMod.PUBLIC, builderFieldElementType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				addMethod.body()._return(JExpr.cast(builderFieldElementType, JExpr._super().invoke(addMethod)));
			}
		}
	}

	@Override
	protected JDefinedClass generateExtendsClause(final BuilderOutline superClassBuilder) {
		return this.builderClass._extends(superClassBuilder.getDefinedBuilderClass().narrow(this.parentBuilderTypeParam));
	}

	@Override
	protected JMethod generateBuildMethod(final JMethod initMethod) {
		final JMethod buildMethod = this.builderClass.method(JMod.PUBLIC, this.definedClass, ApiConstructs.BUILD_METHOD_NAME);
		if(this.definedClass.isAbstract()) {
			buildMethod.body()._return(JExpr.cast(this.definedClass, JExpr._this().ref("_product")));
		} else {
			final JConditional ifStatement = buildMethod.body()._if(JExpr._this().ref("_product").eq(JExpr._null()));
			ifStatement._then()._return((JExpr._this().invoke(initMethod).arg(JExpr._new(this.definedClass))));
			ifStatement._else()._return(JExpr.cast(this.definedClass, JExpr._this().ref("_product")));
		}
		return buildMethod;
	}

	@Override
	protected JMethod generateBuilderMethod() {
		final JMethod builderMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.narrow(Void.class), ApiConstructs.BUILDER_METHOD_NAME);
		builderMethod.body()._return(JExpr._new(this.builderClass.narrow(Void.class)).arg(JExpr._null()).arg(JExpr._null()).arg(JExpr.FALSE));

		final JMethod copyOfMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.narrow(Void.class), "copyOf");
		final JVar otherParam = copyOfMethod.param(JMod.FINAL, this.definedClass, "other");
		copyOfMethod.body()._return(JExpr._new((this.builderClass.narrow(Void.class))).arg(JExpr._null()).arg(otherParam).arg(JExpr.TRUE));

		if (this.partialClone) {
			final JMethod partialCopyOfMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.narrow(Void.class), "copyOf");
			final JVar partialOtherParam = partialCopyOfMethod.param(JMod.FINAL, this.definedClass, "other");
			final JVar propertyPathParam = partialCopyOfMethod.param(JMod.FINAL, this.apiConstructs.codeModel.ref(PropertyPath.class), "propertyPath");
			partialCopyOfMethod.body()._return(JExpr._new((this.builderClass.narrow(Void.class))).arg(JExpr._null()).arg(partialOtherParam).arg(JExpr.TRUE).arg(propertyPathParam));
		}
		return builderMethod;
	}

	void generateCopyConstructor() {
		final JMethod constructor = this.builderClass.constructor(this.builderClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar parentBuilderParam = constructor.param(JMod.FINAL, this.parentBuilderTypeParam, "parentBuilder");
		final JVar otherParam = constructor.param(JMod.FINAL, this.classOutline.implClass, "other");
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
				return (!ChainedBuilderGenerator.this.apiConstructs.canInstantiate(fieldType)) && ChainedBuilderGenerator.this.apiConstructs.cloneThrows(fieldType, false);
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
				}
			}
		}

		if (tryBlock != null) {
			final JCatchBlock catchBlock = tryBlock._catch(this.apiConstructs.codeModel.ref(CloneNotSupportedException.class));
			final JVar exceptionVar = catchBlock.param("cnse");
			catchBlock.body()._throw(JExpr._new(this.apiConstructs.codeModel.ref(RuntimeException.class)).arg(exceptionVar));
		}
	}

	void generatePartialCopyConstructor() {
		final JMethod constructor = this.builderClass.constructor(this.builderClass.isAbstract() ? JMod.PROTECTED : JMod.PUBLIC);
		final JVar parentBuilderParam = constructor.param(JMod.FINAL, this.parentBuilderTypeParam, "parentBuilder");
		final JVar otherParam = constructor.param(JMod.FINAL, this.classOutline.implClass, "other");
		final JVar copyParam = constructor.param(JMod.FINAL, this.apiConstructs.codeModel.BOOLEAN, "copy");
		final JVar pathParam = constructor.param(JMod.FINAL, PropertyPath.class, "propertyPath");

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
				return (!ChainedBuilderGenerator.this.apiConstructs.canInstantiate(fieldType)) && (!ChainedBuilderGenerator.this.apiConstructs.pathCloneableInterface.isAssignableFrom(fieldType)) && ChainedBuilderGenerator.this.apiConstructs.cloneThrows(fieldType, false);
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
					final JVar fieldPathVar = body.decl(JMod.FINAL, this.apiConstructs.codeModel._ref(PropertyPath.class), field.name() + "ClonePath", pathParam.invoke("get").arg(JExpr.lit(field.name())));
					final JExpression includesInvoke = fieldPathVar.invoke("includes");
					final JConditional ifHasClonePath = body._if(includesInvoke);
					currentBlock = ifHasClonePath._then();
					if (field.type().isReference()) {
						final JClass fieldType = (JClass) field.type();
						if (this.apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
							final JClass elementType = fieldType.getTypeParameters().get(0);
							final BuilderOutline childBuilderOutline = this.builderOutlines.get(elementType.fullName());
							if (this.narrow && this.apiConstructs.canInstantiate(elementType)) {
								final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
								final JForEach forLoop = loop(currentBlock, fieldRef, elementType, newField, childBuilderType);
								forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), JExpr._new(childBuilderType).arg(JExpr._this()).arg(forLoop.var()).arg(JExpr.TRUE).arg(fieldPathVar)));
							} else if (childBuilderOutline != null) {
								final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
								final JForEach forLoop = loop(currentBlock, fieldRef, elementType, newField, childBuilderType);
								forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), generateRuntimeTypeExpression(childBuilderType, forLoop.var(), fieldPathVar)));
							} else if (this.apiConstructs.pathCloneableInterface.isAssignableFrom(elementType)) {
								final JForEach forLoop = loop(currentBlock, fieldRef, elementType, newField, elementType);
								forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.apiConstructs.castOnDemand(elementType, forLoop.var().invoke("clone").arg(fieldPathVar))));
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
								body.assign(newField, nullSafe(fieldRef, JExpr._new(childBuilderType).arg(JExpr._this()).arg(fieldRef).arg(JExpr.TRUE).arg(fieldPathVar)));
							} else if(childBuilderOutline != null) {
								final JClass childBuilderType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
								body.assign(newField, nullSafe(fieldRef, generateRuntimeTypeExpression(childBuilderType, fieldRef, fieldPathVar)));
							} else if (this.apiConstructs.pathCloneableInterface.isAssignableFrom(fieldType)) {
								body.assign(newField, nullSafe(fieldRef, this.apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone").arg(fieldPathVar))));
							} else if (this.apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
								body.assign(newField, nullSafe(fieldRef, this.apiConstructs.castOnDemand(fieldType, fieldRef.invoke("clone"))));
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


	private JInvocation generateRuntimeTypeExpression(final JClass childBuilderType, final JExpression instanceVar, final JVar clonePathVar) {
		final JInvocation getConstructorInvocation = this.apiConstructs.builderUtilitiesClass.staticInvoke(ApiConstructs.GET_BUILDER)
				.arg(childBuilderType.dotclass()).arg(instanceVar).arg(JExpr._this()).arg(instanceVar).arg(JExpr.TRUE);
		if (clonePathVar != null) {
			getConstructorInvocation.arg(clonePathVar);
		}
		return getConstructorInvocation;
	}

	private boolean mustCatch(final ApiConstructs apiConstructs, final ClassOutline classOutline, final Predicate<JClass> fieldTypePredicate) {
		final JDefinedClass definedClass = classOutline.implClass;
		for (final JFieldVar field : definedClass.fields().values()) {
			if (field.type().isReference()) {
				JClass fieldType = (JClass) field.type();
				if (apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
					fieldType = fieldType.getTypeParameters().get(0);
				}
				if (fieldTypePredicate.matches(fieldType)) {
					return true;
				}
				;
			}
		}
		return false;
	}

	private static interface Predicate<T> {
		boolean matches(final T arg);
	}
}
