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

package com.kscs.util.plugins.xjc.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.glassfish.jaxb.core.v2.model.core.Element;
import org.glassfish.jaxb.core.v2.model.core.ElementPropertyInfo;
import org.glassfish.jaxb.core.v2.model.core.ReferencePropertyInfo;
import org.glassfish.jaxb.core.v2.model.core.TypeRef;

import com.kscs.util.plugins.xjc.PluginContext;
import com.kscs.util.plugins.xjc.SchemaAnnotationUtils;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.model.nav.NType;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;

import jakarta.xml.bind.JAXBElement;

/**
 * @author Mirko Klemm 2015-01-28
 */
public class DefinedPropertyOutline implements PropertyOutline {
	private final FieldOutline fieldOutline;
	private final List<TagRef> referencedItems;
	private final Map<TagRef, String> referencedAnnotations;
	private final JClass jaxbElementClass;
	private final String annotationText;
	private final JClass mutableListClass;
	private final JDefinedClass referencedModelClass;
	private final boolean choice;

	public DefinedPropertyOutline(final FieldOutline fieldOutline) {
		final var codeModel = fieldOutline.getRawType().owner();
		this.fieldOutline = fieldOutline;
		final CPropertyInfo propertyInfo = fieldOutline.getPropertyInfo();
		this.jaxbElementClass = codeModel.ref(JAXBElement.class);
		final ClassOutline classOutline = fieldOutline.parent();

		// Workaround for bug in XJC generator where the "fullName" given to a type isn't really "full"...
		this.referencedModelClass = fieldOutline.getRawType().fullName().contains(".") ? codeModel._getClass(fieldOutline.getRawType().fullName()) : resolveTypeName(classOutline.getImplClass(), fieldOutline.getRawType().fullName());

		if (propertyInfo instanceof ElementPropertyInfo) {
			final ElementPropertyInfo<NType, NClass> elementPropertyInfo = (ElementPropertyInfo<NType, NClass>)propertyInfo;
			this.referencedItems = new ArrayList<>(elementPropertyInfo.getTypes().size());
			this.referencedAnnotations = new HashMap<>(elementPropertyInfo.getTypes().size());
			for (final TypeRef<NType, NClass> typeRef : elementPropertyInfo.getTypes()) {
				TagRef tagRef = new TagRef(typeRef.getTagName(), typeRef.getTarget());
				this.referencedItems.add(tagRef);
				if (typeRef.getTarget() instanceof CClassInfo) {
					// Get the annotation for the referenced type, i.e. the complex type
					final String referencedItemAnnotation = SchemaAnnotationUtils.getClassAnnotationDescription(
							(CClassInfo)typeRef.getTarget());
					this.referencedAnnotations.put(tagRef, referencedItemAnnotation);
				}
			}
			if (elementPropertyInfo.getTypes() != null && elementPropertyInfo.getTypes().size() > 1) {
				// This appears to be a single java field bound to an xs:choice and there does't appear to be any
				// way of getting the annotation for the xs:choice itself. If we try and get the field annotation
				// we will get the annotation of the first choice. Therefore provide no javadoc.
				this.annotationText = null;
			} else {
				this.annotationText = SchemaAnnotationUtils.getFieldAnnotationDescription(propertyInfo);
			}
		} else if (propertyInfo instanceof ReferencePropertyInfo) {
			final ReferencePropertyInfo<NType, NClass> elementPropertyInfo = (ReferencePropertyInfo<NType, NClass>)propertyInfo;
			this.referencedItems = new ArrayList<>(elementPropertyInfo.getElements().size());
			this.referencedAnnotations = new HashMap<>(elementPropertyInfo.getElements().size());
			for (final Element<NType, NClass> element : elementPropertyInfo.getElements()) {
				this.referencedItems.add(new TagRef(element.getElementName(), element));
			}
			this.annotationText = SchemaAnnotationUtils.getFieldAnnotationDescription(propertyInfo);
		} else {
			this.referencedItems = Collections.emptyList();
			this.referencedAnnotations = Collections.emptyMap();
			this.annotationText = SchemaAnnotationUtils.getFieldAnnotationDescription(propertyInfo);
		}
		this.mutableListClass = PluginContext.extractMutableListClass(fieldOutline);
		this.choice = this.referencedItems.size() > 1;
	}

	@Override
	public String getBaseName() {
		return this.fieldOutline.getPropertyInfo().getName(true);
	}

	@Override
	public String getFieldName() {
		return this.fieldOutline.getPropertyInfo().getName(false);
	}

	@Override
	public JType getRawType() {
		return this.fieldOutline.getRawType();
	}

	@Override
	public JType getElementType() {
		if (isCollection() && !getRawType().isArray()) {
			return ((JClass)getRawType()).getTypeParameters().get(0);
		} else {
			return getRawType();
		}
	}

	@Override
	public boolean isIndirect() {
		return this.jaxbElementClass.fullName().equals(getElementType().erasure().fullName());
	}

	@Override
	public JFieldVar getFieldVar() {
		String propertyName = this.fieldOutline.getPropertyInfo().getName(false);
		if ("any".equals(propertyName)) {
			propertyName = "content";
		}
		return this.fieldOutline.parent().implClass.fields().get(propertyName);
	}

	public boolean hasGetter() {
		for (final JMethod method : this.fieldOutline.parent().implClass.methods()) {
			if ((method.name().equals("get" + this.fieldOutline.getPropertyInfo().getName(true))
					|| method.name().equals("is" + this.fieldOutline.getPropertyInfo().getName(true))) && method.params().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCollection() {
		return this.fieldOutline.getPropertyInfo().isCollection();
	}

	@Override
	public List<TagRef> getChoiceProperties() {
		return this.referencedItems;
	}

	@Override
	public Optional<String> getSchemaAnnotationText() {
		if (this.annotationText == null || this.annotationText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(this.annotationText);
		}
	}

	@Override
	public Optional<String> getSchemaAnnotationText(final TagRef tagRef) {
		if (this.referencedAnnotations == null || this.referencedAnnotations.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.ofNullable(this.referencedAnnotations.get(tagRef));
		}
	}

	public boolean isArray() {
		return getRawType().isArray();
	}

	@Override
	public JClass getMutableListClass() {
		return this.mutableListClass;
	}

	public FieldOutline getFieldOutline() {
		return this.fieldOutline;
	}


	@Override
	public JDefinedClass getReferencedModelClass() {
		return referencedModelClass;
	}

	private static JDefinedClass resolveTypeName(final JDefinedClass scope, final String typeName) {
			return iteratorToList(scope.classes()).stream()
					.filter(
							dc -> dc.name().equals(typeName)
					).findFirst()
					.orElse(
							scope.outer() != null ? resolveTypeName((JDefinedClass)scope.outer(), typeName) : iteratorToList(scope.getPackage().classes()).stream()
									.filter(
											dc -> dc.name().equals(typeName)
									).findFirst().orElse(scope.owner()._getClass(typeName)));
		}

		private static <T> List<T> iteratorToList(final Iterator<T> iterator) {
			final var list = new ArrayList<T>();
			iterator.forEachRemaining(list::add);
			return list;
		}

	public boolean isChoice() {
		return choice;
	}
}
