package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

import java.util.Iterator;

/**
 * Helper class to generate fluent builder classes in two steps
 */
public class BuilderGenerator {
	private final ApiConstructs apiConstructs;

	private final JDefinedClass definedClass;
	private final JDefinedClass builderClass;
	private final JDefinedClass derivedBuilderClass;
	private final ClassOutline classOutline;
	private final boolean hasImmutablePlugin;
	private final boolean fullyFluentApi;

	BuilderGenerator(final ApiConstructs apiConstructs, final ClassOutline classOutline, final Options opt, final boolean fullyFluentApi) throws JClassAlreadyExistsException {
		this.apiConstructs = apiConstructs;
		this.classOutline = classOutline;
		this.definedClass = this.classOutline.implClass;
		this.fullyFluentApi = fullyFluentApi;
		this.hasImmutablePlugin = PluginUtil.hasPlugin(opt, ImmutablePlugin.class);
		final int mods = this.definedClass.isAbstract() ? JMod.PROTECTED | JMod.STATIC | JMod.ABSTRACT : JMod.PUBLIC | JMod.STATIC;
		this.builderClass = this.definedClass._class(mods, ApiConstructs.BUILDER_CLASS_NAME, ClassType.CLASS);

		if(fullyFluentApi) {
			this.derivedBuilderClass = this.definedClass._class(JMod.STATIC | JMod.PUBLIC, ApiConstructs.DERIVED_BUILDER_CLASS_NAME, ClassType.CLASS);
			this.derivedBuilderClass._extends(this.builderClass);
			final JTypeVar typeParam = this.derivedBuilderClass.generify("B");
			final JVar parentBuilderField = this.derivedBuilderClass.field(JMod.FINAL | JMod.PRIVATE, typeParam, "parentBuilder");
			final JMethod constructor = this.derivedBuilderClass.constructor(JMod.NONE);
			final JVar parentBuilderParam = constructor.param(JMod.FINAL, typeParam, "parentBuilder");
			constructor.body().assign(JExpr._this().ref(parentBuilderField), parentBuilderParam);
			final JMethod endMethod = this.derivedBuilderClass.method(JMod.PUBLIC, typeParam, "end");
			endMethod.body()._return(JExpr._this().ref(parentBuilderField));
		} else {
			this.derivedBuilderClass = null;
		}
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
			this.builderClass._extends(findBuilderClass(superClass.implClass, ApiConstructs.BUILDER_CLASS_NAME));
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

		if(this.fullyFluentApi) {
			for (final JMethod builderMethod : this.builderClass.methods()) {
				if((builderMethod.name().startsWith("add") || builderMethod.name().startsWith("with"))) {
					final JClass parameterisedBuilderClass =  this.derivedBuilderClass.narrow(this.derivedBuilderClass.typeParams()[0]);
					if(builderMethod.type().equals(this.builderClass) && builderMethod.params().size() == 1) {
						final JMethod derivedBuilderMethod = this.derivedBuilderClass.method(builderMethod.mods().getValue(), parameterisedBuilderClass, builderMethod.name());
						derivedBuilderMethod.annotate(Override.class);
						final JVar param = derivedBuilderMethod.param(JMod.FINAL, builderMethod.params().get(0).type(), builderMethod.params().get(0).name());
						derivedBuilderMethod.body().add(JExpr._super().invoke(builderMethod).arg(param));
						derivedBuilderMethod.body()._return(JExpr._this());
					} else {
//						final JMethod derivedBuilderMethod = this.derivedBuilderClass.method(builderMethod.mods().getValue(), this.derivedBuilderClass.narrow(parameterisedBuilderClass), builderMethod.name());
//						derivedBuilderMethod.annotate(Override.class);
//						derivedBuilderMethod.body().add(JExpr._super().invoke(builderMethod));
//						derivedBuilderMethod.body()._return(JExpr._this());
					}
				}
			}
		}
	}

	private void generateBuilderMember(final FieldOutline fieldOutline, final JBlock initBody, final JVar productParam) {
		final JFieldVar declaredField = this.definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
		final String propertyName = fieldOutline.getPropertyInfo().getName(true);
		if (fieldOutline.getPropertyInfo().isCollection()) {
			final JClass elementType = ((JClass) declaredField.type()).getTypeParameters().get(0);

			final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name(), JExpr._new(this.apiConstructs.arrayListClass.narrow(elementType)));

			final JMethod addListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
			final JVar addListParam = addListMethod.param(JMod.FINAL, fieldOutline.getRawType(), declaredField.name());
			addListMethod.body().invoke(JExpr._this().ref(declaredField), ApiConstructs.ADD_ALL).arg(addListParam);
			addListMethod.body()._return(JExpr._this());

			final JMethod addVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.ADD_METHOD_PREFIX + propertyName);
			final JVar addVarargsParam = addVarargsMethod.varParam(elementType, declaredField.name());
			addVarargsMethod.body().invoke(addListMethod).arg(this.apiConstructs.asList(addVarargsParam));
			addVarargsMethod.body()._return(JExpr._this());

			final JMethod withListMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar withListParam = withListMethod.param(JMod.FINAL, fieldOutline.getRawType(), declaredField.name());
			withListMethod.body().assign(JExpr._this().ref(declaredField), withListParam);
			withListMethod.body()._return(JExpr._this());

			final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, this.builderClass, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
			final JVar withVarargsParam = withVarargsMethod.varParam(elementType, declaredField.name());
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

			if(this.fullyFluentApi && declaredField.type().isReference()) {
				final JDefinedClass fieldClass = PluginUtil.getClassDefinition(this.apiConstructs.codeModel, declaredField.type());
				if(fieldClass != null) {
					final JClass childBuilderClass = findBuilderClass(fieldClass, ApiConstructs.DERIVED_BUILDER_CLASS_NAME);
					if(childBuilderClass != null) {
						final JClass childBuilderType = childBuilderClass.narrow(this.builderClass);
						final JFieldVar childBuilderField = this.builderClass.field(JMod.PRIVATE, childBuilderType, declaredField.name()+"_Builder");
						final JMethod withChildMethod = this.builderClass.method(JMod.PUBLIC, childBuilderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
						withChildMethod.body()._return(JExpr.assign(JExpr._this().ref(childBuilderField), JExpr._new(childBuilderClass).arg(JExpr._this())));

					}
				}
			}

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

	private JDefinedClass findBuilderClass(final JDefinedClass jClass, final String builderClassName) {
		final Iterator<JDefinedClass> classes = jClass.classes();
		while (classes.hasNext()) {
			final JDefinedClass innerClass = classes.next();
			if (builderClassName.equals(innerClass.name())) {
				return innerClass;
			}
		}
		return null;
	}
}
