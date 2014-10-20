package com.kscs.util.plugins.xjc;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * Plugin that creates a listing of all source packages
 * included in this XJC module.
 */
public class PackageMappingPlugin extends Plugin {
	@Override
	public String getOptionName() {
		return "Xpackage-mapping";
	}

	@Override
	public String getUsage() {
		return null;
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		return false;
	}
}
