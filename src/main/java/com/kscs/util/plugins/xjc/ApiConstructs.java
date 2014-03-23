package com.kscs.util.plugins.xjc;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @description
 */
public class ApiConstructs {
	public static final String BUILDER_CLASS_NAME = "Builder";
	public static final String DERIVED_BUILDER_CLASS_NAME = "DerivedBuilder";
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
	 final JClass collectionClass;
	 final JClass collectionsClass;
	 final JClass arraysClass;

	ApiConstructs(final JCodeModel codeModel) {
		this.codeModel = codeModel;
		this.arrayListClass = codeModel.ref(ArrayList.class);
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
}
