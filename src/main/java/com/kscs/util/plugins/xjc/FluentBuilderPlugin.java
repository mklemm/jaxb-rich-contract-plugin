package com.kscs.util.plugins.xjc;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plugin to generate fluent Builders for generated classes
 */
public class FluentBuilderPlugin extends Plugin {
	private boolean supportBuilderChain = true;

	public FluentBuilderPlugin() {
		// Needed by JAXB Framework
	}

	protected FluentBuilderPlugin(final boolean supportBuilderChain) {
		this.supportBuilderChain = supportBuilderChain;
	}

	@Override
	public String getOptionName() {
		return "Xfluent-builder";
	}

	@Override
	public int parseArgument(final Options opt, final String[] args, final int i) throws BadCommandLineException, IOException {
		final PluginUtil.Arg<Boolean> arg = PluginUtil.parseBooleanArgument("support-builder-chain", this.supportBuilderChain, opt, args, i);
		this.supportBuilderChain = arg.getValue();
		return arg.getArgsParsed();
	}

	@Override
	public String getUsage() {
		return " -Xfluent-builder: Generates an inner \"fluent builder\" for each of the generated classes.\n\tOptions:\n\t\t-support-builder-chain=<yes/no>:\tGenerate extended fluent builders that will allow child objects to be created in the fluent chain.";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {

		final Map<String, BuilderOutline> builderClasses = new LinkedHashMap<String, BuilderOutline>(outline.getClasses().size());

		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;
			try {
				final BuilderOutline builderOutline = new BuilderOutline(classOutline);
				builderClasses.put(definedClass.fullName(), builderOutline);
			} catch (final JClassAlreadyExistsException caex) {
				errorHandler.warning(new SAXParseException("Class \"" + definedClass.name() + "\" already contains inner class \"Builder\". Skipping generation of fluent builder.", classOutline.target.getLocator(), caex));
			}
		}

		final ApiConstructs apiConstructs = new ApiConstructs(outline.getCodeModel(), builderClasses, opt);

		for (final BuilderOutline builderOutline : builderClasses.values()) {
			final BuilderGenerator builderGenerator = this.supportBuilderChain ? new ChainedBuilderGenerator(apiConstructs, builderOutline) : new SimpleBuilderGenerator(apiConstructs, builderOutline);
			builderGenerator.buildProperties();
		}
		return true;
	}

}
