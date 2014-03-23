package com.kscs.util.plugins.xjc;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * @description
 */
public class ImmutablePlugin extends Plugin {

	@Override
	public String getOptionName() {
		return "Ximmutable";
	}

	@Override
	public String getUsage() {
		return "-Ximmutable: Make generated classes immutable. All property setters will be generated as \"protected\".";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			for(final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
				final String setterName = "set" + fieldOutline.getPropertyInfo().getName(true);
				final JMethod setterMethod = definedClass.getMethod(setterName, new JType[] {fieldOutline.getRawType()});
				if(setterMethod != null) {
					setterMethod.mods().setProtected();
				}
			}
		}
		return true;
	}
}
