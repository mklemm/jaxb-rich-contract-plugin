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

import java.lang.reflect.Method;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.kscs.util.jaxb.CollectionProperty;
import com.kscs.util.jaxb.CollectionPropertyInfo;
import com.kscs.util.jaxb.IndirectCollectionProperty;
import com.kscs.util.jaxb.IndirectCollectionPropertyInfo;
import com.kscs.util.jaxb.IndirectPrimitiveCollectionProperty;
import com.kscs.util.jaxb.IndirectPrimitiveCollectionPropertyInfo;
import com.kscs.util.jaxb.ItemProperty;
import com.kscs.util.jaxb.Property;
import com.kscs.util.jaxb.PropertyInfo;
import com.kscs.util.jaxb.PropertyVisitor;
import com.kscs.util.jaxb.SingleProperty;
import com.kscs.util.jaxb.SinglePropertyInfo;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.AbstractXSFunction;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.outline.DefinedPropertyOutline;
import com.kscs.util.plugins.xjc.outline.PropertyOutline;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CDefaultValue;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.reader.Ring;
import com.sun.tools.xjc.reader.xmlschema.BGMBuilder;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.visitor.XSFunction;

/**
 * Generates a helper class to access meta information
 * about a JAXB-generated object.
 * Currently only the generation of property name constants
 * is implemented.
 *
 * @author Mirko Klemm 2015-02-13
 */
public class MetaPlugin extends AbstractPlugin {
	private static final XSFunction<QName> SCHEMA_NAME_FUNC = new AbstractXSFunction<QName>() {
		@Override
		public QName attributeDecl(final XSAttributeDecl decl) {
			return new QName(decl.getTargetNamespace(), decl.getName());
		}

		@Override
		public QName attributeUse(final XSAttributeUse use) {
			return new QName(use.getDecl().getTargetNamespace(), use.getDecl().getName());
		}

		@Override
		public QName elementDecl(final XSElementDecl decl) {
			return new QName(decl.getTargetNamespace(), decl.getName());
		}

		@Override
		public QName particle(final XSParticle particle) {
			return particle.getTerm().apply(MetaPlugin.SCHEMA_NAME_FUNC);
		}

		@Override
		public QName modelGroupDecl(final XSModelGroupDecl decl) {
			return new QName(decl.getTargetNamespace(), decl.getName());
		}

		@Override
		public QName modelGroup(final XSModelGroup group) {
			return new QName("", "");
		}

		@Override
		public QName wildcard(final XSWildcard wc) {
			return new QName("", "*");
		}

		@Override
		public QName simpleType(final XSSimpleType type) {
			if (type.getName() == null) {
				return new QName(type.getTargetNamespace(), "anonymousSimpleType");
			} else {
				return new QName(type.getTargetNamespace(), type.getName());
			}
		}

		@Override
		public QName complexType(final XSComplexType type) {
			if (type.getName() == null) {
				return new QName(type.getTargetNamespace(), "anonymousComplexType");
			} else {
				return new QName(type.getTargetNamespace(), type.getName());
			}
		}
	};
	private static final XSFunction<QName> SCHEMA_TYPE_FUNC = new AbstractXSFunction<QName>() {
		@Override
		public QName attributeDecl(final XSAttributeDecl decl) {
			if (decl.getType().getName() == null) {
				return new QName(decl.getType().getTargetNamespace(), "anonymousAttributeType");
			} else {
				return new QName(decl.getType().getTargetNamespace(), decl.getType().getName());
			}
		}

		@Override
		public QName attributeUse(final XSAttributeUse use) {
			return attributeDecl(use.getDecl());
		}

		@Override
		public QName elementDecl(final XSElementDecl decl) {
			if (decl.getType().getName() == null) {
				return new QName(decl.getType().getTargetNamespace(), "anononymousElementType");
			} else {
				return new QName(decl.getType().getTargetNamespace(), decl.getType().getName());
			}
		}

		@Override
		public QName particle(final XSParticle particle) {
			return particle.getTerm().apply(MetaPlugin.SCHEMA_TYPE_FUNC);
		}

		@Override
		public QName simpleType(final XSSimpleType simpleType) {
			return new QName(simpleType.getTargetNamespace(), simpleType.getName());
		}

		@Override
		public QName wildcard(final XSWildcard wc) {
			return new QName("", "*");
		}

		@Override
		public QName modelGroupDecl(final XSModelGroupDecl decl) {
			return new QName(decl.getTargetNamespace(), decl.getName());
		}

		@Override
		public QName modelGroup(final XSModelGroup group) {
			return new QName("", "");
		}

		@Override
		public QName complexType(final XSComplexType type) {
			if (type.getName() == null) {
				return new QName(type.getTargetNamespace(), "anonymousComplexType");
			} else {
				return new QName(type.getTargetNamespace(), type.getName());
			}
		}
	};
	private static final XSFunction<Boolean> ATTRIBUTE_FUNC = new AbstractXSFunction<Boolean>() {
		@Override
		public Boolean attributeDecl(final XSAttributeDecl decl) {
			return true;
		}

		@Override
		public Boolean attributeUse(final XSAttributeUse use) {
			return true;
		}

		@Override
		public Boolean elementDecl(final XSElementDecl decl) {
			return false;
		}

		@Override
		public Boolean particle(final XSParticle particle) {
			return false;
		}

		@Override
		public Boolean simpleType(final XSSimpleType simpleType) {
			return false;
		}

		@Override
		public Boolean complexType(final XSComplexType type) {
			return false;
		}

		@Override
		public Boolean modelGroupDecl(final XSModelGroupDecl decl) {
			return false;
		}

		@Override
		public Boolean modelGroup(final XSModelGroup group) {
			return false;
		}

		@Override
		public Boolean wildcard(final XSWildcard wc) {
			return false;
		}
	};
	@Opt
	private boolean generateTools = true;
	@Opt
	private boolean extended = false;
	@Opt
	private boolean camelCase = false;
	@Opt
	private String metaClassName = "PropInfo";
	@Opt
	private boolean allowSet = true;
	@Opt
	private String visitMethodName = "visit";
	private boolean fixedAttributeAsConstantProperty;

	@Override
	public String getOptionName() {
		return "Xmeta";
	}

	@Override
	public void postProcessModel(final Model model, final ErrorHandler errorHandler) {
		this.fixedAttributeAsConstantProperty = Ring.get(BGMBuilder.class).getGlobalBinding().getDefaultProperty().isConstantProperty();
	}

	public boolean isExtended() {
		return this.extended;
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final PluginContext pluginContext = PluginContext.get(outline, opt, errorHandler);
		if (this.extended && this.generateTools) {
			pluginContext.writeSourceFile(PropertyInfo.class);
			pluginContext.writeSourceFile(SinglePropertyInfo.class);
			pluginContext.writeSourceFile(CollectionPropertyInfo.class);
			pluginContext.writeSourceFile(IndirectCollectionPropertyInfo.class);
			pluginContext.writeSourceFile(IndirectPrimitiveCollectionPropertyInfo.class);
			pluginContext.writeSourceFile(PropertyVisitor.class);
			pluginContext.writeSourceFile(Property.class);
			pluginContext.writeSourceFile(SingleProperty.class);
			pluginContext.writeSourceFile(CollectionProperty.class);
			pluginContext.writeSourceFile(IndirectCollectionProperty.class);
			pluginContext.writeSourceFile(IndirectPrimitiveCollectionProperty.class);
			pluginContext.writeSourceFile(ItemProperty.class);
		}
		for (final ClassOutline classOutline : outline.getClasses()) {
			generateMetaClass(pluginContext, classOutline, errorHandler);
		}
		return true;
	}

	private void generateMetaClass(final PluginContext pluginContext, final ClassOutline classOutline, final ErrorHandler errorHandler) throws SAXException {
		try {
			final JDefinedClass metaClass = classOutline.implClass._class(JMod.PUBLIC | JMod.STATIC, this.metaClassName);
			final JMethod visitMethod = generateVisitMethod(classOutline);
			for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
				if (this.extended) {
					generateExtendedMetaField(pluginContext, metaClass, visitMethod, fieldOutline);
				} else {
					generateNameOnlyMetaField(pluginContext, metaClass, fieldOutline);
				}
			}
			visitMethod.body()._return(JExpr._this());
		} catch (final JClassAlreadyExistsException e) {
			errorHandler.error(new SAXParseException(getMessage("error.metaClassExists", classOutline.implClass.name(), this.metaClassName), classOutline.target.getLocator()));
		}
	}

	private void generateNameOnlyMetaField(final PluginContext pluginContext, final JDefinedClass metaClass, final FieldOutline fieldOutline) {
		final PropertyOutline propertyOutline = new DefinedPropertyOutline(fieldOutline);
		final String constantName = getConstantName(fieldOutline);
		final Outline outline = pluginContext.outline;
		final String propertyName = constantName != null ? constantName : propertyOutline.getFieldName();
		final String metaFieldName = this.camelCase ? propertyName : fieldOutline.parent().parent().getModel().getNameConverter().toConstantName(propertyName);
		metaClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL | JMod.TRANSIENT, String.class, metaFieldName, JExpr.lit(propertyName));
	}

	private void generateExtendedMetaField(final PluginContext pluginContext, final JDefinedClass metaClass, final JMethod visitMethod, final FieldOutline fieldOutline) {
		final PropertyOutline propertyOutline = new DefinedPropertyOutline(fieldOutline);
		final String constantName = getConstantName(fieldOutline);
		final Outline outline = pluginContext.outline;
		final String propertyName = constantName != null ? constantName : propertyOutline.getFieldName();
		final String metaFieldName = this.camelCase ? propertyName : outline.getModel().getNameConverter().toConstantName(propertyName);
		final JType rawType = propertyOutline.getElementType();
		final Class<? extends PropertyInfo> infoClass;
		final JType fieldType;
		final JClass typeArg;
		final Class<? extends Property> propertyWrapperClass;
		final F1<JExpression, JVar> getMaker;
		final F3<JExpression, JBlock, JVar, JVar> setMaker;
		final JClass jaxbElementClass = outline.getCodeModel().ref(JAXBElement.class);
		if (propertyOutline.isIndirect()) {
			final JClass propertyType = ((JClass)rawType).getTypeParameters().get(0);
			if (propertyOutline.isCollection() && !rawType.isArray()) {
				getMaker = new F1<JExpression, JVar>() {
					@Override
					public JExpression f(final JVar param) {
						return param.ref(propertyName);
					}
				};
				setMaker = new F3<JExpression, JBlock, JVar, JVar>() {
					@Override
					public JExpression f(final JBlock block, final JVar instanceParam, final JVar valueParam) {
						block.assign(instanceParam.ref(propertyName), valueParam);
						return null;
					}
				};
				infoClass = propertyType.name().startsWith("?") ? IndirectCollectionPropertyInfo.class : IndirectPrimitiveCollectionPropertyInfo.class;
				typeArg = propertyType.name().startsWith("?") ? propertyType._extends() : propertyType;
				propertyWrapperClass = propertyType.name().startsWith("?") ? IndirectCollectionProperty.class : IndirectPrimitiveCollectionProperty.class;
				fieldType = outline.getCodeModel().ref(List.class).narrow(jaxbElementClass.narrow(propertyType));
			} else {
				getMaker = new F1<JExpression, JVar>() {
					@Override
					public JExpression f(final JVar param) {
						return JOp.cond(param.ref(propertyName).eq(JExpr._null()), JExpr._null(), param.ref(propertyName).invoke("getValue"));
					}
				};
				setMaker = new F3<JExpression, JBlock, JVar, JVar>() {
					@Override
					public JExpression f(final JBlock block, final JVar instanceParam, final JVar valueParam) {
						return JExpr.assign(instanceParam.ref(propertyName), JExpr._new(jaxbElementClass).arg(JExpr._this().ref("schemaName")).arg(propertyType._extends().dotclass()).arg(valueParam));
					}
				};
				infoClass = SinglePropertyInfo.class;
				propertyWrapperClass = SingleProperty.class;
				fieldType = propertyType._extends();
				typeArg = propertyType._extends();
			}
		} else {
			getMaker = constantName == null ? new F1<JExpression, JVar>() {
				@Override
				public JExpression f(final JVar param) {
					return param.ref(propertyName);
				}
			} : new F1<JExpression, JVar>() {
				@Override
				public JExpression f(final JVar param) {
					return ((JClass)param.type()).staticRef(propertyName);
				}
			};
			setMaker = new F3<JExpression, JBlock, JVar, JVar>() {
				@Override
				public JExpression f(final JBlock block, final JVar instanceParam, final JVar valueParam) {
					block.assign(instanceParam.ref(propertyName), valueParam);
					return null;
				}
			};
			if (propertyOutline.isCollection() && !rawType.isArray()) {
				infoClass = CollectionPropertyInfo.class;
				typeArg = (JClass)rawType;
				fieldType = outline.getCodeModel().ref(List.class).narrow(rawType);
				propertyWrapperClass = CollectionProperty.class;
			} else {
				infoClass = SinglePropertyInfo.class;
				fieldType = rawType.boxify();
				typeArg = rawType.boxify();
				propertyWrapperClass = SingleProperty.class;
			}
		}
		final JClass metaFieldType = typeArg == null ? outline.getCodeModel().ref(infoClass).narrow(fieldOutline.parent().implClass) : outline.getCodeModel().ref(infoClass).narrow(fieldOutline.parent().implClass, typeArg);
		final JDefinedClass anonymousMetaFieldType = outline.getCodeModel().anonymousClass(metaFieldType);
		generateAccessors(fieldOutline, propertyName, fieldType, anonymousMetaFieldType, getMaker, setMaker);
		final XSComponent schemaComponent = fieldOutline.getPropertyInfo().getSchemaComponent();
		final QName schemaName = schemaComponent.apply(MetaPlugin.SCHEMA_NAME_FUNC);
		final QName schemaType = schemaComponent.apply(MetaPlugin.SCHEMA_TYPE_FUNC);
		final Boolean attribute = schemaComponent.apply(MetaPlugin.ATTRIBUTE_FUNC);
		final JExpression schemaNameExpr = JExpr._new(outline.getCodeModel().ref(QName.class)).arg(schemaName.getNamespaceURI()).arg(schemaName.getLocalPart());
		final JExpression schemaTypeExpr = JExpr._new(outline.getCodeModel().ref(QName.class)).arg(schemaType.getNamespaceURI()).arg(schemaType.getLocalPart());
		final CDefaultValue defaultValue = fieldOutline.getPropertyInfo().defaultValue;
		final JFieldVar staticField = metaClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL | JMod.TRANSIENT, metaFieldType, metaFieldName, JExpr._new(anonymousMetaFieldType)
				.arg(propertyName)
				.arg(fieldOutline.parent().implClass.dotclass())
				.arg(dotClass(typeArg == null ? pluginContext.codeModel.ref(Object.class) : typeArg))
				.arg(JExpr.lit(propertyOutline.isCollection()))
				.arg(defaultValue == null ? JExpr._null() : defaultValue.compute(outline))
				.arg(schemaNameExpr)
				.arg(schemaTypeExpr)
				.arg(JExpr.lit(attribute)));
		final JVar visitorParam = visitMethod.params().get(0);
		final JBlock block = visitMethod.body();
		final JClass propertyCategory = typeArg == null ? pluginContext.codeModel.ref(propertyWrapperClass).narrow(fieldOutline.parent().implClass) : pluginContext.codeModel.ref(propertyWrapperClass).narrow(fieldOutline.parent().implClass, typeArg);
		final boolean generatedClass = typeArg != null && isVisitable(pluginContext, typeArg);
		final JInvocation call = visitorParam.invoke("visit").arg(JExpr._new(propertyCategory).arg(metaClass.staticRef(staticField)).arg(JExpr._this()));
		if (generatedClass) {
			final JFieldRef field = JExpr._this().ref(fieldOutline.getPropertyInfo().getName(false));
			final JConditional ifContinue = block._if(call.cand(field.ne(JExpr._null())));
			if (propertyOutline.isCollection() && !rawType.isArray()) {
				final JForEach forEach = ifContinue._then().forEach(rawType, "_item_", field);
				final JConditional ifNotNull = forEach.body()._if(forEach.var().ne(JExpr._null()));
				ifNotNull._then().add((propertyOutline.isIndirect() ? forEach.var().invoke("getValue") : forEach.var()).invoke(this.visitMethodName).arg(visitorParam));
			} else {
				ifContinue._then().add((propertyOutline.isIndirect() ? field.invoke("getValue") : field).invoke(this.visitMethodName).arg(visitorParam));
			}
		} else {
			block.add(call);
		}
	}

	private boolean isVisitable(final PluginContext pluginContext, final JClass typeArg) {
		return pluginContext.getClassOutline(typeArg) != null || hasReferencedClass(typeArg);
	}

	private boolean hasReferencedClass(final JClass typeArg) {
		try {
			final Class<?> foundClass = Class.forName(typeArg.binaryName());
			final Method visitMethod = foundClass.getMethod("visit", PropertyVisitor.class);
			return visitMethod != null;
		} catch (final Exception cfne) {
			return false;
		}
	}

	private void generateAccessors(final FieldOutline fieldOutline, final String propertyName, final JType returnType, final JDefinedClass declaringClass, final F1<JExpression, JVar> getMaker, final F3<JExpression, JBlock, JVar, JVar> setMaker) {
		final String constantName = getConstantName(fieldOutline);
		final JMethod getMethod = declaringClass.method(JMod.PUBLIC, returnType, "get");
		getMethod.annotate(Override.class);
		final JVar instanceParam = getMethod.param(JMod.FINAL, fieldOutline.parent().implClass, "_instance_");
		getMethod.body()._return(JOp.cond(instanceParam.eq(JExpr._null()), JExpr._null(), getMaker.f(instanceParam)));
		final JMethod setMethod = declaringClass.method(JMod.PUBLIC, void.class, "set");
		setMethod.annotate(Override.class);
		final JVar setInstanceParam = setMethod.param(JMod.FINAL, fieldOutline.parent().implClass, "_instance_");
		final JVar valueParam = setMethod.param(JMod.FINAL, returnType, "_value_");
		if (constantName == null) {
			final JConditional ifNotNull = setMethod.body()._if(setInstanceParam.ne(JExpr._null()));
			setMaker.f(ifNotNull._then(), setInstanceParam, valueParam);
		}
	}

	private JExpression dotClass(final JType cl) {
		if (cl instanceof JClass) {
			return ((JClass)cl).dotclass();
		} else {
			return new JExpressionImpl() {
				public void generate(final JFormatter f) {
					f.g(cl).p(".class");
				}
			};
		}
	}

	private String getConstantName(final FieldOutline fieldOutline) {
		final XSComponent schemaComponent = fieldOutline.getPropertyInfo().getSchemaComponent();
		if (!this.fixedAttributeAsConstantProperty) return null;
		if (schemaComponent instanceof XSAttributeDecl) {
			return ((XSAttributeDecl)schemaComponent).getFixedValue() != null ? fieldOutline.parent().parent().getModel().getNameConverter().toConstantName(((XSAttributeDecl)schemaComponent).getName()) : null;
		} else {
			return schemaComponent instanceof XSAttributeUse && ((XSAttributeUse)schemaComponent).getFixedValue() != null ? fieldOutline.parent().parent().getModel().getNameConverter().toConstantName(((XSAttributeUse)schemaComponent).getDecl().getName()) : null;
		}
	}

	private JMethod generateVisitMethod(final ClassOutline classOutline) {
		final JDefinedClass definedClass = classOutline.implClass;
		final JMethod visitMethod = definedClass.method(JMod.PUBLIC, definedClass, this.visitMethodName);
		final JCodeModel codeModel = definedClass.owner();
		final JClass visitorType = codeModel.ref(PropertyVisitor.class);
		final JVar visitorParam = visitMethod.param(JMod.FINAL, visitorType, "_visitor_");
		if (classOutline.getSuperClass() != null) {
			visitMethod.body().add(JExpr._super().invoke(this.visitMethodName).arg(visitorParam));
		} else {
			visitMethod.body().add(visitorParam.invoke("visit").arg(JExpr._this()));
		}
		return visitMethod;
	}

	public String getVisitMethodName() {
		return this.visitMethodName;
	}

	private interface F1<R, A> {
		R f(final A param);
	}

	private interface F3<R, A, B, C> {
		R f(final A param1, final B param2, final C param3);
	}
}
