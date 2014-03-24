package com.kscs.util.plugins.xjc;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

import java.io.*;

/**
 * Common utilities for XJC plugins
 */
public final class PluginUtil {
	public static final String BOOLEAN_OPTION_ERROR_MSG = " option must be either (\"true\",\"on\",\"y\",\"yes\") or (\"false\", \"off\", \"n\",\"no\").";

	public static void writeSourceFile(final Class<?> thisClass, final File targetDir, final String resourceName) {
		try {
			final String resourcePath = "/" + resourceName.replace('.', '/') + ".java";
			final File targetFile = new File(targetDir.getPath()+resourcePath);
			final File packageDir = targetFile.getParentFile();
			final boolean created = packageDir.mkdirs();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(thisClass.getResourceAsStream(resourcePath)));
			final BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile));
			try {
				String line;
				while((line = reader.readLine()) != null) {
					writer.write(line+"\n");
				}
			} finally {
				reader.close();
				writer.close();
			}
		} catch(final IOException iox) {
			throw new RuntimeException(iox);
		}
	}

	public static boolean hasPlugin(final Options opt, final Class<? extends Plugin> pluginType) {
		for(final Plugin plugin : opt.activePlugins) {
			if(pluginType.isInstance(plugin)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static <P extends Plugin> P findPlugin(final Options opt, final Class<P> pluginType) {
		for(final Plugin plugin : opt.activePlugins) {
			if(pluginType.isInstance(plugin)) {
				return (P)plugin;
			}
		}
		return null;
	}

	public static final class Arg<T> {
		private final T value;
		private final int argsParsed;

		Arg(final T value, final int argsParsed) {
			this.value = value;
			this.argsParsed = argsParsed;
		}

		public T getValue() {
			return this.value;
		}

		public int getArgsParsed() {
			return this.argsParsed;
		}
	}

	public static Arg<Boolean> parseBooleanArgument(final String name, final boolean defaultValue, final Options opt, final String[] args, final int i) throws BadCommandLineException {
		final String arg = args[i].toLowerCase();
		if (arg.startsWith("-"+name.toLowerCase()+"=")) {
			final boolean argTrue = PluginUtil.isTrue(arg);
			final boolean argFalse = PluginUtil.isFalse(arg);
			if (!argTrue && !argFalse) {
				throw new BadCommandLineException("-"+name.toLowerCase()+" " + PluginUtil.BOOLEAN_OPTION_ERROR_MSG);
			} else {
				return new Arg<Boolean>(argTrue, 1);
			}
		}
		return new Arg<Boolean>(defaultValue, 0);
	}

	private static boolean isTrue(final String arg) {
		return arg.endsWith("y") || arg.endsWith("true") || arg.endsWith("on") || arg.endsWith("yes");
	}

	private static boolean isFalse(final String arg) {
		return arg.endsWith("n") || arg.endsWith("false") || arg.endsWith("off") || arg.endsWith("no");
	}

	public static ClassOutline getClassOutline(final Outline outline, final JType typeRef) {
		for(final ClassOutline classOutline : outline.getClasses()) {
			if(typeRef.fullName().equals(classOutline.implClass.fullName())) {
				return classOutline;
			}
		}
		return null;
	}

	public static JDefinedClass getClassDefinition(final JCodeModel codeModel, final JType typeRef) {
		return codeModel._getClass(typeRef.fullName());
	}

}
