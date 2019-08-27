package com.kscs.util.plugins.xjc;

import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BindInfo;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.impl.AttributeUseImpl;
import com.sun.xml.xsom.impl.ParticleImpl;

public class SchemaAnnotationUtils {

    public static String getClassAnnotationDescription(CClassInfo classInfo){
        XSAnnotation annotation = classInfo.getSchemaComponent().getAnnotation();
        return resolveDescription(annotation);
    }

    public static String getFieldAnnotationDescription(CPropertyInfo propertyInfo) {
        XSAnnotation annotation = resolveXSAnnotation(propertyInfo.getSchemaComponent());
        return resolveDescription(annotation);
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
        XSAnnotation annotation;
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
