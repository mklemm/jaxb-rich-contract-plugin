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

import java.beans.PropertyVetoException;
import java.io.StringWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.kscs.util.jaxb.PropertyVisitor;
import com.kscs.util.jaxb._interface.Interface;
import com.kscs.util.jaxb._interface.Interfaces;
import com.kscs.util.plugins.xjc.base.AbstractXSFunction;
import com.kscs.util.plugins.xjc.outline.DefinedInterfaceOutline;
import com.kscs.util.plugins.xjc.outline.InterfaceOutline;
import com.kscs.util.plugins.xjc.outline.ReferencedInterfaceOutline;
import com.kscs.util.plugins.xjc.outline.TypeOutline;
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.util.JavadocEscapeWriter;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.PackageOutline;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIProperty;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BindInfo;
import com.sun.xml.xsom.XSAttContainer;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSDeclaration;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.impl.util.SchemaWriter;
import com.sun.xml.xsom.visitor.XSFunction;

import jakarta.xml.bind.JAXB;

import static com.kscs.util.plugins.xjc.PluginContext.coalesce;

/**
 * @author mirko 2014-05-29
 */
class GroupInterfaceGenerator {
	public static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
	private static final XSFunction<Boolean> IS_FIXED_FUNC = new AbstractXSFunction<Boolean>() {

		@Override
		public Boolean attributeDecl(final XSAttributeDecl decl) {
			return decl.getFixedValue() != null;
		}

		@Override
		public Boolean attributeUse(final XSAttributeUse use) {
			return use.getFixedValue() != null || use.getDecl().getFixedValue() != null;
		}

		@Override
		public Boolean elementDecl(final XSElementDecl decl) {
			return decl.getFixedValue() != null;
		}
	};
	private static final Logger LOGGER = Logger.getLogger(GroupInterfaceGenerator.class.getName());
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(GroupInterfaceGenerator.class.getName());
	private final boolean declareBuilderInterface;
	private final boolean declareModifierInterface;
	private final JClass overrideCollectionClass;
	private final boolean declareVisitMethod;
	private final PluginContext pluginContext;
	private final XSFunction<String> nameFunc = new AbstractXSFunction<String>() {

		@Override
		public String attributeDecl(final XSAttributeDecl decl) {
			final String customName = getCustomPropertyName(decl);
			return customName == null ? GroupInterfaceGenerator.this.pluginContext.outline.getModel().getNameConverter().toPropertyName(decl.getName()) : customName;
		}

		@Override
		public String attributeUse(final XSAttributeUse use) {
			String customName = getCustomPropertyName(use);
			customName = customName == null ? getCustomPropertyName(use.getDecl()) : customName;
			return customName == null ? GroupInterfaceGenerator.this.pluginContext.outline.getModel().getNameConverter().toPropertyName(use.getDecl().getName()) : customName;
		}

		@Override
		public String elementDecl(final XSElementDecl decl) {
			final String customName = getCustomPropertyName(decl);
			return customName == null ? GroupInterfaceGenerator.this.pluginContext.outline.getModel().getNameConverter().toPropertyName(decl.getName()) : customName;
		}

		private String getCustomPropertyName(final XSComponent component) {
			if (component.getAnnotation() != null && (component.getAnnotation().getAnnotation() instanceof BindInfo)) {
				final BindInfo bindInfo = (BindInfo) component.getAnnotation().getAnnotation();
				final BIProperty biProperty = bindInfo.get(BIProperty.class);
				if (biProperty != null) {
					final String customPropertyName = biProperty.getPropertyName(false);
					return customPropertyName != null ? customPropertyName : null;
				}
			}
			return null;
		}
	};
	private final boolean throwsPropertyVetoException;
	private final boolean immutable;
	private final boolean cloneMethodThrows;
	private final boolean needsCloneMethod;
	private final boolean needsCopyMethod;
	private final Map<String, List<TypeOutline>> interfacesByClass = new HashMap<>();
	private final EpisodeBuilder episodeBuilder;
	private final Enumeration<URL> upstreamEpisodes;
	private final GroupInterfaceGeneratorSettings settings;

	private Map<QName, ReferencedInterfaceOutline> referencedInterfaces = null;

	public GroupInterfaceGenerator(final PluginContext pluginContext, final Enumeration<URL> upstreamEpisodes, final EpisodeBuilder episodeBuilder, final GroupInterfaceGeneratorSettings settings) {
		this.pluginContext = pluginContext;
		this.settings = settings;
		final ImmutablePlugin immutablePlugin = this.pluginContext.findPlugin(ImmutablePlugin.class);
		this.immutable = !settings.isDeclareSetters() || immutablePlugin != null;
		this.overrideCollectionClass = immutablePlugin != null && immutablePlugin.overrideCollectionClass != null ? this.pluginContext.codeModel.ref(immutablePlugin.overrideCollectionClass) : null;
		final BoundPropertiesPlugin boundPropertiesPlugin = this.pluginContext.findPlugin(BoundPropertiesPlugin.class);
		this.throwsPropertyVetoException = boundPropertiesPlugin != null && boundPropertiesPlugin.isConstrained() && boundPropertiesPlugin.isSetterThrows();
		final DeepClonePlugin deepClonePlugin = this.pluginContext.findPlugin(DeepClonePlugin.class);
		final DeepCopyPlugin deepCopyPlugin = this.pluginContext.findPlugin(DeepCopyPlugin.class);
		this.declareBuilderInterface = settings.isDeclareBuilderInterface() && settings.getBuilderGeneratorSettings() != null;
		this.declareModifierInterface = pluginContext.hasPlugin(ModifierPlugin.class);
		final MetaPlugin metaPlugin = pluginContext.findPlugin(MetaPlugin.class);
		this.declareVisitMethod = metaPlugin != null && metaPlugin.isExtended();
		this.needsCloneMethod = deepClonePlugin != null;
		this.cloneMethodThrows = this.needsCloneMethod && deepClonePlugin.isCloneThrows();
		this.needsCopyMethod = deepCopyPlugin != null;
		this.upstreamEpisodes = upstreamEpisodes;
		this.episodeBuilder = episodeBuilder;
	}

	private static List<XSModelGroupDecl> findModelGroups(final Iterable<XSParticle> modelGroup) {
		final List<XSModelGroupDecl> elementDecls = new ArrayList<>();
		for (final XSParticle child : modelGroup) {
			if (!child.isRepeated() && (child.getTerm() instanceof XSModelGroupDecl)) {
				elementDecls.add((XSModelGroupDecl) child.getTerm());
			}
		}
		return elementDecls;
	}

	private static Collection<? extends XSDeclaration> findModelGroups(final XSComplexType complexType) {
		XSContentType contentType = complexType.getExplicitContent();
		if (contentType == null) {
			contentType = complexType.getContentType();
		}
		final XSParticle particle = contentType.asParticle();
		if (particle != null && !particle.isRepeated()) {
			final XSTerm term = particle.getTerm();
			if (term instanceof XSModelGroupDecl) {
				return Collections.singletonList((XSModelGroupDecl)term);
			} else {
				final XSModelGroup modelGroup = term.asModelGroup();
				return modelGroup != null ? findModelGroups(modelGroup) : Collections.<XSModelGroupDecl>emptyList();
			}
		} else {
			return Collections.emptyList();
		}
	}

	private static Collection<? extends XSDeclaration> findAttributeGroups(final XSComplexType complexType) {
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

	private Map<QName, ReferencedInterfaceOutline> loadInterfaceEpisodes() {
		try {
			final Transformer transformer = TRANSFORMER_FACTORY.newTransformer(new StreamSource(GroupInterfaceGenerator.class.getResource("interface-bindings.xsl").toString()));
			final Map<QName, ReferencedInterfaceOutline> interfaceMappings = new HashMap<>();
			while(this.upstreamEpisodes.hasMoreElements()) {
				final Map<QName, ReferencedInterfaceOutline> result;
				try {
					final StreamSource episodeInput = new StreamSource(this.upstreamEpisodes.nextElement().toString());
					final DOMResult domResult = new DOMResult();
					transformer.transform(episodeInput, domResult);
					final Interfaces interfaces = JAXB.unmarshal(new DOMSource(domResult.getNode()), Interfaces.class);
					final Map<QName, ReferencedInterfaceOutline> interfaceMappings1 = new HashMap<>();
					for (final Interface iface : interfaces.getInterface()) {
						interfaceMappings1.put(new QName(iface.getSchemaComponent().getNamespace(), iface.getSchemaComponent().getName()), new ReferencedInterfaceOutline(this.pluginContext.codeModel.ref(iface.getName()), this.settings.getSupportInterfaceNameSuffix()));
					}
					result = interfaceMappings1;
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}
				interfaceMappings.putAll(result);
			}
			return interfaceMappings;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<PropertyUse> findElementDecls(final XSModelGroupDecl modelGroup) {
		final List<PropertyUse> elementDecls = new ArrayList<>();
		for (final XSParticle child : modelGroup.getModelGroup()) {
			XSTerm term = child.getTerm();
			if (term instanceof XSElementDecl) {
				elementDecls.add(new PropertyUse(term));
			} else if (term instanceof XSModelGroupDecl && ((XSModelGroupDecl)term).getName().equals(modelGroup.getName())) {
				elementDecls.addAll(findElementDecls((XSModelGroupDecl)term));
			}
		}
		return elementDecls;
	}

	private List<PropertyUse> findAttributeDecls(final XSAttGroupDecl attGroupDecl) {
		final List<PropertyUse> attributeDecls = new ArrayList<>();
		for (final XSAttributeUse child : attGroupDecl.getDeclaredAttributeUses()) {
			attributeDecls.add(new PropertyUse(child));
		}
		return attributeDecls;
	}

	private List<PropertyUse> findChildDecls(final XSDeclaration groupDecl) {
		return (groupDecl instanceof XSAttGroupDecl) ? findAttributeDecls((XSAttGroupDecl) groupDecl) : findElementDecls((XSModelGroupDecl) groupDecl);
	}

	private PackageOutline findPackageForNamespace(final String namespaceUri) {
		for (final PackageOutline packageOutline : this.pluginContext.outline.getAllPackageContexts()) {
			if (namespaceUri.equals(packageOutline.getMostUsedNamespaceURI())) {
				return packageOutline;
			}
		}
		return null;
	}

	public List<TypeOutline> getGroupInterfacesForClass(final String className) {
		final List<TypeOutline> interfacesForClass = this.interfacesByClass.get(className);
		final List<TypeOutline> filteredInterfaces = new ArrayList<>();
		if (interfacesForClass != null) {
			for (final TypeOutline typeOutline : interfacesForClass) {
				if (!typeOutline.getImplClass().fullName().equals(className)) {
					filteredInterfaces.add(typeOutline);
				}
			}
		}
		return filteredInterfaces;
	}

	void putGroupInterfaceForClass(final String className, final TypeOutline groupInterface) {
		List<TypeOutline> interfacesForClass = this.interfacesByClass.get(className);
		if (interfacesForClass == null) {
			interfacesForClass = new ArrayList<>();
			this.interfacesByClass.put(className, interfacesForClass);
		}
		interfacesForClass.add(groupInterface);
	}

	private Map<QName, DefinedInterfaceOutline> generateGroupInterfaces(final Iterator<? extends XSDeclaration> groupIterator) throws SAXException {
		final Map<QName, DefinedInterfaceOutline> groupInterfaces = new HashMap<>();

		// create interface for each group
		while (groupIterator.hasNext()) {
			final XSDeclaration modelGroup = groupIterator.next();
			if (!getReferencedInterfaces().containsKey(PluginContext.getQName(modelGroup))) {
				final DefinedInterfaceOutline interfaceOutline = createInterfaceDeclaration(modelGroup);
				if (interfaceOutline != null) {
					groupInterfaces.put(interfaceOutline.getName(), interfaceOutline);
					if (this.episodeBuilder != null) {
						this.episodeBuilder.addInterface(interfaceOutline.getSchemaComponent(), interfaceOutline.getImplClass());
					}
				}
			}
		}

		// Associate interfaces with superinterfaces
		for (final DefinedInterfaceOutline typeDef : groupInterfaces.values()) {
			final XSDeclaration classComponent = typeDef.getSchemaComponent();
			final Collection<? extends XSDeclaration> groupRefs = (classComponent instanceof XSAttGroupDecl) ? ((XSAttGroupDecl) classComponent).getAttGroups() : findModelGroups(((XSModelGroupDecl) classComponent).getModelGroup());
			for (final XSDeclaration groupRef : groupRefs) {
				if (!PluginContext.getQName(groupRef).equals(typeDef.getName())) {
					InterfaceOutline superInterfaceOutline = groupInterfaces.get(PluginContext.getQName(groupRef));
					if (superInterfaceOutline == null) {
						superInterfaceOutline = getReferencedInterfaceOutline(PluginContext.getQName(groupRef));
					}
					if (superInterfaceOutline != null) {
						typeDef.addSuperInterface(superInterfaceOutline);
						typeDef.getImplClass()._implements(superInterfaceOutline.getImplClass());
						if(typeDef.getSupportInterface() != null) {
							typeDef.getSupportInterface()._implements(superInterfaceOutline.getSupportInterface());
						}
						putGroupInterfaceForClass(typeDef.getImplClass().fullName(), superInterfaceOutline);
					}
				}
			}
		}

		return groupInterfaces;
	}

	public void generateGroupInterfaceModel() throws SAXException {
		final Map<QName, DefinedInterfaceOutline> modelGroupInterfaces = generateGroupInterfaces(this.pluginContext.outline.getModel().schemaComponent.iterateModelGroupDecls());
		final Map<QName, DefinedInterfaceOutline> attGroupInterfaces = generateGroupInterfaces(this.pluginContext.outline.getModel().schemaComponent.iterateAttGroupDecls());

		for (final ClassOutline classOutline : this.pluginContext.outline.getClasses()) {
			final XSComponent xsTypeComponent = classOutline.target.getSchemaComponent();
			final XSComplexType classComponent = getTypeDefinition(xsTypeComponent);
			if (classComponent != null) {
				generateImplementsEntries(attGroupInterfaces, classOutline, findAttributeGroups(classComponent));
				generateImplementsEntries(modelGroupInterfaces, classOutline, findModelGroups(classComponent));
			}
		}

		for (final DefinedInterfaceOutline interfaceOutline : modelGroupInterfaces.values()) {
			removeDummyImplementation(interfaceOutline);
		}
		for (final DefinedInterfaceOutline interfaceOutline : attGroupInterfaces.values()) {
			removeDummyImplementation(interfaceOutline);
		}

		if (this.declareBuilderInterface) {
			final Map<String, BuilderOutline> builderOutlines = new HashMap<>();
			for (final DefinedInterfaceOutline interfaceOutline : modelGroupInterfaces.values()) {
				generateBuilderInterface(builderOutlines, interfaceOutline);
			}
			for (final DefinedInterfaceOutline interfaceOutline : attGroupInterfaces.values()) {
				generateBuilderInterface(builderOutlines, interfaceOutline);
			}

			for (final BuilderOutline builderOutline : builderOutlines.values()) {
				final BuilderGenerator builderGenerator = new BuilderGenerator(this.pluginContext, builderOutlines, builderOutline, this.settings.getBuilderGeneratorSettings());
				builderGenerator.buildProperties();
			}
		}


		if(this.declareModifierInterface) {
			final ModifierPlugin modifierPlugin = this.pluginContext.findPlugin(ModifierPlugin.class);
			for (final DefinedInterfaceOutline interfaceOutline : modelGroupInterfaces.values()) {
				try {
					ModifierGenerator.generateInterface(this.pluginContext, interfaceOutline, modifierPlugin.modifierClassName, interfaceOutline.getSuperInterfaces(), modifierPlugin.modifierMethodName);
				} catch (JClassAlreadyExistsException e) {
					this.pluginContext.errorHandler.error(new SAXParseException(e.getMessage(), interfaceOutline.getSchemaComponent().getLocator()));
				}
			}
			for (final DefinedInterfaceOutline interfaceOutline : attGroupInterfaces.values()) {
				try {
					ModifierGenerator.generateInterface(this.pluginContext, interfaceOutline, modifierPlugin.modifierClassName, interfaceOutline.getSuperInterfaces(), modifierPlugin.modifierMethodName);
				} catch (JClassAlreadyExistsException e) {
					this.pluginContext.errorHandler.error(new SAXParseException(e.getMessage(), interfaceOutline.getSchemaComponent().getLocator()));
				}
			}
		}

	}

	private void generateBuilderInterface(final Map<String, BuilderOutline> builderOutlines, final DefinedInterfaceOutline interfaceOutline) throws SAXException {
		try {
			builderOutlines.put(interfaceOutline.getImplClass().fullName(), new BuilderOutline(interfaceOutline,
					/* interfaceOutline.getImplClass()._class(JMod.NONE, this.settings.getBuilderGeneratorSettings().getFluentClassName().getInterfaceName(), ClassType.INTERFACE) */
					interfaceOutline.getImplClass()._class(JMod.NONE, this.settings.getBuilderGeneratorSettings().getBuilderClassName().getInterfaceName(), ClassType.INTERFACE)
					/*interfaceOutline.getImplClass()._class(JMod.NONE, this.settings.getBuilderGeneratorSettings().getWrapperClassName().getInterfaceName(), ClassType.INTERFACE),
					interfaceOutline.getImplClass()._class(JMod.NONE, this.settings.getBuilderGeneratorSettings().getModifierClassName().getInterfaceName(), ClassType.INTERFACE) */
					));
		} catch (final JClassAlreadyExistsException e) {
			this.pluginContext.errorHandler.error(new SAXParseException(MessageFormat.format(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("error.interface-exists"), interfaceOutline.getImplClass().fullName(), PluginContext.BUILDER_INTERFACE_NAME), interfaceOutline.getSchemaComponent().getLocator()));
		}
	}

	private void removeDummyImplementation(final DefinedInterfaceOutline interfaceOutline) {
		final var allClasses = this.pluginContext.outline.getClasses(); // save class list here before we remove anything, which will cause an assertion failure
		final ClassOutline classToRemove = interfaceOutline.getClassOutline();
		if (classToRemove != null) {
			final List<JMethod> methodsToRemove = new ArrayList<>();
			for (final JMethod method : classToRemove._package().objectFactory().methods()) {
				if (method.name().equals("create" + classToRemove.implClass.name())) {
					methodsToRemove.add(method);
				}
			}
			for (final JMethod method : methodsToRemove) {
				classToRemove._package().objectFactory().methods().remove(method);
			}
			this.pluginContext.outline.getModel().beans().remove(classToRemove.target.getClazz());
			allClasses.remove(classToRemove);
		}
	}

	private void generateImplementsEntries(final Map<QName, DefinedInterfaceOutline> groupInterfaces, final ClassOutline classOutline, final Iterable<? extends XSDeclaration> groupUses) throws SAXException {
		for (final XSDeclaration groupUse : groupUses) {
			final DefinedInterfaceOutline definedGroupType = groupInterfaces.get(PluginContext.getQName(groupUse));
			if (definedGroupType == null) {
				final ReferencedInterfaceOutline referencedInterfaceOutline = getReferencedInterfaceOutline(PluginContext.getQName(groupUse));
				if(referencedInterfaceOutline == null) {
					final String interfaceName = this.pluginContext.outline.getModel().getNameConverter().toClassName(groupUse.getName());
					this.pluginContext.errorHandler.error(new SAXParseException(MessageFormat.format(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("error.interface-not-found"), groupUse.getName(), interfaceName), groupUse.getLocator()));
				} else {
					classOutline.implClass._implements(coalesce(referencedInterfaceOutline.getSupportInterface(), referencedInterfaceOutline.getImplClass()));
					putGroupInterfaceForClass(classOutline.implClass.fullName(), referencedInterfaceOutline);
				}

			} else {
				classOutline.implClass._implements(coalesce(definedGroupType.getSupportInterface(),definedGroupType.getImplClass()));
				putGroupInterfaceForClass(classOutline.implClass.fullName(), definedGroupType);
			}
		}
	}

	private DefinedInterfaceOutline createInterfaceDeclaration(final XSDeclaration groupDecl) throws SAXException {
		final PackageOutline packageOutline = findPackageForNamespace(groupDecl.getTargetNamespace());
		if (packageOutline == null) {
			this.pluginContext.errorHandler.error(new SAXParseException(MessageFormat.format(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("error.package-not-found"), groupDecl.getTargetNamespace()), groupDecl.getLocator()));
			return null;
		}

		final JPackage container = packageOutline._package();
		final ClassOutline dummyImplementation = this.pluginContext.classesBySchemaComponent.get(PluginContext.getQName(groupDecl));
		if (dummyImplementation == null) {
			this.pluginContext.errorHandler.error(new SAXParseException(MessageFormat.format(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("error.no-implementation"), this.pluginContext.outline.getModel().getNameConverter().toClassName(groupDecl.getName()), groupDecl.getTargetNamespace(), groupDecl.getName()), groupDecl.getLocator()));
			return null;
		}

		final String interfaceName = dummyImplementation.implClass.name();
		container.remove(dummyImplementation.implClass);
		final JDefinedClass groupInterface;
		final JDefinedClass supportInterface;
		try {
			groupInterface = container._interface(JMod.PUBLIC, interfaceName);
			supportInterface = this.settings.isGeneratingSupportInterface() ? container._interface(JMod.PUBLIC, interfaceName + this.settings.getSupportInterfaceNameSuffix())._implements(groupInterface) : null;
		} catch (final JClassAlreadyExistsException e) {
			this.pluginContext.errorHandler.error(new SAXParseException(MessageFormat.format(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("error.interface-exists"), interfaceName, ""), groupDecl.getLocator()));
			return null;
		}
		final DefinedInterfaceOutline interfaceDecl = new DefinedInterfaceOutline(groupDecl, groupInterface, dummyImplementation, supportInterface);

		// Generate Javadoc with schema fragment
		final StringWriter out = new StringWriter();
		out.write("<pre>\n");
		final SchemaWriter sw = new SchemaWriter(new JavadocEscapeWriter(out));
		groupDecl.visit(sw);
		out.write("</pre>");

		final JDocComment comment = groupInterface.javadoc();
		comment.append(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("comment.generated-from-xs-decl.header")).
				append("\n").
				append(MessageFormat.format(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("comment.generated-from-xs-decl.qname"),
						groupDecl.getTargetNamespace(),
						groupDecl.getName())).
				append("\n").
				append(MessageFormat.format(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("comment.generated-from-xs-decl.locator"),
						groupDecl.getLocator().getSystemId(),
						groupDecl.getLocator().getLineNumber(),
						groupDecl.getLocator().getColumnNumber()))
				.append("\n")
				.append(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("comment.generated-from-xs-decl.source"))
				.append("\n")
				.append(out.toString());


		for (final PropertyUse propertyUse : findChildDecls(groupDecl)) {
			final FieldOutline field = findField(dummyImplementation, propertyUse);
			if (field != null) {
				generateProperty(interfaceDecl, field);
			}
		}

		if(this.declareVisitMethod) {
			final JDefinedClass target = supportInterface != null ? supportInterface : groupInterface;
			target.method(JMod.NONE, target, this.pluginContext.findPlugin(MetaPlugin.class).getVisitMethodName()).param(JMod.FINAL, PropertyVisitor.class, "visitor_");
		}

		return interfaceDecl;
	}


	private FieldOutline findField(final ClassOutline implClass, final PropertyUse propertyUse) throws SAXException {
		if (!propertyUse.isFixed()) {
			for (final FieldOutline field : implClass.getDeclaredFields()) {
				if (field.getPropertyInfo().getName(true).equals(propertyUse.getName())) {
					return field;
				}
			}
			this.pluginContext.errorHandler.error(new SAXParseException(MessageFormat.format(GroupInterfaceGenerator.RESOURCE_BUNDLE.getString("error.property-not-found"), propertyUse.declaration.toString(), propertyUse.getName(), implClass.implClass.fullName()), propertyUse.declaration.getLocator()));
		}
		return null;
	}

	private FieldOutline generateProperty(final DefinedInterfaceOutline groupInterface, final FieldOutline implementedField) {
		if (implementedField != null) {
			final JMethod implementedGetter = PluginContext.findGetter(implementedField);
			if (implementedGetter != null) {
				if(this.overrideCollectionClass != null && implementedField.getPropertyInfo().isCollection()) {
					groupInterface.getImplClass().method(JMod.NONE, this.overrideCollectionClass.narrow(((JClass)implementedGetter.type()).getTypeParameters().get(0)), implementedGetter.name());
				} else {
					groupInterface.getImplClass().method(JMod.NONE, implementedGetter.type(), implementedGetter.name());
				}
				if (!this.immutable) {
					final JMethod implementedSetter = PluginContext.findSetter(implementedField);
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

	private ReferencedInterfaceOutline getReferencedInterfaceOutline(final QName schemaComponent) {
		return getReferencedInterfaces().get(schemaComponent);
	}

	Map<QName, ReferencedInterfaceOutline> getReferencedInterfaces() {
		if (this.referencedInterfaces == null) {
			if (this.upstreamEpisodes != null) {
				this.referencedInterfaces = loadInterfaceEpisodes();
			} else {
				this.referencedInterfaces = Collections.emptyMap();
			}
		}
		return this.referencedInterfaces;
	}

	private class PropertyUse {
		final XSComponent declaration;

		PropertyUse(final XSComponent declaration) {
			this.declaration = declaration;
		}

		String getName() {
			return this.declaration.apply(GroupInterfaceGenerator.this.nameFunc);
		}

		boolean isFixed() {
			return this.declaration.apply(GroupInterfaceGenerator.IS_FIXED_FUNC);
		}

	}


}

