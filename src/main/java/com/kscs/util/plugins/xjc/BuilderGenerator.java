package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

import java.util.Iterator;

/**
 * @description
 */
public class BuilderGenerator {
	private final ApiConstructs apiConstructs;

	private final JDefinedClass definedClass;
	private final JDefinedClass builderClass;
	private final ClassOutline classOutline;
	private final boolean hasImmutablePlugin;

	BuilderGenerator(final ApiConstructs apiConstructs, final ClassOutline classOutline, final Options opt) throws JClassAlreadyExistsException {
		this.apiConstructs = apiConstructs;
		this.classOutline = classOutline;
		this.definedClass = this.classOutline.implClass;
		this.hasImmutablePlugin = PluginUtil.hasPlugin(opt, ImmutablePlugin.class);
		final int mods = this.definedClass.isAbstract() ? JMod.PROTECTED | JMod.STATIC | JMod.ABSTRACT : JMod.PUBLIC | JMod.STATIC;
		this.builderClass = this.definedClass._class(mods, ApiConstructs.BUILDER_CLASS_NAME, ClassType.CLASS);
		/* TODO: Implement higher order builder functionality
		final JDefinedClass derivedBuilderClass = this.definedClass._class(JMod.STATIC, ApiConstructs.DERIVED_BUILDER_CLASS_NAME, ClassType.CLASS);
		derivedBuilderClass._extends(this.builderClass);
		final JTypeVar typeParam = derivedBuilderClass.generify("B");
		final JMethod endMethod = derivedBuilderClass.method(JMod.NONE, typeParam, "end");
		*/
	}

	void buildProperties() {
		final ClassOutline superClass = this.classOutline.getSuperClass();

		final JMethod initMethod = this.builderClass.method(JMod.PROTECTED, this.definedClass, ApiConstructs.INIT_METHOD_NAME);
		final JTypeVar typeVar = initMethod.generify("P", this.definedClass);
		initMethod.type(typeVar);
		final JVar productParam = initMethod.param(JMod.FINAL, typeVar, ApiConstructs.PRODUCT_INSTANCE_NAME);
		final JBlock initBody = initMethod.body();

		for (final FieldOutline fieldOutline : this.classOutline.getDeclaredFields()) {
			generateBuilderMember(fieldOutline, initBody, productParam);
		}

		if (superClass != null) {
			this.builderClass._extends(findBuilderClass(superClass));
			initBody._return(JExpr._super().invoke(initMethod).arg(productParam));
			buildWithMethodOverrides(superClass);
		} else {
			initBody._return(productParam);
		}

		if (!this.definedClass.isAbstract()) {
			final JMethod buildMethod = this.builderClass.method(JMod.PUBLIC, this.definedClass, ApiConstructs.BUILD_METHOD_NAME);
			buildMethod.body()._return((JExpr._this().invoke(initMethod).arg(JExpr._new(this.definedClass))));

			final JMethod builderMethod = this.definedClass.method(JMod.PUBLIC | JMod.STATIC, this.builderClass, ApiConstructs.BUILDER_METHOD_NAME);
			builderMethod.body()._return(JExpr._new(this.builderClass));
		}
	}

	private void generateBuilderMember(final FieldOutline fieldOutline, final JBlock initBody, final JVar productParam) {
		final JFieldVar declaredField = this.definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
		final String propertyName = fieldOutline.getPropertyInfo().getName(true);
		if (fieldOutline.getPropertyInfo().isCollection()) {
			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name(), JExpr._new(this.apiConstructs.arrayListClass));

			final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
			final JVar addListParam = addListMethod.param(JMod.FINAL, fieldOutline.getRawType(), declaredField.name());
			addListMethod.body().invoke(JExpr._this().ref(declaredField), ApiConstructs.ADD_ALL).arg(addListParam);
			addListMethod.body()._return(JExpr._this());

			final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
			final JVar addVarargsParam = addVarargsMethod.varParam(((JClass) declaredField.type()).getTypeParameters().get(0), declaredField.name());
			addVarargsMethod.body().invoke(addListMethod).arg(this.apiConstructs.asList(addVarargsParam));
			addVarargsMethod.body()._return(JExpr._this());

			final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar withListParam = withListMethod.param(JMod.FINAL, fieldOutline.getRawType(), declaredField.name());
			withListMethod.body().assign(JExpr._this().ref(declaredField), withListParam);
			withListMethod.body()._return(JExpr._this());

			final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar withVarargsParam = withVarargsMethod.varParam(((JClass) declaredField.type()).getTypeParameters().get(0), declaredField.name());
			withVarargsMethod.body().invoke(withListMethod).arg(this.apiConstructs.asList(withVarargsParam));
			withVarargsMethod.body()._return(JExpr._this());

			if (this.hasImmutablePlugin) {
				initBody.assign(productParam.ref(declaredField), this.apiConstructs.unmodifiableList(JExpr._this().ref(builderField)));
			} else {
				initBody.assign(productParam.ref(declaredField), JExpr._this().ref(builderField));
			}
		} else {
			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name());
			final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar param = withMethod.param(JMod.FINAL, fieldOutline.getRawType(), declaredField.name());
			final JBlock body = withMethod.body();
			body.assign(JExpr._this().ref(builderField), param);
			body._return(JExpr._this());
			initBody.assign(productParam.ref(declaredField), JExpr._this().ref(builderField));
		}
	}

	private void buildWithMethodOverrides(final ClassOutline superClass) {
		final JDefinedClass definedSuperClass = superClass.implClass;
		for (final FieldOutline superFieldOutline : superClass.getDeclaredFields()) {
			final JFieldVar declaredSuperField = definedSuperClass.fields().get(superFieldOutline.getPropertyInfo().getName(false));
			final String superPropertyName = superFieldOutline.getPropertyInfo().getName(true);
			if (superFieldOutline.getPropertyInfo().isCollection()) {
				final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
				addListMethod.annotate(Override.class);
				final JVar addListParam = addListMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
				addListMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addListParam);
				addListMethod.body()._return(JExpr._this());

				final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + superPropertyName);
				addVarargsMethod.annotate(Override.class);
				final JVar addVarargsParam = addVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
				addVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.ADD_METHOD_PREFIX + superPropertyName).arg(addVarargsParam);
				addVarargsMethod.body()._return(JExpr._this());

				final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				withListMethod.annotate(Override.class);
				final JVar withListParam = withListMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
				withListMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withListParam);
				withListMethod.body()._return(JExpr._this());

				final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				withVarargsMethod.annotate(Override.class);
				final JVar withVarargsParam = withVarargsMethod.varParam(((JClass) declaredSuperField.type()).getTypeParameters().get(0), declaredSuperField.name());
				withVarargsMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(withVarargsParam);
				withVarargsMethod.body()._return(JExpr._this());
			} else {
				final JMethod withMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + superPropertyName);
				final JVar param = withMethod.param(JMod.FINAL, superFieldOutline.getRawType(), declaredSuperField.name());
				withMethod.body().invoke(JExpr._super(), ApiConstructs.WITH_METHOD_PREFIX + superPropertyName).arg(param);
				withMethod.body()._return(JExpr._this());
			}

		}

		if (superClass.getSuperClass() != null) {
			buildWithMethodOverrides(superClass.getSuperClass());
		}
	}

	private JDefinedClass findBuilderClass(final ClassOutline classOutline) {
		final Iterator<JDefinedClass> classes = classOutline.implClass.classes();
		while (classes.hasNext()) {
			final JDefinedClass innerClass = classes.next();
			if (ApiConstructs.BUILDER_CLASS_NAME.equals(innerClass.name())) {
				return innerClass;
			}
		}
		return null;
	}
}
