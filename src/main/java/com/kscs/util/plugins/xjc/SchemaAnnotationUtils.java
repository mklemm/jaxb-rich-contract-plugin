/*
 * MIT License
 *
 * Copyright 2019 Crown Copyright
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

import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CElementPropertyInfo;
import com.sun.tools.xjc.model.CEnumLeafInfo;
import com.sun.tools.xjc.model.CNonElement;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BindInfo;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.impl.AttributeUseImpl;
import com.sun.xml.xsom.impl.ParticleImpl;

// Credit for some of this code goes to
// https://github.com/Hubbitus/xjc-documentation-annotation-plugin (also licensed under MIT)
// and to https://github.com/Nogyara for his input to an issue in that repo.

public class SchemaAnnotationUtils {

    private SchemaAnnotationUtils() {
    }

    public static String getClassAnnotationDescription(CClassInfo classInfo) {
        // To get annotations in the class level javadoc, there needs to be an annotation
        // element as a child of the complexType element (be it named or anonymous), e.g.
        //<xs:element name="SomeElement">
        //   <xs:complexType>
        //      <xs:annotation>
        //         <xs:documentation>This annotation will be used in the class javadoc</xs:documentation>
        //      </xs:annotation>
        // or
        //   <xs:complexType name="SomeComplexType">
        //      <xs:annotation>
        //         <xs:documentation>This annotation will be used in the class javadoc</xs:documentation>
        //      </xs:annotation>
        XSAnnotation annotation = classInfo.getSchemaComponent().getAnnotation();
        return resolveDescription(annotation);
    }

    public static String getEnumAnnotationDescription(CEnumLeafInfo enumLeafInfo) {
        // To get annotations in the class level javadoc, there needs to be an annotation
        // element as a child of the simpleType element , e.g.
        //<xs:simpleType name="SomeSimpleType">
        //   <xs:annotation>
        //      <xs:documentation>This annotation will be used in the class javadoc</xs:documentation>
        //   </xs:annotation>
        XSAnnotation annotation = enumLeafInfo.getSchemaComponent().getAnnotation();
        return resolveDescription(annotation);
    }

    public static String getFieldAnnotationDescription(CPropertyInfo propertyInfo) {
        XSAnnotation annotation = resolveXSAnnotation(propertyInfo.getSchemaComponent());
        String annotationText = resolveDescription(annotation);

        // if there is no annotation on the element itself then see if there is an annotation for the type
        // of that element (e.g. its anonymous or named type)

        //<xs:element name="SomeElement">
        //   <xs:annotation>
        //      <xs:documentation>This is the annotation for the element so will be considered first.</xs:documentation>
        //   </xs:annotation>
        //   <xs:complexType>
        //      <xs:annotation>
        //         <xs:documentation>This is the annotation for the type so will be considered second</xs:documentation>
        //      </xs:annotation>
        if (annotationText.isEmpty()) {
            if (propertyInfo instanceof CElementPropertyInfo) {
                CElementPropertyInfo elementPropertyInfo = (CElementPropertyInfo) propertyInfo;
                if (elementPropertyInfo.getTypes() != null && !elementPropertyInfo.getTypes().isEmpty()) {
                    CNonElement target = elementPropertyInfo.getTypes().get(0).getTarget();
                    if (target instanceof CClassInfo) {
                        annotationText = getClassAnnotationDescription((CClassInfo) target);
                    }
                }
            }
        }
        return annotationText;
    }

    private static String resolveDescription(XSAnnotation annotation) {
        String description = "";
        if (annotation != null && annotation.getAnnotation() != null) {
            description = ((BindInfo) annotation.getAnnotation()).getDocumentation();
            if (description == null) {
                description = "";
            }
        }
        return description.trim();
    }

    private static XSAnnotation resolveXSAnnotation(XSComponent schemaComponent) {
        final XSAnnotation annotation;
        //<xs:complexType name="MyType">
        //		<xs:attribute name="someAttribute" use="required">
        //			<xs:annotation>
        //				<xs:documentation>This is some text</xs:documentation>
        if (schemaComponent instanceof AttributeUseImpl) {
            annotation = ((AttributeUseImpl) schemaComponent).getDecl().getAnnotation();
        }
        // <xs:complexType name="MyType">
        //		<xs:element name="someAttribute" type="stCom:TInterdepStatementHeader" minOccurs="0">
        //			<xs:annotation>
        //				<xs:documentation>This is some text</xs:documentation>
        else if (schemaComponent instanceof ParticleImpl) {
            annotation = (((ParticleImpl) schemaComponent).getTerm()).getAnnotation();
        } else {
            annotation = schemaComponent.getAnnotation();
        }
        return annotation;
    }

}
