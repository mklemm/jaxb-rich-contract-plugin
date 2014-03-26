package com.kscs.util.plugins.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;

import java.util.*;

/**
 * Common constants and constructs
 */
public class ApiConstructs {
	public static final String BUILDER_CLASS_NAME = "Builder";
	public static final String BUILD_METHOD_NAME = "build";
	public static final String BUILDER_METHOD_NAME = "builder";
	public static final String INIT_METHOD_NAME = "init";
	public static final String PRODUCT_INSTANCE_NAME = "product";
	public static final String ADD_METHOD_PREFIX = "add";
	public static final String WITH_METHOD_PREFIX = "with";
	public static final String AS_LIST = "asList";
	public static final String UNMODIFIABLE_LIST = "unmodifiableList";
	public static final String ADD_ALL = "addAll";

	final JCodeModel codeModel;
	final JClass arrayListClass;
	final JClass listClass;
	final JClass collectionClass;
	final JClass collectionsClass;
	final JClass arraysClass;
	final Map<String, BuilderOutline> builderClassOutlines;
	final Options opt;

	ApiConstructs(final JCodeModel codeModel, final Map<String, BuilderOutline> builderClassOutlines, final Options opt) {
		this.codeModel = codeModel;
		this.builderClassOutlines = builderClassOutlines;
		this.opt = opt;
		this.arrayListClass = codeModel.ref(ArrayList.class);
		this.listClass = codeModel.ref(List.class);
		this.collectionClass = codeModel.ref(Collection.class);
		this.collectionsClass = codeModel.ref(Collections.class);
		this.arraysClass = codeModel.ref(Arrays.class);
	}

	public JInvocation asList(final JExpression expression) {
		return this.arraysClass.staticInvoke(ApiConstructs.AS_LIST).arg(expression);
	}

	public JInvocation unmodifiableList(final JExpression expression) {
		return this.collectionsClass.staticInvoke(ApiConstructs.UNMODIFIABLE_LIST).arg(expression);
	}

	public boolean hasPlugin(final Class<? extends Plugin> pluginClass) {
		return PluginUtil.hasPlugin(this.opt, pluginClass);
	}

	public <P extends Plugin> P findPlugin(final Class<P> pluginClass) {
		return PluginUtil.findPlugin(this.opt, pluginClass);
	}

	public BuilderOutline getDeclaration(final JType type) {
		return this.builderClassOutlines.get(type.fullName());
	}

	public boolean isBuildableClass(final JType type) {
		return getDeclaration(type) != null && !((JClass) type).isAbstract();
	}

}
