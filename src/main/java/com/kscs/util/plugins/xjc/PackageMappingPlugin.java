package com.kscs.util.plugins.xjc;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.codemodel.JResourceFile;
import com.sun.codemodel.fmt.JPropertyFile;
import com.sun.codemodel.fmt.JTextFile;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * Plugin that creates a listing of all source packages
 * included in this XJC module.
 */
public class PackageMappingPlugin extends Plugin {
	private String packageMappingFileName = "META-INF/jaxb-packages.properties";

	@Override
	public String getOptionName() {
		return "Xpackage-mapping";
	}

	@Override
	public String getUsage() {
		return "Generates a package mapping file. Option: package-mapping-file=<filename>, default : " + this.packageMappingFileName;
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		final PluginUtil.Arg<String> arg = PluginUtil.parseStringArgument("package-mapping-file", this.packageMappingFileName, opt, args, i);
		this.packageMappingFileName = arg.getValue();
		return arg.getArgsParsed();
	}


	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		final JPropertyFile mappingFile = new JPropertyFile(this.packageMappingFileName);

		for(final PackageOutline packageOutline : outline.getAllPackageContexts()) {

			final String name = packageOutline._package().name();
			final String namespaceURI = packageOutline.getMostUsedNamespaceURI();
			mappingFile.add(name, namespaceURI == null ? "" : namespaceURI);
		}

		outline.getCodeModel().rootPackage().addResourceFile(mappingFile);
		return true;
	}
}
