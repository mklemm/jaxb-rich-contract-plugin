package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * Created by mirko on 24.03.14.
 */
public abstract class BuilderGenerator {
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
			generateExtendsClause(this.apiConstructs.getDeclaration(superClass.implClass));
			initBody._return(JExpr._super().invoke(initMethod).arg(productParam));
			generateBuilderMemeberOverrides(superClass);
		} else {
			initBody._return(productParam);
		}

		if (!this.definedClass.isAbstract()) {
			generateBuildMethod(initMethod);
			generateBuilderMethod();
		}

	}

	private void generateBuilderMemeberOverrides(final ClassOutline superClass) {
		final JDefinedClass definedSuperClass = superClass.implClass;
		for (final FieldOutline superFieldOutline : superClass.getDeclaredFields()) {
			final JFieldVar declaredSuperField = definedSuperClass.fields().get(superFieldOutline.getPropertyInfo().getName(false));
			final String superPropertyName = superFieldOutline.getPropertyInfo().getName(true);
			generateBuilderMemberOverride(superFieldOutline, declaredSuperField, superPropertyName);

		}

		if (superClass.getSuperClass() != null) {
			generateBuilderMemeberOverrides(superClass.getSuperClass());
		}
	}

	protected abstract void generateBuilderMember(final FieldOutline fieldOutline, final JBlock initBody, final JVar productParam);

	protected abstract void generateBuilderMemberOverride(final FieldOutline superFieldOutline, final JFieldVar declaredSuperField, final String superPropertyName);

	protected abstract JDefinedClass generateExtendsClause(final BuilderOutline superBuilder);

	protected abstract JMethod generateBuildMethod(final JMethod initMethod);

	protected abstract JMethod generateBuilderMethod();
}
