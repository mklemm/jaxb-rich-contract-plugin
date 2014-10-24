package com.kscs.util.plugins.xjc;

import com.sun.codemodel.fmt.JPropertyFile;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * Plugin that creates a listing of all source packages
 * included in this XJC module.
 */
public class PackageMappingPlugin extends AbstractPlugin {
	private volatile String fileName = "META-INF/jaxb-packages.properties";

	public PackageMappingPlugin() {
		super("package-mapping");
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final JPropertyFile mappingFile = new JPropertyFile(this.fileName);

		for(final PackageOutline packageOutline : outline.getAllPackageContexts()) {

			final String name = packageOutline._package().name();
			final String namespaceURI = packageOutline.getMostUsedNamespaceURI();
			mappingFile.add(name, namespaceURI == null ? "" : namespaceURI);
		}

		outline.getCodeModel().rootPackage().addResourceFile(mappingFile);
		return true;
	}
}
