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

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import com.kscs.util.jaxb.PropertyInfo;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.AbstractXSFunction;
import com.kscs.util.plugins.xjc.base.Opt;
import com.kscs.util.plugins.xjc.outline.PropertyOutline;
import com.kscs.util.plugins.xjc.outline.DefinedPropertyOutline;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JExpressionImpl;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CDefaultValue;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
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
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Generates a helper class to access meta information
 * about a JAXB-generated object.
 * Currently only the generation of property name constants
 * is implemented.
 * @author Mirko Klemm 2015-02-13
 */
public class MetaPlugin extends AbstractPlugin {
	private static final XSFunction<QName> SCHEMA_NAME_FUNC = new AbstractXSFunction<QName>() {

		@Override
		public QName attributeDecl(final XSAttributeDecl decl) {
			return new QName(decl.getTargetNamespace(),decl.getName());
		}

		@Override
		public QName attributeUse(final XSAttributeUse use) {
			return new QName(use.getDecl().getTargetNamespace(),use.getDecl().getName());
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
			return new QName("","");
		}

		@Override
		public QName wildcard(final XSWildcard wc) {
			return new QName("","*");
		}

		@Override
		public QName simpleType(final XSSimpleType type) {
			if(type.getName() == null) {
				return new QName(type.getTargetNamespace(), "anonymousSimpleType");
			} else {
				return new QName(type.getTargetNamespace(), type.getName());
			}
		}

		@Override
		public QName complexType(final XSComplexType type) {
			if(type.getName() == null) {
				return new QName(type.getTargetNamespace(), "anonymousComplexType");
			} else {
				return new QName(type.getTargetNamespace(), type.getName());
			}
		}
	};
	private static final XSFunction<QName> SCHEMA_TYPE_FUNC = new AbstractXSFunction<QName>() {

		@Override
		public QName attributeDecl(final XSAttributeDecl decl) {
			if(decl.getType().getName() == null) {
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
			if(decl.getType().getName() == null) {
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
			return new QName("","*");
		}

		@Override
		public QName modelGroupDecl(final XSModelGroupDecl decl) {
			return new QName(decl.getTargetNamespace(), decl.getName());
		}

		@Override
		public QName modelGroup(final XSModelGroup group) {
			return new QName("","");
		}

		@Override
		public QName complexType(final XSComplexType type) {
			if(type.getName() == null) {
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

	@Override
	public String getOptionName() {
		return "Xmeta";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final PluginContext pluginContext = PluginContext.get(outline, opt, errorHandler);
		if(this.extended && this.generateTools) {
			pluginContext.writeSourceFile(PropertyInfo.class);
		}
		for(final ClassOutline classOutline:outline.getClasses()) {
			generateMetaClass(classOutline, errorHandler);
		}
		return true;
	}

	private void generateMetaClass(final ClassOutline classOutline, final ErrorHandler errorHandler) throws SAXException {
		try {
			final JDefinedClass metaClass = classOutline.implClass._class(JMod.PUBLIC | JMod.STATIC, this.metaClassName);
			for(final FieldOutline fieldOutline:classOutline.getDeclaredFields()) {
				if(this.extended) {
					generateExtendedMetaField(metaClass, fieldOutline);
				} else {
					generateNameOnlyMetaField(metaClass, fieldOutline);
				}
			}
		} catch (final JClassAlreadyExistsException e) {
			errorHandler.error(new SAXParseException(getMessage("error.metaClassExists", classOutline.implClass.name(), this.metaClassName), classOutline.target.getLocator()));
		}
	}

	private void generateNameOnlyMetaField(final JDefinedClass metaClass, final FieldOutline fieldOutline) {
		final String propertyName = fieldOutline.getPropertyInfo().getName(false);
		final String metaFieldName = this.camelCase ? propertyName : fieldOutline.parent().parent().getModel().getNameConverter().toConstantName(propertyName);
		metaClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL | JMod.TRANSIENT, String.class, metaFieldName, JExpr.lit(propertyName));
	}
	private void generateExtendedMetaField(final JDefinedClass metaClass, final FieldOutline fieldOutline) {
		final PropertyOutline propertyOutline = new DefinedPropertyOutline(fieldOutline);
		final String propertyName = propertyOutline.getFieldName();
		final Outline outline = fieldOutline.parent().parent();
		final String metaFieldName = this.camelCase ? propertyName : outline.getModel().getNameConverter().toConstantName(propertyName);
		JType propertyType = propertyOutline.getElementType();
		if(outline.getCodeModel().ref(JAXBElement.class).fullName().equals(propertyType.erasure().fullName())) {
			propertyType = ((JClass)propertyType).getTypeParameters().get(0);
		}

		final JClass metaFieldType = outline.getCodeModel().ref(PropertyInfo.class).narrow(fieldOutline.parent().implClass, propertyType.boxify());

		final XSComponent schemaComponent = fieldOutline.getPropertyInfo().getSchemaComponent();
		final QName schemaName = schemaComponent.apply(MetaPlugin.SCHEMA_NAME_FUNC);
		final QName schemaType = schemaComponent.apply(MetaPlugin.SCHEMA_TYPE_FUNC);
		final Boolean attribute = schemaComponent.apply(MetaPlugin.ATTRIBUTE_FUNC);

		final JExpression schemaNameExpr = JExpr._new(outline.getCodeModel().ref(QName.class)).arg(schemaName.getNamespaceURI()).arg(schemaName.getLocalPart());
		final JExpression schemaTypeExpr = JExpr._new(outline.getCodeModel().ref(QName.class)).arg(schemaType.getNamespaceURI()).arg(schemaType.getLocalPart());

		final CDefaultValue defaultValue = fieldOutline.getPropertyInfo().defaultValue;
		metaClass.field(JMod.PUBLIC | JMod.STATIC | JMod.FINAL | JMod.TRANSIENT, metaFieldType, metaFieldName, JExpr._new(metaFieldType)
				.arg(propertyName)
				.arg(fieldOutline.parent().implClass.dotclass())
				.arg(dotClass(propertyType))
				.arg(JExpr.lit(propertyOutline.isCollection()))
				.arg(defaultValue == null ? JExpr._null() : defaultValue.compute(outline))
				.arg(schemaNameExpr)
				.arg(schemaTypeExpr)
				.arg(JExpr.lit(attribute)));
	}

	private JExpression dotClass(final JType cl) {
		if(cl instanceof JClass) {
			return ((JClass)cl).dotclass();
		} else {
			return new JExpressionImpl() {
				public void generate(JFormatter f) {
					f.g(cl).p(".class");
				}
			};
		}
	}



}
