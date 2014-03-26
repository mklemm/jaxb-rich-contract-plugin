package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * Helper class to generate fluent builder classes in two steps
 */
public class ChainedBuilderGenerator extends BuilderGenerator {
	private final JTypeVar parentBuilderTypeParam;
	private final JClass builderType;

	ChainedBuilderGenerator(final ApiConstructs apiConstructs, final BuilderOutline builderOutline) {
		super(apiConstructs, builderOutline);
		this.parentBuilderTypeParam = this.builderClass.generify("TParentBuilder");
		this.builderType = this.builderClass.narrow(this.parentBuilderTypeParam);


		final JMethod defaultConstructor = this.builderClass.constructor(JMod.PROTECTED);
		defaultConstructor.body().invoke("this").arg(JExpr._null()).arg(JExpr._null());


		final JMethod childConstructor = this.builderClass.constructor(JMod.PROTECTED);
		JVar parentBuilderParam = childConstructor.param(JMod.FINAL, this.parentBuilderTypeParam, "parentBuilder");
		childConstructor.body().invoke("this").arg(parentBuilderParam).arg(JExpr._null());


		final JMethod childProductConstructor = this.builderClass.constructor(JMod.PROTECTED);
		parentBuilderParam = childProductConstructor.param(JMod.FINAL, this.parentBuilderTypeParam, "parentBuilder");
		final JVar productParam = childProductConstructor.param(JMod.FINAL, this.definedClass, "product");


		if (builderOutline.getClassOutline().getSuperClass() == null) {
			final JFieldVar parentBuilderField = this.builderClass.field(JMod.PROTECTED | JMod.FINAL, this.parentBuilderTypeParam, "_parentBuilder");
			final JFieldVar productField = this.builderClass.field(JMod.PROTECTED | JMod.FINAL, this.definedClass, "_product");

			final JMethod endMethod = this.builderClass.method(JMod.PUBLIC, this.parentBuilderTypeParam, "end");
			endMethod.body()._return(JExpr._this().ref(parentBuilderField));

			childProductConstructor.body().assign(JExpr._this().ref(parentBuilderField), parentBuilderParam);
			childProductConstructor.body().assign(JExpr._this().ref(productField), productParam);
		} else {
			childProductConstructor.body().invoke("super").arg(parentBuilderParam).arg(productParam);
		}


	}

	protected void generateBuilderMember(final FieldOutline fieldOutline, final JBlock initBody, final JVar productParam) {
		final JFieldVar declaredField = this.definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
		final String propertyName = fieldOutline.getPropertyInfo().getName(true);
		if (fieldOutline.getPropertyInfo().isCollection()) {
			final JClass elementType = ((JClass) declaredField.type()).getTypeParameters().get(0);

			final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
			final JVar addListParam = addListMethod.param(JMod.FINAL, declaredField.type(), declaredField.name());

			final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar withListParam = withListMethod.param(JMod.FINAL, declaredField.type(), declaredField.name());

			final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
			final JVar addVarargsParam = addVarargsMethod.varParam(elementType, declaredField.name());
			addVarargsMethod.body().invoke(addListMethod).arg(this.apiConstructs.asList(addVarargsParam));
			addVarargsMethod.body()._return(JExpr._this());

			final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar withVarargsParam = withVarargsMethod.varParam(elementType, declaredField.name());
			withVarargsMethod.body().invoke(withListMethod).arg(this.apiConstructs.asList(withVarargsParam));
			withVarargsMethod.body()._return(JExpr._this());

			final JVar collectionVar;

			final BuilderOutline childBuilderOutline = this.apiConstructs.getDeclaration(elementType);

			if (childBuilderOutline == null || childBuilderOutline.getClassOutline().implClass.isAbstract()) {
				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name(), JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)));
				addListMethod.body().invoke(JExpr._this().ref(builderField), ApiConstructs.ADD_ALL).arg(addListParam);
				addListMethod.body()._return(JExpr._this());
				withListMethod.body().assign(JExpr._this().ref(builderField), withListParam);
				withListMethod.body()._return(JExpr._this());
				collectionVar = builderField;
			} else {
				final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
				final JClass builderArrayListClass = this.apiConstructs.arrayListClass.narrow(builderFieldElementType);
				final JClass builderListClass = this.apiConstructs.listClass.narrow(builderFieldElementType);
				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE | JMod.FINAL, builderListClass, declaredField.name(), JExpr._new(builderArrayListClass));

				final JMethod addMethod = this.builderClass.method(JMod.PUBLIC, builderFieldElementType, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
				final JVar childBuilderVar = addMethod.body().decl(JMod.FINAL, builderFieldElementType, declaredField.name() + "Builder", JExpr._new(builderFieldElementType).arg(JExpr._this()));
				addMethod.body().add(JExpr._this().ref(builderField).invoke("add").arg(childBuilderVar));
				addMethod.body()._return(childBuilderVar);

				final JForEach addListForEach = addListMethod.body().forEach(elementType, "item", addListParam);
				addListForEach.body().add(JExpr._this().ref(builderField).invoke("add").arg(JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(addListForEach.var())));
				addListMethod.body()._return(JExpr._this());

				withListMethod.body().add(JExpr._this().ref(builderField).invoke("clear"));
				withListMethod.body()._return(JExpr.invoke(addListMethod).arg(withListParam));

				collectionVar = initBody.decl(JMod.FINAL, this.apiConstructs.listClass.narrow(elementType), declaredField.name(), JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)).arg(JExpr._this().ref(builderField).invoke("size")));
				final JForEach initForEach = initBody.forEach(builderFieldElementType, "item", JExpr._this().ref(builderField));
				initForEach.body().add(collectionVar.invoke("add").arg(initForEach.var().invoke(ApiConstructs.BUILD_METHOD_NAME)));
			}

			if (this.hasImmutablePlugin) {
				initBody.assign(productParam.ref(declaredField), this.apiConstructs.unmodifiableList(collectionVar));
			} else {
				initBody.assign(productParam.ref(declaredField), JExpr._this().ref(collectionVar));
			}
		} else {
			final BuilderOutline childBuilderOutline = this.apiConstructs.getDeclaration(declaredField.type());
			if (childBuilderOutline == null || childBuilderOutline.getClassOutline().implClass.isAbstract()) {
				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name());
				final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
				final JVar param = withMethod.param(JMod.FINAL, declaredField.type(), declaredField.name());
				withMethod.body().assign(JExpr._this().ref(builderField), param);
				withMethod.body()._return(JExpr._this());

				initBody.assign(productParam.ref(declaredField), JExpr._this().ref(builderField));
			} else {
				final JClass elementType = (JClass) declaredField.type();
				final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderClass.narrow(this.parentBuilderTypeParam));
				final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, builderFieldElementType, declaredField.name());

				final JMethod withValueMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
				final JVar param = withValueMethod.param(JMod.FINAL, elementType, declaredField.name());
				withValueMethod.body().assign(JExpr._this().ref(builderField), JExpr._new(builderFieldElementType).arg(JExpr._this()).arg(param));
				withValueMethod.body()._return(JExpr._this());

				final JMethod withBuilderMethod = this.builderClass.method(JMod.PUBLIC, builderFieldElementType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
				withBuilderMethod.body()._return(JExpr._this().ref(builderField).assign(JExpr._new(builderFieldElementType).arg(JExpr._this())));

				initBody.assign(productParam.ref(declaredField), JExpr._this().ref(builderField).invoke(ApiConstructs.BUILD_METHOD_NAME));
			}
		}
	}


	protected void generateBuilderMemberOverride(final FieldOutline superFieldOutline, final JFieldVar declaredSuperField, final String superPropertyName) {
		if (superFieldOutline.getPropertyInfo().isCollection()) {
			final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
			addListMethod.annotate(Override.class);
			final JVar addListParam = addListMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
			addListMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addListParam);
			addListMethod.body()._return(JExpr._this());

			final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
			addVarargsMethod.annotate(Override.class);
			final JVar addVarargsParam = addVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
			addVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addVarargsParam);
			addVarargsMethod.body()._return(JExpr._this());

			final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
			withListMethod.annotate(Override.class);
			final JVar withListParam = withListMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
			withListMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
			withListMethod.body()._return(JExpr._this());

			final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
			withVarargsMethod.annotate(Override.class);
			final JVar withVarargsParam = withVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
			withVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
			withVarargsMethod.body()._return(JExpr._this());
			final BuilderOutline childBuilderOutline = this.apiConstructs.getDeclaration(declaredSuperField.type());
			if (childBuilderOutline != null && childBuilderOutline.getClassOutline().implClass.isAbstract()) {
				final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
				final JMethod addMethod = this.builderClass.method(JMod.PUBLIC, builderFieldElementType, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
				addMethod.body()._return(JExpr.cast(builderFieldElementType, JExpr._super().invoke(addMethod)));
			}
		} else {
			final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderType, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
			final JVar param = withMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
			withMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(param);
			withMethod.body()._return(JExpr._this());

			final BuilderOutline childBuilderOutline = this.apiConstructs.getDeclaration(declaredSuperField.type());
			if (childBuilderOutline != null && childBuilderOutline.getClassOutline().implClass.isAbstract()) {
				final JClass builderFieldElementType = childBuilderOutline.getDefinedBuilderClass().narrow(this.builderType);
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
		final JConditional ifStatement = buildMethod.body()._if(JExpr._this().ref("_product").eq(JExpr._null()));
		ifStatement._then()._return((JExpr._this().invoke(initMethod).arg(JExpr._new(this.definedClass))));
		ifStatement._else()._return(JExpr.cast(this.definedClass, JExpr._this().ref("_product")));
		return buildMethod;

	}

	@Override
	protected JMethod generateBuilderMethod() {
		final JMethod builderMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass.narrow(Void.class), ApiConstructs.BUILDER_METHOD_NAME);
		builderMethod.body()._return(JExpr._new(this.builderClass.narrow(Void.class)));
		return builderMethod;
	}
}
