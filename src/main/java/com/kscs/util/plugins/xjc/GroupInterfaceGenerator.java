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

import com.sun.codemodel.*;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIProperty;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BindInfo;
import com.sun.xml.bind.api.impl.NameConverter;
import com.sun.xml.xsom.*;

import javax.xml.namespace.QName;
import java.beans.PropertyVetoException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author mirko 2014-05-29
 */
public class GroupInterfaceGenerator {
	private static final Logger LOGGER = Logger.getLogger(GroupInterfaceGenerator.class.getName());
	private final boolean declareBuilderInterface;
	private final ApiConstructs apiConstructs;
	private final boolean throwsPropertyVetoException;
	private final boolean immutable;
	private final boolean cloneMethodThrows;
	private final boolean needsCloneMethod;
	private final NameConverter nameConverter;
	private final Map<String, List<InterfaceOutline<?>>> implementations = new HashMap<String, List<InterfaceOutline<?>>>();

	public GroupInterfaceGenerator(final ApiConstructs apiConstructs, final boolean declareSetters, final boolean declareBuilderInterface) {
		this.apiConstructs = apiConstructs;
		this.immutable = !declareSetters || this.apiConstructs.hasPlugin(ImmutablePlugin.class);

		final BoundPropertiesPlugin boundPropertiesPlugin = this.apiConstructs.findPlugin(BoundPropertiesPlugin.class);
		this.throwsPropertyVetoException = boundPropertiesPlugin != null && boundPropertiesPlugin.isConstrained() && boundPropertiesPlugin.isSetterThrows();
		final DeepClonePlugin deepClonePlugin = this.apiConstructs.findPlugin(DeepClonePlugin.class);
		final FluentBuilderPlugin fluentBuilderPlugin = this.apiConstructs.findPlugin(FluentBuilderPlugin.class);
		this.declareBuilderInterface = declareBuilderInterface && fluentBuilderPlugin != null;
		this.needsCloneMethod = deepClonePlugin != null;
		this.cloneMethodThrows = this.needsCloneMethod && deepClonePlugin.isThrowCloneNotSupported();
		this.nameConverter = this.apiConstructs.outline.getModel().getNameConverter();
	}

	private static PackageOutline findPackageForNamespace(final Outline model, final String namespaceUri) {
		for (final PackageOutline packageOutline : model.getAllPackageContexts()) {
			if (namespaceUri.equals(packageOutline.getMostUsedNamespaceURI())) {
				return packageOutline;
			}
		}
		return null;
	}

	private static List<PropertyUse<XSElementDecl>> findElementDecls(final XSModelGroupDecl modelGroup) {
		final List<PropertyUse<XSElementDecl>> elementDecls = new ArrayList<PropertyUse<XSElementDecl>>();
		for (final XSParticle child : modelGroup.getModelGroup()) {
			if (child.getTerm() instanceof XSElementDecl) {
				elementDecls.add(new PropertyUse<XSElementDecl>((XSElementDecl) child.getTerm(), child.getAnnotation()));
			}
		}
		return elementDecls;
	}

	private static List<PropertyUse<XSAttributeDecl>> findAttributeDecls(final XSAttGroupDecl attGroupDecl) {
		final List<PropertyUse<XSAttributeDecl>> attributeDecls = new ArrayList<PropertyUse<XSAttributeDecl>>();
		for (final XSAttributeUse child : attGroupDecl.getDeclaredAttributeUses()) {
			attributeDecls.add(new PropertyUse<XSAttributeDecl>(child.getDecl(), child.getAnnotation()));
		}
		return attributeDecls;
	}

	private static List<XSModelGroupDecl> findModelGroups(final Iterable<XSParticle> modelGroup) {
		final List<XSModelGroupDecl> elementDecls = new ArrayList<XSModelGroupDecl>();
		for (final XSParticle child : modelGroup) {
			if (child.getTerm() instanceof XSModelGroupDecl) {
				elementDecls.add((XSModelGroupDecl) child.getTerm());
			}
		}
		return elementDecls;
	}

	private static Collection<? extends XSModelGroupDecl> findModelGroups(final XSComplexType complexType) {
		XSContentType contentType = complexType.getExplicitContent();
		if (contentType == null) {
			contentType = complexType.getContentType();
		}
		final XSParticle particle = contentType.asParticle();
		if (particle != null) {
			final XSTerm term = particle.getTerm();
			if (term instanceof XSModelGroupDecl) {
				return Arrays.asList((XSModelGroupDecl) term);
			} else {
				final XSModelGroup modelGroup = term.asModelGroup();
				return modelGroup != null ? findModelGroups(modelGroup) : Collections.<XSModelGroupDecl>emptyList();
			}
		} else {
			return Collections.emptyList();
		}
	}

	private static Collection<? extends XSAttGroupDecl> findAttributeGroups(final XSComplexType complexType) {
		return complexType.getAttGroups();
	}

	private static XSComplexType getTypeDefinition(final XSComponent xsTypeComponent) {
		if (xsTypeComponent instanceof XSAttContainer) {
			return (XSComplexType) xsTypeComponent;
		} else if (xsTypeComponent instanceof XSElementDecl) {
			return ((XSElementDecl) xsTypeComponent).getType().asComplexType();
		} else {
			return null;
		}
	}

	private static QName getQName(final XSDeclaration declaration) {
		return new QName(declaration.getTargetNamespace(), declaration.getName());
	}


	private static String getPropertyName(final PropertyUse<? extends XSDeclaration> propertyUse, final NameConverter conv) {
		// First, look for propertyName annotation on attribute/element use, then on attribute/element declaration, if neither is set,
		// use XSD name of element/attribute
		return getAnnotatedPropertyName(propertyUse.annotation, getAnnotatedPropertyName(propertyUse.declaration.getAnnotation(), conv.toPropertyName(propertyUse.declaration.getName())));
	}

	private static String getAnnotatedPropertyName(final XSAnnotation annotation, final String defaultName) {
		if (annotation != null && annotation.getAnnotation() != null && annotation.getAnnotation() instanceof BindInfo) {
			final BindInfo bindInfo = (BindInfo) annotation.getAnnotation();
			final BIProperty propertyBindingInfo = bindInfo.get(BIProperty.class);
			if (propertyBindingInfo != null) {
				return propertyBindingInfo.getPropertyName(false);
			}
		}
		return defaultName;
	}

	private static JMethod findGetter(final NameConverter conv, final ClassOutline classOutline, final PropertyUse<? extends XSDeclaration> element) {
		final String propertyName = getPropertyName(element, conv);
		String getterName = "get" + propertyName;
		JMethod m = classOutline.implClass.getMethod(getterName, new JType[0]);
		if (m == null) {
			getterName = "is" + propertyName;
			m = classOutline.implClass.getMethod(getterName, new JType[0]);
		}
		return m;
	}

	private static FieldOutline getFieldOutline(final NameConverter conv, final ClassOutline classOutline, final PropertyUse<? extends XSDeclaration> element) {
		final String propertyName = getPropertyName(element, conv);
		for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
			if (fieldOutline.getPropertyInfo().getName(true).equals(propertyName)) {
				return fieldOutline;
			}
		}
		return null;
	}


	private static JMethod findSetter(final NameConverter conv, final ClassOutline classOutline, final PropertyUse<? extends XSDeclaration> element) {
		final String propertyName = getPropertyName(element, conv);
		final String setterName = "set" + propertyName;
		for (final JMethod method : classOutline.implClass.methods()) {
			if (method.name().equals(setterName) && method.listParams().length == 1) {
				return method;
			}
		}
		return null;
	}

	public List<InterfaceOutline<?>> getGroupInterfacesForClass(final ClassOutline classOutline) {
		final List<InterfaceOutline<?>> implementedInterfaces = this.implementations.get(classOutline.implClass.fullName());
		return implementedInterfaces == null ? Collections.<InterfaceOutline<?>>emptyList() : implementedInterfaces;
	}

	private <T extends XSDeclaration> Map<QName, InterfaceOutline<T>> generateGroupInterfaces(final Iterator<? extends T> groupIterator, final Finder<T, T> findGroupFunc) {
		final Map<QName, InterfaceOutline<T>> groupInterfaces = new HashMap<QName, InterfaceOutline<T>>();

		// create interface for each group
		while (groupIterator.hasNext()) {
			final T modelGroup = groupIterator.next();
			final InterfaceOutline<T> interfaceOutline = createInterfaceDeclaration(modelGroup);
			if (interfaceOutline != null) {
				groupInterfaces.put(interfaceOutline.getName(), interfaceOutline);
			}
		}

		// Associate interfaces with superinterfaces
		for (final InterfaceOutline<T> typeDef : groupInterfaces.values()) {
			final T classComponent = typeDef.getSchemaComponent();
			for (final T groupRef : findGroupFunc.find(classComponent)) {
				associateSuperInterface(typeDef, groupInterfaces.get(getQName(groupRef)));
			}
		}
		return groupInterfaces;
	}

	public void generateGroupInterfaceModel() {
		final Map<QName, InterfaceOutline<XSModelGroupDecl>> modelGroupInterfaces = generateGroupInterfaces(this.apiConstructs.outline.getModel().schemaComponent.iterateModelGroupDecls(), new Finder<XSModelGroupDecl, XSModelGroupDecl>() {
			@Override
			public Collection<? extends XSModelGroupDecl> find(final XSModelGroupDecl declaration) {
				return findModelGroups(declaration.getModelGroup());
			}
		});

		final Map<QName, InterfaceOutline<XSAttGroupDecl>> attributeGroupInterfaces = generateGroupInterfaces(this.apiConstructs.outline.getModel().schemaComponent.iterateAttGroupDecls(), new Finder<XSAttGroupDecl, XSAttGroupDecl>() {
			@Override
			public Collection<? extends XSAttGroupDecl> find(final XSAttGroupDecl declaration) {
				return declaration.getAttGroups();
			}
		});

		for (final ClassOutline classOutline : this.apiConstructs.outline.getClasses()) {
			final XSComponent xsTypeComponent = classOutline.target.getSchemaComponent();
			final XSComplexType classComponent = getTypeDefinition(xsTypeComponent);
			if (classComponent != null) {
				generateProperties(attributeGroupInterfaces, classOutline, findAttributeGroups(classComponent), new Finder<PropertyUse<XSAttributeDecl>, XSAttGroupDecl>() {
					@Override
					public Collection<? extends PropertyUse<XSAttributeDecl>> find(final XSAttGroupDecl attributeGroup) {
						return findAttributeDecls(attributeGroup);
					}

				});

				generateProperties(modelGroupInterfaces, classOutline, findModelGroups(classComponent), new Finder<PropertyUse<XSElementDecl>, XSModelGroupDecl>() {
					@Override
					public Collection<? extends PropertyUse<XSElementDecl>> find(final XSModelGroupDecl declaration) {
						return findElementDecls(declaration);
					}
				});
			}
		}

		if (this.declareBuilderInterface) {
			final Map<String, BuilderOutline> builderOutlines = new HashMap<String, BuilderOutline>();
			for (final InterfaceOutline<XSModelGroupDecl> interfaceOutline : modelGroupInterfaces.values()) {
				try {
					builderOutlines.put(interfaceOutline.getImplClass().fullName(), new BuilderOutline(interfaceOutline, interfaceOutline.getImplClass()._class(JMod.NONE, ApiConstructs.BUILDER_INTERFACE_NAME, ClassType.INTERFACE)));
				} catch (final JClassAlreadyExistsException e) {
					GroupInterfaceGenerator.LOGGER.severe("Interface " + interfaceOutline.getImplClass().fullName() + "#" + ApiConstructs.BUILDER_INTERFACE_NAME + " already exists. Skipping builder interface generation.");
				}
			}
			for (final InterfaceOutline<XSAttGroupDecl> interfaceOutline : attributeGroupInterfaces.values()) {
				try {
					builderOutlines.put(interfaceOutline.getImplClass().fullName(), new BuilderOutline(interfaceOutline, interfaceOutline.getImplClass()._class(JMod.NONE, ApiConstructs.BUILDER_INTERFACE_NAME, ClassType.INTERFACE)));
				} catch (final JClassAlreadyExistsException e) {
					GroupInterfaceGenerator.LOGGER.severe("Interface " + interfaceOutline.getImplClass().fullName() + "#" + ApiConstructs.BUILDER_INTERFACE_NAME + " already exists. Skipping builder interface generation.");
				}
			}

			for (final BuilderOutline builderOutline : builderOutlines.values()) {
				final BuilderGenerator builderGenerator = new BuilderGenerator(this.apiConstructs, builderOutlines, builderOutline, false, false);

				builderGenerator.buildProperties();
			}
		}

	}

	private <E extends PropertyUse<? extends XSDeclaration>, G extends XSDeclaration> void generateProperties(final Map<QName, InterfaceOutline<G>> groupInterfaces, final ClassOutline classOutline, final Iterable<? extends G> groupUses, final Finder<E, G> findGroupFunc) {
		for (final G groupUse : groupUses) {
			final InterfaceOutline definedGroupType = groupInterfaces.get(getQName(groupUse));
			if (definedGroupType != null) {
				classOutline.implClass._implements(definedGroupType.getImplClass());

				List<InterfaceOutline<?>> interfaceOutlines = this.implementations.get(classOutline.implClass.fullName());
				if (interfaceOutlines == null) {
					interfaceOutlines = new ArrayList<InterfaceOutline<?>>();
					this.implementations.put(classOutline.implClass.fullName(), interfaceOutlines);
				}
				interfaceOutlines.add(definedGroupType);

				if (definedGroupType.getDeclaredFields() == null) {
					for (final E propertyUse : findGroupFunc.find(groupUse)) {
						generateProperty(classOutline, definedGroupType, propertyUse);
					}
				}
			}
		}
	}

	private void associateSuperInterface(final InterfaceOutline typeDefinition, final InterfaceOutline typeReference) {
		if (typeReference != null) {
			typeDefinition.setSuperInterface(typeReference);
			typeDefinition.getImplClass()._implements(typeReference.getImplClass());
		}
	}

	private <G extends XSDeclaration> InterfaceOutline<G> createInterfaceDeclaration(final G modelGroup) {
		final PackageOutline packageOutline = findPackageForNamespace(this.apiConstructs.outline, modelGroup.getTargetNamespace());
		if (packageOutline != null) {
			final JPackage container = packageOutline._package();

			final JDefinedClass groupInterface = this.apiConstructs.outline.getClassFactory().createInterface(container, this.nameConverter.toClassName(modelGroup.getName()), modelGroup.getLocator());

			if (this.needsCloneMethod) {
				final JMethod cloneMethod = groupInterface.method(JMod.PUBLIC, groupInterface, "clone");
				if (this.cloneMethodThrows) {
					cloneMethod._throws(CloneNotSupportedException.class);
				}
			}

			return new InterfaceOutline<G>(modelGroup, groupInterface);
		} else {
			return null;
		}
	}

	private <G extends XSDeclaration> FieldOutline generateProperty(final ClassOutline implementingClass, final InterfaceOutline<G> groupInterface, final PropertyUse<? extends XSDeclaration> propertyUse) {
		final FieldOutline implementedField = getFieldOutline(this.nameConverter, implementingClass, propertyUse);
		if (implementedField != null) {
			final JMethod implementedGetter = findGetter(this.nameConverter, implementingClass, propertyUse);
			if(implementedGetter != null) {
				final JMethod newGetter = groupInterface.getImplClass().method(JMod.NONE, implementedGetter.type(),
						implementedGetter.name());
				if (!this.immutable) {
					final JMethod implementedSetter = findSetter(this.nameConverter, implementingClass, propertyUse);
					if (implementedSetter != null) {
						final JMethod newSetter = groupInterface.getImplClass().method(JMod.NONE, implementedSetter.type(),
								implementedSetter.name());
						newSetter.param(implementedSetter.listParamTypes()[0], implementedSetter.listParams()[0].name());
						if (this.throwsPropertyVetoException) {
							newSetter._throws(PropertyVetoException.class);
						}
					}
				}
				groupInterface.addField(implementedField);
			}
		}
		return implementedField;
	}


	private static interface Finder<R, P> {
		Collection<? extends R> find(final P declaration);
	}

	private static class PropertyUse<D extends XSDeclaration> {
		final D declaration;
		final XSAnnotation annotation;

		PropertyUse(final D declaration, final XSAnnotation annotation) {
			this.declaration = declaration;
			this.annotation = annotation;
		}
	}

}

