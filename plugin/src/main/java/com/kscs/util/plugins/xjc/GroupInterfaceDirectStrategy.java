/*
 * GNU General Public License
 *
 * Copyright (c) 2018 Klemm Software Consulting, Mirko Klemm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kscs.util.plugins.xjc;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;

import com.kscs.util.plugins.xjc.base.Namespaces;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.reader.Ring;
import com.sun.tools.xjc.reader.xmlschema.BGMBuilder;
import com.sun.tools.xjc.reader.xmlschema.ClassSelector;
import com.sun.tools.xjc.reader.xmlschema.SimpleTypeBuilder;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIClass;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BindInfo;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSDeclaration;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.impl.AnnotationImpl;
import com.sun.xml.xsom.impl.ComplexTypeImpl;
import com.sun.xml.xsom.impl.EmptyImpl;
import com.sun.xml.xsom.impl.ForeignAttributesImpl;
import com.sun.xml.xsom.impl.ModelGroupImpl;
import com.sun.xml.xsom.impl.ParticleImpl;
import com.sun.xml.xsom.impl.Ref;
import com.sun.xml.xsom.impl.SchemaImpl;
import com.sun.xml.xsom.impl.SchemaSetImpl;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

public class GroupInterfaceDirectStrategy implements GroupInterfaceModelProcessingStrategy {
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(GroupInterfaceDirectStrategy.class.getName());

	@Override
	public void onPluginActivation(final Options opts) {
		// do nothing
	}

	@Override
	public void onPostProcessModel(final Model model, final ErrorHandler errorHandler) {
		final ClassSelector cs = Ring.get(ClassSelector.class);
		final SimpleTypeBuilder stb = Ring.get(SimpleTypeBuilder.class);
		final XSSchemaSet schemaSet = Ring.get(XSSchemaSet.class);
		for (final XSSchema s : schemaSet.getSchemas()) {
			// fill in typeUses
			for (final XSAttGroupDecl t : s.getAttGroupDecls().values()) {
				model.typeUses().put(getName(t), cs.bindToType(generateComplexTypeFromAttGroupDecl(model, t), s, true));
			}
			for (final XSModelGroupDecl t : s.getModelGroupDecls().values()) {
				model.typeUses().put(getName(t), cs.bindToType(generateComplexTypeFromModelGroupDecl(model, t), s, true));
			}
		}
	}

	public static QName getName(final XSDeclaration decl) {
		final String local = decl.getName();
		if (local == null) return null;
		return new QName(decl.getTargetNamespace(), local);
	}

	/**
	 * Dirty hack because XJC consumes annotations of modelgroups ond AttributeGroups in the context of the
	 * complexType which use them before even giving them to a plugin
	 * and recognizes them as unacknowledged before we even get a chance to see them.
	 */
	private AnnotationHolder replaceInterfaceAnnotation(final Model model, final XSDeclaration component) {
		return model.getCustomizations().stream().filter(GroupInterfaceDirectStrategy::isInterfaceDecl).filter(c -> isAssignedToComponent(c, component)).findFirst().map(
				inputAnnotation -> {
					inputAnnotation.markAsAcknowledged();
					try {
						final var jaxbContext = JAXBContext.newInstance(BIClass.class);
						final var unmarshaller = jaxbContext.createUnmarshaller();
						final var newElement = inputAnnotation.element.getOwnerDocument().createElementNS(Namespaces.JAXB_NS, "class");
						if (!inputAnnotation.element.getAttribute("name").isEmpty()) {
							newElement.setAttribute("name", inputAnnotation.element.getAttribute("name"));
						}
						if (!inputAnnotation.element.getAttribute("ref").isEmpty()) {
							newElement.setAttribute("ref", inputAnnotation.element.getAttribute("ref"));
						}
						final var biClass = unmarshaller.unmarshal(newElement, BIClass.class);
						final var newBindInfo = new BindInfo();
						newBindInfo.setOwner(Ring.get(BGMBuilder.class), component);
						newBindInfo.addDecl(biClass.getValue());
						return new AnnotationHolder(new AnnotationImpl(newBindInfo, component.getLocator()), (AnnotationImpl)component.getAnnotation());
					} catch (final JAXBException e) {
						throw new RuntimeException(e);
					}
				}
		).orElse(new AnnotationHolder((AnnotationImpl)component.getAnnotation(), (AnnotationImpl)component.getAnnotation()));
	}

	private static boolean isInterfaceDecl(final CPluginCustomization customization) {
		return customization.element.getNamespaceURI().equals(Namespaces.KSCS_BINDINGS_NS) && customization.element.getLocalName().equals("interface");
	}

	private static boolean isAssignedToComponent(final CPluginCustomization customization, XSDeclaration component) {
		return customization.element.getAttribute("groupName").equals(component.getName()) && customization.element.getAttribute("groupNamespace").equals(component.getTargetNamespace());
	}

	private static class AnnotationHolder {
		private final AnnotationImpl annotation;
		private final AnnotationImpl originalAnnotation;

		private AnnotationHolder(final AnnotationImpl annotation, final AnnotationImpl originalAnnotation) {
			this.annotation = annotation;
			this.originalAnnotation = originalAnnotation;
		}

		final void setOwner(final XSComponent owner) {
			if (this.annotation != null) {
				final var bindInfo = (BindInfo)this.annotation.getAnnotation();
				bindInfo.setOwner(bindInfo.getBuilder(), owner);
			}
		}
	}

	private ComplexTypeImpl generateComplexTypeFromAttGroupDecl(final Model model, final XSAttGroupDecl attGroupDecl) {
		final var schemaSet = model.schemaComponent;
		final var replacedAnnotation = replaceInterfaceAnnotation(model, attGroupDecl);
		final ComplexTypeImpl complexType = new ComplexTypeImpl(
				(SchemaDocumentImpl)attGroupDecl.getSourceDocument(),
				replacedAnnotation.annotation,
				attGroupDecl.getLocator(),
				(ForeignAttributesImpl)attGroupDecl.getForeignAttributes().get(0),
				attGroupDecl.getName(),
				false,
				true,
				XSType.EXTENSION,
				(SchemaSetImpl.AnyType)schemaSet.getAnyType(),
				0,
				0,
				false
		);
		complexType.addAttGroup(new AttGroup(
				attGroupDecl.getLocator(),
				(SchemaImpl)attGroupDecl.getOwnerSchema(),
				attGroupDecl)
		);
		complexType.setContentType(new EmptyImpl());
		complexType.setExplicitContent(new EmptyImpl());
		replacedAnnotation.setOwner(complexType);
		return complexType;
	}

	private ComplexTypeImpl generateComplexTypeFromModelGroupDecl(final Model model, final XSModelGroupDecl modelGroupDecl) {
		final var schemaSet = model.schemaComponent;
		final var replacedAnnotation = replaceInterfaceAnnotation(model, modelGroupDecl);
		final SchemaDocumentImpl sourceDocument = (SchemaDocumentImpl)modelGroupDecl.getSourceDocument();
		final SchemaImpl ownerSchema = (SchemaImpl)modelGroupDecl.getOwnerSchema();
		final Locator locator = modelGroupDecl.getLocator();
		final ComplexTypeImpl complexType = new ComplexTypeImpl(
				sourceDocument,
				replacedAnnotation.annotation,
				locator,
				(ForeignAttributesImpl)modelGroupDecl.getForeignAttributes().get(0),
				modelGroupDecl.getName(),
				modelGroupDecl.isLocal(),
				true,
				XSType.EXTENSION,
				(SchemaSetImpl.AnyType)schemaSet.getAnyType(),
				0,
				0,
				false
		);
		final var annotation = replacedAnnotation.originalAnnotation;
		final var modelGroupRefParticle = new ParticleImpl(sourceDocument,
				annotation,
				new ModelGroup(
						locator,
						ownerSchema,
						modelGroupDecl
				),
				locator);
		final var contentModelGroup = new ModelGroupImpl(sourceDocument,
				annotation,
				locator,
				(ForeignAttributesImpl)modelGroupDecl.getForeignAttributes().get(0), XSModelGroup.Compositor.SEQUENCE, new ParticleImpl[]{modelGroupRefParticle});
		complexType.setContentType(new ParticleImpl(sourceDocument,
				annotation, contentModelGroup, locator));
		complexType.setExplicitContent(new EmptyImpl());
		replacedAnnotation.setOwner(complexType);
		return complexType;
	}

	private static String formatMessage(final String messageKey, final String... args) {
		return MessageFormat.format(GroupInterfaceDirectStrategy.RESOURCE_BUNDLE.getString(messageKey), (Object[])args);
	}

	public static class AttGroup implements Ref.AttGroup {
		private final Locator locator;
		private final SchemaImpl schema;
		private final XSAttGroupDecl ref;

		public AttGroup(final Locator locator, final SchemaImpl schema, final XSAttGroupDecl ref) {
			this.locator = locator;
			this.schema = schema;
			this.ref = ref;
		}

		public XSAttGroupDecl get() {
			return this.ref;
		}
	}

	public static class ModelGroup implements Ref.Term {
		private final Locator locator;
		private final SchemaImpl schema;
		private final XSModelGroupDecl ref;

		public ModelGroup(final Locator locator, final SchemaImpl schema, final XSModelGroupDecl ref) {
			this.locator = locator;
			this.schema = schema;
			this.ref = ref;
		}

		public XSModelGroupDecl get() {
			return this.ref;
		}

		public XSTerm getTerm() {
			return this.ref;
		}
	}
}
