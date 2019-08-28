package com.kscs.util.plugins.xjc;

import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CElementPropertyInfo;
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
