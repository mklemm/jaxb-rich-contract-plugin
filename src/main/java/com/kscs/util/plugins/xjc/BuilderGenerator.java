package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

import java.util.logging.Logger;

/**
 * Created by mirko on 24.03.14.
 */
public abstract class BuilderGenerator {
	private static final Logger LOGGER = Logger.getLogger(BuilderGenerator.class.getName());
	protected final ApiConstructs apiConstructs;

	protected final JDefinedClass definedClass;
	protected final JDefinedClass builderClass;
	protected final ClassOutline classOutline;
	protected final boolean hasImmutablePlugin;

	protected BuilderGenerator(final ApiConstructs apiConstructs, final BuilderOutline builderOutline) {
		this.apiConstructs = apiConstructs;
		this.classOutline = builderOutline.getClassOutline();
		this.definedClass = this.classOutline.implClass;
		this.hasImmutablePlugin = apiConstructs.hasPlugin(ImmutablePlugin.class);
		this.builderClass = builderOutline.getDefinedBuilderClass();
	}

	public void buildProperties() {
		try {
			final ClassOutline superClass = this.classOutline.getSuperClass();

			final JMethod initMethod = this.builderClass.method(JMod.PROTECTED, this.definedClass, ApiConstructs.INIT_METHOD_NAME);
			final JTypeVar typeVar = initMethod.generify("P", this.definedClass);
			initMethod.type(typeVar);
			final JVar productParam = initMethod.param(JMod.FINAL, typeVar, ApiConstructs.PRODUCT_INSTANCE_NAME);
			final JBlock initBody = initMethod.body();

			for (final FieldOutline fieldOutline : this.classOutline.getDeclaredFields()) {
				final JFieldVar declaredField = this.definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
				if (declaredField != null && (declaredField.mods().getValue() & JMod.STATIC) == 0) {
					generateBuilderMember(fieldOutline, declaredField, initBody, productParam);
				}
			}

			if (superClass != null) {
				generateExtendsClause(this.apiConstructs.getDeclaration(superClass.implClass));
				initBody._return(JExpr._super().invoke(initMethod).arg(productParam));
				generateBuilderMemberOverrides(superClass);
			} else {
				initBody._return(productParam);
			}

			if (!this.definedClass.isAbstract()) {
				generateBuildMethod(initMethod);
				generateBuilderMethod();
			}
		} catch(final Exception x) {
			BuilderGenerator.LOGGER.severe(x.getMessage());
			System.err.println(x.getMessage());
			x.printStackTrace();
		}
	}

	private void generateBuilderMemberOverrides(final ClassOutline superClass) {
		final JDefinedClass definedSuperClass = superClass.implClass;
		for (final FieldOutline superFieldOutline : superClass.getDeclaredFields()) {
			final JFieldVar declaredSuperField = definedSuperClass.fields().get(superFieldOutline.getPropertyInfo().getName(false));
			if(declaredSuperField != null && (declaredSuperField.mods().getValue() & JMod.STATIC) == 0) {
				final String superPropertyName = superFieldOutline.getPropertyInfo().getName(true);
				generateBuilderMemberOverride(superFieldOutline, declaredSuperField, superPropertyName);
			}
		}

		if (superClass.getSuperClass() != null) {
			generateBuilderMemberOverrides(superClass.getSuperClass());
		}
	}

	protected void generateArrayProperty(final JBlock initBody, final JVar productParam, final JFieldVar declaredField, final String propertyName, final JType elementType, final JType builderType) {

		final JFieldVar builderField = this.builderClass.field(JMod.PRIVATE, declaredField.type(), declaredField.name(), JExpr._null());

		final JMethod withVarargsMethod = this.builderClass.method(JMod.PUBLIC, builderType, ApiConstructs.WITH_METHOD_PREFIX + propertyName);
		final JVar withVarargsParam = withVarargsMethod.varParam(elementType, declaredField.name());
		withVarargsMethod.body().assign(JExpr._this().ref(builderField), withVarargsParam);
		withVarargsMethod.body()._return(JExpr._this());

		initBody.assign(productParam.ref(declaredField), builderField);
	}

	protected abstract void generateBuilderMember(final FieldOutline fieldOutline, final JFieldVar declaredField, final JBlock initBody, final JVar productParam);

	protected abstract void generateBuilderMemberOverride(final FieldOutline superFieldOutline, final JFieldVar declaredSuperField, final String superPropertyName);

	protected abstract JDefinedClass generateExtendsClause(final BuilderOutline superBuilder);

	protected abstract JMethod generateBuildMethod(final JMethod initMethod);

	protected abstract JMethod generateBuilderMethod();
}
