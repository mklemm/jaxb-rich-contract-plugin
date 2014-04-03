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
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import com.sun.xml.bind.api.impl.NameConverter;
import com.sun.xml.xsom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.beans.PropertyVetoException;
import java.util.*;

public class GroupInterfacePlugin extends Plugin {
	private static PackageOutline findPackageForNamespace(final Outline model, final String namespaceUri) {
		for (final PackageOutline packageOutline : model.getAllPackageContexts()) {
			if (namespaceUri.equals(packageOutline.getMostUsedNamespaceURI())) {
				return packageOutline;
			}
		}
		return null;
	}

	private static List<XSParticle> findElementDecls(final Iterable<XSParticle> modelGroup) {
		final List<XSParticle> elementDecls = new ArrayList<XSParticle>();
		for (final XSParticle child : modelGroup) {
			if (child.getTerm() instanceof XSElementDecl) {
				elementDecls.add(child);
			}
		}
		return elementDecls;
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

	private static List<XSModelGroupDecl> findModelGroups(final XSComplexType complexType) {
		XSContentType contentType = complexType.getExplicitContent();
		if (contentType == null) {
			contentType = complexType.getContentType();
		}
		final XSParticle particle = contentType.asParticle();
		if (particle != null) {
			final XSTerm term = particle.getTerm();
			final XSModelGroup modelGroup = term.asModelGroup();
			return modelGroup != null ? GroupInterfacePlugin.findModelGroups(modelGroup) : Collections.<XSModelGroupDecl>emptyList();
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

	private static List<ClassOutline> findImplementingClasses(final Outline outline, final XSModelGroupDecl modelGroupDecl) {
		final List<ClassOutline> implementingClasses = new ArrayList<ClassOutline>();
		for (final ClassOutline classOutline : outline.getClasses()) {
			final XSComplexType typeDefinition = GroupInterfacePlugin.getTypeDefinition(classOutline.target.getSchemaComponent());
			if (typeDefinition != null) {
				for (final XSModelGroupDecl mg : GroupInterfacePlugin.findModelGroups(typeDefinition)) {
					if (new QName(mg.getTargetNamespace(), mg.getName()).equals(new QName(modelGroupDecl.getTargetNamespace(), modelGroupDecl
							.getName()))) {
						implementingClasses.add(classOutline);
					}
				}
			}
		}
		return implementingClasses;
	}

	private static List<ClassOutline> findImplementingClasses(final Outline outline, final XSAttGroupDecl attGroupDecl) {
		final List<ClassOutline> implementingClasses = new ArrayList<ClassOutline>();
		for (final ClassOutline classOutline : outline.getClasses()) {
			final XSComplexType typeDefinition = GroupInterfacePlugin.getTypeDefinition(classOutline.target.getSchemaComponent());
			if (typeDefinition != null) {
				for (final XSAttGroupDecl mg : GroupInterfacePlugin.findAttributeGroups(typeDefinition)) {
					if (new QName(mg.getTargetNamespace(), mg.getName()).equals(new QName(attGroupDecl.getTargetNamespace(), attGroupDecl
							.getName()))) {
						implementingClasses.add(classOutline);
					}
				}
			}
		}
		return implementingClasses;
	}

	private static JMethod findGetter(final NameConverter conv, final ClassOutline classOutline, final XSElementDecl element) {
		String getterName = "get" + conv.toPropertyName(element.getName());
		for (final JMethod method : classOutline.implClass.methods()) {
		}
		JMethod m = classOutline.implClass.getMethod(getterName, new JType[0]);
		if (m == null) {
			getterName = "is" + conv.toPropertyName(element.getName());
			m = classOutline.implClass.getMethod(getterName, new JType[0]);
		}
		return m;
	}

	private static JMethod findGetter(final NameConverter conv, final ClassOutline classOutline, final XSAttributeUse attributeDecl) {
		String getterName = "get" + conv.toPropertyName(attributeDecl.getDecl().getName());
		JMethod m = classOutline.implClass.getMethod(getterName, new JType[0]);
		if (m == null) {
			getterName = "is" + conv.toPropertyName(attributeDecl.getDecl().getName());
			m = classOutline.implClass.getMethod(getterName, new JType[0]);
		}
		return m;
	}

	private static JMethod findSetter(final NameConverter conv, final ClassOutline classOutline, final XSElementDecl element) {
		final String setterName = "set" + conv.toPropertyName(element.getName());
		for (final JMethod method : classOutline.implClass.methods()) {
			if (method.name().equals(setterName) && method.listParams().length == 1) {
				return method;
			}
		}
		return null;
	}

	private static JMethod findSetter(final NameConverter conv, final ClassOutline classOutline, final XSAttributeUse attributeDecl) {
		final String setterName = "set" + conv.toPropertyName(attributeDecl.getDecl().getName());
		for (final JMethod method : classOutline.implClass.methods()) {
			if (method.name().equals(setterName) && method.listParams().length == 1) {
				return method;
			}
		}
		return null;
	}

	@Override
	public String getOptionName() {
		return "Xgroup-contract";
	}

	@Override
	public String getUsage() {
		return new PluginUsageBuilder(ResourceBundle.getBundle(GroupInterfacePlugin.class.getName()), "usage").addMain("group-contract").build();
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler)
			throws SAXException {
		final Map<QName, TypeDef<XSAttContainer>> groupInterfaces = generateAttributeGroupInterfaces(outline, opt);
		final Map<QName, TypeDef<XSModelGroupDecl>> modelGroupInterfaces = generateModelGroupInterfaces(outline, opt);
		for (final TypeDef<XSAttContainer> typeDef : groupInterfaces.values()) {
			final XSAttContainer classComponent = typeDef.schemaComponent;
			for (final XSAttGroupDecl attributeGroupRef : classComponent.getAttGroups()) {
				final TypeDef<XSAttContainer> definedGroupType = groupInterfaces.get(new QName(attributeGroupRef.getTargetNamespace(),
						attributeGroupRef
								.getName()
				));
				if (definedGroupType != null) {
					typeDef.definedClass._implements(definedGroupType.definedClass);
				}

			}
		}
		for (final TypeDef<XSModelGroupDecl> typeDef : modelGroupInterfaces.values()) {
			final XSModelGroupDecl classComponent = typeDef.schemaComponent;
			for (final XSModelGroupDecl attributeGroupRef : GroupInterfacePlugin.findModelGroups(classComponent.getModelGroup())) {
				final TypeDef<XSModelGroupDecl> definedGroupType = modelGroupInterfaces.get(new
						QName(attributeGroupRef.getTargetNamespace(),
						attributeGroupRef.getName()));
				if (definedGroupType != null) {
					typeDef.definedClass._implements(definedGroupType.definedClass);
				}
			}
		}
		for (final ClassOutline classOutline : outline.getClasses()) {
			final XSComponent xsTypeComponent = classOutline.target.getSchemaComponent();
			final XSComplexType classComponent = GroupInterfacePlugin.getTypeDefinition(xsTypeComponent);
			if (classComponent != null) {
				for (final XSAttGroupDecl attributeGroupRef : classComponent.getAttGroups()) {
					final TypeDef<XSAttContainer> definedGroupType = groupInterfaces.get(new QName(attributeGroupRef.getTargetNamespace(),
							attributeGroupRef
									.getName()
					));
					if (definedGroupType != null) {
						classOutline.implClass._implements(definedGroupType.definedClass);
					}

				}

				for (final XSModelGroupDecl attributeGroupRef : GroupInterfacePlugin.findModelGroups(classComponent)) {
					final TypeDef<XSModelGroupDecl> definedGroupType = modelGroupInterfaces.get(new QName(attributeGroupRef.getTargetNamespace(),
							attributeGroupRef
									.getName()
					));
					if (definedGroupType != null) {
						classOutline.implClass._implements(definedGroupType.definedClass);
					}

				}
			}
		}
		return true;
	}

	private Map<QName, TypeDef<XSModelGroupDecl>> generateModelGroupInterfaces(final Outline outline, final Options opt) {
		final BoundPropertiesPlugin boundPropertiesPlugin = PluginUtil.findPlugin(opt, BoundPropertiesPlugin.class);
		final boolean throwsPropertyVetoException = boundPropertiesPlugin != null && boundPropertiesPlugin.isConstrained() && boundPropertiesPlugin.isSetterThrows();
		final DeepClonePlugin deepClonePlugin = PluginUtil.findPlugin(opt, DeepClonePlugin.class);
		final boolean needsCloneMethod = deepClonePlugin != null;
		final boolean cloneMethodThrows = needsCloneMethod && deepClonePlugin.isThrowCloneNotSupported();
		final boolean immutable = PluginUtil.hasPlugin(opt, ImmutablePlugin.class);

		final NameConverter nameConverter = outline.getModel().getNameConverter();
		final Map<QName, TypeDef<XSModelGroupDecl>> groupInterfaces = new HashMap<QName, TypeDef<XSModelGroupDecl>>();
		final Iterator<XSModelGroupDecl> modelGroupIterator =
				outline.getModel().schemaComponent.iterateModelGroupDecls();
		while (modelGroupIterator.hasNext()) {
			final XSModelGroupDecl modelGroup = modelGroupIterator.next();
			final PackageOutline packageOutline = GroupInterfacePlugin.findPackageForNamespace(outline, modelGroup.getTargetNamespace());
			if (packageOutline != null) {
				final JPackage container = packageOutline._package();

				final List<ClassOutline> implementingClasses = GroupInterfacePlugin.findImplementingClasses(outline, modelGroup);
				if (!implementingClasses.isEmpty()) {
					final ClassOutline implementingClass = implementingClasses.get(0);

					final JDefinedClass groupInterface = outline.getClassFactory().createInterface(container,
							nameConverter.toClassName(modelGroup.getName()),
							modelGroup.getLocator());

					for (final XSParticle particle : GroupInterfacePlugin.findElementDecls(modelGroup.getModelGroup())) {
						final XSElementDecl elementDecl = (XSElementDecl) particle.getTerm();
						if (elementDecl.getFixedValue() == null) {
							final JMethod implementedGetter = GroupInterfacePlugin.findGetter(nameConverter, implementingClass, elementDecl);
							final JMethod newGetter = groupInterface.method(JMod.NONE, implementedGetter.type(),
									implementedGetter.name());
							if (!immutable) {
								final JMethod implementedSetter = GroupInterfacePlugin.findSetter(nameConverter, implementingClass, elementDecl);
								if (implementedSetter != null) {
									final JMethod newSetter = groupInterface.method(JMod.NONE, implementedSetter.type(),
											implementedSetter.name());
									newSetter.param(implementedSetter.listParamTypes()[0], implementedSetter.listParams()[0].name());
									if (throwsPropertyVetoException) {
										newSetter._throws(PropertyVetoException.class);
									}
								}
							}
						}
					}

					if (needsCloneMethod) {
						final JMethod cloneMethod = groupInterface.method(JMod.PUBLIC, groupInterface, "clone");
						if (cloneMethodThrows) {
							cloneMethod._throws(CloneNotSupportedException.class);
						}
					}

					groupInterfaces.put(new QName(modelGroup.getTargetNamespace(), modelGroup.getName()), new
							TypeDef<XSModelGroupDecl>(groupInterface,
							modelGroup));
				}
			}
		}
		return groupInterfaces;
	}

	private Map<QName, TypeDef<XSAttContainer>> generateAttributeGroupInterfaces(final Outline outline, final Options opt) {
		final BoundPropertiesPlugin boundPropertiesPlugin = PluginUtil.findPlugin(opt, BoundPropertiesPlugin.class);
		final boolean throwsPropertyVetoException = boundPropertiesPlugin != null && boundPropertiesPlugin.isConstrained() && boundPropertiesPlugin.isSetterThrows();
		final DeepClonePlugin deepClonePlugin = PluginUtil.findPlugin(opt, DeepClonePlugin.class);
		final boolean needsCloneMethod = deepClonePlugin != null;
		final boolean cloneMethodThrows = needsCloneMethod && deepClonePlugin.isThrowCloneNotSupported();
		final boolean immutable = PluginUtil.hasPlugin(opt, ImmutablePlugin.class);

		final NameConverter nameConverter = outline.getModel().getNameConverter();
		final Map<QName, TypeDef<XSAttContainer>> groupInterfaces = new HashMap<QName, TypeDef<XSAttContainer>>();
		final Iterator<XSAttGroupDecl> attributeGroupIterator = outline.getModel().schemaComponent.iterateAttGroupDecls();
		while (attributeGroupIterator.hasNext()) {
			final XSAttGroupDecl attributeGroup = attributeGroupIterator.next();
			final PackageOutline packageOutline = GroupInterfacePlugin.findPackageForNamespace(outline, attributeGroup.getTargetNamespace());
			if (packageOutline != null) {
				final JPackage container = packageOutline._package();

				final List<ClassOutline> implementingClasses = GroupInterfacePlugin.findImplementingClasses(outline, attributeGroup);
				if (!implementingClasses.isEmpty()) {
					final ClassOutline implementingClass = implementingClasses.get(0);

					final JDefinedClass groupInterface = outline.getClassFactory().createInterface(container,
							nameConverter.toClassName(attributeGroup.getName()),
							attributeGroup.getLocator());

					for (final XSAttributeUse attributeUse : attributeGroup.getAttributeUses()) {
						if (attributeUse.getFixedValue() == null) {
							final JMethod implementedGetter = GroupInterfacePlugin.findGetter(nameConverter, implementingClass, attributeUse);
							final JMethod newGetter = groupInterface.method(JMod.NONE, implementedGetter.type(),
									implementedGetter.name());
							if (!immutable) {
								final JMethod implementedSetter = GroupInterfacePlugin.findSetter(nameConverter, implementingClass, attributeUse);
								if (implementedSetter != null) {
									final JMethod newSetter = groupInterface.method(JMod.NONE, implementedSetter.type(),
											implementedSetter.name());
									newSetter.param(implementedSetter.listParamTypes()[0], implementedSetter.listParams()[0].name());
									if (throwsPropertyVetoException) {
										newSetter._throws(PropertyVetoException.class);
									}
								}
							}
						}
					}

					if (needsCloneMethod) {
						final JMethod cloneMethod = groupInterface.method(JMod.PUBLIC, groupInterface, "clone");
						if (cloneMethodThrows) {
							cloneMethod._throws(CloneNotSupportedException.class);
						}
					}

					groupInterfaces.put(new QName(attributeGroup.getTargetNamespace(), attributeGroup.getName()), new TypeDef<XSAttContainer>(
							groupInterface,
							attributeGroup));
				}

			}
		}
		return groupInterfaces;
	}

	private static class TypeDef<T extends XSComponent> {
		final JDefinedClass definedClass;
		final T schemaComponent;

		TypeDef(final JDefinedClass definedClass, final T schemaComponent) {
			this.definedClass = definedClass;
			this.schemaComponent = schemaComponent;
		}
	}

}